package kvj.taskw.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import timber.log.Timber;

import kvj.taskw.App;

/**
 * Created by kvorobyev on 11/25/15.
 */
public class BootReceiver extends BroadcastReceiver {

    Controller controller = App.controller();

    @Override
    public void onReceive(Context context, Intent intent) {
        Timber.i("Application started");
        controller.messageShort("Auto-sync timers have started");
    }
}
