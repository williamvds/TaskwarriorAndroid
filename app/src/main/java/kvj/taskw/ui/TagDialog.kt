package kvj.taskw.ui

import java.util.UUID

import android.os.Bundle
import android.text.TextUtils

import kvj.taskw.App
import kvj.taskw.R
import kvj.taskw.data.Controller

import kotlinx.android.synthetic.main.dialog_add_tag.*

class TagDialog : AppDialog() {
    internal var controller = App.controller<Controller>()
    private val form = Form()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        form.load(intent.extras)
        if (!form.valid) finish()

        setContentView(R.layout.dialog_add_tag)

        cancel.setOnClickListener { finish() }
        ok.setOnClickListener { doSave() }
    }

    override fun onBackPressed() = finish()

    private fun doSave() {
        val text = form.tag

        if (TextUtils.isEmpty(text)) { // Nothing to save
            controller.messageShort("Input is mandatory")
            return
        }

        SaveTask(this).execute(text)
    }

    private inner class Form {
        var account: String? = null
        var uuid: UUID? = null

        val tag: String
            get() = text.text.toString()
        val valid: Boolean
            get() = account != null && uuid != null

        fun load(data: Bundle?) {
            data ?: return
            account = data.getString(App.KEY_ACCOUNT)
            uuid    = data.getSerializable(App.KEY_EDIT_UUID) as UUID
        }
    }

    companion object {
        private class SaveTask(activity: TagDialog)
            : StaticAsyncTask<TagDialog, String, Void, String?>(activity) {
            override fun background(context: TagDialog, vararg params: String): String? {
                val accountController = context.controller.accountController(context.form.account)
                return accountController.taskAddTag(context.form.uuid!!, params[0])
            }

            override fun finish(context: TagDialog, result: String?) {
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
