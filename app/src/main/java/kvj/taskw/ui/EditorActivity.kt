package kvj.taskw.ui

import java.util.Calendar
import java.util.Date

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText

import timber.log.Timber

import org.kvj.bravo7.util.DataUtil

import kvj.taskw.data.Task
import kvj.taskw.data.Task.Companion.Status
import kvj.taskw.App
import kvj.taskw.R
import kvj.taskw.data.AccountController

import kotlinx.android.synthetic.main.activity_editor.*
import kotlinx.android.parcel.Parcelize
import java.lang.Exception

class EditorActivity : AppForm<EditorActivity.Form>() {
    override val layout = R.layout.activity_editor

    private var priorities = listOf<String>()
    private lateinit var progressListener: AccountController.TaskListener
    private lateinit var ac: AccountController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(toolbar)

        progressListener = MainActivity.setupProgressListener(this, progress)
        GetPrioritiesTask(this).execute()

        setupDatePicker(editor_due_btn, due)
        setupDatePicker(editor_wait_btn, wait)
        setupDatePicker(editor_scheduled_btn, scheduled)
        setupDatePicker(editor_until_btn, until)
    }

    override fun onResume() {
        super.onResume()
        ac.listeners().add(progressListener, true)
    }

    override fun onPause() {
        super.onPause()
        ac.listeners().remove(progressListener)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        menu.findItem(R.id.menu_tb_add_another).isVisible = (data.task != null)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_tb_save ->         submit(false)
            R.id.menu_tb_add_another ->  submit(false)
            R.id.menu_tb_add_shortcut -> createShortcut()
        }

        return true
    }

    override fun loadFromForm() {
        ac = controller.accountController(data.account)
        toolbar.subtitle = ac.name()

        val task = data.task ?: return

        description.setText(task.description)
        complete.isChecked = task.status == Status.COMPLETED
        project.setText(task.project)
        tags.setText(MainListAdapter.join(",", task.tags))
        recur.setText(task.recur)

        due.setText(data.due ?: MainListAdapter.formatDate(task.due))
        wait.setText(data.wait ?: MainListAdapter.formatDate(task.wait))
        scheduled.setText(data.scheduled ?: MainListAdapter.formatDate(task.scheduled))
        until.setText(data.until ?: MainListAdapter.formatDate(task.until))

        data.original = data.original ?: task.copy()
    }

    override fun saveToForm() {
        data.task?.let {
            it.description = description.text.toString()
            it.status = if (complete.isChecked) Status.COMPLETED else Status.PENDING
            it.project = project.text.toString()
            it.tags = tags.text.toString().split("([,;]\\s*|\\s+)".toRegex())
            it.priority = priority.selectedItem as String
            it.recur = recur.text.toString()
        }

        data.let {
            it.due = due.text.toString()
            it.wait = wait.text.toString()
            it.scheduled = scheduled.text.toString()
            it.until = until.text.toString()
        }

        data.let {
            it.due = due.text.toString()
            it.wait = wait.text.toString()
            it.scheduled = scheduled.text.toString()
            it.until = until.text.toString()
        }
    }

    override fun hasChanges(): Boolean {
        return !data.due.isNullOrEmpty()
                || !data.wait.isNullOrEmpty()
                || !data.scheduled.isNullOrEmpty()
                || !data.until.isNullOrEmpty()
                || (data.task?.equals(data.original) ?: true)
    }

    private fun submit(addAnother: Boolean) {
        super.submit()
        SaveTask(this, addAnother).execute()
    }

    private fun createShortcut() {
        saveToForm()

        val shortcutIntent = Intent(this, EditorActivity::class.java).apply {
            putExtra(App.KEY_EDIT_DATA, data)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val callback = DataUtil.Callback<CharSequence> { value ->
            controller.createShortcut(shortcutIntent, value.toString().trim { it <= ' ' })
            true
        }

        controller.input(this, "Shortcut name:", ac.name(), callback, null)
    }

    private fun setupDatePicker(button: View, input: EditText) {
        fun getCalendar(input: EditText): Calendar {
            val date = try {
                MainListAdapter.formattedFormat.parse(input.text.toString().trim()) ?: Date()
            } catch (ex: Exception) {
                Date()
            }

            return Calendar.getInstance().apply { time = date }
        }

        button.setOnClickListener {
            val calendar = getCalendar(input)

            DatePickerDialog(this, { _, year, month, day ->
                calendar.set(year, month, day)
                input.setText(MainListAdapter.formattedFormat.format(calendar.time))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        button.setOnLongClickListener {
            val calendar = getCalendar(input)
            val format24 = DateFormat.is24HourFormat(this)

            TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                calendar.apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }

                input.setText(MainListAdapter.formattedISO.format(calendar.time))
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), format24).show()

            true
        }
    }

    fun save(): String? {
        val changes = mutableListOf<String>()

        val map = mapOf(
            "description" to Pair(data.task?.description, data.original?.description),
            "project" to Pair(data.task?.project, data.original?.project),
            "priority" to Pair(data.task?.priority, data.original?.priority),
            "due" to Pair(data.due, data.original?.due),
            "scheduled" to Pair(data.scheduled, data.original?.scheduled),
            "wait" to Pair(data.wait, data.original?.wait),
            "until" to Pair(data.until, data.original?.until),
            "recur" to Pair(data.task?.recur, data.original?.recur),
            "tags" to Pair(data.task?.tags?.joinToString(separator = ","), data.original?.tags?.joinToString(separator = ","))
        )

        for ((key, pair) in map) {
            if (pair.first == pair.second) continue

            val value = when (pair.first) {
                null -> ""
                else -> AccountController.escape(pair.first.toString())
            }

            changes.add("$key:$value")
        }

        val completed = data.task?.status == Status.COMPLETED
        Timber.d("Saving change: UUID %s, changes %s %s", data.task?.uuid, changes, completed)

        data.task?.uuid?.let { return ac.taskModify(it, changes) }
        return if (completed) ac.taskLog(changes) else ac.taskAdd(changes)
    }

    @Parcelize
    data class Form @JvmOverloads constructor(
            val account: String,
            var task: Task? = null,
            var original: Task? = null,
            var due: String? = null,
            var wait: String? = null,
            var scheduled: String? = null,
            var until: String? = null
    ) : FormData

    companion object {
        @JvmStatic
        fun start(activity: Activity, data: Form) {
            val intent = Intent(activity, EditorActivity::class.java).apply {
                putExtra(App.KEY_EDIT_DATA, data)
            }

            activity.startActivityForResult(intent, App.EDIT_REQUEST)
        }

        private class GetPrioritiesTask(activity: EditorActivity)
            : StaticAsyncTask<EditorActivity, Void, Void, List<String>>(activity) {

            override fun EditorActivity.background(vararg params: Void): List<String>
                = ac.taskPriority()

            override fun EditorActivity.finish(result: List<String>) {
                priorities = result
                priority.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, priorities)
                priority.setSelection(priorities.indexOf(data.task?.priority ?: ""))
            }
        }

        private class SaveTask(context: EditorActivity,
                               val addAnother: Boolean)
            : StaticAsyncTask<EditorActivity, Void, Void, String?>(context) {

            override fun EditorActivity.background(vararg params: Void): String? {
                return save()
            }

            override fun EditorActivity.finish(result: String?) {
                if (!result.isNullOrBlank()) {
                    controller.messageLong(result)
                    return
                }

                controller.messageShort(
                    getString(if (data.task != null) R.string.edit_task_success else R.string.add_task_success))

                setResult(Activity.RESULT_OK)

                if (!addAnother) {
                    finish()
                    return
                }

                data.task = null
                description.requestFocus()
            }
        }
    }
}
