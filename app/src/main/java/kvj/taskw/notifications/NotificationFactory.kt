package kvj.taskw.notifications

import kotlin.collections.HashMap

import android.app.PendingIntent;
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat

import kvj.taskw.App;
import kvj.taskw.R;
import kvj.taskw.data.AccountController
import kvj.taskw.notifications.NotificationChannels as C
import kvj.taskw.ui.MainActivity

/**
 * Creates and manages notifications created by the application
 */
class NotificationFactory(val controller: AccountController) {
    private val context = controller.context()
    private val manager = NotificationManagerCompat.from(context)

    /**
     * Set the enabled notification channels from the configuration value
     * `android.sync.notification`
     * @param config comma delimited list or 'all'
     */
    public fun setEnabledChannels(config: String) {
        enabledChannels.clear()

        if (config == "all") return

        config
            .split(',')
            .map(String::trim)
            .forEach { enabledChannels.set(it, true) }
    }

    /**
     * Send a notification in the given channel
     * @param id channel ID
     * @param body notification body text
     * @see NotificationChannels
     */
    @JvmOverloads
    public fun create(id: String, body: String? = null) {
        val syncChannels = arrayOf(
                C.SYNC_ONGOING,
                C.SYNC_SUCCESS,
                C.SYNC_ERROR
        )

        // Cancel all sync notifications when one is being created
        if (id in syncChannels)
            syncChannels.forEach {
                manager.cancel(controller.id(), it.hashCode())
            }

        if (!isChannelEnabled(id)) return

        val n = NotificationCompat.Builder(context, id).apply {
            setSmallIcon(R.drawable.ic_stat_logo)

            val title = context.getString(getTitle(id))
            setContentTitle(title)
            setTicker(title)

            setContentText(body)
            setSubText(controller.name())

            when (id) {
                C.SYNC_ONGOING -> {
                    setOngoing(true)
                }
                C.SYNC_SUCCESS -> {
                    addAction(R.drawable.ic_action_sync,
                        context.getString(R.string.notification_sync_again),
                        controller.syncIntent("notification"))
                }
                C.SYNC_ERROR -> {
                    addAction(R.drawable.ic_action_sync,
                        context.getString(R.string.notification_sync_retry),
                        controller.syncIntent("notification"))
                }
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra(App.KEY_ACCOUNT, controller.id());
            }
            setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT))
        }

        manager.notify(controller.id(), id.hashCode(), n.build())
    }

    private fun getTitle(id: String): Int = when(id) {
        C.SYNC_ONGOING -> R.string.notification_sync_ongoing
        C.SYNC_SUCCESS -> R.string.notification_sync_success
        C.SYNC_ERROR -> R.string.notification_sync_error
        else -> 0
    }

    private fun isChannelEnabled(id: String): Boolean {
        return enabledChannels.isEmpty()
            || enabledChannels.getOrDefault(id, false)
    }

    companion object {
        private var enabledChannels = HashMap<String, Boolean>()
    }
}
