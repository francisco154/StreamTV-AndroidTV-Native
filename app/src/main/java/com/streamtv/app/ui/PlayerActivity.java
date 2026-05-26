package com.streamtv.app.ui;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
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
import com.streamtv.app.remote.OtpManager;
import com.streamtv.app.remote.RemoteControlReceiver;

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

        // KILL ALL RECTANGULAR HIGHLIGHTS - force circular outline on every button
        setupCircularButton(btnPlayPause, 1.15f);
        setupCircularButton(btnPrevious, 1.15f);
        setupCircularButton(btnNext, 1.15f);
        setupCircularButton(btnFavorite, 1.15f);

        // Get current station from holder
        currentStation = StationListHolder.getCurrentStation();
        if (currentStation == null) {
            currentStation = buildStationFromIntent();
        }

        if (currentStation != null) {
            loadStationUI(currentStation);
        }

        // Update play/pause button
        updatePlayPauseButton();

        // Button click listeners
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnPrevious.setOnClickListener(v -> playPrevious());
        btnNext.setOnClickListener(v -> playNext());
        btnFavorite.setOnClickListener(v -> toggleFavorite());

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
     * Force a circular outline and eliminate ALL rectangular highlights.
     */
    private void setupCircularButton(ImageButton btn, float focusScale) {
        btn.setDefaultFocusHighlightEnabled(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            btn.setStateListAnimator(null);
            btn.setOutlineProvider(ViewOutlineProvider.BOUNDS);
            btn.setElevation(0f);
            btn.setTranslationZ(0f);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            btn.setClipToOutline(true);
            btn.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, android.graphics.Outline outline) {
                    int w = view.getWidth();
                    int h = view.getHeight();
                    if (w > 0 && h > 0) {
                        int r = Math.min(w, h) / 2;
                        outline.setOval(0, 0, r * 2, r * 2);
                    }
                }
            });
        }
        btn.setOnFocusChangeListener((v, hasFocus) -> {
            float scale = hasFocus ? focusScale : 1.0f;
            v.animate()
                    .scaleX(scale)
                    .scaleY(scale)
                    .alpha(hasFocus ? 1.0f : 0.7f)
                    .setDuration(200)
                    .start();
        });
        btn.setAlpha(0.7f);
    }

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
        String subtitle = getIntent().getStringExtra("subtitle");
        if (subtitle != null) station.setArtist(subtitle);
        return station;
    }

    private void loadStationUI(Station station) {
        if (station.getName() != null) tvPlayerTitle.setText(station.getName());

        String subtitle = station.getSubtitle();
        if (subtitle != null && !subtitle.isEmpty()) {
            tvPlayerSubtitle.setText(subtitle);
            tvPlayerSubtitle.setVisibility(View.VISIBLE);
        } else {
            tvPlayerSubtitle.setVisibility(View.GONE);
        }

        if (station.getGenre() != null && !station.getGenre().isEmpty()) {
            tvPlayerGenre.setText(station.getGenre());
            tvPlayerGenre.setVisibility(View.VISIBLE);
        } else {
            tvPlayerGenre.setVisibility(View.GONE);
        }

        if (station.getDescription() != null && !station.getDescription().isEmpty()) {
            tvPlayerDescription.setText(station.getDescription());
            tvPlayerDescription.setVisibility(View.VISIBLE);
        } else {
            tvPlayerDescription.setVisibility(View.GONE);
        }

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

        updateFavoriteButton();
        updateStationCounter();
    }

    private void playStation(Station station) {
        if (station == null || !station.isPlayable()) return;
        currentStation = station;
        String playbackUrl = station.getPlaybackUrl();
        Log.d(TAG, "Playing: " + station.getName() + " | URL: " + playbackUrl);
        audioService.play(playbackUrl);
        loadStationUI(station);
        updatePlayPauseButton();
        RemoteControlReceiver.sendPlaybackUpdate(this);
    }

    private void playPrevious() {
        Station prev = StationListHolder.getPreviousStation();
        if (prev != null) {
            playStation(prev);
            Toast.makeText(this, "Anterior: " + prev.getName(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No hay estacion anterior", Toast.LENGTH_SHORT).show();
        }
    }

    private void playNext() {
        Station next = StationListHolder.getNextStation();
        if (next != null) {
            playStation(next);
            Toast.makeText(this, "Siguiente: " + next.getName(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No hay estacion siguiente", Toast.LENGTH_SHORT).show();
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
        RemoteControlReceiver.sendPlaybackUpdate(this);
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
        RemoteControlReceiver.sendPlaybackUpdate(this);
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
            RemoteControlReceiver.sendPlaybackUpdate(PlayerActivity.this);
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
