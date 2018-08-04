package xyz.janboerman.scalaloader;

import org.bukkit.plugin.java.JavaPlugin;
import scala.Option;
import scala.Some;
import xyz.janboerman.scalaloader.version.Scala;
import xyz.janboerman.scalaloader.version.ScalaVersion;

@Scala(version = ScalaVersion.v2_12_6)
public class ExamplePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        Option<String> some = new Some<>("Hello, World!");
        Option<String> none = Option.apply(null);

        getServer().broadcastMessage("Some = " + some);
        getServer().broadcastMessage("None = " + none);
    }

}
