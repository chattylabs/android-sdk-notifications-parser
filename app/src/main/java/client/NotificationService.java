package client;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;


import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import chattylabs.notifications.NotificationBinderInterface;

public class NotificationService extends Service {

    private final NotificationBinderInterface.Stub binder = new NotificationBinderInterface.Stub() {
        @Override public boolean isConnected() {
            return true;
        }

        @Override public void post(Intent intent) {
            LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(intent);
        }

        @Override public void notifyServerConnected() {
        }
    };

    @Nullable @Override public IBinder onBind(Intent intent) {
        return binder;
    }
}
