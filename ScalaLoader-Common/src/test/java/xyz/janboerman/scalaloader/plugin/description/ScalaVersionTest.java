package xyz.janboerman.scalaloader.plugin.description;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import xyz.janboerman.scalaloader.plugin.PluginScalaVersion;

public class ScalaVersionTest {

    @Test
    public void test2_12() {
        ScalaVersion scalaVersion = ScalaVersion.v2_12_15;

        assertEquals("2.12.15", scalaVersion.getVersion());
        assertTrue(scalaVersion.isStable());
        assertEquals("https://search.maven.org/remotecontent?filepath=org/scala-lang/scala-library/2.12.15/scala-library-2.12.15.jar", scalaVersion.getUrls().get(PluginScalaVersion.SCALA2_LIBRARY_URL));
    }

    @Test
    public void test2_13() {
        ScalaVersion scalaVersion = ScalaVersion.v2_13_8;

        assertEquals("2.13.8", scalaVersion.getVersion());
        assertTrue(scalaVersion.isStable());
        assertEquals("https://search.maven.org/remotecontent?filepath=org/scala-lang/scala-reflect/2.13.8/scala-reflect-2.13.8.jar", scalaVersion.getUrls().get(PluginScalaVersion.SCALA2_REFLECT_URL));
    }

    @Test
    public void test3_1() {
        ScalaVersion scalaVersion = ScalaVersion.v3_1_2;

        assertEquals("3.1.2", scalaVersion.getVersion());
        assertTrue(scalaVersion.isStable());
        assertEquals("https://search.maven.org/remotecontent?filepath=org/scala-lang/scala3-library_3/3.1.2/scala3-library_3-3.1.2.jar", scalaVersion.getUrls().get(PluginScalaVersion.SCALA3_LIBRARY_URL));
        assertNotNull(scalaVersion.getUrls().get(PluginScalaVersion.SCALA2_LIBRARY_URL));
    }

}
