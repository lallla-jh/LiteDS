package me.magnum.melonds.ui.settings.fragments

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import me.magnum.melonds.R
import me.magnum.melonds.ui.about.AboutActivity
import me.magnum.melonds.ui.settings.PreferenceFragmentTitleProvider

@AndroidEntryPoint
class MainPreferencesFragment : BasePreferenceFragment(), PreferenceFragmentTitleProvider {

    private val advancedKeys = listOf(
        "pref_category_system",
        "pref_category_video",
        "pref_category_audio",
        "pref_category_input",
        "pref_category_retroachievements",
        "pref_category_cheats",
    )

    override fun getTitle() = getString(R.string.settings)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_main, rootKey)
        applyAdvancedVisibility()
        findPreference<SwitchPreferenceCompat>("show_advanced_settings")
            ?.setOnPreferenceChangeListener { _, newValue ->
                applyAdvancedVisibility(newValue as Boolean)
                true
            }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        return when (preference.key) {
            "about" -> {
                startActivity(Intent(requireContext(), AboutActivity::class.java))
                true
            }
            else -> super.onPreferenceTreeClick(preference)
        }
    }

    private fun applyAdvancedVisibility(
        showAdvanced: Boolean = preferenceManager.sharedPreferences
            ?.getBoolean("show_advanced_settings", false) ?: false
    ) {
        advancedKeys.forEach { key ->
            findPreference<Preference>(key)?.isVisible = showAdvanced
        }
    }
}
