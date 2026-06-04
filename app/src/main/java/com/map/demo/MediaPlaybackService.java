package com.map.demo;

import android.media.AudioAttributes;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.service.media.MediaBrowserService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MediaPlaybackService extends MediaBrowserService {

    private static final String ROOT_ID = "media_root";
    private static final String SONGS_ID = "songs";
    private static final String ALBUMS_ID = "albums";
    private static final String PLAYLISTS_ID = "playlists";
    private static final String DEMO_ALBUM_ID = "album_demo_drive";
    private static final String DEMO_PLAYLIST_ID = "playlist_demo_drive";

    private static final Track[] TRACKS = {
            new Track(
                    "song_1",
                    "SoundHelix Song 1",
                    "SoundHelix",
                    "Demo Drive Album",
                    "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
            new Track(
                    "song_2",
                    "SoundHelix Song 2",
                    "SoundHelix",
                    "Demo Drive Album",
                    "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
            new Track(
                    "song_3",
                    "SoundHelix Song 3",
                    "SoundHelix",
                    "Demo Drive Album",
                    "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3")
    };

    private MediaSession mediaSession;
    private MediaPlayer mediaPlayer;
    private Track currentTrack = TRACKS[0];

    @Override
    public void onCreate() {
        super.onCreate();

        mediaSession = new MediaSession(this, "DemoMediaSession");
        mediaSession.setFlags(
                MediaSession.FLAG_HANDLES_MEDIA_BUTTONS
                        | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                playCurrentTrack();
            }

            @Override
            public void onPlayFromMediaId(String mediaId, Bundle extras) {
                Track selectedTrack = findTrack(mediaId);
                if (selectedTrack != null) {
                    currentTrack = selectedTrack;
                    playCurrentTrack();
                }
            }

            @Override
            public void onPause() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                }
                updatePlaybackState(PlaybackState.STATE_PAUSED);
            }

            @Override
            public void onStop() {
                stopPlayer();
                updatePlaybackState(PlaybackState.STATE_STOPPED);
            }
        });

        setSessionToken(mediaSession.getSessionToken());
        updateMetadata(currentTrack);
        updatePlaybackState(PlaybackState.STATE_STOPPED);
        mediaSession.setActive(true);
    }

    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        return new BrowserRoot(ROOT_ID, null);
    }

    @Override
    public void onLoadChildren(String parentId, Result<List<MediaBrowser.MediaItem>> result) {
        List<MediaBrowser.MediaItem> items = new ArrayList<>();

        switch (parentId) {
            case ROOT_ID:
                items.add(createBrowsableItem(SONGS_ID, getString(R.string.browse_songs), null));
                items.add(createBrowsableItem(ALBUMS_ID, getString(R.string.browse_albums), null));
                items.add(createBrowsableItem(PLAYLISTS_ID, getString(R.string.browse_playlists), null));
                break;
            case SONGS_ID:
            case DEMO_ALBUM_ID:
            case DEMO_PLAYLIST_ID:
                for (Track track : TRACKS) {
                    items.add(createPlayableItem(track));
                }
                break;
            case ALBUMS_ID:
                items.add(createBrowsableItem(
                        DEMO_ALBUM_ID,
                        getString(R.string.demo_album_title),
                        getString(R.string.demo_album_subtitle)));
                break;
            case PLAYLISTS_ID:
                items.add(createBrowsableItem(
                        DEMO_PLAYLIST_ID,
                        getString(R.string.demo_playlist_title),
                        getString(R.string.demo_playlist_subtitle)));
                break;
            default:
                break;
        }

        result.sendResult(items);
    }

    @Override
    public void onDestroy() {
        stopPlayer();
        mediaSession.release();
        super.onDestroy();
    }

    private MediaBrowser.MediaItem createBrowsableItem(String mediaId, String title, String subtitle) {
        MediaDescription description = new MediaDescription.Builder()
                .setMediaId(mediaId)
                .setTitle(title)
                .setSubtitle(subtitle)
                .build();

        return new MediaBrowser.MediaItem(description, MediaBrowser.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowser.MediaItem createPlayableItem(Track track) {
        MediaDescription description = new MediaDescription.Builder()
                .setMediaId(track.id)
                .setTitle(track.title)
                .setSubtitle(track.artist)
                .setMediaUri(Uri.parse(track.url))
                .build();

        return new MediaBrowser.MediaItem(description, MediaBrowser.MediaItem.FLAG_PLAYABLE);
    }

    private void playCurrentTrack() {
        updateMetadata(currentTrack);
        updatePlaybackState(PlaybackState.STATE_BUFFERING);
        stopPlayer();

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build());
        mediaPlayer.setOnPreparedListener(player -> {
            player.start();
            updatePlaybackState(PlaybackState.STATE_PLAYING);
        });
        mediaPlayer.setOnCompletionListener(player -> updatePlaybackState(PlaybackState.STATE_STOPPED));
        mediaPlayer.setOnErrorListener((player, what, extra) -> {
            updatePlaybackState(PlaybackState.STATE_ERROR);
            return true;
        });

        try {
            mediaPlayer.setDataSource(currentTrack.url);
            mediaPlayer.prepareAsync();
        } catch (IOException | IllegalArgumentException exception) {
            updatePlaybackState(PlaybackState.STATE_ERROR);
        }
    }

    private void stopPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private Track findTrack(String mediaId) {
        for (Track track : TRACKS) {
            if (track.id.equals(mediaId)) {
                return track;
            }
        }
        return null;
    }

    private void updateMetadata(Track track) {
        MediaMetadata metadata = new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, track.id)
                .putString(MediaMetadata.METADATA_KEY_TITLE, track.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, track.album)
                .build();

        mediaSession.setMetadata(metadata);
    }

    private void updatePlaybackState(int state) {
        PlaybackState playbackState = new PlaybackState.Builder()
                .setActions(
                        PlaybackState.ACTION_PLAY
                                | PlaybackState.ACTION_PLAY_FROM_MEDIA_ID
                                | PlaybackState.ACTION_PAUSE
                                | PlaybackState.ACTION_STOP)
                .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f,
                        SystemClock.elapsedRealtime())
                .build();

        mediaSession.setPlaybackState(playbackState);
    }

    private static class Track {
        final String id;
        final String title;
        final String artist;
        final String album;
        final String url;

        Track(String id, String title, String artist, String album, String url) {
            this.id = id;
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.url = url;
        }
    }
}
