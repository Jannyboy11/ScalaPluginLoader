package xyz.janboerman.scalaloader.plugin.paper;

import java.io.File;

public class ScalaPluginClassLoader extends ClassLoader {

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
