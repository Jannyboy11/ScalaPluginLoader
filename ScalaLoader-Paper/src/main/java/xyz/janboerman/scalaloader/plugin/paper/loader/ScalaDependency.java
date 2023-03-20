package xyz.janboerman.scalaloader.plugin.paper.loader;

import org.bukkit.configuration.Configuration;
import xyz.janboerman.scalaloader.plugin.description.ScalaVersion;
import static xyz.janboerman.scalaloader.plugin.paper.loader.ScalaDependency.*;

import java.util.Map;

public sealed interface ScalaDependency permits Builtin, Custom, YamlDefined {

    public static record Builtin(ScalaVersion scalaVersion) implements ScalaDependency {

    }

    public static record Custom(String version, Map<String, String> urls) implements ScalaDependency {

    }

    public static record YamlDefined(Configuration configuration) implements ScalaDependency {

    }

}
