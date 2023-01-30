# Teslasoft ID client
This library enable you to authenticate users through Teslasoft ID. 
Also this library can help you to personalize user experience by saving app's settings into 
Teslasoft ID account and syncing it between multiple devices.

## Installation

Add the following line to your build.gradle:

```gradle
implementation 'org.teslasoft.core.auth:teslasoft-id:1.1.0'
```

Or add the following dependency if you are using Maven:

```xml
<dependency>
    <groupId>org.teslasoft.core.auth</groupId>
    <artifactId>teslasoft-id</artifactId>
    <version>1.1.0</version>
    <type>aar</type>
</dependency>
```

## Usage

### Teslasoft ID button

Add the following code to your layout:

```xml
...
<androidx.fragment.app.FragmentContainerView
        android:id="@+id/teslasoft_id_btn"
        android:name="org.teslasoft.core.auth.widget.TeslasoftIDButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        tools:layout="@layout/widget_teslasoft_id" 
        ... other params ...
        />
...
```

Circluar button:

```xml
...
<androidx.fragment.app.FragmentContainerView
        android:id="@+id/teslasoft_id_btn"
        android:name="org.teslasoft.core.auth.widget.TeslasoftIDCircledButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        tools:layout="@layout/widget_teslasoft_id_circle" 
        ... other params ...
        />
...
```

> **Warning**
>
> Do not use both buttons on the same Activity/Fragment. It will break account sync.

Add the following code to your Activity/Fragment:

```kotlin
private var teslasoftIDButton: TeslasoftIDButton? = null

private val accountDataListener: AccountSyncListener = object :
    AccountSyncListener {
    
    /**
     * onAuthFinished triggers when authentication was successful.
     *
     * @param name First and last name of the account.
     * @param email Email of the account.
     * @param isDev Determines if user a developer. Can be used to deliver beta features.
     * @param token An auth token. Use it to sync settings and perform actions in your account.
     * */
    override fun onAuthFinished(name: String, email: String, isDev: Boolean, token: String) { 
        /* Auth finished */
        
        ...
        
        runOnUiThread {
            /* Work with UI elements here otherwise an Exception will be raised */
        }
    }

    /**
     * onAuthCanceled triggers when user dismissed account picker dialog without selecting any options.
     * */
    override fun onAuthCanceled() { 
        /* Auth canceled */ 
        
        ...
        
        runOnUiThread {
            /* Work with UI elements here otherwise an Exception will be raised */
        }
    }

    /**
     * onSignedOut triggers when user clicked "Turn off sync" button or user session has expired.
     * */
    override fun onSignedOut() { 
        /* User signed out */
        
        ...
        
        runOnUiThread {
            /* Work with UI elements here otherwise an Exception will be raised */
        }
    }

    /**
     * onAuthFailed triggers when internal error is occurred. (ex. Teslasoft Core is not installed, no Internet connection
     * or account database is corrupted).
     *
     * @param state Reason of failure.
     * @param message Error message. Please use android string instead od this message. Android strings can be translated to other languages.
     * This message is for developers only.
     * */
    override fun onAuthFailed(state: String, message: String) { 
        /* Auth failed */
        
        ...
        
        runOnUiThread {
            /* Work with UI elements here otherwise an Exception will be raised */
        }
    }
}

...
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
        
    teslasoftIDButton = supportFragmentManager.findFragmentById(R.id.teslasoft_id_btn) as TeslasoftIDButton
    if (teslasoftIDButton != null) teslasoftIDButton?.setAccountSyncListener(accountDataListener)
    ...
}

```

> **Note**
>
> Please use childFragmentManager instead of supportFragmentManager if you are using this code inside Fragment
>
> Also do not forget to replace all "this" calls with requireActivity()

### Sync settings

Create an instance of Teslasoft ID client:

```kotlin
val client = TeslasoftIDClientBuilder(this)
                .setApiKey("PASTE YOU API KEY HERE")
                .setAppId("PASTE YOU APP ID HERE")
                .setSettingsListener(settingsListener)
                .setSyncListener(syncListener)
                .build()
```

> **Note**
>
> Contact me to request an API key: dostapenko82@gmail.com

Add settings listener and sync listener:

```kotlin
private val settingsListener: SettingsListener = object : SettingsListener {

        /**
         * onSuccess triggers when app retrieved their settings from the server.
         *
         * @param settings JSON string.
         * */
        override fun onSuccess(settings: String) {
            ...
        
            runOnUiThread {
                /* Work with UI elements here otherwise an Exception will be raised */
            }
        }

        /**
         * onError triggers when internal error is occurred. (ex. no Internet connection, invalid api key/session token/app signature
         * or database is corrupted).
         *
         * @param state Reason of failure
         * @param message Error message. Please use android string instead od this message. Android strings can be translated to other languages.
         * This message is for developers only.
         * */
        override fun onError(state: String, message: String) {
            ...
        
            runOnUiThread {
                /* Work with UI elements here otherwise an Exception will be raised */
            }
        }
    }
```

```kotlin
private val syncListener: SyncListener = object : SyncListener {

        /**
         * onSuccess triggers when app retrieved their settings from the server.
         * */
        override fun onSuccess() {
            ...
        
            runOnUiThread {
                /* Work with UI elements here otherwise an Exception will be raised */
            }
        }

        /**
         * onError triggers when internal error is occurred. (ex. no Internet connection, invalid api key/session token/app signature
         * or database is corrupted).
         *
         * @param state Reason of failure
         * @param message Error message. Please use android string instead od this message. Android strings can be translated to other languages.
         * This message is for developers only.
         * */
        override fun onError(state: String, message: String) {
            ...
        
            runOnUiThread {
                /* Work with UI elements here otherwise an Exception will be raised */
            }
        }
    }
```

Determine if user is signed in:

```kotlin
if (client?.doesUserSignedIn() == true) {
    // Do your stuff here
}
```

Save settings to the server:

```kotlin
client?.syncAppSettings(settings)
```

Load settings from the server:

```kotlin
client?.getAppSettings()
```

> **Warning**
>
> To avoid errors please make sure that user is signed in.

## License

```
Copyright (c) 2023 Dmytro Ostapenko. All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```