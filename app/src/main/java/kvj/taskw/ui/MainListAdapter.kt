package kvj.taskw.ui

import java.lang.Exception
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

import org.json.JSONArray
import org.json.JSONObject

import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import org.kvj.bravo7.log.Logger

import kvj.taskw.R
import kvj.taskw.data.ReportInfo

import kotlinx.android.synthetic.main.item_one_card.view.*
import kotlinx.android.synthetic.main.item_one_task.view.*
import kotlinx.android.synthetic.main.item_one_annotation.view.*

class MainListAdapter : RecyclerView.Adapter<MainListAdapter.ViewHolder>() {
    var listener: ItemListener? = null
    private val data = ArrayList<JSONObject>()
    private var info: ReportInfo? = null
    private var minUrgency = 0.0
    private var maxUrgency = 0.0


    override fun getItemCount() = data.size

    override fun onCreateViewHolder(parent: ViewGroup, type: Int) =
            ViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_one_card, parent, false))

    override fun onBindViewHolder(holder: MainListAdapter.ViewHolder, position: Int) {
        val json = data[position]

        holder.card.apply {
            val inflater = LayoutInflater.from(context)

            task_description.text = json.optString("description")
            task_id.text = json.optInt("id").toString()

            task_status_btn.setImageResource(STATUS_ICON_MAP[json.optString("status")]
                    ?: STATUS_ICON_DEFAULT)
            task_start_stop_btn.setImageResource(
                    if (json.optString("start").isEmpty()) PROGRESS_ICON_START else PROGRESS_ICON_STOP
            )

            task_start_stop_btn.visibility = when (json.optString("status", "pending")) {
                "pending" -> View.VISIBLE
                else -> View.GONE
            }

            task_priority.apply {
                val index = info?.priorities?.indexOf(json.optString("priority", "")) ?: -1
                max = (info?.priorities?.size ?: 0) - 1
                progress = if (index == -1) 0 else max - index - 1
            }

            task_urgency.apply {
                max = maxUrgency.toInt()
                progress = Math.round(json.optDouble("urgency") - minUrgency).toInt()
            }

            task_labels_left.removeAllViews()
            task_labels_right.removeAllViews()

            fun addLabel(text: CharSequence?, drawable: Int, right: Boolean = false): View? {
                text ?: return null

                val parent = if (right) task_labels_right else task_labels_left

                inflater.inflate(
                        if (right) R.layout.item_one_label_right else R.layout.item_one_label_left,
                        parent
                )

                return parent.getChildAt(parent.childCount - 1).apply {
                    findViewById<TextView>(R.id.label_text).text = text
                    findViewById<ImageView>(R.id.label_icon).setImageResource(drawable)
                }
            }

            addLabel(asDate(json.optString("due", null)), R.drawable.ic_label_due)
            addLabel(asDate(json.optString("wait", null)), R.drawable.ic_label_wait)
            addLabel(asDate(json.optString("scheduled", null)), R.drawable.ic_label_scheduled)

            val recur = json.optString("recur", null)
            if (recur != null) {
                var text = recur

                val until = asDate(json.optString("until"))
                if (!TextUtils.isEmpty(until)) text += " ~ $until"

                addLabel(text, R.drawable.ic_label_recur)
            }

            addLabel(json.optString("project", null), R.drawable.ic_label_project, true)
                    ?.setOnLongClickListener { listener?.onLabelClick(json, "project", true); true }

            val tags = json.optJSONArray("tags")
            if (tags != null) addLabel(join(", ", array2List(tags)), R.drawable.ic_label_tags, true)

            val annotations = json.optJSONArray("annotations")
            if (annotations != null) {
                task_annotations_flag.visibility = View.VISIBLE

                for (i in 0 until annotations.length()) {
                    inflater.inflate(R.layout.item_one_annotation, task_annotations)

                    // inflate() only returns the inflated view the first time, so get it manually
                    task_annotations.getChildAt(i).apply {
                        val annotation = annotations.optJSONObject(annotations.length() - i - 1)
                        val description = annotation.optString("description")

                        task_ann_text.text = description
                        task_ann_date.text = asDate(annotation.optString("entry"), formattedFormatDT)

                        task_ann_text.setOnLongClickListener { listener?.onCopyText(json, description); true }
                        task_ann_delete_btn.setOnClickListener { listener?.onDenotate(json, annotation) }
                    }
                }
            }

            task_more_btn.setOnClickListener {
                val visibility = if (task_bottom_btns.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                task_id.visibility = visibility
                task_bottom_btns.visibility = visibility
                task_annotations.visibility = visibility
            }

            task_edit_btn.setOnClickListener { listener?.onEdit(json) }
            task_status_btn.setOnClickListener { listener?.onStatus(json) }
            task_delete_btn.setOnClickListener { listener?.onDelete(json) }
            task_annotate_btn.setOnClickListener { listener?.onAnnotate(json) }
            task_start_stop_btn.setOnClickListener { listener?.onStartStop(json) }
        }
    }

    fun update(list: List<JSONObject>, info: ReportInfo) {
        this.info = info

        if (!list.isEmpty() && info.fields.containsKey("urgency")) {
            val urgency = ArrayList<Double>()
            list.forEach { urgency.add(it.optDouble("urgency")) }
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
        fun onEdit(json: JSONObject)
        fun onStatus(json: JSONObject)
        fun onDelete(json: JSONObject)
        fun onAnnotate(json: JSONObject)
        fun onStartStop(json: JSONObject)
        fun onDenotate(json: JSONObject, annJson: JSONObject)
        fun onCopyText(json: JSONObject, text: String)
        fun onLabelClick(json: JSONObject, type: String, longClick: Boolean)
    }

    companion object {
        private const val STATUS_ICON_DEFAULT = R.drawable.ic_status_pending
        private val STATUS_ICON_MAP = mapOf(
                "deleted" to R.drawable.ic_status_deleted,
                "completed" to R.drawable.ic_status_completed,
                "waiting" to R.drawable.ic_status_waiting,
                "recurring" to R.drawable.ic_status_recurring
        )
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
        fun array2List(array: JSONArray): Collection<String> {
            val result = ArrayList<String>()
            for (i in 0..array.length()) {
                val str = array.optString(i)
                if (!TextUtils.isEmpty(str)) result.add(str)
            }

            return result
        }

        @JvmStatic
        @JvmOverloads
        fun asDate(value: String?, format: DateFormat? = null): String? {
            value ?: return null

            val jsonFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US)
            jsonFormat.timeZone = TimeZone.getTimeZone("UTC")

            try {
                val parsed = jsonFormat.parse(value)

                if (format != null) return format.format(parsed)

                val c = Calendar.getInstance()
                c.time = parsed

                // Just show date if time is midnight
                if (c.get(Calendar.HOUR_OF_DAY) == 0 && c.get(Calendar.MINUTE) == 0) { // 00:00
                    return formattedFormat.format(parsed)
                }

                return formattedISO.format(parsed)

            } catch (e: Exception) {
                logger.e(e, "Failed to parse Date:", value)
            }

            return null
        }

        @JvmStatic
        fun join(separator: String, list: Iterable<String>) =
                list.reduce { string, element ->
                    if (TextUtils.isEmpty(element)) string else "$string$separator$element"
                }
    }
}
