package kvj.taskw.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.TextUtils

import timber.log.Timber

import org.kvj.bravo7.util.Tasks

import kvj.taskw.App

class SyncIntentReceiver : BroadcastReceiver() {
    internal var controller = App.controller<Controller>()

    override fun onReceive(context: Context, intent: Intent) {
        // Lock and run sync
        val lock = controller.lock()
        lock.acquire(10 * 60 * 1000L)

        Timber.d("Sync from receiver: %s", intent.data)

        object : Tasks.SimpleTask<String>() {

            override fun doInBackground(): String? {
                var account: String? = intent.getStringExtra(App.KEY_ACCOUNT)
                if (TextUtils.isEmpty(account)) {
                    account = controller.currentAccount()
                }
                return controller.accountController(account).taskSync()
            }

            override fun onPostExecute(s: String?) {
                Timber.d("Sync from receiver done: %s", s)
                if (null != s) {
                    // Failed
                    controller.messageShort(s)
                }
                lock.release()
            }
        }.exec()
    }
}
