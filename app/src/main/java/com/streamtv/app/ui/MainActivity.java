package com.streamtv.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.streamtv.app.model.Station;
import com.streamtv.app.network.AudioService;
import com.streamtv.app.network.DataFetcher;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AudioService.PlayerStateListener {

    private static final String TAG = "MainActivity";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvError;
    private TextView tvHeaderInfo;

    // Tabs
    private TextView tabRadios, tabDemos;

    // Now playing bar
    private LinearLayout nowPlayingBar;
    private ImageView npCover;
    private TextView npTitle, npSubtitle;
    private ImageButton npPlayPause;

    // Data
    private DataFetcher dataFetcher;
    private CategoriesResponse categoriesData;
    private StationAdapter radioAdapter;
    private StationAdapter demoAdapter;
    private AudioService audioService;

    // Current playing station (stored for player activity)
    private Station currentStation;

    private int currentTab = 0; // 0 = radios, 1 = demos

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
        nowPlayingBar = findViewById(R.id.nowPlayingBar);
        npCover = findViewById(R.id.npCover);
        npTitle = findViewById(R.id.npTitle);
        npSubtitle = findViewById(R.id.npSubtitle);
        npPlayPause = findViewById(R.id.npPlayPause);

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
        recyclerView.setAdapter(radioAdapter);

        // Tab click listeners
        tabRadios.setOnClickListener(v -> switchTab(0));
        tabDemos.setOnClickListener(v -> switchTab(1));

        // Tab focus handling
        setupTabFocus(tabRadios, 0);
        setupTabFocus(tabDemos, 1);

        // Now playing bar - clicking opens player
        nowPlayingBar.setOnClickListener(v -> {
            if (currentStation != null) openPlayerForStation(currentStation);
        });
        nowPlayingBar.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) nowPlayingBar.setBackgroundColor(0xFF252545);
            else nowPlayingBar.setBackgroundColor(0xFF1A1A2E);
        });

        npPlayPause.setOnClickListener(v -> togglePlayPause());

        // Audio service
        audioService = AudioService.getInstance(this);
        audioService.setStateListener(this);

        // Fetch data
        dataFetcher = new DataFetcher();
        loadData();
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
                List<Station> allRadios = new ArrayList<>();
                for (Category cat : data.getCategories()) {
                    if ("Radios en Vivo".equals(cat.getName()) && cat.getStations() != null) {
                        allRadios.addAll(cat.getStations());
                    }
                }
                radioAdapter.setStations(allRadios);
                Log.d(TAG, "Loaded " + allRadios.size() + " radio stations");

                // Populate demo songs
                List<Station> allDemos = new ArrayList<>();
                for (Category cat : data.getCategories()) {
                    if ("Demos".equals(cat.getName()) && cat.getStations() != null) {
                        allDemos.addAll(cat.getStations());
                    }
                }
                demoAdapter.setStations(allDemos);
                Log.d(TAG, "Loaded " + allDemos.size() + " demo songs");
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                tvError.setVisibility(View.VISIBLE);
                tvError.setText("Error de conexión: " + message);
                Log.e(TAG, "Error loading data: " + message);
            }
        });
    }

    private void switchTab(int tab) {
        currentTab = tab;
        if (tab == 0) {
            tabRadios.setTextColor(getColor(R.color.tab_active));
            tabDemos.setTextColor(getColor(R.color.tab_inactive));
            recyclerView.setAdapter(radioAdapter);
        } else {
            tabRadios.setTextColor(getColor(R.color.tab_inactive));
            tabDemos.setTextColor(getColor(R.color.tab_active));
            recyclerView.setAdapter(demoAdapter);
        }
    }

    private void onStationClicked(Station station) {
        if (station.isYouTube()) {
            Toast.makeText(this, "FM Luzu: Solo disponible vía YouTube", Toast.LENGTH_LONG).show();
            return;
        }

        String playbackUrl = station.getPlaybackUrl();
        if (playbackUrl == null) {
            Toast.makeText(this, "No se puede reproducir: " + station.getName(), Toast.LENGTH_LONG).show();
            return;
        }

        Log.d(TAG, "Playing: " + station.getName() + " | URL: " + playbackUrl);

        // Play the stream using ExoPlayer
        audioService.play(playbackUrl);

        // Store current station
        currentStation = station;

        // Update now playing bar
        updateNowPlaying(station);

        // Open player activity
        openPlayerForStation(station);
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
    }

    @Override
    public void onStateChanged(boolean isPlaying) {
        runOnUiThread(() -> {
            npPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        });
    }

    @Override
    public void onError(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Error: " + message, Toast.LENGTH_LONG).show();
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
