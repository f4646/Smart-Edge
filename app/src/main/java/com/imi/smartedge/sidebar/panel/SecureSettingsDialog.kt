package com.imi.smartedge.sidebar.panel

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SecureSettingsDialog : BottomSheetDialogFragment() {

    private var onPermissionGranted: (() -> Unit)? = null

    companion object {
        const val TAG = "SecureSettingsDialog"
        
        fun show(context: Context, onPermissionGranted: () -> Unit) {
            val dialog = SecureSettingsDialog()
            dialog.onPermissionGranted = onPermissionGranted
            (context as? androidx.fragment.app.FragmentActivity)?.let {
                dialog.show(it.supportFragmentManager, TAG)
            }
        }
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val density = ctx.resources.displayMetrics.density
        val packageName = ctx.packageName
        val adbCommand = "adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (24 * density).toInt(),
                (12 * density).toInt(),
                (24 * density).toInt(),
                (28 * density).toInt()
            )
        }

        // Drag handle
        val handle = View(ctx).apply {
            val lp = LinearLayout.LayoutParams((40 * density).toInt(), (4 * density).toInt())
            lp.gravity = Gravity.CENTER_HORIZONTAL
            lp.bottomMargin = (20 * density).toInt()
            layoutParams = lp
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8 * density
                setColor(Color.parseColor("#33FFFFFF"))
            }
        }
        root.addView(handle)

        // Header
        val headerRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (24 * density).toInt() }
        }

        val iconBg = FrameLayout(ctx).apply {
            val size = (48 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = (16 * density).toInt() }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14 * density
                setColor(Color.parseColor("#1A4A9EFF"))
            }
        }
        val iconView = ImageView(ctx).apply {
            setImageResource(android.R.drawable.ic_menu_manage)
            imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#4A9EFF"))
            val pad = (12 * density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        iconBg.addView(iconView)
        headerRow.addView(iconBg)

        val titleCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvTitle = TextView(ctx).apply {
            text = "Native Gesture Engine"
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }
        val isGranted = ctx.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
        val tvStatus = TextView(ctx).apply {
            text = if (isGranted) "Permission: Granted" else "Permission: Missing"
            textSize = 12f
            setTextColor(if (isGranted) Color.parseColor("#00FF00") else Color.parseColor("#99FFFFFF"))
            if (isGranted) typeface = Typeface.DEFAULT_BOLD
        }
        titleCol.addView(tvTitle)
        titleCol.addView(tvStatus)

        // Add Automation Status
        val tvAutoStatus = TextView(ctx).apply {
            val autoStatus = when {
                AutomationManager.isRootAvailable() -> "Engine: Root Active"
                AutomationManager.isShizukuAvailable() -> "Engine: Shizuku Active"
                else -> "Engine: Service Not Running"
            }
            text = autoStatus
            textSize = 11f
            setTextColor(Color.parseColor("#B3FFFFFF"))
            setPadding(0, (2 * density).toInt(), 0, 0)
        }
        titleCol.addView(tvAutoStatus)
        headerRow.addView(titleCol)
        root.addView(headerRow)

        val tvDesc = TextView(ctx).apply {
            text = "Enable high-performance gestures without traditional Accessibility Services."
            textSize = 14f
            setTextColor(Color.parseColor("#B3FFFFFF"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (24 * density).toInt() }
        }
        root.addView(tvDesc)

        // Divider
        root.addView(View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (1 * density).toInt()).apply {
                bottomMargin = (24 * density).toInt()
            }
            setBackgroundColor(Color.parseColor("#1AFFFFFF"))
        })

        // Shizuku Row
        root.addView(createAutomationRow(ctx, density, "Shizuku", "Wireless ADB automation",
            onGrant = {
                try {
                    if (rikka.shizuku.Shizuku.pingBinder()) {
                        if (rikka.shizuku.Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                            val sh = "pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"
                            val process = rikka.shizuku.Shizuku.newProcess(arrayOf("sh", "-c", sh), null, null)
                            process.waitFor()
                            refreshUI(ctx, tvStatus)
                        } else {
                            rikka.shizuku.Shizuku.requestPermission(1001)
                        }
                    }
                } catch (e: Exception) {}
            },
            onRevoke = {
                try {
                    if (rikka.shizuku.Shizuku.pingBinder()) {
                        if (rikka.shizuku.Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                            val sh = "pm revoke $packageName android.permission.WRITE_SECURE_SETTINGS"
                            val process = rikka.shizuku.Shizuku.newProcess(arrayOf("sh", "-c", sh), null, null)
                            process.waitFor()
                            refreshUI(ctx, tvStatus)
                        }
                    }
                } catch (e: Exception) {}
            }
        ))

        // Root Row
        root.addView(createAutomationRow(ctx, density, "Root Access", "Direct system grant",
            onGrant = {
                AutomationManager.requestRootPermission { success ->
                    if (success) {
                        try {
                            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"))
                            p.waitFor()
                            activity?.runOnUiThread { refreshUI(ctx, tvStatus) }
                        } catch (e: Exception) {}
                    }
                }
            },
            onRevoke = {
                try {
                    val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "pm revoke $packageName android.permission.WRITE_SECURE_SETTINGS"))
                    p.waitFor()
                    activity?.runOnUiThread { refreshUI(ctx, tvStatus) }
                } catch (e: Exception) {}
            }
        ))

        // ADB Section
        val adbHeaderRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (20 * density).toInt(); bottomMargin = (8 * density).toInt() }
        }
        
        val adbTitle = TextView(ctx).apply {
            text = "MANUAL SETUP (ADB)"
            textSize = 11f
            setTextColor(Color.parseColor("#66FFFFFF"))
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.1f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        adbHeaderRow.addView(adbTitle)

        val btnCopy = TextView(ctx).apply {
            text = "Copy"
            setTextColor(Color.parseColor("#4A9EFF"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 4 * density
                setColor(Color.parseColor("#1A4A9EFF"))
            }
            setOnClickListener {
                val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", adbCommand))
                root.showModernToast("Command copied to clipboard")
            }
        }
        adbHeaderRow.addView(btnCopy)
        root.addView(adbHeaderRow)

        val adbBox = TextView(ctx).apply {
            text = adbCommand
            textSize = 10f
            setTextColor(Color.parseColor("#E6FFFFFF"))
            typeface = Typeface.MONOSPACE
            setPadding((16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12 * density
                setColor(Color.parseColor("#1AFFFFFF"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", adbCommand))
                root.showModernToast("Command copied to clipboard")
            }
        }
        root.addView(adbBox)

        // Close Button
        val btnClose = Button(ctx).apply {
            text = "Done"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14 * density
                setColor(Color.parseColor("#4A9EFF"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (54 * density).toInt()
            ).apply { topMargin = (28 * density).toInt() }
            setOnClickListener { dismiss() }
        }
        root.addView(btnClose)

        return root
    }

    private fun createAutomationRow(
        ctx: Context,
        density: Float,
        title: String,
        subtitle: String,
        onGrant: () -> Unit,
        onRevoke: () -> Unit
    ): View {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (20 * density).toInt() }
        }

        val textCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        textCol.addView(TextView(ctx).apply {
            text = title
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        })
        textCol.addView(TextView(ctx).apply {
            text = subtitle
            textSize = 12f
            setTextColor(Color.parseColor("#99FFFFFF"))
        })
        row.addView(textCol)

        val btnRevoke = Button(ctx).apply {
            text = "Revoke"
            setTextColor(Color.parseColor("#FF5252"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 19 * density
                setStroke((1 * density).toInt(), Color.parseColor("#FF5252"))
                setColor(Color.TRANSPARENT)
            }
            layoutParams = LinearLayout.LayoutParams((80 * density).toInt(), (38 * density).toInt()).apply {
                marginEnd = (12 * density).toInt()
            }
            setOnClickListener { onRevoke() }
        }
        row.addView(btnRevoke)

        val btnGrant = Button(ctx).apply {
            text = "Grant"
            setTextColor(Color.parseColor("#4A9EFF"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 19 * density
                setStroke((1 * density).toInt(), Color.parseColor("#4A9EFF"))
                setColor(Color.TRANSPARENT)
            }
            layoutParams = LinearLayout.LayoutParams((80 * density).toInt(), (38 * density).toInt())
            setOnClickListener { onGrant() }
        }
        row.addView(btnGrant)
        return row
    }

    private fun refreshUI(ctx: Context, tvStatus: TextView) {
        val isGranted = ctx.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
        tvStatus.text = if (isGranted) "Status: Active" else "Status: Inactive"
        tvStatus.setTextColor(if (isGranted) Color.parseColor("#00FF00") else Color.parseColor("#99FFFFFF"))
        if (isGranted) {
            tvStatus.typeface = Typeface.DEFAULT_BOLD
            onPermissionGranted?.invoke()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                val density = resources.displayMetrics.density
                cornerRadii = floatArrayOf(
                    24 * density, 24 * density,
                    24 * density, 24 * density,
                    0f, 0f, 0f, 0f
                )
                setColor(Color.parseColor("#1F2732"))
            }
        }
        return dialog
    }
}
