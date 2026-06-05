package com.map.demo;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaBrowserServiceCompat;

import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import java.util.ArrayList;
import java.util.List;

public class MyMediaBrowserService extends MediaBrowserServiceCompat {

    private static final String ROOT_ID = "root";
    private static final String MEDIA_ID = "music_1";

    private MediaSessionCompat mediaSession;
    private MediaPlayer mediaPlayer;

    @Override
    public void onCreate() {
        super.onCreate();

        mediaSession = new MediaSessionCompat(this, "Session11MediaSession");
        setSessionToken(mediaSession.getSessionToken());
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                        | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mediaPlayer = MediaPlayer.create(this, R.raw.music);
        if (mediaPlayer != null) {
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build());
        }

        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                if (mediaPlayer != null) {
                    mediaPlayer.start();
                    updateState(PlaybackStateCompat.STATE_PLAYING);
                }
            }

            @Override
            public void onPause() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    updateState(PlaybackStateCompat.STATE_PAUSED);
                }
            }

            @Override
            public void onStop() {
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    updateState(PlaybackStateCompat.STATE_STOPPED);
                }
            }
        });

        MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, MEDIA_ID)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Demo Song")
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Automotive Demo")
                .putLong(
                        MediaMetadataCompat.METADATA_KEY_DURATION,
                        mediaPlayer != null ? mediaPlayer.getDuration() : -1)
                .build();
        mediaSession.setMetadata(metadata);

        mediaSession.setActive(true);
        updateState(PlaybackStateCompat.STATE_PAUSED);
    }

    private void updateState(int state) {
        PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY
                                | PlaybackStateCompat.ACTION_PAUSE
                                | PlaybackStateCompat.ACTION_PLAY_PAUSE
                                | PlaybackStateCompat.ACTION_STOP)
                .setState(
                        state,
                        mediaPlayer != null
                                ? mediaPlayer.getCurrentPosition()
                                : PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                        1.0f)
                .build();

        mediaSession.setPlaybackState(playbackState);
    }

    @Override
    @NonNull
    public BrowserRoot onGetRoot(
            @NonNull String clientPackageName,
            int clientUid,
            @Nullable Bundle rootHints) {
        return new BrowserRoot(ROOT_ID, null);
    }

    @Override
    public void onLoadChildren(
            @NonNull String parentId,
            @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        List<MediaBrowserCompat.MediaItem> items = new ArrayList<>();

        if (ROOT_ID.equals(parentId)) {
            MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                    .setMediaId(MEDIA_ID)
                    .setTitle("Demo Song")
                    .setSubtitle("Automotive Demo")
                    .build();
            items.add(new MediaBrowserCompat.MediaItem(
                    description,
                    MediaBrowserCompat.MediaItem.FLAG_PLAYABLE));
        }

        result.sendResult(items);
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        super.onDestroy();
    }
}
