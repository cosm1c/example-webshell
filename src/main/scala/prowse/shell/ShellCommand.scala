package prowse.shell

import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.model.ContentTypes.{`application/octet-stream`, `text/plain(UTF-8)`}

sealed trait ShellCommand {

    def name: String

    def commandLine: String

    def fileExt: String

    def contentType: ContentType
}

case class TextShellCommand(name: String, commandLine: String) extends ShellCommand {

    val fileExt: String = "txt"

    val contentType: ContentType = `text/plain(UTF-8)`
}

case class BinaryShellCommand(name: String, commandLine: String) extends ShellCommand {

    val fileExt: String = "bin"

    val contentType: ContentType = `application/octet-stream`
}
