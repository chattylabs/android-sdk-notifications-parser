# Notifications Parser Component

![SDK build status](https://img.shields.io/bitrise/0f5311bda229a817/master.svg?token=6TVvj6K_Q13Yyy5m1-gcHg&label=SDK%20build)
![DEMO build status](https://img.shields.io/bitrise/ef9e1e7821137ca7/master.svg?token=BcSO-w9XIDnMVG78GuxXLw&label=Demo%20build)
[![Apache 2.0 Licence](https://img.shields.io/github/license/chattylabs/android-sdk-notifications-parser.svg)](https://github.com/chattylabs/android-sdk-notifications-parser/blob/master/LICENSE)

Android SDK that allows you to retrieve the current active notifications and let you listen for incoming new ones.

By default, it contains built-in models that parses and extracts the principal information from several instant messages apps and others. 

The current built-in models are:

- WhatsApp
- Facebook Messenger
- Telegram
- Line
- WeChat
- Spotify
- Netflix

By default, if there is no model for the notification it returns a `NotificationData` object that contains all the extracted information.

### Prerequisites
- The SDK works on Android version 5.0 (Lollipop) and above. _(for lower versions [contact us](mailto:hello@chattylabs.com))_
- You need to setup an **APP_ID** key on [ChattyLabs](http://chattylabs.com/developer).

### Setup
Add the following code to your main gradle file.

```groovy
repositories {
    maven { url "https://dl.bintray.com/chattylabs/maven" }
}
 
android {
    defaultConfig {
        manifestPlaceholders = [CHATTYLABS_APP_ID: "##GENERATED_UUID_NUMBER##"]
    }
}
 
dependencies {
    implementation 'com.chattylabs.sdk.android:notifications-parser:x.y.z'
}
```

Add the following line inside the `<application/>` tag of your **AndroidManifest.xml**

```xml
<meta-data android:name="com.chattylabs.sdk.APP_ID" android:value="${CHATTYLABS_APP_ID}" />
```

### Usage
In order to get the notification objects on your project, you have to create an `Intent Service` class
and extract the notifications from the intent extras bundles.

```java
public class NotificationListenerIntentService extends IntentService {
    
    public NotificationListenerIntentService() {
        super("NotificationListenerIntentService");
    }
    
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        // Avoid empty intents
        if (intent == null || intent.getAction() == null || intent.getExtras() == null) return;
        
        // Initializes and provides a NotificationParserComponent instance
        NotificationParserComponent NotificationParserComponent = NotificationParserModule.provideNotificationParserComponent();
        
        // Extracts the current notification item from the intent extras bundles
        NotificationItem item = notificationParserComponent.extract(intent);
        
        // ...
    }
}
```

Remember to register your `Intent Service` class inside your **AndroidManifest.xml** and to add the various actions you want to receive.

```xml
<service android:name="com.example.NotificationListenerIntentService"
    android:exported="false">
    
    <intent-filter>
        <action android:name="com.chattylabs.sdk.android.notifications.action.POST"/>
        <action android:name="com.chattylabs.sdk.android.notifications.action.REMOVE"/>
    </intent-filter>
    
</service>
```

##### Use with dependency injection
If you make use of [Dagger 2](https://google.github.io/dagger/) in your project, you may provide the `NotificationParserComponent` instance with the `@Inject` annotation.

```java
public class NotificationListenerIntentService extends DaggerIntentService {
    
    @Inject NotificationParserComponent notificationParserComponent;
    
    // ...
}
```