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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.streamtv.app.R;
import com.streamtv.app.model.FavoritesManager;
import com.streamtv.app.model.Station;
import com.streamtv.app.model.StationListHolder;
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
    private TextView tvStationCounter;
    private ImageButton btnPlayPause;
    private ImageButton btnPrevious;
    private ImageButton btnNext;
    private ImageButton btnFavorite;

    private AudioService audioService;
    private FavoritesManager favoritesManager;
    private Station currentStation;

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
        tvStationCounter = findViewById(R.id.tvStationCounter);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        btnFavorite = findViewById(R.id.btnFavorite);

        audioService = AudioService.getInstance(this);
        audioService.setStateListener(this);
        favoritesManager = FavoritesManager.getInstance(this);

        // Get current station from holder
        currentStation = StationListHolder.getCurrentStation();
        if (currentStation == null) {
            // Fallback: get from intent extras
            currentStation = buildStationFromIntent();
        }

        if (currentStation != null) {
            loadStationUI(currentStation);
        }

        // Update play/pause button
        updatePlayPauseButton();

        // Play/pause click
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnPlayPause.setOnFocusChangeListener((v, hasFocus) -> {
            float scale = hasFocus ? 1.12f : 1.0f;
            btnPlayPause.animate().scaleX(scale).scaleY(scale).setDuration(200).start();
        });

        // Previous button
        btnPrevious.setOnClickListener(v -> playPrevious());
        btnPrevious.setOnFocusChangeListener((v, hasFocus) -> {
            float scale = hasFocus ? 1.12f : 1.0f;
            btnPrevious.animate().scaleX(scale).scaleY(scale).setDuration(200).start();
        });

        // Next button
        btnNext.setOnClickListener(v -> playNext());
        btnNext.setOnFocusChangeListener((v, hasFocus) -> {
            float scale = hasFocus ? 1.12f : 1.0f;
            btnNext.animate().scaleX(scale).scaleY(scale).setDuration(200).start();
        });

        // Favorite button
        updateFavoriteButton();
        btnFavorite.setOnClickListener(v -> toggleFavorite());
        btnFavorite.setOnFocusChangeListener((v, hasFocus) -> {
            float scale = hasFocus ? 1.12f : 1.0f;
            btnFavorite.animate().scaleX(scale).scaleY(scale).setDuration(200).start();
        });

        // Update station counter
        updateStationCounter();

        // Auto-update status
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (audioService.isPlaying()) {
                tvStatus.setText("En vivo");
            }
        }, 3000);
    }

    /**
     * Build a Station object from intent extras (fallback)
     */
    private Station buildStationFromIntent() {
        Station station = new Station();
        station.setName(getIntent().getStringExtra("name"));
        station.setUrl(getIntent().getStringExtra("url"));
        station.setGenre(getIntent().getStringExtra("genre"));
        station.setCoverImage(getIntent().getStringExtra("coverImage"));
        station.setStreamType(getIntent().getStringExtra("streamType"));
        station.setDescription(getIntent().getStringExtra("description"));
        station.setFrequency(getIntent().getStringExtra("frequency"));
        station.setLocation(getIntent().getStringExtra("location"));
        // Try to get subtitle from extras
        String subtitle = getIntent().getStringExtra("subtitle");
        if (subtitle != null) station.setArtist(subtitle);
        return station;
    }

    /**
     * Load all UI elements for a given station
     */
    private void loadStationUI(Station station) {
        // Set title
        if (station.getName() != null) tvPlayerTitle.setText(station.getName());

        // Set subtitle
        String subtitle = station.getSubtitle();
        if (subtitle != null && !subtitle.isEmpty()) {
            tvPlayerSubtitle.setText(subtitle);
            tvPlayerSubtitle.setVisibility(View.VISIBLE);
        } else {
            tvPlayerSubtitle.setVisibility(View.GONE);
        }

        // Set genre
        if (station.getGenre() != null && !station.getGenre().isEmpty()) {
            tvPlayerGenre.setText(station.getGenre());
            tvPlayerGenre.setVisibility(View.VISIBLE);
        } else {
            tvPlayerGenre.setVisibility(View.GONE);
        }

        // Set description
        if (station.getDescription() != null && !station.getDescription().isEmpty()) {
            tvPlayerDescription.setText(station.getDescription());
            tvPlayerDescription.setVisibility(View.VISIBLE);
        } else {
            tvPlayerDescription.setVisibility(View.GONE);
        }

        // Set frequency/location
        StringBuilder freqInfo = new StringBuilder();
        String frequency = station.getFrequency();
        String location = station.getLocation();
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
        String coverImage = station.getCoverImage();
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

        // Update favorite button
        updateFavoriteButton();

        // Update station counter
        updateStationCounter();
    }

    /**
     * Play a new station and update UI
     */
    private void playStation(Station station) {
        if (station == null || !station.isPlayable()) return;

        currentStation = station;
        String playbackUrl = station.getPlaybackUrl();

        Log.d(TAG, "Playing: " + station.getName() + " | URL: " + playbackUrl);

        audioService.play(playbackUrl);
        loadStationUI(station);
        updatePlayPauseButton();
    }

    private void playPrevious() {
        Station prev = StationListHolder.getPreviousStation();
        if (prev != null) {
            playStation(prev);
            Toast.makeText(this, "Anterior: " + prev.getName(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No hay estación anterior", Toast.LENGTH_SHORT).show();
        }
    }

    private void playNext() {
        Station next = StationListHolder.getNextStation();
        if (next != null) {
            playStation(next);
            Toast.makeText(this, "Siguiente: " + next.getName(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No hay estación siguiente", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleFavorite() {
        if (currentStation == null) return;
        boolean isNowFav = favoritesManager.toggleFavorite(currentStation);
        updateFavoriteButton();
        if (isNowFav) {
            Toast.makeText(this, "Agregado a favoritos", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateFavoriteButton() {
        if (currentStation == null || btnFavorite == null) return;
        boolean isFav = favoritesManager.isFavorite(currentStation);
        btnFavorite.setImageResource(isFav ? R.drawable.ic_favorite : R.drawable.ic_favorite_outline);
    }

    private void updateStationCounter() {
        if (tvStationCounter == null) return;
        int total = StationListHolder.getStations() != null ? StationListHolder.getStations().size() : 0;
        int index = StationListHolder.getCurrentIndex();
        if (total > 1) {
            tvStationCounter.setText((index + 1) + " / " + total);
            tvStationCounter.setVisibility(View.VISIBLE);
        } else {
            tvStationCounter.setVisibility(View.GONE);
        }
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
            // Let the focused button handle it
            return super.onKeyDown(keyCode, event);
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
        if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
            playNext();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
            playPrevious();
            return true;
        }
        // D-pad left/right for prev/next when no button is focused
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            View focused = getCurrentFocus();
            if (focused == btnNext || focused == btnPlayPause || focused == btnPrevious || focused == btnFavorite) {
                return super.onKeyDown(keyCode, event);
            }
            playNext();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            View focused = getCurrentFocus();
            if (focused == btnNext || focused == btnPlayPause || focused == btnPrevious || focused == btnFavorite) {
                return super.onKeyDown(keyCode, event);
            }
            playPrevious();
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
        updateFavoriteButton();
    }
}
