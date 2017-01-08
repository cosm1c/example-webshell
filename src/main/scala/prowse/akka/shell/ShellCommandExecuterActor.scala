package prowse.akka.shell

import java.io.InputStream
import java.nio.file.Paths
import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}

import akka.actor.{Actor, ActorLogging, Props}
import akka.event.LoggingAdapter
import akka.http.scaladsl.server.directives.RouteDirectives
import akka.http.scaladsl.server.{Directives, Route, ValidationRejection}
import akka.stream.scaladsl.{FileIO, Keep, Sink, StreamConverters}
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.ByteString
import prowse.shell.ShellCommand

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.sys.process._

object ShellCommandExecuterActor {

    def props(stagingDirectory: String): Props =
        Props(new ShellCommandExecuterActor(stagingDirectory))


    case class ExecuteShellCommand(shellCommand: ShellCommand)

    case class ExecuteShellCommandResponse(eventualRoute: Future[Route])


    private val filenameDateFormatter: DateTimeFormatter =
        DateTimeFormatter
            .ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneId.of("UTC"))

    def formatOutputFilename(shellCommand: ShellCommand): String =
        s"${shellCommand.name}-${filenameDateFormatter.format(Instant.now())}.${shellCommand.fileExt}"
}

class ShellCommandExecuterActor(stagingDirectory: String) extends Actor with ActorLogging with RouteDirectives {

    import ShellCommandExecuterActor._

    private implicit val executionContextExecutor = context.dispatcher
    private implicit val materializer = ActorMaterializer()
    private implicit val _log = log

    override def receive: Receive = {
        case ExecuteShellCommand(shellCommand) =>
            sender() ! ExecuteShellCommandResponse(exec(shellCommand))
    }

    private def exec(shellCommand: ShellCommand): Future[Route] = {
        try {
            val outputPath = Paths.get(stagingDirectory, formatOutputFilename(shellCommand))
            log.info(
                """Executing commandName="{}" commandLine="{}" outputPath="{}"""",
                shellCommand.name, shellCommand.commandLine, outputPath)

            val stdoutProcessor = new OutputProcessor(FileIO.toPath(outputPath).named("stdout"))

            // This could be streamed over WebSocket?
            val stderrProcessor = new OutputProcessor(Sink.foreach { data: ByteString =>
                log.error(String.valueOf(data.asByteBuffer.array()))
            }.named("stderr"))

            val exitValue = shellCommand.commandLine.run(new ProcessIO(
                _ => {
                    // Nothing to stdin
                },
                stdoutProcessor.attach,
                stderrProcessor.attach,
                daemonizeThreads = false
            )).exitValue()

            if (exitValue == 0) {
                Future.sequence(Seq(stdoutProcessor.future, stderrProcessor.future))
                    .map(_ => Directives.getFromFile(outputPath.toFile, shellCommand.contentType))
                    .recover {
                        case throwable: Throwable =>
                            log.error(throwable, "{} failed IO", shellCommand.name)
                            reject(ValidationRejection(s"${shellCommand.name} failed IO: ${throwable.getMessage}"))
                    }

            } else {
                val errorMessage = s"${shellCommand.name} process failed with exitValue $exitValue"
                log.error(errorMessage)
                Future(reject(ValidationRejection(errorMessage)))
            }

        } catch {
            case ex: Exception =>
                log.error(ex, s"{} failed to execute", shellCommand.name)
                Future(reject(ValidationRejection(s"{shellCommand.name} failed to execute: ${ex.getMessage}")))
        }
    }
}

class OutputProcessor(sink: Sink[ByteString, Future[_]]) {

    private val promise = Promise[Any]

    def attach(inputStream: InputStream)(implicit log: LoggingAdapter, materizer: Materializer, executionContext: ExecutionContext): Unit = {
        val (inputFuture, outputFuture) = StreamConverters.fromInputStream(() => inputStream)
            .toMat(sink)(Keep.both)
            .run()

        Future.sequence(Seq(inputFuture, outputFuture))
            .onComplete(promise.complete)
    }

    def future: Future[Any] = promise.future
}
