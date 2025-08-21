package com.jingnb666.saltSnowPlugin

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.IOException
import java.nio.file.*

object Config {
    private const val CONFIG_FILE = "snow_config.json"
    private val configPath: Path = Path.of(
        System.getenv("APPDATA") + "/Salt Player for Windows/workshop/",
        CONFIG_FILE
    )
    private var configData: ConfigData? = null

    init {
        loadConfig()
        Thread { this.configWatcher() }.start()
    }

    private fun loadConfig() {
        val configPath: Path = configPath
        val gson = Gson()

        if (Files.exists(configPath)) {
            try {
                Files.newBufferedReader(configPath).use { reader ->
                    configData = gson.fromJson(reader, ConfigData::class.java)
                    println("配置文件加载成功")
                }
            } catch (e: IOException) {
                System.err.println("读取配置文件失败: " + e.message)
                configData = ConfigData()
            }
        } else {
            // 创建默认配置文件
            configData = ConfigData()
            saveConfig()
        }
    }

    fun saveConfig() {
        val gson: Gson = GsonBuilder().setPrettyPrinting().create()
        val configPath: Path = configPath
        try {
            Files.newBufferedWriter(configPath).use { writer ->
                gson.toJson(configData, writer)
                println("配置文件保存成功")
            }
        } catch (e: IOException) {
            System.err.println("保存配置文件失败: " + e.message)
        }
    }

    private fun configWatcher() {
        try {
            val configPath: Path = configPath.parent
            val watcher: WatchService = FileSystems.getDefault().newWatchService()
            configPath.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY)

            while (true) {
                val key: WatchKey = watcher.take()
                for (event in key.pollEvents()) {
                    val kind: WatchEvent.Kind<*>? = event.kind()
                    if (kind === StandardWatchEventKinds.OVERFLOW) {
                        continue
                    }

                    val ev: WatchEvent<Path> = event as WatchEvent<Path>
                    val fileName: Path = ev.context()

                    if (fileName.toString() == CONFIG_FILE) {
                        println("配置文件已修改，重新加载配置")
                        loadConfig()
                    }
                }

                key.reset()
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
    }

    fun getSnowIconPath(): String {
        return configData?.snowIconPath ?: "snow.png"
    }

    fun getSnowDensity(): Int {
        return configData?.snowDensity ?: 20
    }

    fun getSnowSpeed(): Double {
        return configData?.snowSpeed ?: 50.0
    }

    fun getSnowSize(): Double {
        return configData?.snowSize ?: 1.0
    }

    class ConfigData {
        var snowIconPath: String = ""
        var snowDensity: Int = 20
        var snowSpeed: Double = 60.0
        var snowSize: Double = 1.0

        override fun toString(): String {
            return "ConfigData(snowIconPath='$snowIconPath', snowDensity=$snowDensity, snowSpeed=$snowSpeed, snowSize=$snowSize)"
        }
    }
}
