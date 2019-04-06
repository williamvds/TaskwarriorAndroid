package kvj.taskw.ui

import java.lang.Exception
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import timber.log.Timber;

import kvj.taskw.App
import kvj.taskw.R
import kvj.taskw.data.ReportInfo
import kvj.taskw.data.Task

import kotlinx.android.synthetic.main.icon_label.view.*
import kotlinx.android.synthetic.main.item_one_task.view.*

class MainListAdapter : RecyclerView.Adapter<MainListAdapter.ViewHolder>() {
    private val data = ArrayList<Task>()
    private var info: ReportInfo? = null
    private var minUrgency = 0.0
    private var maxUrgency = 0.0

    override fun getItemCount() = data.size

    override fun onCreateViewHolder(parent: ViewGroup, type: Int) =
        ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_one_task, parent, false))

    override fun onBindViewHolder(holder: MainListAdapter.ViewHolder, position: Int) {
        val task = data[position]

        holder.view.apply {
            task_description.text = task.description

            task_id.text = context.getString(R.string.id_format, task.id.toString())

            task_start_flag.visibility = if (task.start != null) View.VISIBLE else View.GONE

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
                    start to task.start,
                    due to task.due,
                    project to task.project
            ).forEach {
                it.key.visibility = if (it.value != null) View.VISIBLE else View.GONE
            }

            task.start?.let {
                start.value.text = context.getString(R.string.started_format, MainListAdapter.formatDate(it))
            }

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

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val view = itemView

        init {
            view.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            val context = view.context
            val task = data[adapterPosition]

            val intent = Intent(context, TaskActivity::class.java).apply {
                putExtra(App.KEY_TASK, task)
            }

            context.startActivity(intent)
        }
    }

    companion object {
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
                Timber.e(e, "Failed to parse date '%s'", value)
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
