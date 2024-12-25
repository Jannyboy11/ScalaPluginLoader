package xyz.janboerman.scalaloader.paper.plugin.description;

import xyz.janboerman.scalaloader.compat.IScalaVersion;
import xyz.janboerman.scalaloader.plugin.description.ScalaVersion;

import static xyz.janboerman.scalaloader.compat.Compat.emptyMap;
import static xyz.janboerman.scalaloader.paper.plugin.description.ScalaDependency.*;

import java.util.Map;

public sealed interface ScalaDependency extends IScalaVersion permits Builtin, Custom, YamlDefined {

    public String getVersionString();

    public static record Builtin(ScalaVersion scalaVersion) implements ScalaDependency {
        @Override
        public String getVersionString() {
            return scalaVersion().getVersion();
        }
    }

    public static record Custom(String scalaVersion, Map<String, String> urls, Map<String, String> sha1hashes) implements ScalaDependency {

        /** @deprecated Use canonical constructor instead. */
        @Deprecated
        Custom(String scalaVersion, Map<String, String> urls) {
            this(scalaVersion, urls, emptyMap());
        }

        @Override
        public String getVersionString() {
            return scalaVersion();
        }
    }

    public static record YamlDefined(String scalaVersion) implements ScalaDependency {
        @Override
        public String getVersionString() {
            return scalaVersion();
        }
    }

}
