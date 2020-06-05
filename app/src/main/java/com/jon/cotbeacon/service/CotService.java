package com.jon.cotbeacon.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.jon.cotbeacon.BuildConfig;
import com.jon.cotbeacon.R;
import com.jon.cotbeacon.ui.CotActivity;
import com.jon.cotbeacon.utils.GenerateInt;
import com.jon.cotbeacon.utils.PrefUtils;

import timber.log.Timber;

public class CotService extends Service {
    private static final String BASE_INTENT_ID = BuildConfig.APPLICATION_ID + ".CotService.";
    private static final int LAUNCH_ACTIVITY_PENDING_INTENT = GenerateInt.next();
    private static final int STOP_SERVICE_PENDING_INTENT = GenerateInt.next();

    public static final String START_SERVICE = BASE_INTENT_ID + "START";
    public static final String STOP_SERVICE = BASE_INTENT_ID + "STOP";
    public static final String CLOSE_SERVICE_INTERNAL = BASE_INTENT_ID + "CLOSE_SERVICE_INTERNAL";

    private CotManager cotManager;
    private SharedPreferences prefs;

    @Override
    public IBinder onBind(Intent intent) {
        /* TODO: Return the communication channel to the service */
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        cotManager = new CotManager(prefs);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case START_SERVICE:
                    Timber.i("Start service");
                    cotManager.start();
                    startForegroundService();
                    break;
                case STOP_SERVICE:
                    Timber.i("Stop service");
                    cotManager.shutdown();
                    stopForegroundService();
                    LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(CLOSE_SERVICE_INTERNAL));
                    break;
            }
        }
        return Service.START_STICKY;
    }

    private void startForegroundService() {
        /* Intent to launch main activity when tapping the notification */
        PendingIntent launchPendingIntent = PendingIntent.getActivity(
                this,
                LAUNCH_ACTIVITY_PENDING_INTENT,
                new Intent(this, CotActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        /* Intent to stop the service when the notification button is tapped */
        Intent stopIntent = new Intent(this, CotService.class).setAction(CotService.STOP_SERVICE);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, STOP_SERVICE_PENDING_INTENT, stopIntent, 0);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            Timber.e("NotificationManager == null");
            return;
        }

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, BuildConfig.APPLICATION_ID)
                .setOngoing(true)
                .setSmallIcon(R.drawable.target)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(PrefUtils.getPresetInfoString(prefs))
                .setContentIntent(launchPendingIntent)
                .addAction(R.drawable.stop, getString(R.string.stop), stopPendingIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notification.setCategory(Notification.CATEGORY_SERVICE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    BuildConfig.APPLICATION_ID,
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setLightColor(Color.BLUE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            channel.enableVibration(false);
            channel.setSound(null, null);
            channel.setShowBadge(false);
            manager.createNotificationChannel(channel);
        } else {
            notification.setPriority(NotificationCompat.PRIORITY_MAX);
        }
        startForeground(3, notification.build());
    }

    private void stopForegroundService() {
        stopForeground(true);
        stopSelf();
    }
}
