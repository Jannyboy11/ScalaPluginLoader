package xyz.janboerman.scalaloader.plugin.paper;

import xyz.janboerman.scalaloader.bytecode.TransformerRegistry;
import xyz.janboerman.scalaloader.plugin.ScalaPluginLoaderException;
import xyz.janboerman.scalaloader.plugin.description.ApiVersion;
import xyz.janboerman.scalaloader.plugin.paper.description.MainClassScanner;
import xyz.janboerman.scalaloader.plugin.paper.description.ScalaDependency;
import xyz.janboerman.scalaloader.plugin.paper.description.ScalaDependency.YamlDefined;

import java.util.Map;

class PluginJarScanResult {

    MainClassScanner mainClassScanner;
    Map<String, Object> pluginYaml;
    TransformerRegistry transformerRegistry;

    @Override
    public String toString() {
        return "PluginJarScanResult"
                + "{mainClassScanner=" + mainClassScanner
                + ",pluginYaml=" + pluginYaml
                + ",transformerRegistry=" + transformerRegistry
                + "}";
    }

    ScalaDependency getScalaVersion() {
        if (mainClassScanner.hasScalaAnnotation()) {
            return mainClassScanner.getScalaDependency();
        } else if (pluginYaml.containsKey("scala-version")) {
            return new YamlDefined(pluginYaml.get("scala-version").toString());
        } else {
            return null;
        }
    }

    ApiVersion getApiVersion() {
        if (mainClassScanner.hasApiVersion()) {
            return mainClassScanner.getApiVersion();
        } else if (pluginYaml.containsKey("api-version")) {
            return ApiVersion.byVersion(pluginYaml.get("api-version").toString());
        } else {
            return ApiVersion.latest();
        }
    }

    String getMainClass() throws ScalaPluginLoaderException {
        if (pluginYaml.containsKey("main")) {
            return pluginYaml.get("main").toString();
        } else {
            return mainClassScanner.getMainClass().orElseThrow(() -> new ScalaPluginLoaderException("ScalaPlugin without a main class?! :O"));
        }
    }

}
