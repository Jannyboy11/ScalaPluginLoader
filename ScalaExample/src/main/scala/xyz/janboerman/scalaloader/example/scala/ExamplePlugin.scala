package xyz.janboerman.scalaloader.example.scala

import org.bukkit.Material
import org.bukkit.command.{Command, CommandSender}
import org.bukkit.event.{EventPriority, Listener}
import org.bukkit.permissions.PermissionDefault
import xyz.janboerman.scalaloader.event.EventBus
import xyz.janboerman.scalaloader.plugin.ScalaPluginDescription.{Command => SPCommand, Permission => SPPermission}
import xyz.janboerman.scalaloader.plugin.{ScalaPlugin, ScalaPluginDescription}
import xyz.janboerman.scalaloader.plugin.description.{Api, ApiVersion, Scala, ScalaVersion}

@Scala(version = ScalaVersion.v2_12_10)
@Api(ApiVersion.v1_15)
object ExamplePlugin
    extends ScalaPlugin(new ScalaPluginDescription("ScalaExample", "0.13.1-SNAPSHOT")
        .addCommand(new SPCommand("foo") permission "scalaexample.foo")
        .addCommand(new SPCommand("home") permission "scalaexample.home" usage "/home set|tp")
        .permissions(new SPPermission("scalaexample.home") permissionDefault PermissionDefault.TRUE)) {

    getLogger.info("ScalaExample - I am constructed!")

    override def onLoad(): Unit = {
        getLogger.info("ScalaExample - I am loaded!")
    }

    override def onEnable(): Unit = {
        getLogger.info("ScalaExample - I am enabled!")
        eventBus.registerEvents(PlayerJoinListener, this)
        eventBus.registerEvents(RandomHomeTeleportBlocker, this)
        eventBus.registerEvent(classOf[HomeTeleportEvent], new Listener() {}, EventPriority.MONITOR, (l: Listener, ev: HomeTeleportEvent) => {
            if (ev.isCancelled) {
                getLogger.info("Player " + ev.player.getName + " tried to teleport home, but couldn't!")
            } else {
                getLogger.info("Player " + ev.player.getName + " teleported home!")
            }
        }, this, false);
        getCommand("home").setExecutor(HomeExecutor)

        listConfigs()
        checkMaterials()
    }

    override def onDisable(): Unit = {
        getLogger.info("ScalaExample - I am disabled!")
    }

    override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
        sender.sendMessage("Executed foo command!")
        true
    }

    def getInt() = 42

    private def listConfigs(): Unit = {
        val urls = getClassLoader.findResources("config.yml")
        while (urls.hasMoreElements) {
            val url = urls.nextElement()
            getLogger.info(s"Found resource: ${url.toString}")
        }
    }

    private def checkMaterials(): Unit = {
        val materials = Material.values()
        var foundModern: Boolean = false
        var foundLegacy: Boolean = false
        val iterator: Iterator[Material] = materials.iterator

        while (iterator.hasNext && (!(foundModern && foundLegacy))) {
            val material = iterator.next();
            //Material's javadoc says I can't use Material#isLegacy for ANY reason - let's ignore that! >:D
            if (material.isLegacy) {
                foundLegacy = true;
            } else {
                foundModern = true;
            }
        }

        getLogger.info(s"Found legacy material?: $foundLegacy, modern material?: $foundModern")
        if (foundModern == foundLegacy)
            getLogger.info("This is a pluginloader bug!")
        else
            getLogger.info("Materials work as intended!")
    }

    def eventBus: EventBus = super.getEventBus()
}

