package com.streamtv.app.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.streamtv.app.R;
import com.streamtv.app.network.AudioService;

public class PlayerActivity extends AppCompatActivity implements AudioService.PlayerStateListener {

    private static final String TAG = "PlayerActivity";

    private ImageView ivBackground;
    private ImageView ivCoverLarge;
    private TextView tvPlayerTitle;
    private TextView tvPlayerSubtitle;
    private TextView tvPlayerGenre;
    private TextView tvPlayerDescription;
    private TextView tvPlayerFrequency;
    private TextView tvStatus;
    private ImageButton btnPlayPause;

    private AudioService audioService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        ivBackground = findViewById(R.id.ivBackground);
        ivCoverLarge = findViewById(R.id.ivCoverLarge);
        tvPlayerTitle = findViewById(R.id.tvPlayerTitle);
        tvPlayerSubtitle = findViewById(R.id.tvPlayerSubtitle);
        tvPlayerGenre = findViewById(R.id.tvPlayerGenre);
        tvPlayerDescription = findViewById(R.id.tvPlayerDescription);
        tvPlayerFrequency = findViewById(R.id.tvPlayerFrequency);
        tvStatus = findViewById(R.id.tvStatus);
        btnPlayPause = findViewById(R.id.btnPlayPause);

        audioService = AudioService.getInstance(this);
        audioService.setStateListener(this);

        // Get intent extras
        String name = getIntent().getStringExtra("name");
        String subtitle = getIntent().getStringExtra("subtitle");
        String genre = getIntent().getStringExtra("genre");
        String coverImage = getIntent().getStringExtra("coverImage");
        String streamType = getIntent().getStringExtra("streamType");
        String description = getIntent().getStringExtra("description");
        String frequency = getIntent().getStringExtra("frequency");
        String location = getIntent().getStringExtra("location");

        Log.d(TAG, "Player for: " + name + " | stream: " + streamType);

        // Set title
        if (name != null) tvPlayerTitle.setText(name);

        // Set subtitle (frequency + location or artist + mood)
        if (subtitle != null && !subtitle.isEmpty()) {
            tvPlayerSubtitle.setText(subtitle);
            tvPlayerSubtitle.setVisibility(View.VISIBLE);
        } else {
            tvPlayerSubtitle.setVisibility(View.GONE);
        }

        // Set genre
        if (genre != null && !genre.isEmpty()) {
            tvPlayerGenre.setText(genre);
            tvPlayerGenre.setVisibility(View.VISIBLE);
        } else {
            tvPlayerGenre.setVisibility(View.GONE);
        }

        // Set description
        if (description != null && !description.isEmpty()) {
            tvPlayerDescription.setText(description);
            tvPlayerDescription.setVisibility(View.VISIBLE);
        } else {
            tvPlayerDescription.setVisibility(View.GONE);
        }

        // Set frequency/location extra info
        StringBuilder freqInfo = new StringBuilder();
        if (frequency != null && !frequency.isEmpty()) freqInfo.append(frequency);
        if (location != null && !location.isEmpty()) {
            if (freqInfo.length() > 0) freqInfo.append(" · ");
            freqInfo.append(location);
        }
        if (freqInfo.length() > 0) {
            tvPlayerFrequency.setText(freqInfo.toString());
            tvPlayerFrequency.setVisibility(View.VISIBLE);
        } else {
            tvPlayerFrequency.setVisibility(View.GONE);
        }

        // Load cover image
        if (coverImage != null && !coverImage.isEmpty()) {
            Glide.with(this)
                    .load(coverImage)
                    .placeholder(R.drawable.ic_radio)
                    .error(R.drawable.ic_radio)
                    .centerCrop()
                    .timeout(15000)
                    .into(ivCoverLarge);

            Glide.with(this)
                    .load(coverImage)
                    .centerCrop()
                    .timeout(15000)
                    .into(ivBackground);
        }

        // Update play/pause button
        updatePlayPauseButton();

        // Play/pause click
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnPlayPause.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) btnPlayPause.setScaleX(1.15f);
            else btnPlayPause.setScaleX(1.0f);
        });

        // Auto-update status
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (audioService.isPlaying()) {
                tvStatus.setText("En vivo");
            }
        }, 3000);
    }

    private void togglePlayPause() {
        if (audioService.isPlaying()) {
            audioService.pause();
            tvStatus.setText("Pausado");
        } else {
            audioService.resume();
            tvStatus.setText("Reproduciendo...");
        }
    }

    private void updatePlayPauseButton() {
        if (audioService.isPlaying()) {
            btnPlayPause.setImageResource(R.drawable.ic_pause);
            tvStatus.setText("En vivo");
        } else {
            btnPlayPause.setImageResource(R.drawable.ic_play);
            if (audioService.hasPlayer()) {
                tvStatus.setText("Pausado");
            } else {
                tvStatus.setText("Conectando...");
            }
        }
    }

    @Override
    public void onStateChanged(boolean isPlaying) {
        runOnUiThread(() -> {
            btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
            tvStatus.setText(isPlaying ? "En vivo" : "Pausado");
        });
    }

    @Override
    public void onError(String message) {
        runOnUiThread(() -> {
            tvStatus.setText("Error: " + message);
            Log.e(TAG, "Player error: " + message);
        });
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
        runOnUiThread(() -> {
            if (isLoading) {
                tvStatus.setText("Conectando...");
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            togglePlayPause();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            togglePlayPause();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            audioService.resume();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            audioService.pause();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioService.setStateListener(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePlayPauseButton();
    }
}
