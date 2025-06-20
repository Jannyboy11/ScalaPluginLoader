package xyz.janboerman.scalaloader.example.scala3

import com.mojang.brigadier.Command
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import xyz.janboerman.scalaloader.paper.plugin.ScalaPluginBootstrap

import java.util.Collections

class ExampleBootstrap extends ScalaPluginBootstrap {

    override def bootstrap(context: BootstrapContext): Unit = {
        super.bootstrap(context)

        // Code adapted from test-plugin in Paper repository.

        val lifecycleEventManager = context.getLifecycleManager
        lifecycleEventManager.registerEventHandler(LifecycleEvents.COMMANDS, event => {
            val commands = event.registrar

            commands.register(
                Commands.literal("bootstraptest")
                    .executes(commandContext => Command.SINGLE_SUCCESS)
                    .build(),
                null,
                Collections.emptyList()
            )
        })
    }

}
