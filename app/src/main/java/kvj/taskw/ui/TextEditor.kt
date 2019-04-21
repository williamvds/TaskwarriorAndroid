package kvj.taskw.ui

import java.io.OutputStreamWriter
import java.util.Scanner

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem

import timber.log.Timber

import kvj.taskw.R

import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_editor.*
import kotlinx.android.synthetic.main.activity_text_editor.text_editor_input

class TextEditor : AppForm<TextEditor.Form>() {
    override val layout = R.layout.activity_text_editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(toolbar)

        if (!initialized) {
            data = Form(intent.data)
            LoadTask(this).execute()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_text_editor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_tb_save -> submit()
        }
        return true
    }

    override fun submit() {
        super.submit()
        SaveTask(this).execute()
    }

    override fun loadFromForm() {
        text_editor_input.setText(data.text)
        toolbar.subtitle = data.uri?.path.toString()
    }

    override fun saveToForm() {
        data.text = text_editor_input.text.toString()
    }

    override fun hasChanges() = !text_editor_input.text.isNullOrBlank()

    @Parcelize
    data class Form @JvmOverloads constructor(
            var uri: Uri? = null,
            var text: String? = null
    ) : FormData

    companion object {
        private class LoadTask(activity: TextEditor)
            : StaticAsyncTask<TextEditor, Void, Void, String?>(activity) {

            override fun TextEditor.background(vararg params: Void) = try {
                val stream = contentResolver.openInputStream(data.uri!!)
                val s = Scanner(stream).useDelimiter("\\A")
                if (s.hasNext()) s.next() else ""
            } catch (ex: Exception) {
                Timber.e(ex, "Failed to read file")
                null
            }

            override fun TextEditor.finish(result: String?) {
                if (result != null) {
                    Timber.d("File loaded: %s", data.uri)
                    data.text = result
                    loadFromForm()
                } else {
                    controller.messageLong(getString(R.string.error_read_failed))
                    finish()
                }
            }
        }

        private class SaveTask(activity: TextEditor)
            : StaticAsyncTask<TextEditor, Void, Void, Boolean>(activity) {
            override fun TextEditor.background(vararg params: Void) = try {
                val stream = OutputStreamWriter(contentResolver.openOutputStream(data.uri!!))
                stream.write(data.text)
                stream.close()
                true
            } catch (ex: Exception) {
                Timber.e(ex, "Failed to save file")
                false
            }

            override fun TextEditor.finish(result: Boolean) {
                if (result) {
                    setResult(Activity.RESULT_OK)
                    finish()
                    return
                } else
                    controller.messageLong(getString(R.string.error_save_failed))
            }
        }
    }
}
