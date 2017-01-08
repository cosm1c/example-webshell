package prowse.akka

import akka.NotUsed
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.scaladsl.{Flow, Sink, Source, SourceQueueWithComplete}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import spray.json.{DefaultJsonProtocol, RootJsonFormat, pimpString}

case class Payload(message: String)

object JsonCodec {

    import DefaultJsonProtocol._

    implicit val jsonFormat: RootJsonFormat[Payload] = DefaultJsonProtocol.jsonFormat1(Payload.apply)

    def marshall(model: Payload): String = jsonFormat.write(model).compactPrint

    def unmarshall(text: String): Payload = jsonFormat.read(text.parseJson)
}

object WebsocketFlow {

    def create(log: LoggingAdapter)(implicit materializer: ActorMaterializer): Flow[Message, Message, NotUsed] = {

        val inbound: Sink[Message, Any] = Sink.foreach {
            case _: BinaryMessage =>
                log.warning("Ignoring binary WebSocket frame")

            case tm: TextMessage =>
                tm.textStream.runForeach(text => {
                    val model = JsonCodec.unmarshall(text)
                    log.info("RCV txt: {}", model.message)
                })
                ()
        }

        val outbound: Source[Message, SourceQueueWithComplete[Message]] = Source.queue[Message](16, OverflowStrategy.fail)

        Flow.fromSinkAndSourceMat(inbound, outbound)((_, outboundMat) => {
            outboundMat.offer(TextMessage(JsonCodec.marshall(Payload("Text from server"))))
            NotUsed
        })
    }

}
