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
    open val style = Style.NORMAL
    private var activeTheme: Theme = Theme.LIGHT
    @JvmField var preferences: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        loadPreferences()

        activeTheme = globalTheme
        setTheme(getThemeResource())

        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        if (activeTheme != globalTheme) recreate()
    }

    private fun loadPreferences() {
        if (preferences == null) return

        darkMode    = preferences?.getBoolean(PREF_DARK_MODE, false) ?: false
        globalTheme = if (darkMode == true) Companion.Theme.DARK else Companion.Theme.LIGHT
    }

    open fun getThemeResource() = when (style) {
        Style.NORMAL -> when (activeTheme) {
            Companion.Theme.LIGHT -> R.style.Theme_App_Light
            Companion.Theme.DARK -> R.style.Theme_App_Dark
        }
        Style.DIALOG -> when (activeTheme) {
            Companion.Theme.LIGHT -> R.style.Theme_App_Light_Dialog
            Companion.Theme.DARK -> R.style.Theme_App_Dark_Dialog
        }
    }

    companion object {
        const val PREF_DARK_MODE = "DarkMode"

        enum class Theme {
            LIGHT,
            DARK,
        }

        enum class Style {
            NORMAL,
            DIALOG,
        }

        var darkMode: Boolean? = null
        var globalTheme = Theme.LIGHT
    }
}
