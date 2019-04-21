package kvj.taskw.ui

import java.util.UUID

import android.app.Activity
import android.content.Intent
import android.os.Bundle

import kvj.taskw.App
import kvj.taskw.R
import kvj.taskw.ui.AppActivity.Companion.Style

import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.dialog_add_annotation.*

class AnnotationDialog : AppForm<AnnotationDialog.Form>() {
    override val style = Style.DIALOG
    override val layout = R.layout.dialog_add_annotation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ann_cancel_btn.setOnClickListener { cancel() }
        ann_ok_btn.setOnClickListener { submit() }
    }

    override fun submit() {
        super.submit()
        SaveTask(this).execute(data.annotation)
    }

    override fun loadFromForm() {
        ann_text.setText(data.annotation)
    }

    override fun saveToForm() {
        data.annotation = ann_text.text.toString()
    }

    override fun hasChanges() = !data.annotation.isNullOrBlank()

    @Parcelize
    data class Form @JvmOverloads constructor(
        val account: String,
        val uuid: UUID,
        var annotation: String? = null
    ) : FormData

    companion object {
        @JvmStatic
        fun start(activity: Activity, data: Form) {
            val intent = Intent(activity, AnnotationDialog::class.java).apply {
                putExtra(App.KEY_EDIT_DATA, data)
            }

            activity.startActivityForResult(intent, App.ANNOTATE_REQUEST)
        }

        private class SaveTask(activity: AnnotationDialog)
            : StaticAsyncTask<AnnotationDialog, String, Void, String?>(activity) {
            override fun AnnotationDialog.background(vararg params: String): String? {
                val accountController = controller.accountController(data.account)
                return accountController.taskAnnotate(data.uuid, params[0])
            }

            override fun AnnotationDialog.finish(result: String?) {
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
