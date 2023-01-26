package org.teslasoft.core.auth

interface AccountSyncListener {
    fun onAuthFinished(name: String, email: String, isDev: Boolean, token: String)
    fun onAuthCanceled()
    fun onSignedOut()
    fun onAuthFailed(state: String)
}