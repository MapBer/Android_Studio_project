package com.map.demo;

import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "AutomotiveMediaDemo";

    private MediaPlayer mediaPlayer;
    private MediaSession mediaSession;
    private TextView infoText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button playBtn = findViewById(R.id.btnPlay);
        Button pauseBtn = findViewById(R.id.btnPause);
        infoText = findViewById(R.id.infoText);

        mediaSession = new MediaSession(this, "AutomotiveMediaSession");
        mediaSession.setActive(true);

        mediaPlayer = createMediaPlayer();

        playBtn.setOnClickListener(view -> {
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
                infoText.setText(R.string.media_playing);
                Log.d(TAG, "Playing media");
            }
        });

        pauseBtn.setOnClickListener(view -> {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                infoText.setText(R.string.media_paused);
                Log.d(TAG, "Paused media");
            }
        });
    }

    private MediaPlayer createMediaPlayer() {
        MediaPlayer player = new MediaPlayer();
        player.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());

        try (AssetFileDescriptor descriptor = getResources().openRawResourceFd(R.raw.music_1)) {
            player.setDataSource(
                    descriptor.getFileDescriptor(),
                    descriptor.getStartOffset(),
                    descriptor.getLength());
            player.prepare();
        } catch (IOException exception) {
            Log.e(TAG, "Could not load raw media file", exception);
        }

        return player;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
    }
}
