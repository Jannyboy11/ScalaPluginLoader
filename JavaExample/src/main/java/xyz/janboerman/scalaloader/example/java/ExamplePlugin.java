package xyz.janboerman.scalaloader.example.java;

import scala.Option;
import scala.Some;
import xyz.janboerman.scalaloader.plugin.ScalaPlugin;
import xyz.janboerman.scalaloader.plugin.description.CustomScala;
import xyz.janboerman.scalaloader.plugin.ScalaPluginDescription;
import xyz.janboerman.scalaloader.plugin.description.Version;

@CustomScala(@Version(value = "2.12.6",
        scalaLibraryUrl = "https://bintray.com/bintray/jcenter/download_file?file_path=org%2Fscala-lang%2Fscala-reflect%2F2.12.6%2Fscala-reflect-2.12.6.jar",
        scalaReflectUrl = "https://bintray.com/bintray/jcenter/download_file?file_path=org%2Fscala-lang%2Fscala-library%2F2.12.6%2Fscala-library-2.12.6.jar"))
public class ExamplePlugin extends ScalaPlugin {

    protected ExamplePlugin() {
        super(new ScalaPluginDescription("JavaExample", "0.1-SNAPSHOT"));
    }

    @Override
    public void onEnable() {
        Option<String> some = new Some<>("Hello, World!");
        Option<String> none = Option.apply(null);

        getServer().broadcastMessage("Some = " + some);
        getServer().broadcastMessage("None = " + none);
    }

}
