package kvj.taskw.data

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

data class Task(
    @JvmField val uuid: UUID,
    @JvmField val id: Int,
    @JvmField val description: String,
    @JvmField val entry: Date,
    @JvmField val status: Status,
    @JvmField val annotations: List<Annotation> = ArrayList(),
    @JvmField val depends: List<UUID> = ArrayList(),
    @JvmField val tags: List<String> = ArrayList(),
    @JvmField val urgency: Double? = null,
    @JvmField val priority: String? = null,
    @JvmField val project: String? = null,
    @JvmField val due: Date? = null,
    @JvmField val recur: String? = null,
    @JvmField val until: Date? = null,
    @JvmField val wait: Date? = null,
    @JvmField val scheduled: Date? = null,
    @JvmField val start: Date? = null
) {
    companion object {
        enum class Status {
            PENDING,
            COMPLETED,
            WAITING,
            RECURRING,
            DELETED,
        }

        data class Annotation(@JvmField val entry: Date, @JvmField val description: String) {
            companion object {
                fun fromJSON(json: JSONObject) = Annotation(
                    entry = getDate(json.getString("entry"))!!,
                    description = json.getString("description")
                )
            }
        }

        private fun getDate(string: String?): Date? {
            string ?: return null

            val format = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC")
            return format.parse(string)
        }

        private fun <K> getList(array: JSONArray?, init: (array: JSONArray, index: Int) -> K): List<K> = when (array) {
            null -> listOf()
            else -> List(array.length()) { init(array, it) }
        }

        private fun getStatus(string: String) = when (string) {
            "pending" -> Status.PENDING
            "completed" -> Status.COMPLETED
            "waiting" -> Status.WAITING
            "recurring" -> Status.RECURRING
            "deleted" -> Status.DELETED
            else -> Status.PENDING
        }

        @JvmStatic
        fun fromJSON(json: JSONObject) = Task(
            uuid = UUID.fromString(json.getString("uuid")),
            id = json.getInt("id"),
            description = json.getString("description"),
            entry = getDate(json.getString("entry"))!!,
            status = getStatus(json.getString("status")),
            annotations = getList(json.optJSONArray("annotations")) { array, index ->
                Annotation.fromJSON(array.getJSONObject(index))
            },
            depends = getList(json.optJSONArray("depends")) { array, index ->
                UUID.fromString(array.getString(index))
            },
            tags = getList(json.optJSONArray("tags")) { array, index -> array.getString(index) },
            urgency = json.getDouble("urgency"),
            priority = json.optString("priority", null),
            project = json.optString("project", null),
            due = getDate(json.optString("due", null)),
            recur = json.optString("recur", null),
            until = getDate(json.optString("until", null)),
            wait = getDate(json.optString("wait", null)),
            scheduled = getDate(json.optString("scheduled", null)),
            start = getDate(json.optString("start", null))
        )
    }
}

