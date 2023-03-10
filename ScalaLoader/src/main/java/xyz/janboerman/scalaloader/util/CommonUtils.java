package xyz.janboerman.scalaloader.util;

import xyz.janboerman.scalaloader.compat.IScalaPlugin;
import xyz.janboerman.scalaloader.plugin.ScalaPluginLoaderException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

public class CommonUtils {

    private CommonUtils() {
    }

    //TODO move code for registering commands, configuration and bStats here (for now)


    /**
     * Tries to get the plugin instance from the scala plugin class.
     * This method is able to get the static instances from scala `object`s,
     * as well as it is able to create plugins using their public NoArgsConstructors.
     * @param clazz the plugin class
     * @param <P> the plugin type
     * @return the plugin's instance.
     * @throws ScalaPluginLoaderException when a plugin instance could not be created for the given class
     */
    public static <P extends IScalaPlugin> P createPluginInstance(Class<P> clazz) throws ScalaPluginLoaderException {
        boolean endsWithDollar = clazz.getName().endsWith("$");
        boolean hasStaticFinalModule$;
        Field module$Field;
        boolean hasPrivateConstructor;
        try {
            module$Field = clazz.getDeclaredField("MODULE$");
            int modifiers = module$Field.getModifiers();
            if (module$Field.getType().equals(clazz)
                    && (modifiers & Modifier.STATIC) == Modifier.STATIC
                    && (modifiers & Modifier.FINAL) == Modifier.FINAL) {
                hasStaticFinalModule$ = true;
            } else {
                hasStaticFinalModule$ = false;
            }
        } catch (NoSuchFieldException e) {
            hasStaticFinalModule$ = false;
            module$Field = null;
        }
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        hasPrivateConstructor = constructors.length == 1 && (constructors[0].getModifiers() & Modifier.PRIVATE) == Modifier.PRIVATE;

        //this seems to be how the scala compiler encodes 'object's. not sure if this is actually specified or an implementation detail.
        //in any case, it works good enough for me!
        boolean isObjectSingleton = endsWithDollar && hasStaticFinalModule$ && hasPrivateConstructor;

        if (isObjectSingleton) {
            //we found a scala singleton object.
            //the instance is already present in the MODULE$ field when this class is loaded.

            try {
                Object pluginInstance = module$Field.get(null);
                return clazz.cast(pluginInstance);
            } catch (IllegalAccessException e) {
                throw new ScalaPluginLoaderException("Couldn't access static field MODULE$ in class " + clazz.getName(), e);
            }
        } else {
            //we found are a regular class.
            //it should have a public zero-argument constructor

            try {
                Constructor<?> ctr = clazz.getConstructor();
                Object pluginInstance = ctr.newInstance();

                return clazz.cast(pluginInstance);
            } catch (IllegalAccessException e) {
                throw new ScalaPluginLoaderException("Could not access the NoArgsConstructor of " + clazz.getName() + ", please make it public", e);
            } catch (InvocationTargetException e) {
                throw new ScalaPluginLoaderException("Error instantiating class " + clazz.getName() + ", its constructor threw something at us", e);
            } catch (NoSuchMethodException e) {
                throw new ScalaPluginLoaderException("Could not find NoArgsConstructor in class " + clazz.getName(), e);
            } catch (InstantiationException e) {
                throw new ScalaPluginLoaderException("Could not instantiate class " + clazz.getName(), e);
            }
        }
    }

}
