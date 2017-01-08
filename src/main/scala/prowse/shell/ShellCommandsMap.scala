package prowse.shell

import java.util.Map.Entry

import com.typesafe.config.{ConfigFactory, ConfigObject, ConfigValue}

import scala.collection.JavaConverters._

object ShellCommandsMap {

    val shellCommandsMap: Map[String, ShellCommand] = {
        for {
            item: ConfigObject <- ConfigFactory.load.getObjectList("webshell.commands").asScala
            entry: Entry[String, ConfigValue] <- item.entrySet().asScala
            commandConfig = entry.getValue.atKey("cmd")
            isBinary = commandConfig.getBoolean("cmd.isBinary")
            name = entry.getKey
            exec = if (isBinary)
                BinaryShellCommand(name, commandConfig.getString("cmd.commandLine"))
            else
                TextShellCommand(name, commandConfig.getString("cmd.commandLine"))
        } yield (name, exec)
    }.toMap

}
