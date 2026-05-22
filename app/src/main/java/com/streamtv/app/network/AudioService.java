package com.streamtv.app.network;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;

public class AudioService {
    private static final String TAG = "AudioService";
    private static final String CHANNEL_ID = "streamtv_playback";
    private static AudioService instance;

    private ExoPlayer player;
    private MediaSession mediaSession;
    private Context context;
    private PlayerStateListener stateListener;

    public interface PlayerStateListener {
        void onStateChanged(boolean isPlaying);
        void onError(String message);
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
        if (player != null) {
            player.stop();
            player.release();
        }
        if (mediaSession != null) {
            mediaSession.release();
        }

        player = new ExoPlayer.Builder(context)
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
                if (stateListener != null) stateListener.onStateChanged(isPlaying);
            }

            @Override
            public void onPlayerError(androidx.media3.common.PlaybackException error) {
                if (stateListener != null) stateListener.onError(error.getMessage());
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
    }

    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    public boolean hasPlayer() {
        return player != null;
    }

    public void setStateListener(PlayerStateListener listener) {
        this.stateListener = listener;
    }
}
