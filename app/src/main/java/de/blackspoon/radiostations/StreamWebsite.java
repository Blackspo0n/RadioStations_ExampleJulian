package de.blackspoon.radiostations;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Activity;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class StreamWebsite extends Activity {
    protected Bundle extras;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream);
        extras = getIntent().getExtras();

        if(extras == null) {
            finish();
        }

        getActionBar().setDisplayHomeAsUpEnabled(true);

        StationInfo source = (StationInfo)extras.getSerializable("station");
        this.setTitle("Webradio: " + source.name);

        loadWebview();
    }

    @Override
    public void onBackPressed() {
        WebView webView = findViewById(R.id.webview);
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();

    }

    private void loadWebview() {
        StationInfo source = (StationInfo)extras.getSerializable("station");

        WebView webView = findViewById(R.id.webview);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(source.stationWebsite.toString());

    }
}
