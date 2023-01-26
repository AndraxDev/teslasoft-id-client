package org.teslasoft.id.example

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.teslasoft.core.auth.AccountSyncListener

class MainActivity : FragmentActivity() {

    private val accountDataListener: AccountSyncListener = object :
        AccountSyncListener {
        override fun onAuthFinished(name: String, email: String, isDev: Boolean, token: String) { /* Auth finished */ }

        override fun onAuthCanceled() { /* Auth canceled */ }

        override fun onSignedOut() { /* User signed out */ }

        override fun onAuthFailed(state: String) { /* Auth failed */
            MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("Error")
                    .setMessage(state)
                    .setPositiveButton("Close") { _, _ -> run {} }
                    .show()
        }
    }

    private var teslasoftIDButton: org.teslasoft.core.auth.TeslasoftIDButton? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        teslasoftIDButton = supportFragmentManager.findFragmentById(R.id.teslasoft_id_btn) as org.teslasoft.core.auth.TeslasoftIDButton

        if (teslasoftIDButton == null) Toast.makeText(this, "Could not find Teslasoft ID button in $this", Toast.LENGTH_SHORT).show()
        else teslasoftIDButton?.setAccountSyncListener(accountDataListener)
    }
}