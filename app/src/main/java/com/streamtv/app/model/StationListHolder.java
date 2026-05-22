package com.streamtv.app.model;

import java.util.List;

/**
 * Holds the current station list and index so PlayerActivity
 * can navigate between stations with prev/next buttons.
 */
public class StationListHolder {
    private static List<Station> stations;
    private static int currentIndex;

    public static void setStations(List<Station> stations, int currentIndex) {
        StationListHolder.stations = stations;
        StationListHolder.currentIndex = currentIndex;
    }

    public static List<Station> getStations() {
        return stations;
    }

    public static int getCurrentIndex() {
        return currentIndex;
    }

    public static void setCurrentIndex(int index) {
        StationListHolder.currentIndex = index;
    }

    public static Station getCurrentStation() {
        if (stations != null && currentIndex >= 0 && currentIndex < stations.size()) {
            return stations.get(currentIndex);
        }
        return null;
    }

    public static Station getPreviousStation() {
        if (stations == null || stations.isEmpty()) return null;
        int prevIndex = (currentIndex - 1 + stations.size()) % stations.size();
        // Skip YouTube stations
        for (int i = 0; i < stations.size(); i++) {
            Station s = stations.get(prevIndex);
            if (s.isPlayable()) {
                currentIndex = prevIndex;
                return s;
            }
            prevIndex = (prevIndex - 1 + stations.size()) % stations.size();
        }
        return null;
    }

    public static Station getNextStation() {
        if (stations == null || stations.isEmpty()) return null;
        int nextIndex = (currentIndex + 1) % stations.size();
        // Skip YouTube stations
        for (int i = 0; i < stations.size(); i++) {
            Station s = stations.get(nextIndex);
            if (s.isPlayable()) {
                currentIndex = nextIndex;
                return s;
            }
            nextIndex = (nextIndex + 1) % stations.size();
        }
        return null;
    }

    public static boolean hasPrevious() {
        if (stations == null || stations.size() <= 1) return false;
        // Check if there's at least one playable station before current
        for (int i = 1; i < stations.size(); i++) {
            int idx = (currentIndex - i + stations.size()) % stations.size();
            if (stations.get(idx).isPlayable()) return true;
        }
        return false;
    }

    public static boolean hasNext() {
        if (stations == null || stations.size() <= 1) return false;
        for (int i = 1; i < stations.size(); i++) {
            int idx = (currentIndex + i) % stations.size();
            if (stations.get(idx).isPlayable()) return true;
        }
        return false;
    }
}
