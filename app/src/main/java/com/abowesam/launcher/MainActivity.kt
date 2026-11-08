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
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var grid: GridLayout
    private lateinit var search: EditText
    private lateinit var pageLabel: TextView
    private lateinit var sectionTitle: TextView
    private lateinit var pageDots: LinearLayout
    private var apps: List<LaunchApp> = emptyList()
    private var drawerOpen = false
    private var currentPage = 0
    private val accent = Color.rgb(255, 196, 112)
    private val muted = 0xFFB9C5D8.toInt()
    private val prefs by lazy { getSharedPreferences("launcher_settings", MODE_PRIVATE) }

    data class LaunchApp(val label: String, val packageName: String, val icon: Drawable)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(10, 17, 32)
        window.navigationBarColor = Color.rgb(10, 17, 32)
        window.decorView.systemUiVisibility = 0
        buildHome()
        loadApps()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun panel(radius: Float, color: Int): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = radius }

    private fun gradient(radius: Float, colors: IntArray): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply { cornerRadius = radius }

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
                intArrayOf(Color.rgb(8, 18, 38), Color.rgb(31, 35, 66), Color.rgb(15, 22, 42))
            )
        }
        setContentView(root)
        if (Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                view.setPadding(dp(18), bars.top + dp(8), dp(18), bars.bottom + dp(8))
                insets
            }
        } else {
            @Suppress("DEPRECATION")
            root.setPadding(dp(18), dp(30), dp(18), dp(12))
        }

        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), dp(2), dp(2), dp(8))
        }
        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        brand.addView(label("أبو وسام", 27f, Color.WHITE, true).apply { gravity = Gravity.START })
        brand.addView(label("A B O   W E S A M", 9f, accent, true).apply {
            gravity = Gravity.START
            letterSpacing = 0.12f
        })
        header.addView(brand, LinearLayout.LayoutParams(0, dp(58), 1f))
        val version = label("3D  •  SHELL", 10f, accent, true).apply {
            background = panel(dp(14).toFloat(), 0x22FFC470)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        header.addView(version)
        root.addView(header)

        val clockCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = gradient(dp(22).toFloat(), intArrayOf(0xFF283B5D.toInt(), 0xFF202B49.toInt()))
            setPadding(dp(16), dp(12), dp(16), dp(12))
            elevation = dp(5).toFloat()
        }
        val clockColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        clockColumn.addView(TextClock(this).apply {
            format12Hour = "hh:mm"
            format24Hour = "HH:mm"
            textSize = 31f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            gravity = Gravity.START
        })
        clockColumn.addView(TextClock(this).apply {
            format12Hour = "EEEE، d MMMM"
            format24Hour = "EEEE، d MMMM"
            textSize = 11f
            setTextColor(muted)
            gravity = Gravity.START
        })
        clockCard.addView(clockColumn, LinearLayout.LayoutParams(0, -2, 1f))
        clockCard.addView(label("✦", 31f, accent, true))
        root.addView(clockCard, LinearLayout.LayoutParams(-1, dp(88)).apply {
            bottomMargin = dp(14)
        })

        val sectionRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), 0, dp(2), dp(8))
        }
        sectionTitle = label("تطبيقاتي", 16f, Color.WHITE, true).apply { gravity = Gravity.START }
        sectionRow.addView(sectionTitle, LinearLayout.LayoutParams(0, dp(32), 1f))
        sectionRow.addView(label("المساحة الرئيسية", 10f, muted))
        root.addView(sectionRow)

        search = EditText(this).apply {
            hint = "ابحث عن تطبيق أو لعبة..."
            setHintTextColor(muted)
            setTextColor(Color.WHITE)
            textSize = 14f
            isSingleLine = true
            background = panel(dp(17).toFloat(), 0x22FFFFFF)
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
        root.addView(search, LinearLayout.LayoutParams(-1, dp(46)).apply { bottomMargin = dp(8) })

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(0, dp(2), 0, dp(8))
        }
        grid = GridLayout(this).apply {
            columnCount = 4
            useDefaultMargins = false
            alignmentMode = GridLayout.ALIGN_BOUNDS
        }
        scroll.addView(grid)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val pageBar = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(4))
        }
        pageBar.addView(dockItem("السابق", "‹") { changePage(-1) }, LinearLayout.LayoutParams(0, dp(38), 1f))
        pageLabel = label("1 / 3", 11f, Color.WHITE, true)
        pageBar.addView(pageLabel, LinearLayout.LayoutParams(dp(52), dp(38)))
        pageDots = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        pageBar.addView(pageDots, LinearLayout.LayoutParams(dp(58), dp(38)))
        pageBar.addView(dockItem("التالي", "›") { changePage(1) }, LinearLayout.LayoutParams(0, dp(38), 1f))
        root.addView(pageBar)

        val dock = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = gradient(dp(24).toFloat(), intArrayOf(0xCC344564.toInt(), 0xCC222F4D.toInt()))
            setPadding(dp(5), dp(5), dp(5), dp(5))
            elevation = dp(9).toFloat()
        }
        dock.addView(dockItem("التطبيقات", "▦") { toggleDrawer() }, LinearLayout.LayoutParams(0, dp(66), 1f))
        dock.addView(dockItem("الخلفية", "◈") { chooseWallpaper() }, LinearLayout.LayoutParams(0, dp(66), 1f))
        dock.addView(dockItem("الإعدادات", "⚙") { showSettings() }, LinearLayout.LayoutParams(0, dp(66), 1f))
        dock.addView(dockItem("تحديث", "↻") { loadApps(); toast("تم تحديث قائمة التطبيقات") }, LinearLayout.LayoutParams(0, dp(66), 1f))
        root.addView(dock, LinearLayout.LayoutParams(-1, dp(76)).apply { topMargin = dp(7) })
        updatePageDots()
    }

    private fun dockItem(title: String, symbol: String, action: () -> Unit): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(label(symbol, 23f, accent, true))
            addView(label(title, 10f, Color.WHITE))
            setOnClickListener { action() }
            isClickable = true
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
        sectionTitle.text = "تطبيقاتي  •  الصفحة ${currentPage + 1}"
        val selected = homePackages(currentPage)
        val visible = if (selected.isEmpty() && currentPage == 0) apps.take(12)
            else apps.filter { selected.contains(it.packageName) }
        visible.forEach { grid.addView(appTile(it, false)) }
        if (visible.isEmpty()) {
            val hint = label("مساحة جديدة بانتظارك\nافتح التطبيقات واضغط مطولاً لإضافة اختصاراتك", 13f, 0xFFE4E8F0.toInt())
            hint.background = panel(dp(20).toFloat(), 0x1EFFFFFF)
            grid.addView(hint, GridLayout.LayoutParams().apply {
                width = dp(290); height = dp(120); columnSpec = GridLayout.spec(0, 4)
                setMargins(dp(4), dp(8), dp(4), dp(8))
            })
        }
        updatePageDots()
    }

    private fun updatePageDots() {
        if (!::pageDots.isInitialized) return
        pageDots.removeAllViews()
        repeat(3) { index ->
            val dot = View(this).apply {
                background = panel(dp(8).toFloat(), if (index == currentPage) accent else 0x66FFFFFF)
                setOnClickListener { currentPage = index; renderHome() }
            }
            pageDots.addView(dot, LinearLayout.LayoutParams(dp(if (index == currentPage) 18 else 7), dp(7)).apply {
                setMargins(dp(3), 0, dp(3), 0)
            })
        }
    }

    private fun renderApps(filter: String = "") {
        if (!drawerOpen) return
        sectionTitle.text = "كل التطبيقات"
        grid.removeAllViews()
        apps.filter { it.label.contains(filter, true) || it.packageName.contains(filter, true) }
            .forEach { grid.addView(appTile(it, true)) }
    }

    private fun appTile(app: LaunchApp, inDrawer: Boolean): View {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(3), dp(8), dp(3), dp(7))
            background = panel(dp(19).toFloat(), 0x18FFFFFF)
            isClickable = true
            isFocusable = true
        }
        val iconFrame = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = gradient(dp(18).toFloat(), intArrayOf(0x44FFFFFF, 0x18FFFFFF))
            elevation = dp(3).toFloat()
        }
        iconFrame.addView(ImageView(this).apply {
            setImageDrawable(app.icon)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        tile.addView(iconFrame, LinearLayout.LayoutParams(dp(60), dp(60)).apply { bottomMargin = dp(5) })
        tile.addView(label(app.label, 10.5f, Color.WHITE, true).apply { maxLines = 2 })
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
            height = dp(112)
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(dp(3), dp(3), dp(3), dp(3))
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
        if (drawerOpen) {
            search.setText("")
            renderApps()
            search.requestFocus()
        } else renderHome()
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
                        .setMessage("الإصدار 0.3.0\nواجهة عربية بتصميم زجاجي، ساعة وتاريخ، ثلاث صفحات رئيسية، درج تطبيقات وبحث واختصارات قابلة للتخصيص.")
                        .setPositiveButton("حسناً", null)
                        .show()
                }
            }.show()
    }

    @Deprecated("Legacy back callback retained for Android compatibility")
    override fun onBackPressed() {
        if (drawerOpen) toggleDrawer() else super.onBackPressed()
    }
}
