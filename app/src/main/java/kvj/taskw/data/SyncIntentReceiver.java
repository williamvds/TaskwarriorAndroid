package kvj.taskw.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.text.TextUtils;

import org.kvj.bravo7.util.Tasks;

import timber.log.Timber;

import kvj.taskw.App;

/**
 * Created by vorobyev on 11/25/15.
 */
public class SyncIntentReceiver extends BroadcastReceiver {

    Controller controller = App.controller();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        // Lock and run sync
        final PowerManager.WakeLock lock = controller.lock();
        lock.acquire(10*60*1000L);
        Timber.d("Sync from receiver: %s", intent.getData());
        new Tasks.SimpleTask<String>() {

            @Override
            protected String doInBackground() {
                String account = intent.getStringExtra(App.KEY_ACCOUNT);
                if (TextUtils.isEmpty(account)) {
                    account = controller.currentAccount();
                }
                return controller.accountController(account).taskSync();
            }

            @Override
            protected void onPostExecute(String s) {
                Timber.d("Sync from receiver done: %s", s);
                if (null != s) {
                    // Failed
                    controller.messageShort(s);
                }
                lock.release();
            }
        }.exec();
    }
}
