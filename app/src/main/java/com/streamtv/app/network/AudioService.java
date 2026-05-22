package com.streamtv.app.network;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.session.MediaSession;

import java.util.HashMap;
import java.util.Map;

public class AudioService {
    private static final String TAG = "AudioService";
    private static final String CHANNEL_ID = "streamtv_playback";
    private static AudioService instance;

    private ExoPlayer player;
    private MediaSession mediaSession;
    private Context context;
    private PlayerStateListener stateListener;
    private String currentUrl;

    public interface PlayerStateListener {
        void onStateChanged(boolean isPlaying);
        void onError(String message);
        void onLoadingChanged(boolean isLoading);
    }

    public static synchronized AudioService getInstance(Context context) {
        if (instance == null) {
            instance = new AudioService(context.getApplicationContext());
        }
        return instance;
    }

    private AudioService(Context context) {
        this.context = context;
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "StreamTV Playback",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Radio streaming playback");
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    public void play(String url) {
        Log.d(TAG, "play() called with URL: " + url);

        if (player != null) {
            player.stop();
            player.release();
        }
        if (mediaSession != null) {
            mediaSession.release();
        }

        currentUrl = url;

        // Create custom HTTP DataSource with proper User-Agent for redirects
        DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent("StreamTV/3.0 (Android)")
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(15000)
                .setAllowCrossProtocolRedirects(true);

        // Build ExoPlayer with custom data source
        player = new ExoPlayer.Builder(context)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(context)
                        .setDataSourceFactory(httpDataSourceFactory))
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(), true)
                .setHandleAudioBecomingNoisy(true)
                .build();

        mediaSession = new MediaSession.Builder(context, player)
                .build();

        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                Log.d(TAG, "isPlaying changed: " + isPlaying);
                if (stateListener != null) stateListener.onStateChanged(isPlaying);
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Log.e(TAG, "Player error: " + error.getMessage(), error);
                if (stateListener != null) stateListener.onError(error.getMessage());
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                String stateStr;
                switch (playbackState) {
                    case Player.STATE_IDLE: stateStr = "IDLE"; break;
                    case Player.STATE_BUFFERING: stateStr = "BUFFERING"; break;
                    case Player.STATE_READY: stateStr = "READY"; break;
                    case Player.STATE_ENDED: stateStr = "ENDED"; break;
                    default: stateStr = "UNKNOWN"; break;
                }
                Log.d(TAG, "Playback state: " + stateStr);
                if (stateListener != null) {
                    stateListener.onLoadingChanged(playbackState == Player.STATE_BUFFERING);
                }
            }
        });

        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(url)
                .setLiveConfiguration(
                        new MediaItem.LiveConfiguration.Builder()
                                .setTargetOffsetMs(C.TIME_UNSET)
                                .build()
                )
                .build();

        player.setMediaItem(mediaItem);
        player.prepare();
        player.setPlayWhenReady(true);
    }

    public void pause() {
        if (player != null) player.pause();
    }

    public void resume() {
        if (player != null) player.play();
    }

    public void stop() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        currentUrl = null;
    }

    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    public boolean isBuffering() {
        return player != null && player.getPlaybackState() == Player.STATE_BUFFERING;
    }

    public boolean hasPlayer() {
        return player != null;
    }

    public String getCurrentUrl() {
        return currentUrl;
    }

    public void setStateListener(PlayerStateListener listener) {
        this.stateListener = listener;
    }
}
