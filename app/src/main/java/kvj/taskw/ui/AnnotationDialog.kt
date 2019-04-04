package kvj.taskw.ui

import java.util.UUID

import android.os.Bundle
import android.text.TextUtils

import org.kvj.bravo7.form.FormController
import org.kvj.bravo7.form.impl.ViewFinder
import org.kvj.bravo7.form.impl.bundle.StringBundleAdapter
import org.kvj.bravo7.form.impl.widget.TextViewCharSequenceAdapter
import org.kvj.bravo7.form.impl.widget.TransientAdapter

import kvj.taskw.App
import kvj.taskw.R
import kvj.taskw.data.Controller
import kvj.taskw.data.UUIDBundleAdapter

import kotlinx.android.synthetic.main.dialog_add_annotation.*

class AnnotationDialog : AppDialog() {
    internal var controller = App.controller<Controller>()

    internal var form = FormController(ViewFinder.ActivityViewFinder(this))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_add_annotation)

        form.add<Any, String>(TransientAdapter(StringBundleAdapter(), null), App.KEY_ACCOUNT)
        form.add<Any, UUID>(TransientAdapter(UUIDBundleAdapter(), null), App.KEY_EDIT_UUID)
        form.add<Any, CharSequence>(TextViewCharSequenceAdapter(R.id.ann_text, ""), App.KEY_EDIT_TEXT)
        form.load(this, savedInstanceState)

        ann_cancel_btn.setOnClickListener { doFinish() }
        ann_ok_btn.setOnClickListener { doSave() }
    }

    override fun onBackPressed() = doFinish()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        form.save(outState)
    }

    private fun doSave() {
        val text = form.getValue<String>(App.KEY_EDIT_TEXT)

        if (TextUtils.isEmpty(text)) { // Nothing to save
            controller.messageShort("Input is mandatory")
            return
        }

        SaveTask(this).execute(text)
    }

    private fun doFinish() {
        if (form.changed()) { // Ask for confirmation
            controller.question(this,
                "There are some changes, discard?",
                Runnable { finish() },
                null)
        } else {
            finish()
        }
    }

    companion object {
        private class SaveTask(activity: AnnotationDialog)
            : StaticAsyncTask<AnnotationDialog, String, Void, String?>(activity) {
            override fun background(context: AnnotationDialog, vararg params: String): String? {
                val account = context.form.getValue(App.KEY_ACCOUNT, String::class.java)
                val uuid = context.form.getValue<UUID>(App.KEY_EDIT_UUID)
                val accountController = context.controller.accountController(account)
                return accountController.taskAnnotate(uuid, params[0])
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
