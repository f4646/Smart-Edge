package com.imi.smartedge.sidebar.panel

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.content.Intent

class NotchHandleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        private const val TAG = "NotchHandleView"
    }

    private val panelPrefs = PanelPreferences(context)
    private val handler = Handler(Looper.getMainLooper())
    private var tapCount = 0
    private val tapTimeoutMs = ViewConfiguration.getDoubleTapTimeout().toLong()

    private val tapRunnable = Runnable {
        when (tapCount) {
            1 -> performAction(panelPrefs.notchTapAction)
            2 -> performAction(panelPrefs.notchDoubleTapAction)
            else -> if (tapCount >= 3) performAction(panelPrefs.notchTripleTapAction)
        }
        tapCount = 0
    }

    private val longPressRunnable = Runnable {
        if (panelPrefs.notchLongPressAction != PanelPreferences.ACTION_NONE) {
            performAction(panelPrefs.notchLongPressAction)
            vibrateHaptic(40)
        }
    }

    private fun performAction(actionId: Int) {
        if (actionId == PanelPreferences.ACTION_NONE) return
        
        vibrateHaptic()
        val intent = Intent(context, FloatingPanelService::class.java).apply {
            when (actionId) {
                PanelPreferences.ACTION_OPEN_LAUNCHER -> action = FloatingPanelService.ACTION_OPEN
                PanelPreferences.ACTION_SCREENSHOT -> action = FloatingPanelService.ACTION_SCREENSHOT
                PanelPreferences.ACTION_FLASHLIGHT -> action = FloatingPanelService.ACTION_TOGGLE_FLASHLIGHT
                PanelPreferences.ACTION_CAMERA -> action = FloatingPanelService.ACTION_LAUNCH_CAMERA
                PanelPreferences.ACTION_AUTO_ROTATION -> action = FloatingPanelService.ACTION_TOGGLE_ROTATION
                PanelPreferences.ACTION_OPEN_FAVORITE_APP -> action = FloatingPanelService.ACTION_OPEN_FAV_APP
                else -> {
                    // Forward to Accessibility Service for system actions
                    val accAction = when (actionId) {
                        PanelPreferences.ACTION_BACK -> PanelAccessibilityService.ACTION_BACK
                        PanelPreferences.ACTION_HOME -> PanelAccessibilityService.ACTION_HOME
                        PanelPreferences.ACTION_RECENTS -> PanelAccessibilityService.ACTION_RECENTS
                        PanelPreferences.ACTION_NOTIFICATIONS -> PanelAccessibilityService.ACTION_NOTIFICATIONS
                        PanelPreferences.ACTION_QUICK_SETTINGS -> PanelAccessibilityService.ACTION_QUICK_SETTINGS
                        PanelPreferences.ACTION_LOCK_SCREEN -> PanelAccessibilityService.ACTION_LOCK_SCREEN
                        PanelPreferences.ACTION_POWER_MENU -> PanelAccessibilityService.ACTION_SHOW_POWER_MENU
                        PanelPreferences.ACTION_PREVIOUS_APP -> PanelAccessibilityService.ACTION_PREVIOUS_APP
                        else -> null
                    }
                    if (accAction != null) {
                        val accIntent = Intent(context, PanelAccessibilityService::class.java).apply {
                            action = accAction
                        }
                        context.startService(accIntent)
                        return
                    }
                }
            }
        }
        if (intent.action != null) {
            context.startService(intent)
        }
    }

    private fun vibrateHaptic(durationMs: Long = 25) {
        if (!panelPrefs.hapticEnabled) return
        val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(durationMs)
        }
    }

    private var downTime = 0L

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                handler.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout().toLong())
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)
                if (event.action == MotionEvent.ACTION_UP) {
                    val duration = System.currentTimeMillis() - downTime
                    if (duration < ViewConfiguration.getLongPressTimeout()) {
                        tapCount++
                        handler.removeCallbacks(tapRunnable)
                        handler.postDelayed(tapRunnable, tapTimeoutMs)
                    }
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = Math.abs(event.x - width / 2)
                val dy = Math.abs(event.y - height / 2)
                if (dx > width || dy > height) {
                    handler.removeCallbacks(longPressRunnable)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    init {
        // Transparent but clickable
        setBackgroundColor(Color.TRANSPARENT)
    }
}
