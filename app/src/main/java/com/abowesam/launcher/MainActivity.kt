package com.abowesam.launcher

import android.app.Activity
import android.app.WallpaperManager
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var grid: GridLayout
    private lateinit var search: EditText
    private var apps: List<LaunchApp> = emptyList()
    private var drawerOpen = false
    private val accent = Color.rgb(255, 190, 92)

    data class LaunchApp(val label: String, val packageName: String, val icon: android.graphics.drawable.Drawable)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(15, 20, 36)
        window.navigationBarColor = Color.rgb(12, 16, 29)
        buildHome()
        loadApps()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun panel(radius: Float, color: Int): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = radius }

    private fun label(value: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun buildHome() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(12))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(13, 24, 46), Color.rgb(51, 36, 72), Color.rgb(10, 17, 32))
            )
        }
        setContentView(root)
        root.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            view.setPadding(dp(18), bars.top + dp(10), dp(18), bars.bottom + dp(10))
            insets
        }

        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(label("أبو وسام", 26f, Color.WHITE, true), LinearLayout.LayoutParams(0, dp(52), 1f))
        header.addView(label("3D SHELL", 12f, accent, true))
        root.addView(header)
        root.addView(label("اسحب، افتح، واكتشف تطبيقاتك", 13f, 0xFFD4D8E2.toInt()).apply {
            gravity = Gravity.START
            setPadding(0, dp(2), 0, dp(16))
        })

        val info = label("واجهة ثلاثية الأبعاد قيد التطوير", 14f, Color.WHITE)
        info.background = panel(dp(22).toFloat(), 0x332FFFFFF)
        root.addView(info, LinearLayout.LayoutParams(-1, dp(54)).apply { bottomMargin = dp(14) })

        search = EditText(this).apply {
            hint = "ابحث عن تطبيق..."
            setHintTextColor(0xFFB8C0D0.toInt())
            setTextColor(Color.WHITE)
            textSize = 15f
            singleLine = true
            background = panel(dp(18).toFloat(), 0x33FFFFFF)
            setPadding(dp(14), 0, dp(14), 0)
            visibility = View.GONE
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    renderApps(s?.toString().orEmpty())
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(48)).apply { bottomMargin = dp(8) })

        val scroll = ScrollView(this)
        grid = GridLayout(this).apply { columnCount = 4; useDefaultMargins = true }
        scroll.addView(grid)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val dock = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = panel(dp(25).toFloat(), 0x44FFFFFF)
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        dock.addView(dockItem("التطبيقات", "▦") { toggleDrawer() }, LinearLayout.LayoutParams(0, dp(68), 1f))
        dock.addView(dockItem("الخلفية", "◈") { chooseWallpaper() }, LinearLayout.LayoutParams(0, dp(68), 1f))
        dock.addView(dockItem("تحديث", "↻") { loadApps(); Toast.makeText(this, "تم تحديث التطبيقات", Toast.LENGTH_SHORT).show() }, LinearLayout.LayoutParams(0, dp(68), 1f))
        root.addView(dock, LinearLayout.LayoutParams(-1, dp(80)).apply { topMargin = dp(10) })
    }

    private fun dockItem(title: String, symbol: String, action: () -> Unit): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(label(symbol, 26f, accent, true))
            addView(label(title, 11f, Color.WHITE))
            setOnClickListener { action() }
        }

    private fun loadApps() {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        apps = packageManager.queryIntentActivities(query, PackageManager.MATCH_ALL)
            .mapNotNull { item ->
                val activityInfo = item.activityInfo ?: return@mapNotNull null
                LaunchApp(item.loadLabel(packageManager)?.toString() ?: activityInfo.packageName,
                    activityInfo.packageName, item.loadIcon(packageManager))
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
        if (drawerOpen) renderApps(search.text.toString()) else renderHome()
    }

    private fun renderHome() {
        grid.removeAllViews()
        apps.take(12).forEach { grid.addView(appTile(it)) }
    }

    private fun renderApps(filter: String = "") {
        if (!drawerOpen) return
        grid.removeAllViews()
        apps.filter { it.label.contains(filter, true) || it.packageName.contains(filter, true) }
            .forEach { grid.addView(appTile(it)) }
    }

    private fun appTile(app: LaunchApp): View {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(5), dp(9), dp(5), dp(9))
            background = panel(dp(18).toFloat(), 0x22FFFFFF)
        }
        tile.addView(ImageView(this).apply {
            setImageDrawable(app.icon)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }, LinearLayout.LayoutParams(dp(52), dp(52)))
        tile.addView(label(app.label, 11f, Color.WHITE).apply { maxLines = 2 })
        tile.setOnClickListener { openApp(app) }
        tile.setOnLongClickListener {
            val clip = ClipData.newPlainText("application", app.packageName)
            tile.startDragAndDrop(clip, View.DragShadowBuilder(tile), app.packageName, 0)
            true
        }
        tile.setOnDragListener { _, event ->
            event.action == DragEvent.ACTION_DRAG_STARTED ||
                event.action == DragEvent.ACTION_DRAG_ENTERED ||
                event.action == DragEvent.ACTION_DRAG_EXITED ||
                event.action == DragEvent.ACTION_DROP
        }
        tile.layoutParams = GridLayout.LayoutParams().apply {
            width = 0
            height = dp(116)
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(dp(4), dp(4), dp(4), dp(4))
        }
        return tile
    }

    private fun openApp(app: LaunchApp) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(app.packageName)
            if (intent == null) Toast.makeText(this, "لا يوجد اختصار تشغيل لهذا التطبيق", Toast.LENGTH_SHORT).show()
            else startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "تعذر فتح التطبيق", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleDrawer() {
        drawerOpen = !drawerOpen
        search.visibility = if (drawerOpen) View.VISIBLE else View.GONE
        if (drawerOpen) renderApps() else renderHome()
    }

    private fun chooseWallpaper() {
        try {
            startActivity(Intent(Intent.ACTION_ATTACH_DATA).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_DEFAULT)
            })
        } catch (_: Exception) {
            try { startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)) }
            catch (_: Exception) { Toast.makeText(this, "غيّر الخلفية من إعدادات الهاتف", Toast.LENGTH_SHORT).show() }
        }
    }

    @Deprecated("Legacy back callback retained for Android 16 compatibility setting")
    override fun onBackPressed() {
        if (drawerOpen) toggleDrawer() else super.onBackPressed()
    }
}
