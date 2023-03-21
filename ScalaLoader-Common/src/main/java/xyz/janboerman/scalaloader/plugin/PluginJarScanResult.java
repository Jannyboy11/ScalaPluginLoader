package xyz.janboerman.scalaloader.plugin;

import xyz.janboerman.scalaloader.bytecode.TransformerRegistry;
import xyz.janboerman.scalaloader.plugin.description.DescriptionScanner;

import java.util.Map;

public class PluginJarScanResult {

    public DescriptionScanner mainClassCandidate;
    public Map<String, Object> pluginYaml;
    public boolean isJavaPluginExplicitly;
    public TransformerRegistry transformerRegistry;

    public PluginJarScanResult() {
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
