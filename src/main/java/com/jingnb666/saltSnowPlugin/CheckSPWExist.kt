import java.io.BufferedReader
import java.io.InputStreamReader

fun isSaltPlayerRunning(): Int {
    val processName = "Salt Player for Windows"

    return try {
        // 使用 tasklist 命令检查进程
        val process = Runtime.getRuntime().exec("tasklist")
        val reader = BufferedReader(InputStreamReader(process.inputStream))

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            if (line!!.contains(processName)) {
                reader.close()
                return 1  // 找到进程
            }
        }

        reader.close()
        0  // 未找到进程

    } catch (e: Exception) {
        println("检查进程时发生错误: ${e.message}")
        -1  // 发生错误
    }
}

fun main() {
    val result = isSaltPlayerRunning()

    if (result == 1) {
        println("1 - SPW正在运行")
    } else if (result == 0) {
        println("0 - SPW未在运行")
    } else {
        println("检查失败")
    }
}