package kvj.taskw.ui

import java.util.UUID

import android.app.Activity
import android.content.Intent
import android.os.Bundle

import kvj.taskw.App
import kvj.taskw.R
import kvj.taskw.ui.AppActivity.Companion.Style

import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.dialog_add_tag.*

class TagDialog : AppForm<TagDialog.Form>() {
    override val style = Style.DIALOG
    override val layout = R.layout.dialog_add_tag

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cancel.setOnClickListener { cancel() }
        ok.setOnClickListener { submit() }
    }

    override fun submit() {
        super.submit()

        if (validate())
            SaveTask(this).execute(data.tag)
    }

    override fun loadFromForm() {
        text.setText(data.tag)
    }

    override fun saveToForm() {
        data.tag = text.text.toString()
    }

    override fun hasChanges() = !data.tag.isNullOrBlank()

    override fun validate(): Boolean {
        val tag = data.tag
        if (tag.isNullOrBlank()) {
            controller.messageShort(getString(R.string.error_no_input))
            return false
        }
        else if (tag.contains("\\s".toRegex())) {
            controller.messageShort(getString(R.string.error_tag_whitespace))
            return false
        }

        return true
    }

    @Parcelize
    data class Form @JvmOverloads constructor(
            val account: String,
            val uuid: UUID,
            var tag: String? = null
    ) : FormData

    companion object {
        @JvmStatic
        fun start(activity: Activity, data: Form) {
            val intent = Intent(activity, TagDialog::class.java).apply {
                putExtra(App.KEY_EDIT_DATA, data)
            }

            activity.startActivityForResult(intent, App.TAG_REQUEST)
        }

        private class SaveTask(activity: TagDialog)
            : StaticAsyncTask<TagDialog, String, Void, String?>(activity) {
            override fun TagDialog.background(vararg params: String): String? {
                val accountController = controller.accountController(data.account)
                return accountController.taskAddTag(data.uuid, params[0])
            }

            override fun TagDialog.finish(result: String?) {
                if (result != null) { // Error
                    controller.messageShort(result)
                } else {
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }
    }
}
