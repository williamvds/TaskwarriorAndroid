package kvj.taskw.ui

import kvj.taskw.R

abstract class AppDialog : AppActivity() {
    override fun getThemeResource() = when (activeTheme) {
        Companion.Theme.LIGHT -> R.style.Theme_App_Light_Dialog
        Companion.Theme.DARK -> R.style.Theme_App_Dark_Dialog
    }
}
