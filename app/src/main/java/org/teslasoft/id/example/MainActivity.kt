/*******************************************************************************
 * Copyright (c) 2023 Dmytro Ostapenko. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/


package org.teslasoft.id.example

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.SurfaceColors
import org.teslasoft.core.auth.AccountSyncListener
import org.teslasoft.core.auth.client.SettingsListener
import org.teslasoft.core.auth.client.SyncListener
import org.teslasoft.core.auth.client.TeslasoftIDClient
import org.teslasoft.core.auth.client.TeslasoftIDClientBuilder
import org.teslasoft.core.auth.widget.TeslasoftIDButton
import org.teslasoft.core.auth.widget.TeslasoftIDCircledButton

class MainActivity : FragmentActivity() {

    private var client: TeslasoftIDClient? = null

    private val settingsListener: SettingsListener = object : SettingsListener {
        override fun onSuccess(settings: String) {
            runOnUiThread {
                this@MainActivity.settings = settings
                settingsField?.setText(HtmlCompat.fromHtml(settings, FROM_HTML_MODE_LEGACY))
                switchUI(false)
            }
        }

        override fun onError(state: String, message: String) {
            runOnUiThread {
                switchUI(false)
                MaterialAlertDialogBuilder(this@MainActivity, R.style.App_MaterialAlertDialog)
                        .setTitle("Error loading settings")
                        .setMessage(message)
                        .setPositiveButton("Close") { _, _ -> run {} }
                        .show()
            }
        }
    }

    private val syncListener: SyncListener = object : SyncListener {
        override fun onSuccess() {
            runOnUiThread {
                switchUI(false)
                Toast.makeText(this@MainActivity, "Success", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onError(state: String, message: String) {
            runOnUiThread {
                switchUI(false)
                MaterialAlertDialogBuilder(this@MainActivity, R.style.App_MaterialAlertDialog)
                        .setTitle("Error saving settings")
                        .setMessage(message)
                        .setPositiveButton("Close") { _, _ -> run {} }
                        .show()
            }
        }
    }

    private val accountDataListener: AccountSyncListener = object :
        AccountSyncListener {
        override fun onAuthFinished(name: String, email: String, isDev: Boolean, token: String) { /* Auth finished */
            account = client?.getAccount()!!
            runOnUiThread {
                updateUI()
            }
        }

        override fun onAuthCanceled() { /* Auth canceled */ }

        override fun onSignedOut() { /* User signed out */
            account = null
            runOnUiThread {
                updateUI()
            }
        }

        override fun onAuthFailed(state: String, message: String) { /* Auth failed */
            account = null
            runOnUiThread {
                updateUI()
                MaterialAlertDialogBuilder(this@MainActivity, R.style.App_MaterialAlertDialog)
                        .setTitle("Error")
                        .setMessage(message)
                        .setPositiveButton("Close") { _, _ -> run {} }
                        .show()
            }
        }
    }

    private var teslasoftIDButton: TeslasoftIDButton? = null
    private var teslasoftIDCircledButton: TeslasoftIDCircledButton? = null
    private var account: Map<String, String>? = null
    private var settings: String = ""

    private var accountRestricted: ConstraintLayout? = null
    private var settingsField: EditText? = null
    private var btnSyncSettings: Button? = null
    private var btnUploadSettings: Button? = null
    private var progress: ProgressBar? = null
    private var actionButtons: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        accountRestricted = findViewById(R.id.account_restricted)
        settingsField = findViewById(R.id.settings)
        btnSyncSettings = findViewById(R.id.sync_settings)
        btnUploadSettings = findViewById(R.id.upload_settings)
        progress = findViewById(R.id.settings_progress)
        actionButtons = findViewById(R.id.action_buttons)

        accountRestricted?.background = getSurfaceDrawable(ContextCompat.getDrawable(this, R.drawable.surface2)!!, this)

        client = TeslasoftIDClientBuilder(this)
                .setApiKey("d07985975904997990790c2e5088372a")
                .setAppId("org.teslasoft.id.example")
                .setSettingsListener(settingsListener)
                .setSyncListener(syncListener)
                .build()

        teslasoftIDButton = supportFragmentManager.findFragmentById(R.id.teslasoft_id_btn) as TeslasoftIDButton

        if (teslasoftIDButton == null) Toast.makeText(this, "Could not find Teslasoft ID button in $this", Toast.LENGTH_SHORT).show()
        else teslasoftIDButton?.setAccountSyncListener(accountDataListener)

        teslasoftIDCircledButton = supportFragmentManager.findFragmentById(R.id.teslasoft_id_btn_circled) as TeslasoftIDCircledButton

        if (teslasoftIDCircledButton == null) Toast.makeText(this, "Could not find Teslasoft ID button in $this", Toast.LENGTH_SHORT).show()
        else teslasoftIDCircledButton?.setAccountSyncListener(accountDataListener)

        updateUI()
        switchUI(false)

        settingsField?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { /* unused */ }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                settings = s.toString()
            }

            override fun afterTextChanged(s: Editable?) { /* unused */ }
        })

        btnSyncSettings?.setOnClickListener { loadSettings() }
        btnUploadSettings?.setOnClickListener {
            if (settings != "") {
                sendSettings()
            } else {
                Toast.makeText(this@MainActivity, "Please fill settings file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getSurfaceDrawable(drawable: Drawable, context: Context) : Drawable {
        DrawableCompat.setTint(DrawableCompat.wrap(drawable), getSurfaceColor(context))
        return drawable
    }

    private fun getSurfaceColor(context: Context): Int {
        return SurfaceColors.SURFACE_2.getColor(context)
    }

    private fun updateUI() {
        if (client?.doesUserSignedIn() == true) {
            accountRestricted?.visibility = View.VISIBLE
        } else {
            accountRestricted?.visibility = View.GONE
        }
    }

    private fun loadSettings() {
        if (client?.doesUserSignedIn() == true) {
            switchUI(true)
            client?.getAppSettings()
        } else {
            Toast.makeText(this, "Please sign in to perform this action", Toast.LENGTH_SHORT).show()
            updateUI()
        }
    }

    private fun sendSettings() {
        if (client?.doesUserSignedIn() == true) {
            switchUI(true)
            client?.syncAppSettings(settings)
        } else {
            Toast.makeText(this, "Please sign in to perform this action", Toast.LENGTH_SHORT).show()
            updateUI()
        }
    }

    private fun switchUI(isLoading: Boolean) {
        if (isLoading) {
            progress?.visibility = View.VISIBLE
            settingsField?.visibility = View.GONE
            actionButtons?.visibility = View.GONE
        } else {
            progress?.visibility = View.GONE
            settingsField?.visibility = View.VISIBLE
            actionButtons?.visibility = View.VISIBLE
        }
    }
}