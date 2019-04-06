package kvj.taskw.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.text.TextUtils

import timber.log.Timber

import kvj.taskw.App
import kvj.taskw.ui.StaticAsyncTask

class SyncIntentReceiver : BroadcastReceiver() {
    internal var controller = App.controller<Controller>()

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("Sync from receiver: %s", intent.data)
        SyncTask(this, controller.lock(), intent).execute()
    }

    companion object {
        private class SyncTask(context: SyncIntentReceiver,
                               val lock: PowerManager.WakeLock,
                               val intent: Intent)
            : StaticAsyncTask<SyncIntentReceiver, Void, Void, String?>(context) {

            override fun SyncIntentReceiver.background(vararg params: Void): String? {
                lock.acquire(10 * 60 * 1000L)

                var account: String? = intent.getStringExtra(App.KEY_ACCOUNT)
                if (TextUtils.isEmpty(account)) account = controller.currentAccount()

                return controller.accountController(account).taskSync()
            }

            override fun SyncIntentReceiver.finish(result: String?) {
                Timber.d("Sync from receiver done: %s", result)

                if (result != null) controller.messageShort(result)
                lock.release()
            }

        }
    }
}
