package xyz.janboerman.scalaloader.example.scala3

import org.bukkit.plugin.Plugin
import zio.{Runtime, Executor, ZLayer}
//import zio.internal.Platform
//import zio.ZEnv
//private val syncPlatform: Platform = Platform.fromExecutionContext(syncExecutionContext)
//private val asyncPlatform: Platform = Platform.fromExecutionContext(asyncExecutionContext)

import java.util.logging.Level
import scala.concurrent.ExecutionContext

private[scala3] class BukkitRuntime[P <: Plugin](plugin: P) {

    private val syncExecutionContext = new ExecutionContext {
        override def execute(runnable: Runnable): Unit =
            if plugin.getServer.isPrimaryThread then
                runnable.run()
            else
                plugin.getServer.getScheduler.runTask(plugin, runnable)

        override def reportFailure(cause: Throwable): Unit =
            plugin.getLogger.log(Level.SEVERE, "Error caused by synchronous effect", cause)
    }

    private val asyncExecutionContext = new ExecutionContext {
        override def execute(runnable: Runnable): Unit =
            if plugin.getServer.isPrimaryThread then
                plugin.getServer.getScheduler.runTaskAsynchronously(plugin, runnable)
            else
                runnable.run()

        override def reportFailure(cause: Throwable): Unit =
            plugin.getLogger.log(Level.SEVERE, "Error caused by asynchronous effect", cause)
    }

    val syncExecutor: zio.Executor = Executor.fromExecutionContext(syncExecutionContext)
    val asyncExecutor: zio.Executor = Executor.fromExecutionContext(asyncExecutionContext)

    private val syncLayer: ZLayer[Any, Nothing, Unit] = Runtime.setExecutor(syncExecutor)
    private val asyncLayer: ZLayer[Any, Nothing, Unit] = Runtime.setExecutor(asyncExecutor)

    //given zio.Unsafe = new zio.Unsafe {} //can't subclass a sealed trait.

    val syncRuntime = Runtime.unsafe.fromLayer(layer = syncLayer)   //no given instance Unsafe not found
    val asyncRuntime = Runtime.unsafe.fromLayer(layer = asyncLayer) //idem

}

