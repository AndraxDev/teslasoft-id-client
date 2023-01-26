package org.teslasoft.core.auth

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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
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

class TeslasoftIDButton : Fragment() {
    private var accountIcon: ImageView? = null
    private var accountName: TextView? = null
    private var accountEmail: TextView? = null
    private var authBtn: LinearLayout? = null
    private var accountLoader: ProgressBar? = null
    private var verificationApi: RequestNetwork? = null
    private var accountSettings: SharedPreferences? = null
    private var listener: AccountSyncListener? = null

    private var token: String = ""

    private var verificationApiListener: RequestNetwork.RequestListener = object : RequestNetwork.RequestListener {
        override fun onResponse(tag: String, message: String) {
            try {
                val gson = Gson()
                val accountData: Map<*,*> = gson.fromJson(message, Map::class.java)
                accountName?.text = accountData["user_name"].toString()
                accountEmail?.text = accountData["user_email"].toString()

                accountIcon?.visibility = View.VISIBLE
                accountLoader?.visibility = View.INVISIBLE

                listener?.onAuthFinished(accountData["user_name"].toString(), accountData["user_email"].toString(), accountData["is_dev"] == true , token)
            } catch (e: Exception) {
                invalidate()
                listener?.onAuthFailed(e.message.toString())
            }
        }

        override fun onErrorResponse(tag: String, message: String) {
            invalidate()
            listener?.onAuthFailed("NO_INTERNET")
        }
    }

    private val activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.data != null && result.resultCode >= 20) {
            val sig: String? = result.data!!.getStringExtra("signature")
            val uid: String? = result.data!!.getStringExtra("account_id")
            sync(uid, sig)
        } else {
            when (result.resultCode) {
                3 -> {
                    invalidate()
                    listener?.onSignedOut()
                }

                2 -> listener?.onAuthFailed("PERMISSION_DENIED")
                Activity.RESULT_CANCELED -> listener?.onAuthFailed("CORE_UNAVAILABLE")
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

        authBtn?.background = getSurfaceDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.teslasoft_services_auth_accent_account)!!, requireActivity())

        accountIcon?.visibility = View.INVISIBLE
        accountLoader?.visibility = View.INVISIBLE

        verificationApi = RequestNetwork(requireActivity())
        accountSettings = requireActivity().getSharedPreferences("account", FragmentActivity.MODE_PRIVATE)

        val uid: String? = accountSettings?.getString("account_id", null)
        val sig: String? = accountSettings?.getString("signature", null)

        try {
            if (uid != null) {
                sync(uid, sig)
            } else disableWidget()
        } catch (_: java.lang.Exception) {
            disableWidget()
        }

        authBtn?.setOnClickListener {
            activityResultLauncher.launch(Intent(requireActivity(), TeslasoftIDAuth::class.java))
        }
    }

    private fun invalidate() {
        val edit = accountSettings?.edit()
        edit?.remove("account_id")
        edit?.remove("signature")
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

    private fun convertDpToPixel(dp: Float, context: Context): Float {
        return dp * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }

    private fun sync(uid : String?, sig : String?) {
        accountName?.text = getString(R.string.teslasoft_services_auth_sync_loading)
        accountEmail?.text = ""

        accountIcon?.visibility = View.INVISIBLE
        accountLoader?.visibility = View.VISIBLE

        val edit: SharedPreferences.Editor? = accountSettings?.edit()
        edit?.putString("account_id", uid)
        edit?.putString("signature", sig)
        edit?.apply()

        var requestOptions = RequestOptions()
        requestOptions = requestOptions.transform(
            CenterCrop(),
            RoundedCorners(
                convertDpToPixel(22f, requireActivity()).toInt()
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

    fun setAccountSyncListener(listener: AccountSyncListener) {
        this.listener = listener
    }
}