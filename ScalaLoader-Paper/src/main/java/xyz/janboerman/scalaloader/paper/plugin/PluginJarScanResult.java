package xyz.janboerman.scalaloader.paper.plugin;

import xyz.janboerman.scalaloader.bytecode.TransformerRegistry;
import xyz.janboerman.scalaloader.paper.plugin.description.MainClassScanner;
import xyz.janboerman.scalaloader.plugin.ScalaPluginLoaderException;
import xyz.janboerman.scalaloader.plugin.description.ApiVersion;
import xyz.janboerman.scalaloader.paper.plugin.description.ScalaDependency;
import xyz.janboerman.scalaloader.paper.plugin.description.ScalaDependency.YamlDefined;

import java.util.Map;
import java.util.Optional;

public class PluginJarScanResult {

    public MainClassScanner mainClassScanner;
    public Map<String, Object> pluginYaml;
    public TransformerRegistry transformerRegistry;

    @Override
    public String toString() {
        return "PluginJarScanResult"
                + "{mainClassScanner=" + mainClassScanner
                + ",pluginYaml=" + pluginYaml
                + ",transformerRegistry=" + transformerRegistry
                + "}";
    }

    public ScalaDependency getScalaVersion() {
        if (mainClassScanner.hasScalaAnnotation()) {
            return mainClassScanner.getScalaDependency();
        } else if (pluginYaml.containsKey("scala-version")) {
            return new YamlDefined(pluginYaml.get("scala-version").toString());
        } else {
            return null;
        }
    }

    public ApiVersion getApiVersion() {
        if (mainClassScanner.hasApiVersion()) {
            return mainClassScanner.getApiVersion();
        } else if (pluginYaml.containsKey("api-version")) {
            return ApiVersion.byVersion(pluginYaml.get("api-version").toString());
        } else {
            return ApiVersion.latest();
        }
    }

    public String getMainClass() throws ScalaPluginLoaderException {
        // Prioritise scanned main class, even if 'main' attribute is set in pluginYaml.
        return mainClassScanner.getMainClass()
                .or(() -> Optional.ofNullable((String) pluginYaml.get("main")))
                .orElseThrow(() -> new ScalaPluginLoaderException("ScalaPlugin without a main class?! :O"));
    }

}
