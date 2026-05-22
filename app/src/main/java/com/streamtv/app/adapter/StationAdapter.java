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
            // Song mode: artist
            String artist = station.getArtist();
            if (holder.tvArtist != null) {
                holder.tvArtist.setText(artist != null ? artist : "");
                holder.tvArtist.setVisibility(artist != null && !artist.isEmpty() ? View.VISIBLE : View.GONE);
            }

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

            // Genre badge (for PREMIUM/NORMAL)
            if (holder.tvGenre != null) {
                String genre = station.getGenre();
                if (genre != null && !genre.isEmpty() && "PREMIUM".equals(genre)) {
                    holder.tvGenre.setVisibility(View.VISIBLE);
                    holder.tvGenre.setText("PREMIUM");
                } else {
                    holder.tvGenre.setVisibility(View.GONE);
                }
            }
        } else {
            // Radio mode: frequency + location
            String subtitle = station.getSubtitle();
            if (holder.tvFrequency != null) {
                holder.tvFrequency.setText(subtitle);
                holder.tvFrequency.setVisibility(subtitle.isEmpty() ? View.GONE : View.VISIBLE);
            }

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

            // Stream type badge
            if (holder.tvStreamType != null) {
                String st = station.getStreamType();
                if (st != null && !st.isEmpty()) {
                    holder.tvStreamType.setVisibility(View.VISIBLE);
                    if ("youtube".equals(st)) {
                        holder.tvStreamType.setText("YT");
                    } else {
                        holder.tvStreamType.setText(st.toUpperCase());
                    }
                } else {
                    holder.tvStreamType.setVisibility(View.GONE);
                }
            }

            // Featured star
            if (holder.ivFeatured != null) {
                holder.ivFeatured.setVisibility(station.isFeatured() ? View.VISIBLE : View.GONE);
            }

            // YouTube indicator
            if (holder.ivYouTubeOverlay != null) {
                holder.ivYouTubeOverlay.setVisibility(station.isYouTube() ? View.VISIBLE : View.GONE);
            }
        }

        // Load cover image with Glide - use GlideApp for OkHttp integration
        String coverUrl = station.getCoverImage();
        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(holder.ivCover.getContext())
                    .load(coverUrl)
                    .placeholder(R.drawable.ic_radio)
                    .error(R.drawable.ic_radio)
                    .centerCrop()
                    .timeout(20000)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                    Target<Drawable> target, boolean isFirstResource) {
                            Log.w(TAG, "Image failed: " + station.getName());
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model,
                                                       Target<Drawable> target, DataSource dataSource,
                                                       boolean isFirstResource) {
                            Log.d(TAG, "Image OK: " + station.getName() + " from " + dataSource);
                            return false;
                        }
                    })
                    .into(holder.ivCover);
        } else {
            holder.ivCover.setImageResource(isSongMode ? R.drawable.ic_music : R.drawable.ic_radio);
        }

        // Focus handling for TV D-pad - ONLY scale + border, NO foreground
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            // Show/hide play overlay
            if (holder.ivPlayOverlay != null) {
                holder.ivPlayOverlay.setVisibility(hasFocus && station.isPlayable() ? View.VISIBLE : View.GONE);
            }

            // Show/hide focus border
            if (holder.focusBorder != null) {
                holder.focusBorder.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
            }

            // Scale animation
            if (hasFocus) {
                v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(200).start();
                v.setElevation(12f);
                v.bringToFront();
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
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
        ImageView ivFeatured;
        View focusBorder;
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
            ivFeatured = itemView.findViewById(R.id.ivFeatured);
            focusBorder = itemView.findViewById(R.id.focusBorder);
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
