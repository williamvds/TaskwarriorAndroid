package kvj.taskw.ui

import android.content.Context
import android.content.DialogInterface
import android.content.Intent

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import kvj.taskw.App
import kvj.taskw.App.*
import kvj.taskw.R
import kvj.taskw.data.Controller
import kvj.taskw.data.Task
import kvj.taskw.data.Task.Companion.Annotation

import kotlinx.android.synthetic.main.activity_task.*
import kotlinx.android.synthetic.main.item_one_annotation.view.*
import kotlinx.android.synthetic.main.task_tag.view.*
import kvj.taskw.data.AccountController

class TaskActivity : AppActivity() {
    lateinit var task: Task

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        reload()
    }

    private fun setupActions() {
        project_group.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra(App.KEY_ACCOUNT, task.account.toString())
                putExtra(App.KEY_QUERY, "pro:\"${task.project}\"")
            }
            startActivity(intent)
        }

        add_annotation.setOnClickListener {
            val intent = Intent(this, AnnotationDialog::class.java).apply {
                putExtra(App.KEY_ACCOUNT, task.account.toString())
                putExtra(App.KEY_EDIT_UUID, task.uuid)
            }
            startActivityForResult(intent, App.ANNOTATE_REQUEST)
        }

        add_tag.setOnClickListener {
            val intent = Intent(this, TagDialog::class.java).apply {
                putExtra(App.KEY_ACCOUNT, task.account.toString())
                putExtra(App.KEY_EDIT_UUID, task.uuid)
            }
            startActivityForResult(intent, App.TAG_REQUEST)
        }
    }

    private fun populateData() {
        id.text = getString(R.string.task_id, task.id.toString())
        description.text = task.description

        task.due?.let {
            due_group.visibility = View.VISIBLE
            due.text = getString(R.string.due_format, MainListAdapter.formatDate(task.due))
        }

        task.wait?.let {
            wait_group.visibility = View.VISIBLE
            wait.text = getString(R.string.wait_format, MainListAdapter.formatDate(it))
        }

        task.scheduled?.let {
            scheduled_group.visibility = View.VISIBLE
            scheduled.text = getString(R.string.scheduled_format, MainListAdapter.formatDate(it))
        }

        task.recur?.let {
            recur_group.visibility = View.VISIBLE
            recur.text = if (task.until != null) getString(R.string.recur_until_format, it, MainListAdapter.formatDate(task.until))
                else getString(R.string.recur_format, it)
        }

        task.project?.let {
            project_group.visibility = View.VISIBLE
            project.text = it
        }

        annotations.apply {
            isNestedScrollingEnabled = false
            adapter = AnnotationsAdapter(this@TaskActivity, task.annotations)

            no_annotations.visibility = if (task.annotations.isEmpty()) View.VISIBLE else View.GONE
        }

        tags.adapter = TagsAdapter(this, task.tags)
    }

    private fun reload() {
        ReloadTask(this).execute()
    }

    private fun removeTag(tag: String) {
        AlertDialog.Builder(this).apply {
            setMessage(getString(R.string.remove_tag_dialog_format, tag))
            setPositiveButton(android.R.string.yes) { _, _ ->
                EditTask(this@TaskActivity) {
                    taskRemoveTag(task.uuid, tag)
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
                        taskDenotate(activity.task.uuid, annotation.description)
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
            override fun background(context: TaskActivity, vararg params: Void): Task {
                val account = context.task.account
                val controller = App.controller<Controller>().accountController(account.toString())
                return controller.getTask(context.task.uuid)
            }

            override fun finish(context: TaskActivity, result: Task) {
                context.apply {
                    task = result
                    populateData()
                }
            }
        }

        private class EditTask(activity: TaskActivity, val action: AccountController.() -> Unit)
            : StaticAsyncTask<TaskActivity, Void, Void, Unit>(activity) {
            override fun background(context: TaskActivity, vararg params: Void) {
                val account = context.task.account
                val controller = App.controller<Controller>().accountController(account.toString())
                action(controller)
            }

            override fun finish(context: TaskActivity, result: Unit) {
                context.reload()
            }
        }
    }
}
