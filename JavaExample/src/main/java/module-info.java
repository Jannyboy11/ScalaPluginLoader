module xyz.janboerman.scalaloader.javaexample {
    requires java.logging;
    requires org.bukkit;
    requires scala.library;
    requires xyz.janboerman.scalaloader;
    requires xyz.janboerman.scalaloader.scalaexample;

    exports xyz.janboerman.scalaloader.example.java;
    opens xyz.janboerman.scalaloader.example.java;
}