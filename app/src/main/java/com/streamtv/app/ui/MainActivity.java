package com.streamtv.app.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.streamtv.app.App;
import com.streamtv.app.R;
import com.streamtv.app.adapter.StationAdapter;
import com.streamtv.app.model.CategoriesResponse;
import com.streamtv.app.model.Category;
import com.streamtv.app.model.FavoritesManager;
import com.streamtv.app.model.Station;
import com.streamtv.app.model.StationListHolder;
import com.streamtv.app.network.AudioService;
import com.streamtv.app.network.DataFetcher;
import com.streamtv.app.remote.OtpManager;
import com.streamtv.app.remote.RemoteControlReceiver;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AudioService.PlayerStateListener {

    private static final String TAG = "MainActivity";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvError;
    private TextView tvHeaderInfo;

    // Tabs
    private TextView tabRadios, tabDemos, tabFavorites;

    // Now playing bar
    private LinearLayout nowPlayingBar;
    private ImageView npCover;
    private TextView npTitle, npSubtitle;
    private ImageButton npPlayPause;
    private ImageButton npPrev;
    private ImageButton npNext;

    // OTP / Remote control
    private LinearLayout otpPanel;
    private TextView tvOtpCode;
    private TextView tvOtpTimer;
    private TextView tvOtpStatus;
    private TextView tvPairedInfo;
    private Handler otpHandler = new Handler(Looper.getMainLooper());
    private Runnable otpRefreshRunnable;

    // Data
    private DataFetcher dataFetcher;
    private CategoriesResponse categoriesData;
    private StationAdapter radioAdapter;
    private StationAdapter demoAdapter;
    private StationAdapter favoriteAdapter;
    private AudioService audioService;
    private FavoritesManager favoritesManager;
    private OtpManager otpManager;

    // Current playing station (stored for player activity)
    private Station currentStation;

    // Station lists
    private List<Station> radioStations = new ArrayList<>();
    private List<Station> demoStations = new ArrayList<>();

    private int currentTab = 0; // 0 = radios, 1 = demos, 2 = favorites

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check login
        if (!App.getInstance().isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Init views
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvError = findViewById(R.id.tvError);
        tvHeaderInfo = findViewById(R.id.tvHeaderInfo);
        tabRadios = findViewById(R.id.tabRadios);
        tabDemos = findViewById(R.id.tabDemos);
        tabFavorites = findViewById(R.id.tabFavorites);
        nowPlayingBar = findViewById(R.id.nowPlayingBar);
        npCover = findViewById(R.id.npCover);
        npTitle = findViewById(R.id.npTitle);
        npSubtitle = findViewById(R.id.npSubtitle);
        npPlayPause = findViewById(R.id.npPlayPause);
        npPrev = findViewById(R.id.npPrev);
        npNext = findViewById(R.id.npNext);

        // OTP views
        otpPanel = findViewById(R.id.otpPanel);
        tvOtpCode = findViewById(R.id.tvOtpCode);
        tvOtpTimer = findViewById(R.id.tvOtpTimer);
        tvOtpStatus = findViewById(R.id.tvOtpStatus);
        tvPairedInfo = findViewById(R.id.tvPairedInfo);

        // Setup recycler with 5 columns
        GridLayoutManager layoutManager = new GridLayoutManager(this, 5);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);

        // Add spacing between cards
        int spacing = 16;
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(5, spacing, false));

        // Setup adapters
        radioAdapter = new StationAdapter(false, this::onStationClicked);
        demoAdapter = new StationAdapter(true, this::onStationClicked);
        favoriteAdapter = new StationAdapter(false, this::onStationClicked);
        recyclerView.setAdapter(radioAdapter);

        // Favorites manager
        favoritesManager = FavoritesManager.getInstance(this);

        // OTP Manager
        otpManager = OtpManager.getInstance(this);
        setupOtpPanel();

        // Tab click listeners
        tabRadios.setOnClickListener(v -> switchTab(0));
        tabDemos.setOnClickListener(v -> switchTab(1));
        tabFavorites.setOnClickListener(v -> switchTab(2));

        // Tab focus handling
        setupTabFocus(tabRadios, 0);
        setupTabFocus(tabDemos, 1);
        setupTabFocus(tabFavorites, 2);

        // Now playing bar - clicking opens player
        nowPlayingBar.setOnClickListener(v -> {
            if (currentStation != null) openPlayerForStation(currentStation);
        });
        nowPlayingBar.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) nowPlayingBar.setBackgroundColor(0xFF252545);
            else nowPlayingBar.setBackgroundColor(0xFF0D0D1F);
        });

        // Kill rectangular highlights on bar buttons
        setupCircularBarButton(npPlayPause);
        setupCircularBarButton(npPrev);
        setupCircularBarButton(npNext);

        npPlayPause.setOnClickListener(v -> togglePlayPause());
        npPrev.setOnClickListener(v -> playPreviousInBar());
        npNext.setOnClickListener(v -> playNextInBar());

        // Audio service
        audioService = AudioService.getInstance(this);
        audioService.setStateListener(this);

        // Remote control listener
        RemoteControlReceiver.setCommandListener(new RemoteControlReceiver.RemoteCommandListener() {
            @Override
            public void onPlaybackStateChanged() {
                runOnUiThread(() -> {
                    if (audioService.hasPlayer()) {
                        nowPlayingBar.setVisibility(View.VISIBLE);
                        npPlayPause.setImageResource(audioService.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
                    }
                    if (currentTab == 2) switchTab(2);
                });
            }

            @Override
            public void onDevicePaired(String deviceId) {
                runOnUiThread(() -> {
                    updateOtpPanel();
                    Toast.makeText(MainActivity.this, "Dispositivo vinculado!", Toast.LENGTH_LONG).show();
                });
            }
        });

        // Fetch data
        dataFetcher = new DataFetcher();
        loadData();
    }

    private void setupOtpPanel() {
        updateOtpPanel();

        // Auto-refresh OTP timer display
        otpRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                updateOtpTimer();
                otpHandler.postDelayed(this, 1000);
            }
        };
        otpHandler.post(otpRefreshRunnable);
    }

    private void updateOtpPanel() {
        if (otpManager.isPaired()) {
            // Show paired state
            if (otpPanel != null) otpPanel.setVisibility(View.VISIBLE);
            if (tvOtpCode != null) tvOtpCode.setVisibility(View.GONE);
            if (tvOtpTimer != null) tvOtpTimer.setVisibility(View.GONE);
            if (tvOtpStatus != null) {
                tvOtpStatus.setText("TV Vinculada");
                tvOtpStatus.setTextColor(getColor(R.color.accent_blue));
                tvOtpStatus.setVisibility(View.VISIBLE);
            }
            if (tvPairedInfo != null) {
                tvPairedInfo.setText("Control remoto activo");
                tvPairedInfo.setVisibility(View.VISIBLE);
            }
        } else {
            // Show OTP code
            if (otpPanel != null) otpPanel.setVisibility(View.VISIBLE);
            String otp = otpManager.getCurrentOtp();
            if (tvOtpCode != null) {
                tvOtpCode.setText(formatOtp(otp));
                tvOtpCode.setVisibility(View.VISIBLE);
            }
            if (tvOtpTimer != null) tvOtpTimer.setVisibility(View.VISIBLE);
            if (tvOtpStatus != null) {
                tvOtpStatus.setText("Codigo de vinculacion");
                tvOtpStatus.setTextColor(getColor(R.color.text_hint));
                tvOtpStatus.setVisibility(View.VISIBLE);
            }
            if (tvPairedInfo != null) {
                tvPairedInfo.setText("Ingresa este codigo en MediaCenter");
                tvPairedInfo.setVisibility(View.VISIBLE);
            }
        }
    }

    private String formatOtp(String otp) {
        if (otp == null || otp.length() != 6) return otp;
        return otp.substring(0, 3) + " " + otp.substring(3);
    }

    private void updateOtpTimer() {
        if (tvOtpTimer == null || otpManager.isPaired()) return;
        long remaining = otpManager.getRemainingSeconds();
        long mins = remaining / 60;
        long secs = remaining % 60;
        tvOtpTimer.setText(String.format("Expira en %d:%02d", mins, secs));

        // Check if OTP expired and needs refresh
        if (!otpManager.isOtpValid() && !otpManager.isPaired()) {
            otpManager.generateOtp();
            updateOtpPanel();
        }
    }

    /**
     * Kill ALL rectangular highlights on a circular button.
     */
    private void setupCircularBarButton(ImageButton btn) {
        btn.setDefaultFocusHighlightEnabled(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            btn.setStateListAnimator(null);
            btn.setElevation(0f);
            btn.setTranslationZ(0f);
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
            btn.setClipToOutline(true);
        }
        btn.setOnFocusChangeListener((v, hasFocus) -> {
            float scale = hasFocus ? 1.15f : 1.0f;
            v.animate().scaleX(scale).scaleY(scale).alpha(hasFocus ? 1.0f : 0.7f).setDuration(200).start();
        });
        btn.setAlpha(0.7f);
    }

    private void setupTabFocus(TextView tab, int tabIndex) {
        tab.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                tab.setTextColor(getColor(R.color.accent_blue));
                tab.setScaleX(1.05f);
                tab.setScaleY(1.05f);
            } else {
                if (currentTab != tabIndex) {
                    tab.setTextColor(getColor(R.color.tab_inactive));
                }
                tab.setScaleX(1.0f);
                tab.setScaleY(1.0f);
            }
        });
    }

    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        dataFetcher.fetchCategories(App.getJsonUrl(), new DataFetcher.DataCallback() {
            @Override
            public void onDataLoaded(CategoriesResponse data) {
                categoriesData = data;
                progressBar.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);

                // Update header info
                if (data.getMeta() != null) {
                    CategoriesResponse.Meta meta = data.getMeta();
                    tvHeaderInfo.setText(meta.getTotalRadios() + " radios · " + meta.getTotalDemos() + " demos");
                    tvHeaderInfo.setVisibility(View.VISIBLE);
                }

                // Populate radio stations (including YouTube ones - shown but marked)
                radioStations = new ArrayList<>();
                for (Category cat : data.getCategories()) {
                    if ("Radios en Vivo".equals(cat.getName()) && cat.getStations() != null) {
                        radioStations.addAll(cat.getStations());
                    }
                }
                radioAdapter.setStations(radioStations);
                Log.d(TAG, "Loaded " + radioStations.size() + " radio stations");

                // Populate demo songs
                demoStations = new ArrayList<>();
                for (Category cat : data.getCategories()) {
                    if ("Demos".equals(cat.getName()) && cat.getStations() != null) {
                        demoStations.addAll(cat.getStations());
                    }
                }
                demoAdapter.setStations(demoStations);
                Log.d(TAG, "Loaded " + demoStations.size() + " demo songs");
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                tvError.setVisibility(View.VISIBLE);
                tvError.setText("Error de conexion: " + message);
                Log.e(TAG, "Error loading data: " + message);
            }
        });
    }

    private void switchTab(int tab) {
        currentTab = tab;
        tabRadios.setTextColor(getColor(tab == 0 ? R.color.tab_active : R.color.tab_inactive));
        tabDemos.setTextColor(getColor(tab == 1 ? R.color.tab_active : R.color.tab_inactive));
        tabFavorites.setTextColor(getColor(tab == 2 ? R.color.tab_active : R.color.tab_inactive));

        if (tab == 0) {
            recyclerView.setAdapter(radioAdapter);
        } else if (tab == 1) {
            recyclerView.setAdapter(demoAdapter);
        } else {
            // Favorites tab: combine radios + demos that are favorited
            List<Station> allStations = new ArrayList<>();
            allStations.addAll(radioStations);
            allStations.addAll(demoStations);
            List<Station> favStations = favoritesManager.getFavoriteStations(allStations);
            favoriteAdapter.setStations(favStations);
            recyclerView.setAdapter(favoriteAdapter);
        }
    }

    private List<Station> getCurrentStationList() {
        if (currentTab == 0) return radioStations;
        if (currentTab == 1) return demoStations;
        // For favorites, get the filtered list
        List<Station> allStations = new ArrayList<>();
        allStations.addAll(radioStations);
        allStations.addAll(demoStations);
        return favoritesManager.getFavoriteStations(allStations);
    }

    private int findStationIndex(List<Station> stations, Station station) {
        for (int i = 0; i < stations.size(); i++) {
            if (stations.get(i).getName() != null && stations.get(i).getName().equals(station.getName())) {
                return i;
            }
        }
        return 0;
    }

    private void onStationClicked(Station station) {
        if (station.isYouTube()) {
            Toast.makeText(this, "FM Luzu: Solo disponible via YouTube", Toast.LENGTH_LONG).show();
            return;
        }

        String playbackUrl = station.getPlaybackUrl();
        if (playbackUrl == null) {
            Toast.makeText(this, "No se puede reproducir: " + station.getName(), Toast.LENGTH_LONG).show();
            return;
        }

        Log.d(TAG, "Playing: " + station.getName() + " | URL: " + playbackUrl);

        // Store station list and index in holder for prev/next navigation
        List<Station> currentList = getCurrentStationList();
        int index = findStationIndex(currentList, station);
        StationListHolder.setStations(currentList, index);

        // Play the stream using ExoPlayer
        audioService.play(playbackUrl);

        // Store current station
        currentStation = station;

        // Update now playing bar
        updateNowPlaying(station);

        // Open player activity
        openPlayerForStation(station);

        // Send playback update to MediaCenter
        RemoteControlReceiver.sendPlaybackUpdate(this);
    }

    private void updateNowPlaying(Station station) {
        nowPlayingBar.setVisibility(View.VISIBLE);
        npTitle.setText(station.getName());
        npSubtitle.setText(station.getSubtitle());

        String coverUrl = station.getCoverImage();
        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(this)
                    .load(coverUrl)
                    .placeholder(R.drawable.ic_radio)
                    .error(R.drawable.ic_radio)
                    .centerCrop()
                    .timeout(15000)
                    .into(npCover);
        } else {
            npCover.setImageResource(R.drawable.ic_radio);
        }
    }

    private void openPlayerForStation(Station station) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("name", station.getName());
        intent.putExtra("subtitle", station.getSubtitle());
        intent.putExtra("genre", station.getGenre() != null ? station.getGenre() : "");
        intent.putExtra("coverImage", station.getCoverImage() != null ? station.getCoverImage() : "");
        intent.putExtra("streamType", station.getStreamType() != null ? station.getStreamType() : "");
        intent.putExtra("description", station.getDescription() != null ? station.getDescription() : "");
        intent.putExtra("frequency", station.getFrequency() != null ? station.getFrequency() : "");
        intent.putExtra("location", station.getLocation() != null ? station.getLocation() : "");
        startActivity(intent);
    }

    private void togglePlayPause() {
        if (audioService.isPlaying()) {
            audioService.pause();
            npPlayPause.setImageResource(R.drawable.ic_play);
        } else {
            audioService.resume();
            npPlayPause.setImageResource(R.drawable.ic_pause);
        }
        RemoteControlReceiver.sendPlaybackUpdate(this);
    }

    private void playPreviousInBar() {
        Station prev = StationListHolder.getPreviousStation();
        if (prev != null) {
            currentStation = prev;
            audioService.play(prev.getPlaybackUrl());
            updateNowPlaying(prev);
            Toast.makeText(this, "Anterior: " + prev.getName(), Toast.LENGTH_SHORT).show();
        }
        RemoteControlReceiver.sendPlaybackUpdate(this);
    }

    private void playNextInBar() {
        Station next = StationListHolder.getNextStation();
        if (next != null) {
            currentStation = next;
            audioService.play(next.getPlaybackUrl());
            updateNowPlaying(next);
            Toast.makeText(this, "Siguiente: " + next.getName(), Toast.LENGTH_SHORT).show();
        }
        RemoteControlReceiver.sendPlaybackUpdate(this);
    }

    @Override
    public void onStateChanged(boolean isPlaying) {
        runOnUiThread(() -> {
            npPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
            RemoteControlReceiver.sendPlaybackUpdate(MainActivity.this);
        });
    }

    @Override
    public void onError(String message) {
        runOnUiThread(() -> {
            Toast.makeText(MainActivity.this, "Error: " + message, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Player error: " + message);
        });
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
        // Can show buffering indicator if needed
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioService.setStateListener(null);
        if (otpRefreshRunnable != null) {
            otpHandler.removeCallbacks(otpRefreshRunnable);
        }
        otpManager.destroy();
        RemoteControlReceiver.setCommandListener(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update now playing bar state
        if (audioService.hasPlayer()) {
            nowPlayingBar.setVisibility(View.VISIBLE);
            npPlayPause.setImageResource(audioService.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
        } else {
            nowPlayingBar.setVisibility(View.GONE);
        }

        // Refresh favorites tab if active
        if (currentTab == 2) {
            switchTab(2);
        }

        // Refresh OTP panel
        updateOtpPanel();
    }

    /**
     * Grid spacing decoration for RecyclerView
     */
    public static class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(android.graphics.Rect outRect, View view,
                                   RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int column = position % spanCount;

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount;
                outRect.right = (column + 1) * spacing / spanCount;
                if (position < spanCount) {
                    outRect.top = spacing;
                }
                outRect.bottom = spacing;
            } else {
                outRect.left = column * spacing / spanCount;
                outRect.right = spacing - (column + 1) * spacing / spanCount;
                if (position >= spanCount) {
                    outRect.top = spacing;
                }
            }
        }
    }
}
