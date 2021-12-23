package xyz.janboerman.scalaloader.example.scala

import org.bukkit.{ChatColor, Material}
import org.bukkit.command.{Command, CommandSender}
import org.bukkit.event.{EventPriority, Listener}
import org.bukkit.permissions.PermissionDefault
import xyz.janboerman.scalaloader.plugin.ScalaPluginDescription.{Command => SPCommand, Permission => SPPermission}
import xyz.janboerman.scalaloader.plugin.{ScalaPlugin, ScalaPluginDescription}
import xyz.janboerman.scalaloader.plugin.description.{Api, ApiVersion, Scala, ScalaVersion}

@Scala(version = ScalaVersion.v2_13_7)
@Api(ApiVersion.v1_16)
object ExamplePlugin
    extends ScalaPlugin(new ScalaPluginDescription("ScalaExample", "0.17.8-SNAPSHOT")
        .addCommand(new SPCommand("foo") permission "scalaexample.foo")
        .addCommand(new SPCommand("home") permission "scalaexample.home" usage "/home set|tp")
        .permissions(new SPPermission("scalaexample.home") permissionDefault PermissionDefault.TRUE)) {

    getLogger.info("ScalaExample - I am constructed!")

    override def onLoad(): Unit = {
        getLogger.info("ScalaExample - I am loaded!")
    }

    override def onEnable(): Unit = {
        getLogger.info("ScalaExample - I am enabled!")

        //works because Home is magically registered at ConfigurationSerialization at the start of the onEnable!
        HomeManager.loadHomes()

        getEventBus.registerEvents(PlayerJoinListener, this)
        getEventBus.registerEvents(RandomHomeTeleportBlocker, this)
        getEventBus.registerEvent(classOf[HomeTeleportEvent], new Listener() {}, EventPriority.MONITOR, (l: Listener, ev: HomeTeleportEvent) => {
            if (ev.isCancelled) {
                getLogger.info(s"Player ${ev.player.getName} tried to teleport home, but couldn't!")
            } else {
                getLogger.info(s"Player ${ev.player.getName} teleported home!")
            }
        }, this, false);

        getCommand("home").setExecutor(HomeExecutor)

        listConfigs()
        checkMaterials()
        SerializationMethodsTest.test()
        ScalaTypesSerializationTest.test()
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
        val console = getServer.getConsoleSender
        console.sendMessage(s"${ChatColor.YELLOW}Test that a ScalaPlugin does not find both legacy and modern materials")

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

        assert(foundModern != foundLegacy, "Found both modern and legacy materials, this is a pluginloader bug!")
        if (assertionsEnabled()) {
            console.sendMessage(s"${ChatColor.GREEN}Test passed!")
        } else if (foundModern != foundLegacy) {
            console.sendMessage(s"${ChatColor.GREEN}Materials work as intended!")
        } else {
            console.sendMessage(s"${ChatColor.RED}Test failed, both legacy and modern materials were found!")
        }
    }

    private[scala] def assertionsEnabled(): Boolean = {
        try {
            assert(false)
            false
        } catch {
            case ae: AssertionError =>
                true
        }
    }

}

