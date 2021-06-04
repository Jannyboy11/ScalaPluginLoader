package xyz.janboerman.scalaloader.example.scala3

import org.bukkit.plugin.Plugin
import zio.{Runtime, ZEnv}
import zio.internal.{Platform, Executor}

import java.util.logging.Level
import scala.concurrent.ExecutionContext

private[scala3] class BukkitRuntime[P <: Plugin](plugin: P) {

    private val syncExecutionContext = new ExecutionContext {
        override def execute(runnable: Runnable): Unit =
            if plugin.getServer.isPrimaryThread then {
                runnable.run()
            } else {
                plugin.getServer.getScheduler.runTask(plugin, runnable)
            }

        override def reportFailure(cause: Throwable): Unit =
            plugin.getLogger.log(Level.SEVERE, "Error caused by syncronous effect", cause)
    }

    private val asyncExecutionContext = new ExecutionContext {
        override def execute(runnable: Runnable): Unit =
            if plugin.getServer.isPrimaryThread then {
                plugin.getServer.getScheduler.runTaskAsynchronously(plugin, runnable)
            } else {
                runnable.run()
            }

        override def reportFailure(cause: Throwable): Unit =
            plugin.getLogger.log(Level.SEVERE, "Error caused by asynchronous effect", cause)
    }

    val syncExecutor = Executor.fromExecutionContext(2048)(syncExecutionContext)
    val asyncExecutor = Executor.fromExecutionContext(2048)(asyncExecutionContext)
    //private val syncPlatform: Platform = Platform.fromExecutionContext(syncExecutionContext)
    //private val asyncPlatform: Platform = Platform.fromExecutionContext(asyncExecutionContext)

    val syncRuntime: Runtime[ZEnv /*TODO with P*/] = Runtime.default.withExecutor(syncExecutor)
    val asyncRuntime: Runtime[ZEnv /*TODO with P*/] = Runtime.default.withExecutor(asyncExecutor)

}

