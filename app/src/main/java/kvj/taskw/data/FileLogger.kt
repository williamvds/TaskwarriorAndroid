package kvj.taskw.data

import android.os.Build
import kvj.taskw.App
import kvj.taskw.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FileLogger(folder: File) {
    private val file = File(folder, App.LOG_FILE)
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    init {
        file.writeText("${Date()}\n")
        log("Device: ${Build.BRAND}, ${Build.MODEL}, ${Build.SUPPORTED_ABIS}, ${Build.VERSION.SDK_INT}")
        log("Application: ${BuildConfig.VERSION_NAME}, ${BuildConfig.VERSION_CODE}")
    }

    fun file() = file

    @Synchronized
    fun log(vararg params: Any?) {
        var str = "${timeFormat.format(Date())}:"
        params.forEach {
            str += " "
            str += when (it) {
                null -> "<NULL>"
                "" -> "<Empty>"
                is Throwable -> android.util.Log.getStackTraceString(it)
                else -> it
            }
        }
        str += "\n"

        file.appendText(str)
    }

    fun logFile(f: File) =
        """
        $f:
        ${if (f.isFile) " file" else " folder"}
        ${if (f.exists()) ", exists" else ""}
        , perm: ${if (f.exists() && f.canRead()) "r" else ""}
        ${if (f.exists() && f.canWrite()) "w" else ""}
        ${if (f.exists()) ", ${f.length()}B" else ""}
        """.trimIndent().filter { it != '\n' }

}
