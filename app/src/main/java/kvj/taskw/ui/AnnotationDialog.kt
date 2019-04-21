package kvj.taskw.ui

import android.app.Activity
import android.content.Intent
import java.util.UUID

import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils

import kvj.taskw.App
import kvj.taskw.R
import kvj.taskw.ui.AppActivity.Companion.Style

import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.dialog_add_annotation.*

class AnnotationDialog : AppDialog() {
    override val style = Style.DIALOG

    internal var controller = App.controller<Controller>()
    private lateinit var form: Form

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.dialog_add_annotation)

        if (!loadForm(intent.extras))
            loadForm(savedInstanceState)

        ann_cancel_btn.setOnClickListener { doFinish() }
        ann_ok_btn.setOnClickListener { doSave() }
    }

    override fun onBackPressed() = doFinish()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        saveForm(outState)
    }

    private fun doSave() {
        applyToForm()
        val text = form.annotation

        if (TextUtils.isEmpty(text)) { // Nothing to save
            controller.messageShort("Input is mandatory")
            return
        }

        SaveTask(this).execute(text)
    }

    private fun doFinish() {
        applyToForm()

        if (!TextUtils.isEmpty(form.annotation)) { // Ask for confirmation
            controller.question(this,
                "There are some changes, discard?",
                Runnable { finish() },
                null)
        } else {
            finish()
        }
    }

    private fun loadForm(data: Bundle?): Boolean {
        val stored = data?.getParcelable<Form?>(App.KEY_EDIT_DATA)
        stored?.let { form = it }

        ann_text.setText(form.annotation)

        return stored != null
    }

    private fun applyToForm() {
        form.annotation = ann_text.text.toString()
    }

    private fun saveForm(data: Bundle) {
        applyToForm()
        data.putParcelable(App.KEY_EDIT_DATA, form)
    }

    @Parcelize
    data class Form @JvmOverloads constructor(
            val account: String,
            val uuid: UUID,
            var annotation: String? = null
    ) : Parcelable

    companion object {
        @JvmStatic
        fun start(activity: Activity, form: Form) {
            val intent = Intent(activity, AnnotationDialog::class.java).apply {
                putExtra(App.KEY_EDIT_DATA, form)
            }

            activity.startActivityForResult(intent, App.ANNOTATE_REQUEST)
        }

        private class SaveTask(activity: AnnotationDialog)
            : StaticAsyncTask<AnnotationDialog, String, Void, String?>(activity) {
            override fun AnnotationDialog.background(vararg params: String): String? {
                val accountController = controller.accountController(form.account)
                return accountController.taskAnnotate(form.uuid!!, params[0])
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
