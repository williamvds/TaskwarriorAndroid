package kvj.taskw.ui

import android.content.Context
import android.content.DialogInterface
import android.content.Intent

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup

import kvj.taskw.App
import kvj.taskw.App.*
import kvj.taskw.R
import kvj.taskw.data.Controller
import kvj.taskw.data.Task
import kvj.taskw.data.Task.Companion.Annotation
import kvj.taskw.data.Task.Companion.Status

import kotlinx.android.synthetic.main.activity_task.*
import kotlinx.android.synthetic.main.item_one_annotation.view.*
import kotlinx.android.synthetic.main.icon_label.view.*
import kotlinx.android.synthetic.main.task_tag.view.*
import kvj.taskw.data.AccountController

class TaskActivity : AppActivity() {
    lateinit var task: Task
    var editExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task)

        val bundledTask: Task? = intent?.getParcelableExtra(KEY_TASK)

        if (bundledTask == null) {
            finish()
            return
        }

        task = bundledTask

        setupActions()
        populateData()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (editExpanded) {
            hideEditList()
            return true
        }

        return super.onTouchEvent(event)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        hideEditList()

        if (resultCode != RESULT_OK) return
        reload()
    }

    private fun setupActions() {
        project.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra(App.KEY_ACCOUNT, task.account.toString())
                putExtra(App.KEY_QUERY, "pro:\"${task.project}\"")
            }
            startActivity(intent)
        }

        add_tag.setOnClickListener {
            val intent = Intent(this, TagDialog::class.java).apply {
                putExtra(App.KEY_ACCOUNT, task.account.toString())
                putExtra(App.KEY_EDIT_UUID, task.uuid)
            }
            startActivityForResult(intent, App.TAG_REQUEST)
        }

        edit.setOnClickListener {
            if (editExpanded) {
                EditTask(this@TaskActivity) {
                    val intent = Intent(this, EditorActivity::class.java)
                    it.intentForEditor(intent, task.uuid)
                    startActivityForResult(intent, App.EDIT_REQUEST)
                }.execute()
            }

            editExpanded = !editExpanded
            updateEditList()
        }

        done.setOnClickListener {
            EditTask(this@TaskActivity) {
                it.taskDone(task.uuid)
                finish()
            }.execute()

            hideEditList()
        }

        start_stop.setOnClickListener {
            EditTask(this@TaskActivity) {
                if (task.start == null) it.taskStart(task.uuid) else it.taskStop(task.uuid)
            }.execute()

            hideEditList()
        }

        annotate.setOnClickListener {
            val intent = Intent(this, AnnotationDialog::class.java).apply {
                putExtra(App.KEY_ACCOUNT, task.account.toString())
                putExtra(App.KEY_EDIT_UUID, task.uuid)
            }

            startActivityForResult(intent, App.ANNOTATE_REQUEST)
            hideEditList()
        }

        delete.setOnClickListener {
            EditTask(this@TaskActivity) {
                it.taskDelete(task.uuid)
                finish()
            }.execute()

            hideEditList()
        }
    }

    private fun updateEditList() {
        edit_submenu.visibility = if (editExpanded) View.VISIBLE else View.GONE
    }

    private fun hideEditList() {
        editExpanded = false
        updateEditList()
    }

    private fun populateData() {
        id.text = getString(R.string.id_format, task.id.toString())
        description.text = task.description

        mapOf<IconLabel, Any?>(
            due to task.due,
            wait to task.wait,
            scheduled to task.scheduled,
            recur to task.recur,
            project to task.project
        ).forEach {
            it.key.visibility = if (it.value != null) View.VISIBLE else View.GONE
        }

        task.due?.let {
            due.value.text = getString(R.string.due_format, MainListAdapter.formatDate(it))
        }

        task.wait?.let {
            wait.value.text = getString(R.string.wait_format, MainListAdapter.formatDate(it))
        }

        task.scheduled?.let {
            scheduled.value.text = getString(R.string.scheduled_format, MainListAdapter.formatDate(it))
        }

        task.recur?.let {
            recur.value.text = if (task.until != null) getString(R.string.recur_until_format, it, MainListAdapter.formatDate(task.until))
                else getString(R.string.recur_format, it)
        }

        task.project?.let {
            project.value.text = it
        }

        annotations.apply {
            isNestedScrollingEnabled = false
            adapter = AnnotationsAdapter(this@TaskActivity, task.annotations)

            no_annotations.visibility = if (task.annotations.isEmpty()) View.VISIBLE else View.GONE
        }

        tags.adapter = TagsAdapter(this, task.tags)

        (done as View).visibility = if (task.status == Status.PENDING) View.VISIBLE else View.GONE
        start_stop.setImageResource(if (task.start == null) R.drawable.ic_action_start else R.drawable.ic_action_stop)
        start_stop_label_text.text = getString(if (task.start == null) R.string.action_start else R.string.action_stop)
    }

    private fun reload() {
        ReloadTask(this).execute()
    }

    private fun removeTag(tag: String) {
        AlertDialog.Builder(this).apply {
            setMessage(getString(R.string.remove_tag_dialog_format, tag))
            setPositiveButton(android.R.string.yes) { _, _ ->
                EditTask(this@TaskActivity) {
                    it.taskRemoveTag(task.uuid, tag)
                }.execute()
            }
            setNegativeButton(android.R.string.no, null)
            show()
        }
    }

    private class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    private class AnnotationsAdapter(private val activity: TaskActivity, private val data: List<Annotation>)
        : RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context)
                                     .inflate(R.layout.item_one_annotation, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, index: Int) {
            val annotation = data[index]
            holder.view.apply {
                task_ann_text.text = annotation.description
                task_ann_date.text = MainListAdapter.formatDate(annotation.entry)
                task_ann_delete_btn.setOnClickListener {
                    EditTask(activity) {
                        it.taskDenotate(activity.task.uuid, annotation.description)
                    }.execute()
                }
            }
        }

        override fun getItemCount() = data.size
    }

    private inner class TagsAdapter(private val activity: TaskActivity, private val data: List<String>)
        : RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                ViewHolder(LayoutInflater.from(parent.context)
                                         .inflate(R.layout.task_tag, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, index: Int) {
            val tag = data[index]
            holder.view.apply {
                name.text = getString(R.string.tag_format, tag)
                setOnClickListener {
                    val intent = Intent(activity, MainActivity::class.java).apply {
                        putExtra(App.KEY_ACCOUNT, task.account.toString())
                        putExtra(App.KEY_QUERY, "+$tag")
                    }
                    startActivity(intent)
                }
                setOnLongClickListener { activity.removeTag(tag); true }
            }
        }

        override fun getItemCount() = data.size
    }

    companion object {
        private class ReloadTask(activity: TaskActivity)
            : StaticAsyncTask<TaskActivity, Void, Void, Task>(activity) {
            override fun TaskActivity.background(vararg params: Void): Task {
                val account = task.account
                val controller = App.controller<Controller>().accountController(account.toString())
                return controller.getTask(task.uuid)
            }

            override fun TaskActivity.finish(result: Task) {
                task = result
                populateData()
            }
        }

        private class EditTask(activity: TaskActivity, val action: TaskActivity.(AccountController) -> Unit)
            : StaticAsyncTask<TaskActivity, Void, Void, Unit>(activity) {
            override fun TaskActivity.background(vararg params: Void) {
                val account = task.account
                val controller = App.controller<Controller>().accountController(account.toString())
                action(controller)
            }

            override fun TaskActivity.finish(result: Unit) {
                reload()
            }
        }
    }
}
