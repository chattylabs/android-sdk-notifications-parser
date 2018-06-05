package com.chattylabs.demo.notifications.parser;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

public class CommonIntentService extends IntentService {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public CommonIntentService() {
        super("CommonIntentService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null || intent.getAction() == null || intent.getExtras() == null) return;
        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(intent);
    }
}
