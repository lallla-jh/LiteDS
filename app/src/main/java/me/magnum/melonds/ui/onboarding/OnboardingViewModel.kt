package me.magnum.melonds.ui.onboarding

import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import me.magnum.melonds.domain.repositories.SettingsRepository
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    fun addRomDirectory(uri: Uri) {
        viewModelScope.launch {
            settingsRepository.addRomSearchDirectory(uri)
        }
    }

    fun markOnboardingComplete() {
        sharedPreferences.edit().putBoolean(PREF_ONBOARDING_COMPLETED, true).apply()
    }

    companion object {
        const val PREF_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}
