// Main.kt (Improved Version)
package com.jingnb666.saltSnowPlugin

import org.pf4j.Plugin
import java.awt.AWTEvent
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.AWTEventListener
import java.awt.event.WindowEvent
import java.util.*
import javax.swing.JFrame
import javax.swing.JWindow
import javax.swing.SwingUtilities

class Main : Plugin() {
    private val snowWindows = Collections.synchronizedMap(WeakHashMap<JFrame, JWindow>())
    private var eventListener: AWTEventListener? = null

    override fun start() {
        println("Salt Snow Plugin started, adding AWT event listener.")

        eventListener = AWTEventListener { event ->
            if (event.id == WindowEvent.WINDOW_OPENED) {
                val window = event.source as? Window
                SwingUtilities.invokeLater {
                    if (!(window is JFrame && window.title == "Salt Player for Windows" && window.isShowing)) {
                        return@invokeLater
                    }

                    if (!snowWindows.containsKey(window)) {
                        println("Found target window: ${window.title}, attempting to add snow overlay.")
                        val snowOverlay = showSnow(window)
                        if (snowOverlay != null) {
                            snowWindows[window] = snowOverlay
                            println("Successfully created snow overlay for ${window.title}")
                        } else {
                            println("Failed to create snow overlay.")
                        }
                    }
                }
            }
        }

        Toolkit.getDefaultToolkit().addAWTEventListener(eventListener, AWTEvent.WINDOW_EVENT_MASK)
    }

    override fun stop() {
        if (eventListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(eventListener)
            eventListener = null
        }

        // 确保在主线程上销毁所有窗口
        SwingUtilities.invokeLater {
            synchronized(snowWindows) {
                snowWindows.values.forEach { it.dispose() }
                snowWindows.clear()
            }
        }
        println("Salt Snow Plugin stopped.")
    }
}