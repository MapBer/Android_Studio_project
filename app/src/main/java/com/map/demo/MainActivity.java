package com.map.demo;

import android.content.ComponentName;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "AutomotiveMediaDemo";

    private MediaBrowserCompat mediaBrowser;
    private MediaControllerCompat mediaController;
    private TextView infoText;

    private final MediaBrowserCompat.ConnectionCallback connectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    try {
                        mediaController = new MediaControllerCompat(
                                MainActivity.this,
                                mediaBrowser.getSessionToken());
                        MediaControllerCompat.setMediaController(
                                MainActivity.this,
                                mediaController);
                        infoText.setText(R.string.media_service_connected);
                        Log.d(TAG, "Media browser connected");
                    } catch (Exception exception) {
                        infoText.setText(R.string.media_service_failed);
                        Log.e(TAG, "Could not create media controller", exception);
                    }
                }

                @Override
                public void onConnectionSuspended() {
                    infoText.setText(R.string.media_service_suspended);
                    Log.d(TAG, "Media browser connection suspended");
                }

                @Override
                public void onConnectionFailed() {
                    infoText.setText(R.string.media_service_failed);
                    Log.d(TAG, "Media browser connection failed");
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button playBtn = findViewById(R.id.btnPlay);
        Button pauseBtn = findViewById(R.id.btnPause);
        Button mediaBtn = findViewById(R.id.btnMedia);
        Button navigationBtn = findViewById(R.id.btnNavigation);
        infoText = findViewById(R.id.infoText);

        mediaBrowser = new MediaBrowserCompat(
                this,
                new ComponentName(this, MyMediaBrowserService.class),
                connectionCallback,
                null);
        mediaBrowser.connect();

        playBtn.setOnClickListener(view -> {
            if (mediaController != null) {
                mediaController.getTransportControls().play();
                infoText.setText(R.string.media_playing);
            }
        });

        pauseBtn.setOnClickListener(view -> {
            if (mediaController != null) {
                mediaController.getTransportControls().pause();
                infoText.setText(R.string.media_paused);
            }
        });

        mediaBtn.setOnClickListener(view ->
                infoText.setText(R.string.media_button_selected));

        navigationBtn.setOnClickListener(view ->
                infoText.setText(R.string.navigation_button_selected));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaBrowser != null) {
            mediaBrowser.disconnect();
            mediaBrowser = null;
        }
    }
}
