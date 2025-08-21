// SnowShowText.kt (Overlay Window Version - Refined)
package com.jingnb666.saltSnowPlugin

import com.xuncorp.spw.workshop.api.WorkshopApi
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.net.URI
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.Timer
import kotlin.math.cos
import kotlin.math.sin

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
    val scale: Double
)

// -------------------- 主逻辑 --------------------
fun showSnow(targetFrame: JFrame): JWindow? {
    val snowIconPath = Config.getSnowIconPath()

    val img: BufferedImage = try {
        ImageIO.read(URI("file:///$snowIconPath").toURL())
    } catch (e: Exception) {
        WorkshopApi.instance.ui.toast("Could not read Snow Icon, $snowIconPath", WorkshopApi.Ui.ToastType.Error)
        e.printStackTrace()
        ImageIO.read(Main::class.java.classLoader.getResource("snow.png"))
    }

    // 创建可重用的雪花列表
    val flakes = Collections.synchronizedList(mutableListOf<Flake>())

    val overlayWindow = JWindow(targetFrame)
    overlayWindow.background = Color(0, 0, 0, 0)

    // 生成一批雪花
    fun generateBatch(width: Int) = synchronized(flakes) {
        val snowDensity = Config.getSnowDensity()
        val snowSpeed = Config.getSnowSpeed()
        val snowSize = Config.getSnowSize()

        val total = (0..5).randInt() + snowDensity
        repeat(total) {
            val life = (20.0..30.0).rand()
            val angleSpeed = 360.0 * 0.02 / (2.5..3.0).rand()
            val wind = (-30.0..30.0).rand()
            val grav = (50.0..120.0).rand()
            val dirDeg = (160.0..20.0).rand()
            val dirRad = degToRad(dirDeg)
            val speed = snowSpeed + rng.nextDouble() * 60
            val scale = (0.05..0.1).rand() * snowSize

            flakes += Flake(
                x = rng.nextDouble() * width,
                y = (-30.0..-20.0).rand(),
                vx = cos(dirRad) * speed + wind,
                vy = sin(dirRad) * speed,
                angleDeg = rng.nextDouble() * 360,
                rotSpeed = if (rng.nextBoolean()) angleSpeed else -angleSpeed,
                lifeSec = life,
                alpha = (0.5..0.9).rand().toFloat(),
                gravity = grav,
                bornAt = System.currentTimeMillis(),
                scale = scale
            )
        }
    }

    var batchSpawned = false

    val snowPanel = object : JComponent() {
        private val at = AffineTransform()

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val now = System.currentTimeMillis()

            var minY = Double.MAX_VALUE

            synchronized(flakes) {
                val iter = flakes.iterator()
                while (iter.hasNext()) {
                    val f = iter.next()
                    val age = (now - f.bornAt) / 1000.0
                    if (age >= f.lifeSec) {
                        iter.remove()
                        continue
                    }

                    val deltaTime = 0.02
                    f.vy += f.gravity * deltaTime
                    f.x += f.vx * deltaTime
                    f.y += f.vy * deltaTime
                    f.angleDeg += f.rotSpeed

                    val sw = img.width * f.scale
                    val sh = img.height * f.scale

                    at.setToIdentity()
                    at.translate(f.x - sw / 2.0, f.y - sh / 2.0)
                    at.rotate(degToRad(f.angleDeg), sw / 2.0, sh / 2.0)
                    at.scale(f.scale, f.scale)

                    g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, f.alpha)
                    g2.drawImage(img, at, null)

                    minY = minOf(minY, f.y)
                }
            }

            // 当整批雪花下边界 > 0 时生成下一批
            if (!batchSpawned && flakes.isEmpty() || (flakes.isNotEmpty() && minY > 0)) {
                batchSpawned = true
                SwingUtilities.invokeLater {
                    generateBatch(this.width)
                    batchSpawned = false
                }
            }
        }
    }

    val timer = Timer(20) { snowPanel.repaint() }

    SwingUtilities.invokeLater {
        overlayWindow.contentPane.add(snowPanel)

        fun syncOverlay() {
            if (targetFrame.isShowing) {
                overlayWindow.location = targetFrame.locationOnScreen
                overlayWindow.size = targetFrame.size

                if (targetFrame.isActive && !overlayWindow.isVisible) {
                    overlayWindow.isVisible = true
                    if (!timer.isRunning) {
                        generateBatch(targetFrame.width)
                        timer.start()
                    }
                }
            } else {
                if (overlayWindow.isVisible) {
                    overlayWindow.isVisible = false
                    timer.stop()
                }
            }
        }

        val componentListener = object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) = syncOverlay()
            override fun componentMoved(e: ComponentEvent?) = syncOverlay()
            override fun componentShown(e: ComponentEvent?) = syncOverlay()
            override fun componentHidden(e: ComponentEvent?) = syncOverlay()
        }
        targetFrame.addComponentListener(componentListener)

        val windowListener = object : WindowAdapter() {
            override fun windowActivated(e: WindowEvent?) = syncOverlay()

            override fun windowIconified(e: WindowEvent?) {
                if (overlayWindow.isVisible) {
                    overlayWindow.isVisible = false
                    timer.stop()
                }
            }

            /*
             * 这段代码会在窗口失去焦点时隐藏雪花窗口
             * 我用着怪怪的
             * 不清楚要不要保留
             */
//            override fun windowDeactivated(e: WindowEvent?) {
//                if (overlayWindow.isVisible) {
//                    overlayWindow.isVisible = false
//                    timer.stop()
//                }
//            }

            override fun windowDeiconified(e: WindowEvent?) {
                syncOverlay()
            }
        }
        targetFrame.addWindowListener(windowListener)

        // 插件启动时，如果目标窗口已存在，立即同步并显示
        syncOverlay()
    }

    return overlayWindow
}