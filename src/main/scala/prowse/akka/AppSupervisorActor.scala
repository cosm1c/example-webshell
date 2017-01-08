package prowse.akka

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers.RemainingPath
import akka.http.scaladsl.server.ValidationRejection
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import prowse.BuildInfoHelper
import prowse.akka.shell.ShellCommandExecuterActor
import prowse.akka.shell.ShellCommandExecuterActor.{ExecuteShellCommand, ExecuteShellCommandResponse}
import prowse.shell.ShellCommandsMap.shellCommandsMap

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class AppSupervisorActor extends Actor with ActorLogging {

    private implicit val actorSystem = context.system
    private implicit val executionContextExecutor = context.dispatcher
    private implicit val materializer = ActorMaterializer()
    private implicit val timeout = Timeout(10.minutes)
    private implicit val _log = log

    private val config = ConfigFactory.load
    private val shellCommandExecuterActor =
        context.actorOf(ShellCommandExecuterActor.props(config.getString("webshell.stagingDirectory")))

    private val route: Flow[HttpRequest, HttpResponse, Any] =
    // WebSocket endpoint first is good for reverse proxy setup
        path("ws") {
            handleWebSocketMessages(WebsocketFlow.create(log))
        } ~
            get {
                pathPrefix("exec" / Remaining) { commandName =>
                    onComplete(
                        shellCommandsMap
                            .get(commandName)
                            .map { shellCommand =>
                                (shellCommandExecuterActor ? ExecuteShellCommand(shellCommand))
                                    .mapTo[ExecuteShellCommandResponse]
                                    .flatMap(_.eventualRoute)
                            }
                            .getOrElse(Future(reject))
                    ) {
                        case Success(routeResult) => routeResult

                        case Failure(ex) =>
                            log.error(ex, "{} failed", commandName)
                            reject(ValidationRejection(s"$commandName failed: ${ex.getMessage}"))
                    }
                } ~
                    path("listCommands") {
                        complete(
                            shellCommandsMap
                                .map(entry => s"""/exec/${entry._1}\n\t(${entry._2.contentType}) \t${entry._2.commandLine}""")
                                .mkString("\n"))
                    } ~
                    pathEndOrSingleSlash {
                        getFromResource("ui/index.html")
                    } ~
                    path("buildInfo") {
                        // Set ContentType as we have pre-calculated JSON response as String
                        complete(HttpEntity(ContentTypes.`application/json`, BuildInfoHelper.buildInfoJson))
                    } ~
                    path(RemainingPath) { filePath =>
                        getFromResource("ui/" + filePath)
                    }
            }

    private var bindingFuture: Future[ServerBinding] = _

    override def preStart(): Unit = {
        bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)
        bindingFuture.onComplete(serverBinding => log.info("Server online - {}", serverBinding))
    }

    override def postStop(): Unit = {
        bindingFuture
            .flatMap { serverBinding =>
                log.info("Server offline - {}", serverBinding)
                serverBinding.unbind()
            }
        ()
    }

    override def receive: Receive = {
        case _ =>
            // This actor is currently only used for supervision
            ???
    }
}
