package com.streamtv.app.remote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;

import com.streamtv.app.network.AudioService;
import com.streamtv.app.model.FavoritesManager;
import com.streamtv.app.model.Station;
import com.streamtv.app.model.StationListHolder;

/**
 * BroadcastReceiver that receives remote control commands from MediaCenter.
 * Actions:
 * - com.radioplayer.tv.OTP_PAIR: Pair with MediaCenter via OTP code
 * - com.radioplayer.tv.COMMAND_PLAY: Resume playback
 * - com.radioplayer.tv.COMMAND_PAUSE: Pause playback
 * - com.radioplayer.tv.COMMAND_PLAY_PAUSE: Toggle play/pause
 * - com.radioplayer.tv.COMMAND_NEXT: Next station
 * - com.radioplayer.tv.COMMAND_PREVIOUS: Previous station
 * - com.radioplayer.tv.COMMAND_FAVORITE: Toggle favorite
 * - com.radioplayer.tv.COMMAND_STOP: Stop playback
 * - com.radioplayer.tv.COMMAND_VOLUME_UP: Volume up
 * - com.radioplayer.tv.COMMAND_VOLUME_DOWN: Volume down
 * - com.radioplayer.tv.REQUEST_STATUS: Request current playback state
 */
public class RemoteControlReceiver extends BroadcastReceiver {

    private static final String TAG = "RemoteControlReceiver";

    // OTP pairing
    public static final String ACTION_OTP_PAIR = "com.radioplayer.tv.OTP_PAIR";
    public static final String ACTION_OTP_RESULT = "com.radioplayer.tv.OTP_RESULT";

    // Commands
    public static final String ACTION_COMMAND_PLAY = "com.radioplayer.tv.COMMAND_PLAY";
    public static final String ACTION_COMMAND_PAUSE = "com.radioplayer.tv.COMMAND_PAUSE";
    public static final String ACTION_COMMAND_PLAY_PAUSE = "com.radioplayer.tv.COMMAND_PLAY_PAUSE";
    public static final String ACTION_COMMAND_NEXT = "com.radioplayer.tv.COMMAND_NEXT";
    public static final String ACTION_COMMAND_PREVIOUS = "com.radioplayer.tv.COMMAND_PREVIOUS";
    public static final String ACTION_COMMAND_FAVORITE = "com.radioplayer.tv.COMMAND_FAVORITE";
    public static final String ACTION_COMMAND_STOP = "com.radioplayer.tv.COMMAND_STOP";
    public static final String ACTION_COMMAND_VOLUME_UP = "com.radioplayer.tv.COMMAND_VOLUME_UP";
    public static final String ACTION_COMMAND_VOLUME_DOWN = "com.radioplayer.tv.COMMAND_VOLUME_DOWN";
    public static final String ACTION_REQUEST_STATUS = "com.radioplayer.tv.REQUEST_STATUS";

    // Playback state broadcast (TV → MediaCenter)
    public static final String ACTION_PLAYBACK_UPDATE = "com.radioplayer.tv.PLAYBACK_UPDATE";

    // Listener for UI updates
    public interface RemoteCommandListener {
        void onPlaybackStateChanged();
        void onDevicePaired(String deviceId);
    }

    private static RemoteCommandListener commandListener;

