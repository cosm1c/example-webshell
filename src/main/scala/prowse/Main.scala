package prowse

import java.util.logging.{Level, LogManager}

import _root_.akka.{Main => AkkaMain}
import com.typesafe.scalalogging.LazyLogging
import org.slf4j.bridge.SLF4JBridgeHandler
import prowse.akka.AppSupervisorActor

object Main extends LazyLogging {

    // Redirect all logging calls to SLF4J
    LogManager.getLogManager.reset()
    SLF4JBridgeHandler.install()
    java.util.logging.Logger.getLogger("global").setLevel(Level.FINEST)

    def main(args: Array[String]): Unit = {
        Thread.currentThread().setUncaughtExceptionHandler((_, e: Throwable) => {
            logger.error("UncaughtException on main thread", e)
        })

        AkkaMain.main(Array(classOf[AppSupervisorActor].getName))
    }

}
