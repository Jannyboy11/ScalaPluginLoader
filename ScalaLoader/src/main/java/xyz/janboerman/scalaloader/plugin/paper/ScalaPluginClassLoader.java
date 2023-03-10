package xyz.janboerman.scalaloader.plugin.paper;

import java.io.File;

//import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader;

public class ScalaPluginClassLoader extends ClassLoader /*PaperPluginClassLoader*/ {

    static {
        registerAsParallelCapable();
    }

    public File getPluginJarFile() {
        //TODO
        return null;
    }

    //TODO loadClass

    //TODO findClass

    //TODO take bytecode transformations into account

}
