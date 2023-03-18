package xyz.janboerman.scalaloader.example.scala

import java.io.{File, FilenameFilter}
import java.util.UUID
import java.util.logging.{Level, LogRecord}

import org.bukkit.configuration.file.{FileConfiguration, YamlConfiguration}

import scala.collection.mutable

object HomeManager {

    private val homesDirectory = new File(ExamplePlugin.getDataFolder, "homes");
    if (!homesDirectory.exists()) {
        homesDirectory.mkdirs();
    }
    private val homes = new mutable.HashMap[UUID, Home]()

    def addHome(playerId: UUID, home: Home): Unit = {
        homes.put(playerId, home)
    }

    def getHome(playerId: UUID): Option[Home] = homes.get(playerId)


    def loadHomes(): Unit = {
        for (file <- homesDirectory.listFiles((dir: File, name: String) => name.endsWith(".yml"))) {
            val fileName = file.getName
            val id = fileName.substring(0, fileName.length - 4)
            try {
                val playerId = UUID.fromString(id)
                val yamlConfig: FileConfiguration = YamlConfiguration.loadConfiguration(file)
                val home: Home = yamlConfig.get("home").asInstanceOf[Home]
                addHome(playerId, home)
            } catch {
                case e: IllegalArgumentException =>
                    ExamplePlugin.getLogger.log(Level.SEVERE, "cannot load file: " + fileName, e)
            }
        }
    }

    def saveHome(playerId: UUID, home: Home): Unit = {
        val file = new File(homesDirectory, playerId.toString + ".yml")
        //do I need to check whether the file exists and create a blank one?

        val config: FileConfiguration = new YamlConfiguration()
        config.set("home", home)
        config.save(file);
    }

}
