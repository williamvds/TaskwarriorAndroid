package kvj.taskw.ui

import java.util.ArrayList
import java.util.UUID

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View

import timber.log.Timber

import org.kvj.bravo7.form.BundleAdapter
import org.kvj.bravo7.form.FormController
import org.kvj.bravo7.form.impl.ViewFinder
import org.kvj.bravo7.form.impl.bundle.StringBundleAdapter
import org.kvj.bravo7.form.impl.widget.TransientAdapter
import org.kvj.bravo7.util.DataUtil

import kvj.taskw.App
import kvj.taskw.R
import kvj.taskw.data.AccountController
import kvj.taskw.data.Controller
import kvj.taskw.data.UUIDBundleAdapter

import kotlinx.android.synthetic.main.activity_editor.*

class EditorActivity : AppActivity() {
    private val form = FormController(ViewFinder.ActivityViewFinder(this))
    internal var controller = App.controller<Controller>()
    private var priorities = listOf<String>()
    private var progressListener: AccountController.TaskListener? = null
    private var ac: AccountController? = null
    private var editor: Editor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)
        setSupportActionBar(toolbar)

        form.apply {
            add<Any, String>(TransientAdapter(StringBundleAdapter(), null), App.KEY_ACCOUNT)
            add<Any, UUID>  (TransientAdapter(UUIDBundleAdapter(),   null), App.KEY_EDIT_UUID)

            add<Any, Bundle?>(TransientAdapter(object : BundleAdapter<Bundle?>() {
                override fun get(bundle: Bundle?, name: String, def: Bundle?) = bundle?.getBundle(name)
                override fun set(bundle: Bundle?, name: String, value: Bundle?) { bundle?.putBundle(name, value) }
            }, null).oneShot(), App.KEY_EDIT_DATA)

            add<Any, ArrayList<String>>(TransientAdapter(object : BundleAdapter<ArrayList<String>>() {
                override fun get(bundle: Bundle, name: String, def: ArrayList<String>?) = bundle.getStringArrayList(name)
                override fun set(bundle: Bundle, name: String, value: ArrayList<String>?) { bundle.putStringArrayList(name, value) }
            }, null).oneShot(), App.KEY_EDIT_DATA_FIELDS)
        }

        editor = supportFragmentManager.findFragmentById(R.id.editor_editor) as Editor
        editor!!.initForm(form)
        form.load(this@EditorActivity, savedInstanceState)

        ac = controller.accountController(form)
        if (ac == null) {
            finish()
            controller.messageShort("Invalid arguments")
            return
        }

        toolbar.subtitle = ac!!.name()
        progressListener = MainActivity.setupProgressListener(this, progress)
        GetPrioritiesTask(this, savedInstanceState).execute()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        form.save(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)

        val uuid = form.getValue<UUID>(App.KEY_EDIT_UUID)
        menu.findItem(R.id.menu_tb_add_another).isVisible = (uuid != null)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_tb_save ->         doSave(false)
            R.id.menu_tb_add_another ->  doSave(true)
            R.id.menu_tb_add_shortcut -> createShortcut()
        }

        return true
    }

    override fun onResume() {
        super.onResume()
        ac?.listeners()?.add(progressListener, true)
    }

    override fun onPause() {
        super.onPause()
        ac?.listeners()?.remove(progressListener)
    }

    override fun onBackPressed() {
        if (!form.changed()) { // No changes - just close
            super.onBackPressed()
            return
        }

        Timber.d("Changed: %s", form.changes())
        controller.question(this,
            "There are some changes, discard?",
            Runnable { super@EditorActivity.onBackPressed() },
            null)
    }

    private fun createShortcut() {
        val ac = ac
        ac ?: return

        val bundle = Bundle()
        form.save(bundle)
        bundle.remove(App.KEY_EDIT_UUID) // Just in case

        val shortcutIntent = Intent(this, EditorActivity::class.java).apply {
            putExtras(bundle)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val callback = DataUtil.Callback<CharSequence> { value ->
            controller.createShortcut(shortcutIntent, value.toString().trim { it <= ' ' })
            true
        }

        controller.input(this, "Shortcut name:", ac.name(), callback, null)
    }

    private fun propertyChange(key: String, modifier: String): String {
        var value = form.getValue<String>(key)
        if (value.isBlank()) value = ""

        return String.format("%s:%s", modifier, value)
    }

    private fun save(): String? {
        if (!form.changed()) return "Nothing has been changed"

        val description = form.getValue<String>(App.KEY_EDIT_DESCRIPTION)
        if (TextUtils.isEmpty(description)) return "Description is mandatory"

        val uuid      = form.getValue<UUID>(App.KEY_EDIT_UUID)
        val completed = form.getValue(App.KEY_EDIT_STATUS,   Int::class.java) > 0
        val priority  = form.getValue(App.KEY_EDIT_PRIORITY, Int::class.java)

        val changes = ArrayList<String>()
        form.changes().forEach { key ->
            when (key) {
                App.KEY_EDIT_DESCRIPTION ->  changes.add(AccountController.escape(description))
                App.KEY_EDIT_PROJECT ->      changes.add(propertyChange(key, "project"))
                App.KEY_EDIT_DUE ->          changes.add(propertyChange(key, "due"))
                App.KEY_EDIT_SCHEDULED ->    changes.add(propertyChange(key, "scheduled"))
                App.KEY_EDIT_WAIT ->         changes.add(propertyChange(key, "wait"))
                App.KEY_EDIT_UNTIL ->        changes.add(propertyChange(key, "until"))
                App.KEY_EDIT_RECUR ->        changes.add(propertyChange(key, "recur"))
                App.KEY_EDIT_PRIORITY ->     changes.add("priority:${priorities[priority]}")
                App.KEY_EDIT_TAGS -> {
                    val tagsStr = form.getValue<String>(App.KEY_EDIT_TAGS)
                    val tags = tagsStr.split("([,;]\\s*|\\s+)".toRegex())
                    changes.add(String.format("tags:%s", tags.joinToString(",")))
                }
            }
        }

        Timber.d("Saving change: %s %s %s", uuid, changes, completed)

        uuid?.let { return ac!!.taskModify(uuid, changes) }
        return if (completed) ac!!.taskLog(changes) else ac!!.taskAdd(changes)
    }

    private fun doSave(addAnother: Boolean) {
        SaveTask(this, addAnother).execute()
    }

    companion object {
        private class GetPrioritiesTask(activity: EditorActivity,
                                        val bundle: Bundle?)
            : StaticAsyncTask<EditorActivity, Void, Void, List<String>>(activity) {

            override fun EditorActivity.background(vararg params: Void): List<String> {
                return ac!!.taskPriority()
            }

            override fun EditorActivity.finish(result: List<String>) {
                priorities = result
                editor!!.setupPriorities(priorities)
                form.load(this, bundle, App.KEY_EDIT_PRIORITY)
                editor!!.show(form)

                val formData = form.getValue<Bundle>(App.KEY_EDIT_DATA)
                val fields   = form.getValue<List<String>>(App.KEY_EDIT_DATA_FIELDS)

                Timber.d("Edit: %s %s", formData, fields)

                if (formData == null || fields == null) return
                fields.forEach { form.setValue(it, formData.getString(it)) }
            }
        }

        private class SaveTask(context: EditorActivity,
                               val addAnother: Boolean)
            : StaticAsyncTask<EditorActivity, Void, Void, String?>(context) {

            override fun EditorActivity.background(vararg params: Void) = save()

            override fun EditorActivity.finish(result: String?) {
                if (!result.isNullOrBlank()) {
                    controller.messageLong(result)
                    return
                }

                val uuid = form.getValue<UUID>(App.KEY_EDIT_UUID)
                controller.messageShort(
                    getString(if (uuid != null) R.string.edit_task_success else R.string.add_task_success))

                setResult(Activity.RESULT_OK)

                if (!addAnother) {
                    finish()
                    return
                }

                form.setValue(App.KEY_EDIT_DESCRIPTION, "")
                form.getView<View>(App.KEY_EDIT_DESCRIPTION).requestFocus()
            }
        }
    }
}
