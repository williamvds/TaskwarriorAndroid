package kvj.taskw.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.text.TextUtils;

import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.util.Tasks;

import kvj.taskw.App;

/**
 * Created by vorobyev on 11/25/15.
 */
public class SyncIntentReceiver extends BroadcastReceiver {

    Controller controller = App.controller();
    Logger logger = Logger.forInstance(this);

    @Override
    public void onReceive(final Context context, final Intent intent) {
        // Lock and run sync
        final PowerManager.WakeLock lock = controller.lock();
        lock.acquire(10*60*1000L);
        logger.d("Sync from receiver", intent.getData());
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
                logger.d("Sync from receiver done:", s);
                if (null != s) {
                    // Failed
                    controller.messageShort(s);
                }
                lock.release();
            }
        }.exec();
    }
}
