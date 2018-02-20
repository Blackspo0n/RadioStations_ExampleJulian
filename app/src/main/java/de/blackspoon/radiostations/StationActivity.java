package de.blackspoon.radiostations;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StationActivity extends Activity {
    public static final int STATUS_PLAYING = 1;
    public static final int STATUS_BUFFERING = 2;
    public static final int STATUS_STOPPED = 3;

    protected List<StationInfo> sI;

    boolean serviceBounded = false;
    StreamService mService;
    WebRadioStateReceiver radioReceiver;

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            StreamService.LocalBinder binder = (StreamService.LocalBinder) service;
            mService = binder.getService();

            Log.v("Radiostation", "Looking for last played Stream");
            SharedPreferences mPrefs = getSharedPreferences("station", MODE_PRIVATE);
            String lastStationJSONString = mPrefs.getString("last", "");


            if(!Objects.equals(lastStationJSONString, "")) {
                Log.v("Radiostation", "Found last Stream");
                StationInfo station = new Gson().fromJson(lastStationJSONString, StationInfo.class);

                startMediaPlayer(station);
            }

            serviceBounded = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serviceBounded = false;
            Log.v("radioStations", "Unbind Service");
        }
    };
    /**
     * Broadcast Receiver to update the ui elements of the activity
     */
    public class WebRadioStateReceiver extends BroadcastReceiver {

        private final Handler handler; // Handler used to execute code on the UI thread

        public WebRadioStateReceiver(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void onReceive(final Context context, final Intent intent) {
            // Post the UI updating code to our Handler
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if(Objects.equals(intent.getAction(), "android.net.conn.CONNECTIVITY_CHANGE")) {
                        ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo netInfo = conMan.getActiveNetworkInfo();
                        if (netInfo == null || netInfo.getType() != ConnectivityManager.TYPE_WIFI) {
                            stopPlayer();
                            Toast.makeText(context, "WLAN-Verbindung verloren!", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else {
                        Bundle extras = intent.getExtras();
                        ImageButton ib = findViewById(R.id.playPauseButton);
                        TextView tv =  findViewById(R.id.streamStatus);

                        switch (extras.getInt("status")) {
                            case STATUS_PLAYING: //Playing
                                tv.setText("Playing Stream ...");
                                ib.setClickable(true);
                                ib.setImageResource(android.R.drawable.ic_media_pause);
                                break;
                            case STATUS_STOPPED:

                                tv.setText("Stream Stopped");
                                ib.setClickable(true);
                                ib.setImageResource(android.R.drawable.ic_media_play);
                                break;
                            case STATUS_BUFFERING:

                                tv.setText("Buffering Stream ...");
                                ib.setClickable(false);
                                ib.setImageResource(android.R.drawable.ic_media_pause);
                                break;
                        }
                    }
                }
            });
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station);

        final RecyclerView recList = findViewById(R.id.cardList);
        recList.setHasFixedSize(true);

        // create StationList
        createStations();

        // use a linear layout manager
        LinearLayoutManager mLayoutManager = new GridLayoutManager(this, 2);
        recList.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        StationAdapter mAdapter = new StationAdapter(this, sI);
        recList.setAdapter(mAdapter);

        final ImageButton ib = findViewById(R.id.playPauseButton);
        ib.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if(mService.mediaPlayer.isPlaying()) stopPlayer();
                else startMediaPlayer(mService.activeSource);
            }
        });

        radioReceiver = new WebRadioStateReceiver(new Handler()); // Create the receiver
        registerReceiver(radioReceiver, new IntentFilter("de.blackspoon.intent.action.updateStationUI")); // Register receiver
        registerReceiver(radioReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")); // Register receiver
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, StreamService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* Programmatically
        menu.add(Menu.NONE,R.id.menuSettings,0,"Settings");
        menu.add(Menu.NONE,R.id.menuAppExit,1,"Beenden");
        */

        getMenuInflater().inflate(R.menu.menu_options_station, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menuAppExit:
                finish();
                break;
            case R.id.menuSettings:
                //startActivity(new Intent(this, SettingsActivity.class));
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();

        serviceBounded = false;
    }

    @Override
    protected void onDestroy() {

        unbindService(serviceConnection);
        stopService(new Intent(this, StreamService.class));

        unregisterReceiver(radioReceiver);
        super.onDestroy();
    }

    private void createStations() {
        sI = new ArrayList<>();

        StationInfo s1 = new StationInfo();
        StationInfo s2 = new StationInfo();
        StationInfo s3 = new StationInfo();
        try {

            s1.host = new URI("https://wdr-1live-live.sslcast.addradio.de/wdr/1live/live/mp3/128/stream.mp3");
            s1.name = "1Live";
            s1.stationImageURL = new URI("http://www.radiowoche.de/wp-content/uploads/2016/02/logo_1live-520x245.png");
            s1.stationWebsite  = new URI("https://www1.wdr.de/radio/1live/uebersicht-einslive-100.html?1livestart=true");

            s2.host = new URI("https://wdr-1live-live.sslcast.addradio.de/wdr/1live/live/mp3/128/stream.mp3");
            s2.name = "WDR";
            s2.stationImageURL = new URI("https://upload.wikimedia.org/wikipedia/commons/thumb/8/8b/Mein_WDR_Radio_Logo.svg/2000px-Mein_WDR_Radio_Logo.svg.png");
            s2.stationWebsite  = new URI("https://www1.wdr.de/radio/radiolayer-100.html");

            s3.host = new URI("http://st01.dlf.de/dlf/01/128/mp3/stream.mp3");
            s3.name = "Deutschlandradio";
            s3.stationImageURL = new URI("http://www.ard.de/image/263692/16x9/4788450643956665328/704");
            s3.stationWebsite = new URI("http://www.deutschlandradio.de/");

            sI.add(s1);
            sI.add(s2);
            sI.add(s3);
        }
        catch (Exception e) {

        }
    }

    @SuppressLint("SetTextI18n")
    protected void startMediaPlayer(StationInfo source) {
        if(source == null) return;

        ((TextView) findViewById(R.id.streamStationName)).setText(source.name);

        SharedPreferences mPrefs = getSharedPreferences("station", MODE_PRIVATE);

        SharedPreferences.Editor edit = mPrefs.edit();
        edit.putString("last", new Gson().toJson(source));
        edit.apply();

        mService.startMediaPlayer(source);
    }

    public void stopPlayer() {
        mService.stopPlayer();
    }
}
