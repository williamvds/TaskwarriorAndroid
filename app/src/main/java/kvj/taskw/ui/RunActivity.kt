package kvj.taskw.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.view.MenuItemCompat
import android.support.v7.widget.ShareActionProvider
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View

import timber.log.Timber

import kvj.taskw.App
import kvj.taskw.R
import kvj.taskw.data.AccountController
import kvj.taskw.data.AccountController.TaskListener

import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_run.*

class RunActivity : AppForm<RunActivity.Form>() {
    override val layout = R.layout.activity_run

    private lateinit var ac: AccountController
    private lateinit var progressListener: TaskListener
    private lateinit var shareProvider: ShareActionProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(toolbar)

        input.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode != KeyEvent.KEYCODE_ENTER) return@OnKeyListener false
            if (event.action == KeyEvent.ACTION_DOWN) submit()
            true
        })

        progressListener = MainActivity.setupProgressListener(this, progress)
    }

    override fun onResume() {
        super.onResume()
        ac.listeners().add(progressListener, true)
    }

    override fun onPause() {
        super.onPause()
        ac.listeners().remove(progressListener)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_run, menu)

        val item = menu.findItem(R.id.menu_tb_run_share)
        shareProvider = MenuItemCompat.getActionProvider(item) as ShareActionProvider

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_tb_run_run -> {
                submit()
                return true
            }
            R.id.menu_tb_run_copy -> {
                copyAll()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun submit() {
        super.submit()
        RunTask(this).execute()
    }

    override fun loadFromForm() {
        input.setText(data.input)
        output.text = data.output

        ac = controller.accountController(data.account)
        toolbar.subtitle = ac.name()
        setShareIntent()
    }

    override fun saveToForm() {
        data.input = input.text.toString()
        data.output = output.text.toString()
    }

    override fun hasChanges() = false

    private fun setShareIntent() {
        if (!::shareProvider.isInitialized) {
            Timber.e("Share provider not initialized!")
            return
        }

        shareProvider.setShareIntent(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, output.text)
            type = "text/plain"
        })
    }

    private fun copyAll() {
        controller.copyToClipboard(output.text)
    }

    @Parcelize
    data class Form @JvmOverloads constructor(
            val account: String,
            var input: String? = null,
            var output: String? = null
    ) : FormData

    companion object {
        @JvmStatic
        fun start(activity: Activity, data: Form) {
            val intent = Intent(activity, RunActivity::class.java).apply {
                putExtra(App.KEY_EDIT_DATA, data)
            }

            activity.startActivity(intent)
        }

        private class RunTask(activity: RunActivity)
            : StaticAsyncTask<RunActivity, Void, Void, Boolean>(activity) {
            private val out = AccountController.StringAggregator()
            private val err = AccountController.StringAggregator()

            override fun RunActivity.background(vararg params: Void): Boolean {
                val result = ac.taskCustom(data.input, out, err)
                return result == 0
            }

            override fun RunActivity.finish(result: Boolean) {
                output.text = getString(R.string.command_result_format, out.text(), err.text())
                setShareIntent()
            }
        }
    }
}
