module xyz.janboerman.scalaloader.javaexample {
    requires java.logging;
    requires org.bukkit;
    requires scala.library;
    requires xyz.janboerman.scalaloader;                //IntelliJ is too dumb to recognize automatic modules from a pom.xml
    requires xyz.janboerman.scalaloader.scalaexample;   //IntelliJ is too dumb to recognize automatic modules from a pom.xml

    exports xyz.janboerman.scalaloader.example.java;
    opens xyz.janboerman.scalaloader.example.java;
}