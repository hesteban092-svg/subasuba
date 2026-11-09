package com.miauto.launcher

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class AppsActivity : AppCompatActivity() {
    private data class App(val pkg: String, val label: String, val ri: android.content.pm.ResolveInfo)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pm = packageManager
        val d = resources.displayMetrics.density
        fun px(v: Int) = (v * d).toInt()
        fun col(id: Int) = ContextCompat.getColor(this, id)

        val all = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { App(it.activityInfo.packageName, it.loadLabel(pm).toString(), it) }
            .sortedBy { it.label.lowercase() }
        var shown: List<App> = all

        val root = FrameLayout(this)
        val bg = ImageView(this)
        bg.setImageResource(R.drawable.apps_bg)
        bg.scaleType = ImageView.ScaleType.CENTER_CROP
        root.addView(bg, FrameLayout.LayoutParams(-1, -1))

        val column = LinearLayout(this)
        column.orientation = LinearLayout.VERTICAL
        column.setPadding(px(16), px(12), px(16), px(12))
        root.addView(column, FrameLayout.LayoutParams(-1, -1))

        val top = LinearLayout(this)
        top.gravity = Gravity.CENTER_VERTICAL
        val back = TextView(this)
        back.text = "←"
        back.textSize = 28f
        back.gravity = Gravity.CENTER
        back.setTextColor(col(R.color.text))
        back.setBackgroundResource(R.drawable.bg_glass_tile)
        back.setOnClickListener { finish() }
        top.addView(back, LinearLayout.LayoutParams(px(72), px(56)))
        val search = EditText(this)
        search.hint = "Buscar app…"
        search.textSize = 20f
        search.setSingleLine(true)
        search.inputType = InputType.TYPE_CLASS_TEXT
        search.setTextColor(col(R.color.text))
        search.setHintTextColor(col(R.color.textSub))
        search.setBackgroundResource(R.drawable.bg_glass_tile)
        search.setPadding(px(20), 0, px(20), 0)
        val slp = LinearLayout.LayoutParams(0, px(56), 1f)
        slp.marginStart = px(12)
        top.addView(search, slp)
        column.addView(top, LinearLayout.LayoutParams(-1, -2))

        val grid = GridView(this)
        grid.numColumns = 6
        grid.verticalSpacing = px(14)
        grid.horizontalSpacing = px(14)
        grid.selector = ColorDrawable(Color.TRANSPARENT)
        grid.setPadding(0, px(14), 0, 0)
        val glp = LinearLayout.LayoutParams(-1, 0, 1f)
        column.addView(grid, glp)

        val adapter = object : BaseAdapter() {
            override fun getCount() = shown.size
            override fun getItem(p: Int) = shown[p]
            override fun getItemId(p: Int) = p.toLong()
            override fun getView(p: Int, v: View?, parent: ViewGroup?): View {
                val app = shown[p]
                val box = LinearLayout(this@AppsActivity)
                box.orientation = LinearLayout.VERTICAL
                box.gravity = Gravity.CENTER
                box.setBackgroundResource(R.drawable.bg_glass_tile)
                box.layoutParams = AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(118))
                val icon = ImageView(this@AppsActivity)
                icon.setImageDrawable(app.ri.loadIcon(pm))
                box.addView(icon, LinearLayout.LayoutParams(px(60), px(60)))
                val tv = TextView(this@AppsActivity)
                tv.text = app.label
                tv.maxLines = 1
                tv.ellipsize = android.text.TextUtils.TruncateAt.END
                tv.gravity = Gravity.CENTER
                tv.textSize = 14f
                tv.setPadding(px(6), px(6), px(6), 0)
                tv.setTextColor(col(R.color.text))
                box.addView(tv)
                return box
            }
        }
        grid.adapter = adapter
        grid.setOnItemClickListener { _, _, pos, _ ->
            val i = pm.getLaunchIntentForPackage(shown[pos].pkg)
            if (i != null) startActivity(i)
        }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = s.toString().trim().lowercase()
                shown = if (q.isEmpty()) all else all.filter { it.label.lowercase().contains(q) }
                adapter.notifyDataSetChanged()
            }
        })
        setContentView(root)
    }

    // Así el menú nunca queda "pegado" como primera pantalla al volver al launcher.
    override fun onStop() {
        super.onStop()
        finish()
    }
}
