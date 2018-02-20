package de.blackspoon.radiostations;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.gson.Gson;

public class StreamService extends Service {
        // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    protected MediaPlayer mediaPlayer = new MediaPlayer();
    protected WifiManager.WifiLock wifiLock;
    protected StationInfo activeSource;

    protected NotificationManager nfcmgr;

    public class LocalBinder extends Binder {
        StreamService getService() {
            return StreamService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.getAction() == null) return super.onStartCommand(intent, flags, startId);

        switch(intent.getAction()) {
            case "stop":
                stopPlayer();
                break;
            case "start":
                startMediaPlayer(activeSource);
                break;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        lockWifi();
        nfcmgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

            @SuppressLint("SetTextI18n")
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.start();

                // Service Intent
                Intent stopIntent = new Intent(StreamService.this, StreamService.class);
                stopIntent.setAction("stop");

                // Pending Intent
                PendingIntent stopPendingIntent = PendingIntent.getService(StreamService.this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                Notification.Action stop = new Notification.Action.Builder(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent).build();

                Intent notificationIntent = new Intent(StreamService.this, StationActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(StreamService.this, 0, notificationIntent, 0);

                Notification.Builder notification = new Notification.Builder(getApplicationContext())
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText("Playing Stream...")
                        .setSmallIcon(R.drawable.ic_dots)
                        .setWhen(System.currentTimeMillis())
                        .setContentIntent(pendingIntent)
                        .addAction(stop);

                Intent updateIntent = new Intent("de.blackspoon.intent.action.updateStationUI");
                updateIntent.putExtra("status",  StationActivity.STATUS_PLAYING);
                sendBroadcast(updateIntent);

                nfcmgr.notify(1, notification.build());
            }
        });


        Intent notificationIntent = new Intent(this, StationActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new Notification.Builder(getApplicationContext())
                .setContentTitle(getString(R.string.app_name))
                .setContentText("No stream")
                .setSmallIcon(R.drawable.ic_dots)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
    }

    @Override
    public void onDestroy() {
        unlockWifi();
        mediaPlayer.release();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    protected void lockWifi() {
        wifiLock = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "stream");
        wifiLock.acquire();
    }

    protected void unlockWifi() {
        if(wifiLock != null) wifiLock.release();
    }

    @SuppressLint("SetTextI18n")
    protected void startMediaPlayer(StationInfo source) {
        if(source == null) return;

        Intent notificationIntent = new Intent(StreamService.this, StationActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(StreamService.this, 0, notificationIntent, 0);

        Notification.Builder notification = new Notification.Builder(getApplicationContext())
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Buffering Stream ...")
                .setSmallIcon(R.drawable.ic_dots)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent);


        nfcmgr.notify(1, notification.build());

        Intent updateIntent = new Intent("de.blackspoon.intent.action.updateStationUI");
        updateIntent.putExtra("status",  StationActivity.STATUS_BUFFERING);
        sendBroadcast(updateIntent);

        try {
            mediaPlayer.setDataSource(source.host.toString());
            activeSource = source;

            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopPlayer() {
        Intent startIntent = new Intent(StreamService.this, StreamService.class);
        startIntent.setAction("start");

        PendingIntent startPendingIntent = PendingIntent.getService(StreamService.this, 0, startIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Action start = new Notification.Action.Builder(android.R.drawable.ic_media_play, "Start", startPendingIntent).build();

        Intent notificationIntent = new Intent(StreamService.this, StationActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(StreamService.this, 0, notificationIntent, 0);

        Notification.Builder notification = new Notification.Builder(getApplicationContext())
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Stream Stopped")
                .setSmallIcon(R.drawable.ic_dots)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent)
                .addAction(start);

        nfcmgr.notify(1, notification.build());

        Intent updateIntent = new Intent("de.blackspoon.intent.action.updateStationUI");
        updateIntent.putExtra("status",  StationActivity.STATUS_STOPPED);
        sendBroadcast(updateIntent);

        mediaPlayer.stop();
        mediaPlayer.reset();
    }
}