    public static void setCommandListener(RemoteCommandListener listener) {
        commandListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        Log.d(TAG, "Received: " + action);

        switch (action) {
            case ACTION_OTP_PAIR: {
                String otp = intent.getStringExtra("otp");
                String deviceId = intent.getStringExtra("device_id");
                if (otp != null && deviceId != null) {
                    OtpManager otpManager = OtpManager.getInstance(context);
                    boolean valid = otpManager.validateOtp(otp, deviceId);

                    // Send result back to MediaCenter
                    Intent result = new Intent(ACTION_OTP_RESULT);
                    result.putExtra("success", valid);
                    result.putExtra("device_id", deviceId);
                    result.setPackage("com.app.mediacenter");
                    if (android.os.Build.VERSION.SDK_INT >= 34) {
                        result.addFlags(0x01000000);
                    }
                    context.sendBroadcast(result);

                    if (valid && commandListener != null) {
                        commandListener.onDevicePaired(deviceId);
                    }
                }
                break;
            }

            case ACTION_COMMAND_PLAY: {
                AudioService audioService = AudioService.getInstance(context);
                audioService.resume();
                sendPlaybackUpdate(context);
                break;
            }

            case ACTION_COMMAND_PAUSE: {
                AudioService audioService = AudioService.getInstance(context);
                audioService.pause();
                sendPlaybackUpdate(context);
                break;
            }

            case ACTION_COMMAND_PLAY_PAUSE: {
                AudioService audioService = AudioService.getInstance(context);
                if (audioService.isPlaying()) {
                    audioService.pause();
                } else {
                    audioService.resume();
                }
                sendPlaybackUpdate(context);
                break;
            }

            case ACTION_COMMAND_NEXT: {
                Station next = StationListHolder.getNextStation();
                if (next != null) {
                    AudioService audioService = AudioService.getInstance(context);
                    audioService.play(next.getPlaybackUrl());
                }
                sendPlaybackUpdate(context);
                break;
            }

            case ACTION_COMMAND_PREVIOUS: {
                Station prev = StationListHolder.getPreviousStation();
                if (prev != null) {
                    AudioService audioService = AudioService.getInstance(context);
                    audioService.play(prev.getPlaybackUrl());
                }
                sendPlaybackUpdate(context);
                break;
            }

            case ACTION_COMMAND_FAVORITE: {
                Station current = StationListHolder.getCurrentStation();
                if (current != null) {
                    FavoritesManager favoritesManager = FavoritesManager.getInstance(context);
                    favoritesManager.toggleFavorite(current);
                }
                sendPlaybackUpdate(context);
                break;
            }

            case ACTION_COMMAND_STOP: {
                AudioService audioService = AudioService.getInstance(context);
                audioService.stop();
                sendPlaybackUpdate(context);
                break;
            }

            case ACTION_COMMAND_VOLUME_UP: {
                AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (am != null) {
                    am.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                }
                break;
            }

            case ACTION_COMMAND_VOLUME_DOWN: {
                AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (am != null) {
                    am.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                }
                break;
            }

            case ACTION_REQUEST_STATUS: {
                sendPlaybackUpdate(context);
                break;
            }
        }

        if (commandListener != null && !ACTION_OTP_PAIR.equals(action)) {
            commandListener.onPlaybackStateChanged();
        }
    }

    /**
     * Send current playback state to MediaCenter
     */
    public static void sendPlaybackUpdate(Context context) {
        try {
            AudioService audioService = AudioService.getInstance(context);
            Station current = StationListHolder.getCurrentStation();
            FavoritesManager favManager = FavoritesManager.getInstance(context);

            Intent intent = new Intent(ACTION_PLAYBACK_UPDATE);
            if (current != null) {
                intent.putExtra("station_name", current.getName() != null ? current.getName() : "");
                intent.putExtra("station_genre", current.getGenre() != null ? current.getGenre() : "");
                intent.putExtra("station_image", current.getCoverImage() != null ? current.getCoverImage() : "");
                intent.putExtra("station_type", current.getStreamType() != null ? current.getStreamType() : "");
                intent.putExtra("station_artist", current.getSubtitle() != null ? current.getSubtitle() : "");
                intent.putExtra("station_frequency", current.getFrequency() != null ? current.getFrequency() : "");
                intent.putExtra("is_favorite", favManager.isFavorite(current));
            } else {
                intent.putExtra("station_name", "");
            }
            intent.putExtra("is_playing", audioService.isPlaying());
            intent.putExtra("has_previous", StationListHolder.hasPrevious());
            intent.putExtra("has_next", StationListHolder.hasNext());
            intent.putExtra("is_paired", OtpManager.getInstance(context).isPaired());

            intent.setPackage("com.app.mediacenter");
            if (android.os.Build.VERSION.SDK_INT >= 34) {
                intent.addFlags(0x01000000);
            }
            context.sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error sending playback update", e);
        }
    }
}
