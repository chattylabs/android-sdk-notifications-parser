package com.chattylabs.demo.notifications.parser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class OnUpgradeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.w("OnUpgradeReceiver", "Device upgraded!");
        //CommonService.start(context.getApplicationContext());
    }
}
