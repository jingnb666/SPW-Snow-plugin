// SnowShowText.kt
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.Timer

// -------------------- 工具函数 --------------------
private val rng = Random()
private fun ClosedFloatingPointRange<Double>.rand() = start + rng.nextDouble() * (endInclusive - start)
private fun IntRange.randInt() = rng.nextInt(last - first + 1) + first
private fun degToRad(d: Double) = Math.toRadians(d)

// -------------------- 单朵雪花数据 --------------------
private data class Flake(
    var x: Double, var y: Double,
    var vx: Double, var vy: Double,
    var angleDeg: Double,
    val rotSpeed: Double,
    val lifeSec: Double,
    val alpha: Float,
    val gravity: Double,
    val bornAt: Long,
    val clockwise: Int,
    val scale: Double
)

// -------------------- 主逻辑 --------------------
fun showSnow(): Int {
    val imgFile = File("res${File.separator}snow.png")
    if (!imgFile.exists()) {
        println("0  - 未找到 snow.png")
        return 0
    }
    val img: BufferedImage = ImageIO.read(imgFile)

    val screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
        .defaultScreenDevice.defaultConfiguration.bounds
    val screenW = screen.width
    val screenH = screen.height

    // 创建可重用的雪花列表
    val flakes = Collections.synchronizedList(mutableListOf<Flake>())

    // 生成一批雪花
    fun generateBatch() = synchronized(flakes) {
        val total = (20..30).randInt()
        repeat(total) {
            val life = (20.0..30.0).rand()
            val angleSpeed = 360.0 / ((2.5..3.0).rand() * 50)
            val wind = (-30.0..30.0).rand()
            val grav = (50.0..120.0).rand()
            val dirDeg = (160.0..20.0).rand()
            val dirRad = degToRad(dirDeg)
            val speed = 80 + rng.nextDouble() * 60
            val scale = (0.05..0.1).rand()

            flakes += Flake(
                x = rng.nextDouble() * screenW,
                y = (-30.0..-20.0).rand(),
                vx = Math.cos(dirRad) * speed + wind,
                vy = Math.sin(dirRad) * speed,
                angleDeg = rng.nextDouble() * 360,
                rotSpeed = if (rng.nextBoolean()) angleSpeed else -angleSpeed,
                lifeSec = life,
                alpha = (0.5..0.9).rand().toFloat(),
                gravity = grav,
                bornAt = System.currentTimeMillis(),
                clockwise = if (rng.nextBoolean()) 1 else 0,
                scale = scale
            )
        }
    }

    // 第一次生成
    generateBatch()

    var batchSpawned = false

    SwingUtilities.invokeLater {
        val panel = object : JPanel() {
            init {
                preferredSize = Dimension(screenW, screenH)
                background = Color(0, 0, 0, 0)
                isOpaque = false
            }

            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2 = g as Graphics2D
                val now = System.currentTimeMillis()

                // 绘制并更新物理
                val iter = flakes.iterator()
                var minY = Double.MAX_VALUE
                while (iter.hasNext()) {
                    val f = iter.next()
                    val age = (now - f.bornAt) / 1000.0
                    if (age >= f.lifeSec) {
                        iter.remove()
                        continue
                    }

                    f.vy += f.gravity / 50.0
                    f.x += f.vx / 50.0
                    f.y += f.vy / 50.0
                    f.angleDeg += f.rotSpeed
                    if (f.angleDeg >= 360) f.angleDeg -= 360
                    if (f.angleDeg < 0) f.angleDeg += 360

                    val sw = (img.width * f.scale).toInt()
                    val sh = (img.height * f.scale).toInt()

                    val at = AffineTransform()
                    at.translate(f.x - sw / 2.0, f.y - sh / 2.0)
                    at.rotate(degToRad(f.angleDeg), sw / 2.0, sh / 2.0)
                    at.scale(f.scale, f.scale)

                    val composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, f.alpha)
                    g2.composite = composite
                    g2.drawImage(img, at, null)

                    minY = minOf(minY, f.y)
                }

                // 当整批雪花下边界 > 0 时生成下一批
                if (!batchSpawned && minY > 0) {
                    batchSpawned = true
                    SwingUtilities.invokeLater {
                        generateBatch()
                        batchSpawned = false
                    }
                }
            }
        }

        val timer = Timer(20) { panel.repaint() }
        timer.start()

        JFrame().apply {
            setUndecorated(true)
            isAlwaysOnTop = true
            background = Color(0, 0, 0, 0)
            isResizable = false
            contentPane = panel
            pack()
            setLocation(screen.x, screen.y)
            isVisible = true
            defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        }
    }

    Thread.sleep(Long.MAX_VALUE)   // 永久保持
    return 1
}

fun main() {
    showSnow()
}

