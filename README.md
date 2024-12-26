#### Motivation

So you want to write plugins in Scala? Great! Or not so great? Scala runtime classes are not on the class/module path
by default. Server administrators *could* add them using the -classpath commandline option, but in reality most bukkit
servers run in managed environments by minecraft-specialized server hosts. The standard workaround for this problem is to
include the classes in your own plugin and relocate them using a shading plugin in your build process.
While this *does* work, it is not ideal because your plugin will increase in size by a lot. As of Scala 2.12.6, the
standard library has a size of 3.5 MB. The reflection library is another 5 MB. Using both libraries in multiple plugins
results unnecessarily large plugins sizes, while really those Scala classes should only be loaded once.
Spigot 1.16.5 introduced a new library loading api, but it has similar problems.
Introducing...

# ScalaLoader

ScalaLoader uses a custom PluginLoader that loads the Scala runtime classes for you!
Javadoc available [here](https://jannyboy11.github.io/ScalaPluginLoader/javadoc/xyz/janboerman/scalaloader/plugin/ScalaPlugin.html)!

[![ScalaLoader](https://api.mcbanners.com/banner/saved/UmfAZlBOygXvQb.png)](https://www.spigotmc.org/resources/scalaloader.59568/)


#### Pros
- Write idiomatic Scala!
- No need to shade anymore!
- Supports different (binary incompatible) Scala versions at the same time! ScalaLoader uses classloader magic to make that work.
- Supports custom scala versions by adding/changing URLs in the config file.
- Annotation-based detection of the plugin's main class - no need to write a plugin.yml.
If you wish to use a plugin.yml still, you can, however I always found it a pain.
- A boilerplate-free event api! #getHandlerList(), #getHandlers(), #setCancelled(boolean) and #isCancelled() can be generated
at class-load time!
- A boilerplate-free ConfigurationSerialization api! #serialize() and #deserialize(Map<String,Object>) can be generated
at class-load time! (Scala collection support is a WIP)

#### Cons
- Scala library classes are only accessible to ScalaPlugins (You can still write them in Java though).
- ScalaLoaders uses a lot of reflection/injection hacks to make ScalaPlugins accessible to JavaPlugins.

#### Caveats
- ScalaPlugin jars go in the <server_root>/plugins/ScalaLoader/scalaplugins/ directory. I made this choice so that ScalaLoader
doesn't try to load JavaPlugins that are loaded already.
- By default, ScalaLoader downloads the scala libraries from over the network the first time. I made this choice to provide
the best possible user experience for server admins. The ScalaLoader jar remains small in size, and there's no manual downloading
involved. If you're very security-focused you might want to provide your own
jars by changing the URLs to "file://some/location.jar". The scala classes aren't actually loaded until there's a plugin
that needs them, so you can run ScalaLoader once without ScalaPlugins to generate the config.
- Some helper libraries based on Bukkit will assume that every calling class is in fact loaded from a JavaPlugin
(they use `JavaPlugin#getProvidingPlugin(Class<?>)` internally), which will error when the class is loaded by the ScalaPluginClassLoader.
If possible, always use methods that let you pass the Plugin instance explicitly!

### Roadmap
There's only ~~seven~~ two features that are missing in my opinion:
- ~~The first con. I want JavaPlugins te be able to access the Scala library classes, however they will need to tell
ScalaLoader somehow which version they want to use.~~ Now implemented in IScalaPluginLoader#openUpToJavaPlugin(ScalaPlugin,JavaPlugin).
Currently this does not inject the Scala library classes into the JavaPlugin's classloader, but it's a start.
- ~~Make the ScalaPluginLoader parallel capable. Right now ScalaPlugins are loaded sequentially.~~
- ~~Use bukkit's api-version to transform classes so that plugins will be compatible once they are loaded.~~
- ~~HandlerList- and cancellable-related boilerplate reduction for custom events.~~
- API to load third-party libraries (can be specific to certain Scala versions, or not).
- ~~ConfigurationSerializable-related boilerplate reduction.~~
- Link using TASTy if the scalaplugin's jar includes TASTy attributes.

### Example Plugin

```scala
package xyz.janboerman.scalaloader.example.scala

import org.bukkit.ChatColor
import org.bukkit.command.{CommandSender, Command}
import org.bukkit.event.{EventHandler, Listener}
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.permissions.PermissionDefault
import xyz.janboerman.scalaloader.plugin.ScalaPluginDescription.{Command => SPCommand, Permission => SPPermission}
import xyz.janboerman.scalaloader.plugin.{ScalaPlugin, ScalaPluginDescription}
import xyz.janboerman.scalaloader.plugin.description.{Scala, ScalaVersion, Api, ApiVersion}

@Scala(version = ScalaVersion.v2_13_10)
@Api(ApiVersion.v1_19)
object ExamplePlugin
    extends ScalaPlugin(new ScalaPluginDescription("ScalaExample", "1.0")
        .commands(new SPCommand("foo")
            .permission("scalaexample.foo"))
        .permissions(new SPPermission("scalaexample.foo")
            .permissionDefault(PermissionDefault.TRUE)))
    with Listener {

    getLogger().info("ScalaExample - I am constructed!")

    override def onLoad(): Unit = {
        getLogger.info("ScalaExample - I am loaded!")
    }

    override def onEnable(): Unit = {
        getLogger.info("ScalaExample - I am enabled!")
        getServer.getPluginManager.registerEvents(this, this)
    }

    override def onDisable(): Unit = {
        getLogger.info("ScalaExample - I am disabled!")
    }

    @EventHandler
    def onJoin(event: PlayerJoinEvent): Unit = {
        event.setJoinMessage(s"${ChatColor.GREEN} Howdy ${event.getPlayer.getName}!")
    }

    override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
        sender.sendMessage("Executed foo command!")
        true
    }

    def getInt() = 42

}
```

### Depending on a ScalaPlugin from a JavaPlugin

plugin.yml:
```yaml
name: DummyPlugin
version: 1.0
main: xyz.janboerman.dummy.dummyplugin.DummyPlugin
depend: [ScalaLoader]
softdepend: [ScalaExample] #A hard dependency will not work! Your plugin will not load!
```

Java code:
```java
package xyz.janboerman.dummy.dummyplugin;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.janboerman.scalaloader.compat.IScalaLoader;
import xyz.janboerman.scalaloader.plugin.ScalaPluginLoader;
import xyz.janboerman.scalaloader.example.scala.ExamplePlugin$;

public final class DummyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        //get the plugin instance
        ExamplePlugin$ scalaPlugin = (ExamplePlugin$) getServer().getPluginManager().getPlugin("ScalaExample");
        
        //make sure all classes from the scala plugin can be accessed
        IScalaPluginLoader.openUpToJavaPlugin(scalaPlugin, this);
        
        //do whatever you want afterwards!
        getLogger().info("We got " + scalaPlugin.getInt() + " from Scala!");
    }

}
```

## Compiling
It's a [maven](https://maven.apache.org/) project, so just run `mvn package` and you're good to go.
The jar file will be built at `./ScalaLoader/target/ScalaLoader-<version>.jar`
Note that while ScalaLoader can run on Java 8, it requires JDK-21 to compile.

### Pre-built plugin jar file?
Available on [SpigotMC](https://www.spigotmc.org/resources/scalaloader.59568/).

### Javadocs?
Available on [GitHub Pages](https://jannyboy11.github.io/ScalaPluginLoader/javadoc).

## Dependency Information [![Build Status](https://ci.codemc.io/buildStatus/icon?job=Jannyboy11%2FScalaPluginLoader)](https://ci.codemc.io/job/Jannyboy11/job/ScalaPluginLoader/)
##### SBT
```scala
resolvers += "jannyboy11-minecraft-repo" at "https://repo.repsy.io/mvn/jannyboy11/minecraft"
libraryDependencies += "com.janboerman.scalaloader" % "ScalaLoader" % "0.18.15-SNAPSHOT" % "provided"
```

##### Maven
```xml
<repository>
    <id>jannyboy11-minecraft</id>
    <name>Jannyboy11 Minecraft Repo</name>
    <url>https://repo.repsy.io/mvn/jannyboy11/minecraft</url>
</repository>

<dependency>
    <groupId>com.janboerman.scalaloader</groupId>
    <artifactId>ScalaLoader</artifactId>
    <version>0.18.15-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

## License
LGPL, because I want forks of this thing to be open for auditing.
If you however which to *include* parts this code base in your own open source project but not adopt the (L)GPL license,
please contact me and I will likely permit you to use this under a different license.
Sending me a private message on the SpigotMC forums or an issue on this repository will do.
