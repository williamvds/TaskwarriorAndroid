package kvj.taskw.ui

import android.os.Bundle
import android.os.Parcelable
import android.support.v7.app.AlertDialog

import kvj.taskw.App
import kvj.taskw.R
import kvj.taskw.data.Controller

abstract class AppForm<T: FormData>: AppActivity() {
    abstract val layout: Int

    protected lateinit var data: T
    protected var controller: Controller = App.controller()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout)

        if (!loadFromBundle(savedInstanceState))
            loadFromBundle(intent.extras)
    }

    override fun onBackPressed() = cancel()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        saveToBundle(outState)
    }

    protected val initialized: Boolean
        get() = ::data.isInitialized

    private fun loadFromBundle(bundle: Bundle?): Boolean {
        val stored = bundle?.getParcelable<T?>(App.KEY_EDIT_DATA)

        stored?.let {
            data = it
            loadFromForm()
        }

        return stored != null
    }

    private fun saveToBundle(bundle: Bundle) {
        saveToForm()
        bundle.putParcelable(App.KEY_EDIT_DATA, data)
    }

    protected open fun cancel() {
        saveToForm()

        if (hasChanges()) {
            AlertDialog.Builder(this)
                .setMessage(R.string.form_discard_confirm)
                .setPositiveButton(android.R.string.yes) { _, _ -> finish() }
                .setNegativeButton(android.R.string.no, null)
                .show()
        } else
            finish()
    }

    protected open fun submit() {
        saveToForm()
    }

    protected abstract fun loadFromForm()
    protected abstract fun saveToForm()
    protected abstract fun hasChanges(): Boolean

    protected open fun validate() = true
}

interface FormData : Parcelable
