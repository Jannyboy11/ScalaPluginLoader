package xyz.janboerman.scalaloader.paper.plugin.description;

import xyz.janboerman.scalaloader.compat.IScalaVersion;
import xyz.janboerman.scalaloader.plugin.description.ScalaVersion;

import static xyz.janboerman.scalaloader.compat.Compat.emptyMap;
import static xyz.janboerman.scalaloader.paper.plugin.description.ScalaDependency.*;

import java.util.Map;

public sealed interface ScalaDependency extends IScalaVersion permits Builtin, Custom, YamlDefined {

    public String getVersionString();

    /** Scala version defined using annotation @Scala(version = ScalaVersion.X_Y_Z)*/
    public static record Builtin(ScalaVersion scalaVersion) implements ScalaDependency {
        @Override
        public String getVersionString() {
            return scalaVersion().getVersion();
        }
    }

    /** Scala version defined using annotation @CustomScala */
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

    /** Scala version defined in the plugin.yml under the "scala-version" key. */
    public static record YamlDefined(String scalaVersion) implements ScalaDependency {
        @Override
        public String getVersionString() {
            return scalaVersion();
        }
    }

}
