package me.magnum.melonds.ui.common.componentbuilders

import android.content.Context
import android.view.View
import me.magnum.melonds.ui.common.LayoutComponentViewBuilder
import me.magnum.melonds.ui.emulator.input.view.JoystickView

class JoystickLayoutComponentViewBuilder : LayoutComponentViewBuilder() {
    override fun build(context: Context): View = JoystickView(context)

    override fun getAspectRatio() = 1f
}
