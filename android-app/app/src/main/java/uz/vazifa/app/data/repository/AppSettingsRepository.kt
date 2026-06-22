package uz.vazifa.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uz.vazifa.app.localization.AppLanguage
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore("lider_vazifa_settings")

enum class ThemeMode(val code: String) {
    DARK("dark"),
    LIGHT("light"),
    SYSTEM("system"),
    ;

    companion object {
        fun fromCode(code: String?): ThemeMode = entries.firstOrNull { it.code == code } ?: LIGHT
    }
}

@Singleton
class AppSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val languageKey = stringPreferencesKey("language")
    private val themeModeKey = stringPreferencesKey("theme_mode")

    val language: Flow<AppLanguage> = context.settingsDataStore.data.map { prefs ->
        AppLanguage.fromCode(prefs[languageKey])
    }

    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { prefs ->
        ThemeMode.fromCode(prefs[themeModeKey])
    }

    fun resolvedDark(themeMode: ThemeMode, isSystemDark: Boolean): Boolean = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemDark
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { it[languageKey] = language.code }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[themeModeKey] = mode.code }
    }
}
