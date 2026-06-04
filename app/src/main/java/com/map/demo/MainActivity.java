package com.map.demo;

import android.content.ComponentName;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String SONGS_ID = "songs";

    private TextView connectionStatusText;
    private TextView browseStatusText;
    private TextView playbackStatusText;
    private TextView nowPlayingText;
    private LinearLayout trackListContainer;
    private Button playButton;
    private Button pauseButton;
    private Button stopButton;

    private MediaBrowser mediaBrowser;
    private MediaController mediaController;
    private final List<MediaBrowser.MediaItem> songs = new ArrayList<>();

    private final MediaBrowser.ConnectionCallback connectionCallback =
            new MediaBrowser.ConnectionCallback() {
                @Override
                public void onConnected() {
                    mediaController = new MediaController(
                            MainActivity.this,
                            mediaBrowser.getSessionToken());
                    setMediaController(mediaController);
                    mediaController.registerCallback(controllerCallback);

                    connectionStatusText.setText(R.string.media_service_connected);
                    setControlsEnabled(true);
                    updateMetadata(mediaController.getMetadata());
                    updatePlaybackState(mediaController.getPlaybackState());
                    mediaBrowser.subscribe(SONGS_ID, subscriptionCallback);
                }

                @Override
                public void onConnectionSuspended() {
                    connectionStatusText.setText(R.string.media_service_suspended);
                    setControlsEnabled(false);
                }

                @Override
                public void onConnectionFailed() {
                    connectionStatusText.setText(R.string.media_service_failed);
                    setControlsEnabled(false);
                }
            };

    private final MediaBrowser.SubscriptionCallback subscriptionCallback =
            new MediaBrowser.SubscriptionCallback() {
                @Override
                public void onChildrenLoaded(
                        String parentId,
                        List<MediaBrowser.MediaItem> children) {
                    songs.clear();
                    songs.addAll(children);
                    renderSongButtons();
                    browseStatusText.setText(getString(R.string.browse_loaded_songs, children.size()));
                }

                @Override
                public void onError(String parentId) {
                    browseStatusText.setText(R.string.browse_load_failed);
                }
            };

    private final MediaController.Callback controllerCallback =
            new MediaController.Callback() {
                @Override
                public void onPlaybackStateChanged(PlaybackState state) {
                    updatePlaybackState(state);
                }

                @Override
                public void onMetadataChanged(MediaMetadata metadata) {
                    updateMetadata(metadata);
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectionStatusText = findViewById(R.id.connection_status_text);
        browseStatusText = findViewById(R.id.browse_status_text);
        playbackStatusText = findViewById(R.id.playback_status_text);
        nowPlayingText = findViewById(R.id.now_playing_text);
        trackListContainer = findViewById(R.id.track_list_container);
        playButton = findViewById(R.id.play_button);
        pauseButton = findViewById(R.id.pause_button);
        stopButton = findViewById(R.id.stop_button);

        playButton.setOnClickListener(view ->
                mediaController.getTransportControls().play());
        pauseButton.setOnClickListener(view ->
                mediaController.getTransportControls().pause());
        stopButton.setOnClickListener(view ->
                mediaController.getTransportControls().stop());

        setControlsEnabled(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mediaBrowser = new MediaBrowser(
                this,
                new ComponentName(this, MediaPlaybackService.class),
                connectionCallback,
                null);
        mediaBrowser.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mediaBrowser != null && mediaBrowser.isConnected()) {
            mediaBrowser.unsubscribe(SONGS_ID);
        }
        if (mediaController != null) {
            mediaController.unregisterCallback(controllerCallback);
            mediaController = null;
        }
        if (mediaBrowser != null && mediaBrowser.isConnected()) {
            mediaBrowser.disconnect();
        }
        mediaBrowser = null;
    }

    private void renderSongButtons() {
        trackListContainer.removeAllViews();

        for (MediaBrowser.MediaItem song : songs) {
            Button songButton = new Button(this);
            songButton.setText(song.getDescription().getTitle());
            songButton.setAllCaps(false);
            songButton.setOnClickListener(view ->
                    mediaController.getTransportControls().playFromMediaId(
                            song.getMediaId(),
                            null));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 8);
            trackListContainer.addView(songButton, params);
        }
    }

    private void updateMetadata(MediaMetadata metadata) {
        if (metadata == null) {
            nowPlayingText.setText(R.string.no_track_selected);
            return;
        }

        CharSequence title = metadata.getText(MediaMetadata.METADATA_KEY_TITLE);
        CharSequence artist = metadata.getText(MediaMetadata.METADATA_KEY_ARTIST);
        nowPlayingText.setText(getString(R.string.now_playing_format, title, artist));
    }

    private void updatePlaybackState(PlaybackState state) {
        if (state == null) {
            playbackStatusText.setText(R.string.playback_state_none);
            return;
        }

        switch (state.getState()) {
            case PlaybackState.STATE_BUFFERING:
                playbackStatusText.setText(R.string.playback_state_buffering);
                break;
            case PlaybackState.STATE_PLAYING:
                playbackStatusText.setText(R.string.playback_state_playing);
                break;
            case PlaybackState.STATE_PAUSED:
                playbackStatusText.setText(R.string.playback_state_paused);
                break;
            case PlaybackState.STATE_STOPPED:
                playbackStatusText.setText(R.string.playback_state_stopped);
                break;
            case PlaybackState.STATE_ERROR:
                playbackStatusText.setText(R.string.playback_state_error);
                break;
            default:
                playbackStatusText.setText(R.string.playback_state_none);
                break;
        }
    }

    private void setControlsEnabled(boolean enabled) {
        playButton.setEnabled(enabled);
        pauseButton.setEnabled(enabled);
        stopButton.setEnabled(enabled);
    }
}
