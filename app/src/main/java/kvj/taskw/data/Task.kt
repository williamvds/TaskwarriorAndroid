package kvj.taskw.data

import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.text.SimpleDateFormat

import org.json.JSONArray
import org.json.JSONObject

import android.os.Parcelable

import kotlinx.android.parcel.Parcelize

@Parcelize
data class Task(
    @JvmField var account: UUID? = null,
    @JvmField var uuid: UUID? = null,
    @JvmField var id: Int? = null,
    @JvmField var description: String? = null,
    @JvmField var entry: Date? = null,
    @JvmField var status: Status? = null,
    @JvmField var annotations: List<Annotation> = ArrayList(),
    @JvmField var depends: List<UUID> = ArrayList(),
    @JvmField var tags: List<String> = ArrayList(),
    @JvmField var urgency: Double? = null,
    @JvmField var priority: String? = null,
    @JvmField var project: String? = null,
    @JvmField var due: Date? = null,
    @JvmField var recur: String? = null,
    @JvmField var until: Date? = null,
    @JvmField var wait: Date? = null,
    @JvmField var scheduled: Date? = null,
    @JvmField var start: Date? = null
) : Parcelable {
    companion object {
        enum class Status {
            PENDING,
            COMPLETED,
            WAITING,
            RECURRING,
            DELETED,
        }

        @Parcelize
        data class Annotation(@JvmField val entry: Date, @JvmField val description: String) : Parcelable {
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
        fun fromJSON(json: JSONObject, account: UUID) = Task(
            account = account,
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

