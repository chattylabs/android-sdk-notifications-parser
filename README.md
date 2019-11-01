# Notifications Parser Component

![Build Status](https://www.bitrise.io/app/0f5311bda229a817/status.svg?token=6TVvj6K_Q13Yyy5m1-gcHg&branch=master)
[![Latest version](https://api.bintray.com/packages/chattylabs/maven/chattylabs.notifications-parser/images/download.svg?label=Latest%20version)](https://bintray.com/chattylabs/maven/chattylabs.notifications-parser/_latestVersion)

Android SDK that allows you to retrieve the current active chattylabs.notifications and let you listen for incoming new ones.

<p align="center"><img src="assets/demo-sample.jpg" alt="demo-sample"/></p>

## Why choosing this SDK?

The **Notifications Parser Component SDK** contains built-in model algorithms that parse and extract the principal information from several instant messages apps and other installed apps. 

It aims to deliver a reliable solution easy to integrate in a project, that measures and takes care of aspects like the order of the incoming chattylabs.notifications,
avoiding duplicated items, handling any required permission, reducing battery consumption processes, and providing a simplified API interface among others.

The current built-in models are:

- _WhatsApp_
- _Facebook Messenger_
- _Telegram_
- _Line_
- _WeChat_
- _Spotify_
- _Netflix_

Click to [learn more about built-in models]()

By default, if there is no model for the notification, it will return a `NotificationData` object that contains all the extracted information.

## Prerequisites
- The SDK works on Android version 5.0 (Lollipop) and above. _(for lower versions [contact us](mailto:hello@chattylabs.com))_
- You need to setup an _APP_ID_ key on [ChattyLabs Developer](http://chattylabs.com/developer) site.

## Setup
Add the following code to your gradle file.

```groovy
repositories {
    maven { url "https://dl.bintray.com/chattylabs/maven" }
}
 
// Optional
android {
    defaultConfig {
        manifestPlaceholders = [CHATTYLABS_APP_ID: "##GENERATED_UUID_NUMBER##"]
    }
}
 
dependencies {
    implementation 'com.chattylabs.sdk.android:chattylabs.notifications-parser:x.y.z'
}
```

Add the following line inside the `<application />` tag of your **AndroidManifest.xml**

```xml
<meta-data android:name="com.chattylabs.sdk.APP_ID" android:value="${CHATTYLABS_APP_ID}" />
```

## Usage
In order to get the chattylabs.notifications objects on your project, you have to create an
[IntentService](https://developer.android.com/reference/android/app/IntentService) class
where you can extract the items from the intent extras bundle.

```java
public class NotificationListenerIntentService extends IntentService {
    
    public NotificationListenerIntentService() {
        super("NotificationListenerIntentService");
    }
    
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        // Avoid empty intents
        if (intent == null || intent.getAction() == null || intent.getExtras() == null) return;
        
        // Initializes and provides a NotificationParser instance
        NotificationParser NotificationParser =
            NotificationParserModule.provideNotificationParser();
        
        // Extracts the current notification item from the intent extras bundle
        NotificationItem item = NotificationParser.extract(intent);
        
        // ...
    }
}
```

Remember to register your 
[IntentService](https://developer.android.com/reference/android/app/IntentService) class 
inside your **AndroidManifest.xml** and to add the various [actions]() you want to receive.

```xml
<service android:name="com.example.NotificationListenerIntentService"
    android:exported="false">
    
    <intent-filter>
        <action android:name="com.chattylabs.sdk.android.chattylabs.notifications.action.POST"/>
        <action android:name="com.chattylabs.sdk.android.chattylabs.notifications.action.REMOVE"/>
        
        ...
        
    </intent-filter>
    
</service>
```

#### Usage with dependency injection
If you make use of [Dagger 2](https://google.github.io/dagger/) in your project, you may provide the `NotificationParser` instance with the `@Inject` annotation.

```java
public class NotificationListenerIntentService extends DaggerIntentService {
    
    @Inject NotificationParser NotificationParser;
    
    // ...
}
```

You will need to add `NotificationParserModule.class` as a module element into your Dagger Component graph.
Take a look on how 
[DemoApplication](https://github.com/chattylabs/android-sdk-chattylabs.notifications-parser/blob/54782dfe9adcc864ba93f1d7bdc7ecb80f2a1d93/app/src/main/java/com/chattylabs/demo/chattylabs.notifications/parser/DemoApplication.java#L15) is handling this.

## Demo
After you have cloned this demo project, run the following command on a terminal console. 
This will get and update the project's build system.

```bash
git submodule update --init
```

## Projects
This is a list of Apps using the **SDK** in their project:

<a href="https://play.google.com/store/apps/details?id=com.Chatty"><img src="https://lh3.googleusercontent.com/BwP_HPbu2G523jUQitRcfgADe5qKxZclxAbESmM4xaTNFS3ckz5uqkh12OimzqPC=s50-rw" alt="Chatty" title="Chatty"/> &nbsp;&nbsp; 
&nbsp;