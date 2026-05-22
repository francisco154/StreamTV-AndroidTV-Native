package com.streamtv.app.adapter;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.streamtv.app.R;
import com.streamtv.app.model.Station;

import java.util.ArrayList;
import java.util.List;

public class StationAdapter extends RecyclerView.Adapter<StationAdapter.ViewHolder> {

    private static final String TAG = "StationAdapter";
    private List<Station> stations = new ArrayList<>();
    private OnStationClickListener listener;
    private boolean isSongMode = false;

    public interface OnStationClickListener {
        void onStationClick(Station station);
    }

    public StationAdapter(boolean isSongMode, OnStationClickListener listener) {
        this.isSongMode = isSongMode;
        this.listener = listener;
    }

    public void setStations(List<Station> stations) {
        this.stations = stations;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = isSongMode ? R.layout.item_song : R.layout.item_station;
        View view = LayoutInflater.from(parent.getContext())
                .inflate(layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Station station = stations.get(position);

        // Station name
        holder.tvName.setText(station.getName());

        // Subtitle info
        if (isSongMode) {
            // Song mode: show artist
            String artist = station.getArtist();
            holder.tvArtist.setText(artist != null ? artist : "");
            holder.tvArtist.setVisibility(artist != null && !artist.isEmpty() ? View.VISIBLE : View.GONE);

            // Mood badge
            if (holder.tvMood != null) {
                String mood = station.getMood();
                if (mood != null && !mood.isEmpty()) {
                    holder.tvMood.setVisibility(View.VISIBLE);
                    holder.tvMood.setText(mood);
                } else {
                    holder.tvMood.setVisibility(View.GONE);
                }
            }

            // Duration badge
            if (holder.tvDuration != null) {
                String dur = station.getFormattedDuration();
                if (!dur.isEmpty()) {
                    holder.tvDuration.setVisibility(View.VISIBLE);
                    holder.tvDuration.setText(dur);
                } else {
                    holder.tvDuration.setVisibility(View.GONE);
                }
            }
        } else {
            // Radio mode: show frequency + location
            String subtitle = station.getSubtitle();
            holder.tvFrequency.setText(subtitle);
            holder.tvFrequency.setVisibility(subtitle.isEmpty() ? View.GONE : View.VISIBLE);

            // Genre badge
            if (holder.tvGenre != null) {
                String genre = station.getGenre();
                if (genre != null && !genre.isEmpty()) {
                    holder.tvGenre.setVisibility(View.VISIBLE);
                    holder.tvGenre.setText(genre);
                } else {
                    holder.tvGenre.setVisibility(View.GONE);
                }
            }

            // Stream type indicator
            if (holder.tvStreamType != null) {
                String st = station.getStreamType();
                if (st != null && !st.isEmpty()) {
                    holder.tvStreamType.setVisibility(View.VISIBLE);
                    if ("youtube".equals(st)) {
                        holder.tvStreamType.setText("YouTube");
                    } else {
                        holder.tvStreamType.setText(st.toUpperCase());
                    }
                } else {
                    holder.tvStreamType.setVisibility(View.GONE);
                }
            }
        }

        // Load cover image with Glide - robust error handling
        String coverUrl = station.getCoverImage();
        if (coverUrl != null && !coverUrl.isEmpty()) {
            Log.d(TAG, "Loading cover: " + coverUrl.substring(0, Math.min(60, coverUrl.length())));

            Glide.with(holder.itemView.getContext())
                    .load(coverUrl)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                    Target<Drawable> target, boolean isFirstResource) {
                            Log.w(TAG, "Glide load failed for " + station.getName() + ": " +
                                    (e != null ? e.getMessage() : "unknown"));
                            return false; // Let Glide handle the error placeholder
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model,
                                                       Target<Drawable> target, DataSource dataSource,
                                                       boolean isFirstResource) {
                            Log.d(TAG, "Glide loaded OK: " + station.getName() + " from " + dataSource);
                            return false;
                        }
                    })
                    .placeholder(R.drawable.ic_radio)
                    .error(R.drawable.ic_radio)
                    .centerCrop()
                    .timeout(15000)
                    .into(holder.ivCover);
        } else {
            holder.ivCover.setImageResource(R.drawable.ic_radio);
        }

        // YouTube overlay indicator
        if (holder.ivYouTubeOverlay != null) {
            holder.ivYouTubeOverlay.setVisibility(station.isYouTube() ? View.VISIBLE : View.GONE);
        }

        // Focus handling for TV D-pad
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (holder.ivPlayOverlay != null) {
                holder.ivPlayOverlay.setVisibility(hasFocus && station.isPlayable() ? View.VISIBLE : View.GONE);
            }
            if (hasFocus) {
                v.setScaleX(1.05f);
                v.setScaleY(1.05f);
                v.setElevation(8f);
            } else {
                v.setScaleX(1.0f);
                v.setScaleY(1.0f);
                v.setElevation(0f);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onStationClick(station);
        });
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        ImageView ivPlayOverlay;
        ImageView ivYouTubeOverlay;
        TextView tvName;
        TextView tvFrequency;
        TextView tvGenre;
        TextView tvStreamType;
        TextView tvArtist;
        TextView tvMood;
        TextView tvDuration;

        ViewHolder(View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivCover);
            ivPlayOverlay = itemView.findViewById(R.id.ivPlayOverlay);
            ivYouTubeOverlay = itemView.findViewById(R.id.ivYouTubeOverlay);
            tvName = itemView.findViewById(R.id.tvName);
            tvFrequency = itemView.findViewById(R.id.tvFrequency);
            tvGenre = itemView.findViewById(R.id.tvGenre);
            tvStreamType = itemView.findViewById(R.id.tvStreamType);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            tvMood = itemView.findViewById(R.id.tvMood);
            tvDuration = itemView.findViewById(R.id.tvDuration);
        }
    }
}
