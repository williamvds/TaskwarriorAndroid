package kvj.taskw.ui

import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.os.Bundle
import android.widget.ArrayAdapter

import kvj.taskw.App
import kvj.taskw.R
import kvj.taskw.data.Controller

import kotlinx.android.synthetic.main.dialog_add_account.*

class AccountAddDialog : AppDialog() {
    private var controller = App.controller<Controller>()
    private var response: AccountAuthenticatorResponse? = null
    private var result = Bundle()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_add_account)

        response = intent.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
        response?.apply { onRequestContinued() }

        val folders = controller.accountFolders()
        folders.add(0, getString(R.string.data_new_folder))

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, folders).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        account_folder_select.adapter = adapter

        ok_button.setOnClickListener {
            val name = account_name_input.text.toString()
            val folderIndex = account_folder_select.selectedItemPosition
            val err = controller.createAccount(name, if (folderIndex > 0) folders[folderIndex] else null)

            err?.let {
                controller.messageLong(err)
                return@setOnClickListener
            }

            result.apply {
                putString(AccountManager.KEY_ACCOUNT_NAME, name)
                putString(AccountManager.KEY_ACCOUNT_TYPE, App.ACCOUNT_TYPE)
            }

            finish()
        }

        cancel_button.setOnClickListener { finish() }
    }

    override fun finish() {
        response?.let {
            // send the result bundle back if set, otherwise send an error.
            if (result.isEmpty) {
                response!!.onResult(result)
            } else {
                response!!.onError(AccountManager.ERROR_CODE_CANCELED, "canceled")
            }

            response = null
        }

        super.finish()
    }
}
