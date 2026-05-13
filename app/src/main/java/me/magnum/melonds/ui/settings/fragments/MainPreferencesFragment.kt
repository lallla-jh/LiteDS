package me.magnum.melonds.ui.settings.fragments

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import me.magnum.melonds.R
import me.magnum.melonds.ui.about.AboutActivity
import me.magnum.melonds.ui.settings.PreferenceFragmentTitleProvider

@AndroidEntryPoint
class MainPreferencesFragment : BasePreferenceFragment(), PreferenceFragmentTitleProvider {

    override fun getTitle() = getString(R.string.settings)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_main, rootKey)
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
}
