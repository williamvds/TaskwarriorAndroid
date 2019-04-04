package kvj.taskw.ui

import java.util.UUID

import android.os.Bundle
import android.text.TextUtils

import kvj.taskw.App
import kvj.taskw.R
import kvj.taskw.data.Controller

import kotlinx.android.synthetic.main.dialog_add_annotation.*

class AnnotationDialog : AppDialog() {
    internal var controller = App.controller<Controller>()
    private val form = Form()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.dialog_add_annotation)

        form.load(intent.extras)
        if (!form.valid) form.load(savedInstanceState)
        if (!form.valid) finish()

        ann_cancel_btn.setOnClickListener { doFinish() }
        ann_ok_btn.setOnClickListener { doSave() }
    }

    override fun onBackPressed() = doFinish()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        form.save(outState)
    }

    private fun doSave() {
        val text = form.annotation

        if (TextUtils.isEmpty(text)) { // Nothing to save
            controller.messageShort("Input is mandatory")
            return
        }

        SaveTask(this).execute(text)
    }

    private fun doFinish() {
        if (!TextUtils.isEmpty(form.annotation)) { // Ask for confirmation
            controller.question(this,
                "There are some changes, discard?",
                Runnable { finish() },
                null)
        } else {
            finish()
        }
    }

    private inner class Form {
        var account: String? = null
        var uuid: UUID? = null

        var annotation: String
            get() = ann_text.text.toString()
            set(value) { ann_text.setText(value) }
        val valid: Boolean
            get() = account != null && uuid != null

        fun load(data: Bundle?) {
            data ?: return
            account    = data.getString(App.KEY_ACCOUNT)
            uuid       = data.getSerializable(App.KEY_EDIT_UUID) as UUID
            annotation = data.getString(App.KEY_TEXT_INPUT) ?: ""
        }

        fun save(data: Bundle) {
            data.putString(App.KEY_ACCOUNT, account)
            data.putSerializable(App.KEY_EDIT_UUID, uuid)
            data.putString(App.KEY_TEXT_INPUT, annotation)
        }
    }

    companion object {
        private class SaveTask(activity: AnnotationDialog)
            : StaticAsyncTask<AnnotationDialog, String, Void, String?>(activity) {
            override fun background(context: AnnotationDialog, vararg params: String): String? {
                val accountController = context.controller.accountController(context.form.account)
                return accountController.taskAnnotate(context.form.uuid!!, params[0])
            }

            override fun finish(context: AnnotationDialog, result: String?) {
                if (result != null) { // Error
                    context.controller.messageShort(result)
                } else {
                    context.setResult(RESULT_OK)
                    context.finish()
                }
            }
        }
    }
}
