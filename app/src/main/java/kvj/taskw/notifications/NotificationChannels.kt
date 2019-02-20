package kvj.taskw.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

import kvj.taskw.R

/**
 * Factory for creating the application's notification channels
 */
class NotificationChannels {
    companion object {
        const val SYNC_ONGOING = "SyncOngoing"
        const val SYNC_SUCCESS = "SyncSuccess"
        const val SYNC_ERROR   = "SyncError"

        /**
         * Create all notification channels
         */
        @JvmStatic
        fun create(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                return

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

            NotificationChannel(SYNC_ONGOING,
                context.getString(R.string.channel_sync_ongoing_name),
                NotificationManager.IMPORTANCE_MIN).apply {
                description = context.getString(R.string.channel_sync_ongoing_desc)
                manager.createNotificationChannel(this)
            }

            NotificationChannel(SYNC_SUCCESS,
                context.getString(R.string.channel_sync_success_name),
                NotificationManager.IMPORTANCE_MIN).apply {
                description = context.getString(R.string.channel_sync_success_desc)
                manager.createNotificationChannel(this)
            }

            NotificationChannel(SYNC_ERROR,
                context.getString(R.string.channel_sync_error_name),
                NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.channel_sync_error_desc)
                manager.createNotificationChannel(this)
            }
        }
    }
}
