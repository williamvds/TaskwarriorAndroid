package kvj.taskw.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity

import kvj.taskw.R

/**
 * Base class of all activities of the application
 */
abstract class AppActivity : AppCompatActivity() {
    private var activeTheme: String? = null
    @JvmField var preferences: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        preferences = PreferenceManager.getDefaultSharedPreferences(this)

        loadPreferences()

        activeTheme = globalTheme

        val resource = when (activeTheme) {
            "light" -> R.style.Theme_App_Light
            "dark" -> R.style.Theme_App_Dark
            else -> R.style.Theme_App_Light
        }
        setTheme(resource)

        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        applyThemeChanges()
    }

    private fun loadPreferences() {
        if (preferences == null) return

        darkMode    = preferences?.getBoolean(PREF_DARK_MODE, false) ?: false
        globalTheme = if (darkMode == true) "dark" else "light"
    }

    private fun applyThemeChanges() {
        if (activeTheme != globalTheme) recreate()
    }

    companion object {
        const val PREF_DARK_MODE = "DarkMode"

        var darkMode: Boolean? = null

        var globalTheme: String? = null
    }
}
