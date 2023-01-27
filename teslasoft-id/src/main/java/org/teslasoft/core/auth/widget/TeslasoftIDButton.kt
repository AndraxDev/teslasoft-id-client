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

package org.teslasoft.core.auth.widget

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.elevation.SurfaceColors
import com.google.gson.Gson
import org.teslasoft.core.auth.*

class TeslasoftIDButton : Fragment() {
    private var accountIcon: ImageView? = null
    private var accountName: TextView? = null
    private var accountEmail: TextView? = null
    private var authBtn: LinearLayout? = null
    private var accountLoader: ProgressBar? = null
    private var verificationApi: RequestNetwork? = null
    private var accountSettings: SharedPreferences? = null
    private var listener: AccountSyncListener? = null

    private var token: String? = null

    private var verificationApiListener: RequestNetwork.RequestListener = object :
        RequestNetwork.RequestListener {

        /* Reminder: Do not forget to add runOnUiThread {} when working with UI elements */
        override fun onResponse(tag: String, message: String) {
            try {
                val gson = Gson()
                val accountData: Map<*,*> = gson.fromJson(message, Map::class.java)

                requireActivity().runOnUiThread {
                    accountName?.text = accountData["user_name"].toString()
                    accountEmail?.text = accountData["user_email"].toString()
                    accountIcon?.visibility = View.VISIBLE
                    accountLoader?.visibility = View.INVISIBLE
                }

                listener?.onAuthFinished(accountData["user_name"].toString(), accountData["user_email"].toString(), accountData["is_dev"] == true , token!!)
            } catch (e: Exception) {
                requireActivity().runOnUiThread { invalidate() }
                listener?.onAuthFailed("INVALID_ACCOUNT", e.stackTraceToString())
            }
        }

        /* Reminder: Do not forget to add runOnUiThread {} when working with UI elements */
        override fun onErrorResponse(tag: String, message: String) {
            requireActivity().runOnUiThread { invalidate() }
            listener?.onAuthFailed("NO_INTERNET", "Failed to connect to the server. Please try again later.")
        }
    }

    private val activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.data != null && result.resultCode >= 20) {
            val sig: String? = result.data!!.getStringExtra("signature")
            val uid: String? = result.data!!.getStringExtra("account_id")
            token = result.data!!.getStringExtra("auth_token")

            val edit: SharedPreferences.Editor? = accountSettings?.edit()
            edit?.putString("token", token)
            edit?.apply()

            sync(uid, sig)
        } else {
            when (result.resultCode) {
                3 -> {
                    invalidate()
                    listener?.onSignedOut()
                }

                2 -> listener?.onAuthFailed("PERMISSION_DENIED", "Permission denied. Please open app settings and allow this app to use Teslasoft ID.")
                Activity.RESULT_CANCELED -> listener?.onAuthFailed("CORE_UNAVAILABLE", "This app requires one ore more Teslasoft Core features that are currently unavailable. Please contact app developer for further assistance.")
                else -> listener?.onAuthCanceled()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.widget_teslasoft_id, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authBtn = view.findViewById(R.id.btn_teslasoft_id)
        accountIcon = view.findViewById(R.id.account_icon)
        accountName = view.findViewById(R.id.account_name)
        accountEmail = view.findViewById(R.id.account_email)
        accountLoader = view.findViewById(R.id.account_loader)

        accountIcon?.setImageResource(R.drawable.teslasoft_services_auth_account_icon)

        authBtn?.background = getSurfaceDrawable(ContextCompat.getDrawable(requireActivity(),
            R.drawable.teslasoft_services_auth_accent_account
        )!!, requireActivity())

        accountIcon?.visibility = View.INVISIBLE
        accountLoader?.visibility = View.INVISIBLE

        verificationApi = RequestNetwork(requireActivity())
        accountSettings = requireActivity().getSharedPreferences("account", FragmentActivity.MODE_PRIVATE)

        val uid: String? = accountSettings?.getString("user_id", null)
        val sig: String? = accountSettings?.getString("signature", null)

        token = accountSettings?.getString("token", null)

        if (uid != null && sig != null && token != null) {
            sync(uid, sig)
        } else disableWidget()

        authBtn?.setOnClickListener {
            activityResultLauncher.launch(Intent(requireActivity(), TeslasoftIDAuth::class.java))
        }
    }

    private fun invalidate() {
        val edit = accountSettings?.edit()
        edit?.remove("user_id")
        edit?.remove("signature")
        edit?.remove("token")
        edit?.apply()
        disableWidget()
    }

    private fun disableWidget() {
        accountIcon?.visibility = View.VISIBLE
        accountLoader?.visibility = View.INVISIBLE

        accountIcon?.setImageResource(R.drawable.teslasoft_services_auth_account_icon)
        accountName?.text = getString(R.string.teslasoft_services_auth_sync_title)
        accountEmail?.text = getString(R.string.teslasoft_services_auth_sync_desc)
    }

    private fun convertDpToPixel(context: Context): Float {
        return 22 * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }

    private fun sync(uid : String?, sig : String?) {
        accountName?.text = getString(R.string.teslasoft_services_auth_sync_loading)
        accountEmail?.text = ""

        accountIcon?.visibility = View.INVISIBLE
        accountLoader?.visibility = View.VISIBLE

        val edit: SharedPreferences.Editor? = accountSettings?.edit()
        edit?.putString("user_id", uid)
        edit?.putString("signature", sig)
        edit?.apply()

        var requestOptions = RequestOptions()
        requestOptions = requestOptions.transform(
            CenterCrop(),
            RoundedCorners(
                convertDpToPixel(requireActivity()).toInt()
            )
        )

        Glide.with(requireActivity())
            .load(Uri.parse("https://id.teslasoft.org/xauth/users/$uid.png"))
            .apply(requestOptions).into(accountIcon as ImageView)

        verificationApi?.startRequestNetwork(RequestNetworkController.GET, "https://id.teslasoft.org/xauth/GetAccountInfo.php?sig=$sig&uid=$uid", "A", verificationApiListener)
    }

    private fun getSurfaceDrawable(drawable: Drawable, context: Context) : Drawable {
        DrawableCompat.setTint(DrawableCompat.wrap(drawable), getSurfaceColor(context))
        return drawable
    }

    private fun getSurfaceColor(context: Context): Int {
        return SurfaceColors.SURFACE_2.getColor(context)
    }

    /**
     * Set a callback listener to the Teslasoft ID button.
     *
     * @param listener An implemented interface AccountSyncListener.
     * */
    fun setAccountSyncListener(listener: AccountSyncListener) {
        this.listener = listener
    }
}