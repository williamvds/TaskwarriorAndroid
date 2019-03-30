package kvj.taskw.ui

import java.lang.Exception
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import android.content.Intent
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.icon_label.view.value

import org.kvj.bravo7.log.Logger

import kvj.taskw.R
import kvj.taskw.data.ReportInfo
import kvj.taskw.data.Task
import kvj.taskw.data.Task.Companion.Status

import kotlinx.android.synthetic.main.item_one_card.view.*
import kotlinx.android.synthetic.main.item_one_task.view.*
import kvj.taskw.App

class MainListAdapter : RecyclerView.Adapter<MainListAdapter.ViewHolder>() {
    var listener: ItemListener? = null
    private val data = ArrayList<Task>()
    private var info: ReportInfo? = null
    private var minUrgency = 0.0
    private var maxUrgency = 0.0


    override fun getItemCount() = data.size

    override fun onCreateViewHolder(parent: ViewGroup, type: Int) =
        ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_one_card, parent, false))

    override fun onBindViewHolder(holder: MainListAdapter.ViewHolder, position: Int) {
        val task = data[position]

        holder.card.apply {
            task_description.text = task.description

            body.setOnClickListener {
                val intent = Intent(context, TaskActivity::class.java).apply {
                    putExtra(App.KEY_TASK, task)
                }
                context.startActivity(intent)
            }

            task_id.text = task.id.toString()

            task_status_btn.setImageResource(getStatusIcon(task.status))
            task_start_stop_btn.setImageResource(if (task.start == null) PROGRESS_ICON_START else PROGRESS_ICON_STOP)

            task_start_flag.visibility = if (task.start != null) View.VISIBLE else View.GONE
            task_start_stop_btn.visibility = if (task.status == Status.PENDING) View.VISIBLE else View.GONE

            task_priority.apply {
                val index = info?.priorities?.indexOf(task.priority) ?: -1
                max = (info?.priorities?.size ?: 0) - 1
                progress = if (index == -1) 0 else max - index - 1
            }

            task_urgency.apply {
                max = maxUrgency.toInt()
                progress = Math.round((task.urgency ?: 0.0) - minUrgency).toInt()
            }

            // Summary
            mapOf<IconLabel, Any?>(
                    due to task.due,
                    project to task.project
            ).forEach {
                it.key.visibility = if (it.value != null) View.VISIBLE else View.GONE
            }

            due.visibility = if (task.due != null) View.VISIBLE else View.GONE
            task.due?.let {
                due.value.text = context.getString(R.string.due_format, MainListAdapter.formatDate(it))
            }

            if (task.annotations.isNotEmpty()) {
                val count = task.annotations.size
                annotation_count.value.text = resources.getQuantityString(R.plurals.annotation_count, count, count)
                annotation_count.visibility = View.VISIBLE
            } else
                annotation_count.visibility = View.GONE

            task.project?.let {
                project.value.text = it
            }

            task_more_btn.setOnClickListener {
                val visibility = if (task_bottom_btns.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                task_bottom_btns.visibility = visibility
            }

            task_edit_btn.setOnClickListener { listener?.onEdit(task) }
            task_status_btn.setOnClickListener { listener?.onStatus(task) }
            task_delete_btn.setOnClickListener { listener?.onDelete(task) }
            task_start_stop_btn.setOnClickListener { listener?.onStartStop(task) }
        }
    }

    fun update(list: List<Task>, info: ReportInfo) {
        this.info = info

        if (!list.isEmpty() && info.fields.containsKey("urgency")) {
            val urgency = List(list.size) { list[it].urgency ?: 0.0 }
            maxUrgency = urgency.max() ?: 0.0
            minUrgency = urgency.min() ?: 0.0
        }

        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: CardView = itemView.card_card
    }

    interface ItemListener {
        fun onEdit(task: Task)
        fun onStatus(task: Task)
        fun onDelete(task: Task)
        fun onAnnotate(task: Task)
        fun onStartStop(task: Task)
        fun onDenotate(task: Task, annotation: Task.Companion.Annotation)
        fun onCopyText(task: Task, text: String)
        fun onLabelClick(task: Task, type: String, longClick: Boolean)
    }

    companion object {
        private fun getStatusIcon(status: Status) = when (status) {
            Status.DELETED -> R.drawable.ic_status_deleted
            Status.COMPLETED -> R.drawable.ic_status_completed
            Status.WAITING -> R.drawable.ic_status_waiting
            Status.RECURRING -> R.drawable.ic_status_recurring
            else -> R.drawable.ic_status_pending
        }

        private const val PROGRESS_ICON_START = R.drawable.ic_action_start
        private const val PROGRESS_ICON_STOP = R.drawable.ic_action_stop

        private val logger = Logger.forClass(MainListAdapter::class.java)

        @JvmField
        val formattedFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        @JvmField
        val formattedFormatDT = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        @JvmField
        val formattedISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)

        @JvmStatic
        @JvmOverloads
        fun formatDate(value: Date?, format: DateFormat? = null): String? {
            value ?: return null

            try {
                format?.let { return format.format(value) }

                val c = Calendar.getInstance()
                c.time = value

                // Just show date if time is midnight
                if (c.get(Calendar.HOUR_OF_DAY) == 0 && c.get(Calendar.MINUTE) == 0) { // 00:00
                    return formattedFormat.format(value)
                }

                return formattedISO.format(value)

            } catch (e: Exception) {
                logger.e(e, "Failed to parse Date:", value)
            }

            return null
        }

        @JvmStatic
        fun join(separator: String, list: Iterable<String>) = if (list.count() < 1) "" else
            list.reduce { string, element ->
                if (TextUtils.isEmpty(element)) string else "$string$separator$element"
            }
    }
}
