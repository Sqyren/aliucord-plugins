package com.github.sqyren

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.aliucord.api.SettingsAPI
import com.aliucord.widgets.BottomSheet

class PluginSettings(private val settings: SettingsAPI) : BottomSheet() {
    private val defaultColor = "#3BA55D"
    private val defaultDuration = 1400
    private val presets = listOf("#3BA55D", "#FF69B4", "#5865F2", "#9B59B6", "#F39C12", "#ED4245")

    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
        val ctx = requireContext()
        val density = ctx.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        fun dot(color: Int) = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(dp(2), Color.parseColor("#44FFFFFF"))
        }

        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(24))
        }

        box.addView(TextView(ctx).apply {
            text = "RingingPulse"
            textSize = 20f
            setTextColor(Color.WHITE)
        })
        box.addView(TextView(ctx).apply {
            text = "Changes apply to the next incoming call"
            textSize = 13f
            setTextColor(Color.parseColor("#B5BAC1"))
            setPadding(0, dp(2), 0, dp(16))
        })

        val swatch = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply { rightMargin = dp(12) }
        }
        val hexInput = EditText(ctx).apply {
            setText(settings.getString("color", defaultColor))
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#80FFFFFF"))
            hint = "#3BA55D"
            backgroundTintList = ColorStateList.valueOf(Color.parseColor("#3BA55D"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        fun refreshSwatch(hex: String) {
            val c = try { Color.parseColor(hex) } catch (e: Exception) { Color.GRAY }
            swatch.background = dot(c)
        }
        refreshSwatch(hexInput.text.toString())
        hexInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val hex = s.toString().trim()
                settings.setString("color", hex)
                refreshSwatch(hex)
            }
        })

        box.addView(TextView(ctx).apply {
            text = "Ring color"
            setTextColor(Color.WHITE)
        })
        box.addView(LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
            addView(swatch)
            addView(hexInput)
        })

        box.addView(LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(16))
            presets.forEach { hex ->
                addView(View(ctx).apply {
                    background = dot(Color.parseColor(hex))
                    layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply { rightMargin = dp(10) }
                    setOnClickListener { hexInput.setText(hex) }
                })
            }
        })

        val speedLabel = TextView(ctx).apply { setTextColor(Color.WHITE) }
        fun refreshLabel(duration: Int) {
            speedLabel.text = "Ring speed (one ripple every %.1fs)".format(duration / 1000f)
        }
        val speedBar = SeekBar(ctx).apply {
            max = 100
            val d = settings.getInt("duration", defaultDuration)
            progress = ((2600 - d) / 20).coerceIn(0, 100)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    val dur = 2600 - p * 20
                    settings.setInt("duration", dur)
                    refreshLabel(dur)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        refreshLabel(settings.getInt("duration", defaultDuration))
        box.addView(speedLabel)
        box.addView(speedBar)

        box.addView(Button(ctx).apply {
            text = "Reset to default"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(Color.parseColor("#5865F2"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(20) }
            setOnClickListener {
                hexInput.setText(defaultColor)
                speedBar.progress = (2600 - defaultDuration) / 20
                settings.setString("color", defaultColor)
                settings.setInt("duration", defaultDuration)
                refreshLabel(defaultDuration)
            }
        })

        addView(box)
    }
}
