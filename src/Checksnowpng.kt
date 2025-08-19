import java.io.File

fun hasSnowPng(): Int {
    // 以脚本运行时的工作目录为基准，拼接出 res/snow.png 的绝对路径
    val pngFile = File("res${File.separator}snow.png")
    return if (pngFile.isFile) 1 else 0
}

fun main() {
    val result = hasSnowPng()
    println(result)
}