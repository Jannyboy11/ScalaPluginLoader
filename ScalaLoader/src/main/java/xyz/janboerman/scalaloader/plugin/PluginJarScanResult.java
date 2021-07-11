package xyz.janboerman.scalaloader.plugin;

import xyz.janboerman.scalaloader.bytecode.TransformerRegistry;
import xyz.janboerman.scalaloader.plugin.description.DescriptionScanner;

import java.util.Map;

class PluginJarScanResult {

    DescriptionScanner mainClassCandidate;
    Map<String, Object> pluginYaml;
    boolean isJavaPluginExplicitly;
    TransformerRegistry transformerRegistry;

    PluginJarScanResult() {
    }

    public String toString() {
        return "PluginJarScanResult"
                + "{mainClassCandidate=" + mainClassCandidate
                + ",pluginYaml=" + pluginYaml
                + ",isJavaPluginExplicitly=" + isJavaPluginExplicitly
                + ",transformerRegistry=" + transformerRegistry
                + "}";
    }

}
