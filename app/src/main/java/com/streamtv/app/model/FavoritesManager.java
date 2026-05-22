package com.streamtv.app.model;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages favorite stations using SharedPreferences.
 * Favorites are stored as a Set of station names (unique identifiers).
 */
public class FavoritesManager {
    private static final String PREFS_NAME = "streamtv_favorites";
    private static final String KEY_FAVORITES = "favorite_stations";

    private static FavoritesManager instance;
    private SharedPreferences prefs;

    private FavoritesManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized FavoritesManager getInstance(Context context) {
        if (instance == null) {
            instance = new FavoritesManager(context);
        }
        return instance;
    }

    /**
     * Check if a station is favorited
     */
    public boolean isFavorite(Station station) {
        Set<String> favorites = getFavoritesSet();
        return favorites.contains(getStationKey(station));
    }

    /**
     * Toggle favorite status and return the new state
     */
    public boolean toggleFavorite(Station station) {
        Set<String> favorites = new HashSet<>(getFavoritesSet());
        String key = getStationKey(station);
        boolean isNowFavorite;
        if (favorites.contains(key)) {
            favorites.remove(key);
            isNowFavorite = false;
        } else {
            favorites.add(key);
            isNowFavorite = true;
        }
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply();
        return isNowFavorite;
    }

    /**
     * Get all favorite station keys
     */
    public Set<String> getFavoritesSet() {
        return prefs.getStringSet(KEY_FAVORITES, new HashSet<>());
    }

    /**
     * Filter a list of stations to only favorites
     */
    public List<Station> getFavoriteStations(List<Station> allStations) {
        Set<String> favorites = getFavoritesSet();
        List<Station> result = new ArrayList<>();
        for (Station station : allStations) {
            if (favorites.contains(getStationKey(station))) {
                result.add(station);
            }
        }
        return result;
    }

    /**
     * Generate a unique key for a station based on name + url
     */
    private String getStationKey(Station station) {
        String name = station.getName() != null ? station.getName() : "";
        String url = station.getUrl() != null ? station.getUrl() : "";
        return name + "|" + url;
    }
}
