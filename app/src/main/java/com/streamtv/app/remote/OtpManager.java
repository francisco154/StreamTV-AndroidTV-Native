package com.streamtv.app.remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import java.util.Random;

/**
 * Manages OTP code generation and validation for remote control pairing.
 * - Generates a 6-digit code
 * - Code auto-expires after 5 minutes
 * - Validates codes received from MediaCenter
 */
public class OtpManager {

    private static final String PREFS_NAME = "radioplayer_tv_remote";
    private static final String KEY_CURRENT_OTP = "current_otp";
    private static final String KEY_OTP_EXPIRY = "otp_expiry";
    private static final String KEY_PAIRED_DEVICE = "paired_device";
    private static final String KEY_IS_PAIRED = "is_paired";

    private static final long OTP_VALIDITY_MS = 5 * 60 * 1000; // 5 minutes
    private static final long OTP_REFRESH_MS = 2 * 60 * 1000; // refresh every 2 minutes

    private static OtpManager instance;
    private final SharedPreferences prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String currentOtp;
    private long otpGeneratedAt;
    private Runnable expiryRunnable;
    private Runnable refreshRunnable;
    private OtpListener listener;

    public interface OtpListener {
        void onOtpGenerated(String otp);
        void onOtpExpired();
        void onDevicePaired(String deviceId);
        void onDeviceUnpaired();
    }

    private OtpManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadState();
    }

    public static synchronized OtpManager getInstance(Context context) {
        if (instance == null) {
            instance = new OtpManager(context);
        }
        return instance;
    }

    private void loadState() {
        currentOtp = prefs.getString(KEY_CURRENT_OTP, null);
        otpGeneratedAt = prefs.getLong(KEY_OTP_EXPIRY, 0) - OTP_VALIDITY_MS;

        // Check if stored OTP is still valid
        if (currentOtp != null && !isOtpValid()) {
            currentOtp = null;
            prefs.edit().remove(KEY_CURRENT_OTP).remove(KEY_OTP_EXPIRY).apply();
        }
    }

    /**
     * Generate a new 6-digit OTP code
     */
    public String generateOtp() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 6-digit number
        currentOtp = String.valueOf(code);
        otpGeneratedAt = System.currentTimeMillis();

        // Save to prefs
        prefs.edit()
            .putString(KEY_CURRENT_OTP, currentOtp)
            .putLong(KEY_OTP_EXPIRY, otpGeneratedAt + OTP_VALIDITY_MS)
            .apply();

        // Schedule expiry
        scheduleExpiry();
        // Schedule auto-refresh
        scheduleRefresh();

        if (listener != null) {
            listener.onOtpGenerated(currentOtp);
        }

        return currentOtp;
    }

    /**
     * Get the current OTP (or generate one if none exists)
     */
    public String getCurrentOtp() {
        if (currentOtp == null || !isOtpValid()) {
            return generateOtp();
        }
        return currentOtp;
    }

    /**
     * Check if current OTP is still valid
     */
    public boolean isOtpValid() {
        if (currentOtp == null) return false;
        long expiry = otpGeneratedAt + OTP_VALIDITY_MS;
        return System.currentTimeMillis() < expiry;
    }

    /**
     * Get remaining seconds before OTP expires
     */
    public long getRemainingSeconds() {
        if (currentOtp == null) return 0;
        long remaining = (otpGeneratedAt + OTP_VALIDITY_MS - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    /**
     * Validate an OTP code received from MediaCenter
     */
    public boolean validateOtp(String otp, String deviceId) {
        if (otp == null || currentOtp == null || !isOtpValid()) {
            return false;
        }

        if (otp.equals(currentOtp)) {
            // Valid! Pair the device
            pairDevice(deviceId);
            // Invalidate the OTP so it can't be reused
            currentOtp = null;
            prefs.edit().remove(KEY_CURRENT_OTP).remove(KEY_OTP_EXPIRY).apply();
            cancelScheduledTasks();
            if (listener != null) {
                listener.onDevicePaired(deviceId);
            }
            return true;
        }

        return false;
    }

    /**
     * Pair a device
     */
    public void pairDevice(String deviceId) {
        prefs.edit()
            .putBoolean(KEY_IS_PAIRED, true)
            .putString(KEY_PAIRED_DEVICE, deviceId)
            .apply();
    }

    /**
     * Unpair current device
     */
    public void unpair() {
        prefs.edit()
            .remove(KEY_IS_PAIRED)
            .remove(KEY_PAIRED_DEVICE)
            .apply();
        if (listener != null) {
            listener.onDeviceUnpaired();
        }
    }

    /**
     * Check if a device is paired
     */
    public boolean isPaired() {
        return prefs.getBoolean(KEY_IS_PAIRED, false);
    }

    /**
     * Get the paired device ID
     */
    public String getPairedDeviceId() {
        return prefs.getString(KEY_PAIRED_DEVICE, "");
    }

    /**
     * Set listener for OTP events
     */
    public void setListener(OtpListener listener) {
        this.listener = listener;
    }

    private void scheduleExpiry() {
        if (expiryRunnable != null) {
            handler.removeCallbacks(expiryRunnable);
        }
        expiryRunnable = () -> {
            if (currentOtp != null && !isOtpValid()) {
                currentOtp = null;
                prefs.edit().remove(KEY_CURRENT_OTP).remove(KEY_OTP_EXPIRY).apply();
                if (listener != null) {
                    listener.onOtpExpired();
                }
            }
        };
        handler.postDelayed(expiryRunnable, OTP_VALIDITY_MS);
    }

    private void scheduleRefresh() {
        if (refreshRunnable != null) {
            handler.removeCallbacks(refreshRunnable);
        }
        refreshRunnable = () -> {
            if (!isPaired()) {
                generateOtp(); // Auto-generate new OTP
            }
        };
        handler.postDelayed(refreshRunnable, OTP_REFRESH_MS);
    }

    private void cancelScheduledTasks() {
        if (expiryRunnable != null) handler.removeCallbacks(expiryRunnable);
        if (refreshRunnable != null) handler.removeCallbacks(refreshRunnable);
    }

    /**
     * Clean up
     */
    public void destroy() {
        cancelScheduledTasks();
        listener = null;
    }
}
