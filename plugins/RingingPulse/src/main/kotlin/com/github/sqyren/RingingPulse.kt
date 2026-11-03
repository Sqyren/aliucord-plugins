package com.github.sqyren

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.before
import com.discord.utilities.anim.RingAnimator
import java.util.WeakHashMap

@AliucordPlugin(requiresRestart = false)
@Suppress("unused")
class RingingPulse : Plugin() {
    private class Holder(
        val host: FrameLayout,
        val anims: List<ObjectAnimator>,
        val rings: List<View>,
    ) {
        fun stop() {
            anims.forEach { it.cancel() }
            host.post { rings.forEach { host.removeView(it) } }
        }
    }

    private val active = WeakHashMap<View, Holder>()

    private fun ringColor(): Int {
        val hex = settings.getString("color", "#3BA55D")
        val base = try {
            Color.parseColor(hex)
        } catch (e: Exception) {
            Color.parseColor("#3BA55D")
        }
        return (base and 0x00FFFFFF) or (110 shl 24)
    }

    override fun start(context: Context) {
        settingsTab = SettingsTab(PluginSettings::class.java, SettingsTab.Type.BOTTOM_SHEET)
            .withArgs(settings)

        patcher.before<RingAnimator>("onUpdate") {
            val v = view
            v.post {
                if (v.rootView.findViewById<View>(0x7f0a08d4) == null) return@post
                val host = v as? FrameLayout ?: return@post
                val avatar = v.findViewById<View>(0x7f0a1121) ?: v
                if (active.containsKey(avatar)) return@post

                host.clipChildren = false
                var p: ViewParent? = host.parent
                while (p is ViewGroup) {
                    p.clipChildren = false
                    p.clipToPadding = false
                    p = p.parent
                }

                val size = if (avatar.width > 0) avatar.width else host.width
                val duration = settings.getInt("duration", 1400).toLong()
                val color = ringColor()
                val anims = mutableListOf<ObjectAnimator>()
                val rings = mutableListOf<View>()

                for (i in 0..1) {
                    val ring = View(host.context).apply {
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(color)
                        }
                        layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
                        alpha = 0f
                    }
                    host.addView(ring, 0)
                    rings.add(ring)
                    anims.add(
                        ObjectAnimator.ofPropertyValuesHolder(
                            ring,
                            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.6f),
                            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.6f),
                            PropertyValuesHolder.ofFloat(View.ALPHA, 0.9f, 0f),
                        ).apply {
                            this.duration = duration
                            startDelay = i * duration / 2
                            repeatCount = ValueAnimator.INFINITE
                            repeatMode = ValueAnimator.RESTART
                            interpolator = DecelerateInterpolator()
                        },
                    )
                }

                val holder = Holder(host, anims, rings)
                active[avatar] = holder
                anims.forEach { it.start() }

                avatar.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(p0: View) {}
                    override fun onViewDetachedFromWindow(p0: View) {
                        active.remove(avatar)?.stop()
                        avatar.removeOnAttachStateChangeListener(this)
                    }
                })
            }
        }
    }

    override fun stop(context: Context) {
        active.values.toList().forEach { it.stop() }
        active.clear()
        patcher.unpatchAll()
    }
}
