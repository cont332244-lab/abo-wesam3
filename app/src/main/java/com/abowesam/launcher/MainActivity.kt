package com.abowesam.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
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
    private lateinit var pageLabel: TextView
    private var apps: List<LaunchApp> = emptyList()
    private var drawerOpen = false
    private var currentPage = 0
    private val accent = Color.rgb(255, 190, 92)
    private val prefs by lazy { getSharedPreferences("launcher_settings", MODE_PRIVATE) }

    data class LaunchApp(val label: String, val packageName: String, val icon: Drawable)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(15, 20, 36)
        window.navigationBarColor = Color.rgb(12, 16, 29)
        window.decorView.systemUiVisibility = 0
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
        if (Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                view.setPadding(dp(18), bars.top + dp(10), dp(18), bars.bottom + dp(10))
                insets
            }
        } else {
            @Suppress("DEPRECATION")
            root.setPadding(dp(18), dp(30), dp(18), dp(12))
        }

        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(label("أبو وسام", 26f, Color.WHITE, true), LinearLayout.LayoutParams(0, dp(52), 1f))
        header.addView(label("3D SHELL", 12f, accent, true))
        root.addView(header)
        root.addView(label("مساحتك الرئيسية • خصّص تطبيقاتك", 13f, 0xFFD4D8E2.toInt()).apply {
            gravity = Gravity.START
            setPadding(0, dp(2), 0, dp(12))
        })

        val info = label("اسحب بين الصفحات واختر تطبيقاتك المفضلة", 13f, Color.WHITE)
        info.background = panel(dp(22).toFloat(), 0x332FFFFFF)
        root.addView(info, LinearLayout.LayoutParams(-1, dp(48)).apply { bottomMargin = dp(10) })

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

        val pageBar = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(5), 0, dp(5))
        }
        pageBar.addView(dockItem("السابق", "‹") { changePage(-1) }, LinearLayout.LayoutParams(0, dp(42), 1f))
        pageLabel = label("1 / 3", 12f, Color.WHITE, true)
        pageBar.addView(pageLabel, LinearLayout.LayoutParams(0, dp(42), 1f))
        pageBar.addView(dockItem("التالي", "›") { changePage(1) }, LinearLayout.LayoutParams(0, dp(42), 1f))
        root.addView(pageBar)

        val dock = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = panel(dp(25).toFloat(), 0x44FFFFFF)
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        dock.addView(dockItem("التطبيقات", "▦") { toggleDrawer() }, LinearLayout.LayoutParams(0, dp(68), 1f))
        dock.addView(dockItem("الخلفية", "◈") { chooseWallpaper() }, LinearLayout.LayoutParams(0, dp(68), 1f))
        dock.addView(dockItem("الإعدادات", "⚙") { showSettings() }, LinearLayout.LayoutParams(0, dp(68), 1f))
        dock.addView(dockItem("تحديث", "↻") { loadApps(); toast("تم تحديث قائمة التطبيقات") }, LinearLayout.LayoutParams(0, dp(68), 1f))
        root.addView(dock, LinearLayout.LayoutParams(-1, dp(80)).apply { topMargin = dp(6) })
    }

    private fun dockItem(title: String, symbol: String, action: () -> Unit): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(label(symbol, 24f, accent, true))
            addView(label(title, 10f, Color.WHITE))
            setOnClickListener { action() }
        }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun homePackages(page: Int): MutableSet<String> =
        prefs.getStringSet("home_page_$page", emptySet())?.toMutableSet() ?: mutableSetOf()

    private fun saveHomePackages(page: Int, values: Set<String>) {
        prefs.edit().putStringSet("home_page_$page", values.toSet()).apply()
    }

    private fun loadApps() {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        apps = packageManager.queryIntentActivities(query, PackageManager.MATCH_ALL)
            .mapNotNull { item ->
                val activityInfo = item.activityInfo ?: return@mapNotNull null
                LaunchApp(
                    item.loadLabel(packageManager)?.toString() ?: activityInfo.packageName,
                    activityInfo.packageName,
                    item.loadIcon(packageManager)
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
        if (drawerOpen) renderApps(search.text.toString()) else renderHome()
    }

    private fun renderHome() {
        grid.removeAllViews()
        pageLabel.text = "${currentPage + 1} / 3"
        val selected = homePackages(currentPage)
        val visible = if (selected.isEmpty() && currentPage == 0) apps.take(12)
            else apps.filter { selected.contains(it.packageName) }
        visible.forEach { grid.addView(appTile(it, false)) }
        if (visible.isEmpty()) {
            val hint = label("هذه الصفحة فارغة\nافتح التطبيقات واضغط مطولاً لإضافة تطبيق", 14f, 0xFFE4E8F0.toInt())
            grid.addView(hint, GridLayout.LayoutParams().apply {
                width = dp(280); height = dp(110); columnSpec = GridLayout.spec(0, 4)
            })
        }
    }

    private fun renderApps(filter: String = "") {
        if (!drawerOpen) return
        grid.removeAllViews()
        apps.filter { it.label.contains(filter, true) || it.packageName.contains(filter, true) }
            .forEach { grid.addView(appTile(it, true)) }
    }

    private fun appTile(app: LaunchApp, inDrawer: Boolean): View {
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
            val options = if (inDrawer) arrayOf("إضافة إلى الصفحة الحالية", "إضافة إلى المفضلة")
                else arrayOf("إزالة من الصفحة الرئيسية", "نقل إلى الصفحة التالية")
            AlertDialog.Builder(this)
                .setTitle(app.label)
                .setItems(options) { _, which ->
                    if (inDrawer) {
                        val chosen = homePackages(currentPage)
                        chosen.add(app.packageName)
                        saveHomePackages(currentPage, chosen)
                        toast("أُضيف التطبيق إلى الصفحة ${currentPage + 1}")
                        toggleDrawer()
                    } else if (which == 0) {
                        val chosen = homePackages(currentPage)
                        chosen.remove(app.packageName)
                        saveHomePackages(currentPage, chosen)
                        renderHome()
                    } else {
                        val chosen = homePackages(currentPage)
                        chosen.remove(app.packageName)
                        saveHomePackages(currentPage, chosen)
                        val next = (currentPage + 1) % 3
                        val target = homePackages(next)
                        target.add(app.packageName)
                        saveHomePackages(next, target)
                        renderHome()
                    }
                }.show()
            true
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
            if (intent == null) toast("لا يوجد اختصار تشغيل لهذا التطبيق")
            else startActivity(intent)
        } catch (_: Exception) {
            toast("تعذر فتح التطبيق")
        }
    }

    private fun toggleDrawer() {
        drawerOpen = !drawerOpen
        search.visibility = if (drawerOpen) View.VISIBLE else View.GONE
        if (drawerOpen) renderApps() else renderHome()
    }

    private fun changePage(delta: Int) {
        if (drawerOpen) return
        currentPage = (currentPage + delta + 3) % 3
        renderHome()
    }

    private fun chooseWallpaper() {
        try {
            startActivity(Intent(Intent.ACTION_SET_WALLPAPER))
        } catch (_: Exception) {
            toast("غيّر الخلفية من إعدادات الهاتف")
        }
    }

    private fun showSettings() {
        val choices = arrayOf("إعادة ضبط الصفحات الرئيسية", "معلومات عن التطبيق")
        AlertDialog.Builder(this)
            .setTitle("إعدادات أبو وسام")
            .setItems(choices) { _, which ->
                if (which == 0) {
                    prefs.edit().clear().apply()
                    currentPage = 0
                    renderHome()
                    toast("تمت إعادة ضبط الصفحات")
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("أبو وسام لانشر")
                        .setMessage("الإصدار 0.2.0\nلانشر أندرويد قيد التطوير، يدعم فتح التطبيقات والبحث وثلاث صفحات رئيسية قابلة للتخصيص.")
                        .setPositiveButton("حسناً", null)
                        .show()
                }
            }.show()
    }

    @Deprecated("Legacy back callback retained for Android 16 compatibility setting")
    override fun onBackPressed() {
        if (drawerOpen) toggleDrawer() else super.onBackPressed()
    }
}
