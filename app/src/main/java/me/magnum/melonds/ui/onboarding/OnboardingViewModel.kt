package me.magnum.melonds.ui.onboarding

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import me.magnum.melonds.domain.repositories.SettingsRepository
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    fun markOnboardingComplete() {
        sharedPreferences.edit().putBoolean(PREF_ONBOARDING_COMPLETED, true).apply()
    }

    companion object {
        const val PREF_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}
