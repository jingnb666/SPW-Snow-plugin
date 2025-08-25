package com.jingnb666.saltSnowPlugin

import com.xuncorp.spw.workshop.api.UnstableSpwWorkshopApi
import com.xuncorp.spw.workshop.api.WorkshopApi
import com.xuncorp.spw.workshop.api.config.ConfigHelper
import com.xuncorp.spw.workshop.api.config.ConfigManager
import java.nio.file.Files


@UnstableSpwWorkshopApi
object Config {
    private var configData: ConfigData = ConfigData()
    var configManager: ConfigManager = WorkshopApi.manager.createConfigManager("雪花窗口插件")
    var configHelper: ConfigHelper = configManager.getConfig()

    init {
        loadConfig()
        configWatcher()
    }

    private fun loadConfig() {
        if (Files.notExists(configHelper.getConfigPath())) {
            // 创建默认配置文件
            saveConfig()
            return
        }

        configHelper.reload()
        configData.snowIconPath = configHelper.get("snowIconPath", "")
        configData.snowDensity = configHelper.get("snowDensity", 20)
        configData.snowSpeed = configHelper.get("snowSpeed", 60.0)
        configData.snowSize = configHelper.get("snowSize", 1.0)
        println("配置文件加载成功")

        WorkshopApi.ui.toast("${getSnowSize()} ${configData.snowSize}", WorkshopApi.Ui.ToastType.Success)
    }

    fun saveConfig() {
        configHelper.set("snowIconPath", configData.snowIconPath)
        configHelper.set("snowDensity", configData.snowDensity)
        configHelper.set("snowSpeed", configData.snowSpeed)
        configHelper.set("snowSize", configData.snowSize)
        configHelper.save()
    }

    private fun configWatcher() {
        configManager.addConfigChangeListener { loadConfig() }
    }

    fun getSnowIconPath(): String {
        return configData.snowIconPath
    }

    fun getSnowDensity(): Int {
        return configData.snowDensity
    }

    fun getSnowSpeed(): Double {
        return configData.snowSpeed
    }

    fun getSnowSize(): Double {
        return configData.snowSize
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
