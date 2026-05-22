package com.streamtv.app.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Station implements Serializable {

    // Radio fields
    @SerializedName("name")
    private String name;

    @SerializedName("url")
    private String url;

    @SerializedName("genre")
    private String genre;

    @SerializedName("frequency")
    private String frequency;

    @SerializedName("location")
    private String location;

    @SerializedName("streamType")
    private String streamType;

    @SerializedName("coverImage")
    private String coverImage;

    @SerializedName("description")
    private String description;

    @SerializedName("website")
    private String website;

    @SerializedName("isFeatured")
    private boolean isFeatured;

    // Demo/song fields
    @SerializedName("audioUrl")
    private String audioUrl;

    @SerializedName("artist")
    private String artist;

    @SerializedName("duration")
    private int duration;

    @SerializedName("mood")
    private String mood;

    @SerializedName("bpm")
    private Object bpm;  // Can be null or integer

    @SerializedName("tags")
    private Object tags;  // Can be null or string

    @SerializedName("isPublic")
    private boolean isPublic;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStreamType() { return streamType; }
    public void setStreamType(String streamType) { this.streamType = streamType; }

    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public boolean isFeatured() { return isFeatured; }
    public void setFeatured(boolean featured) { isFeatured = featured; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }

    public String getTagsString() {
        if (tags == null) return "";
        return tags.toString();
    }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean aPublic) { isPublic = aPublic; }

    /**
     * Returns the actual playback URL for this station.
     * For demo songs: use audioUrl (direct audio file)
     * For radio stations: use url
     * For YouTube: returns null (not supported in native player)
     */
    public String getPlaybackUrl() {
        if ("youtube".equals(streamType)) return null;
        if (audioUrl != null && !audioUrl.isEmpty()) return audioUrl;
        if (url != null && !url.isEmpty()) return url;
        return null;
    }

    /**
     * Returns the display subtitle
     * For radios: frequency + location
     * For songs: artist + mood
     */
    public String getSubtitle() {
        if (isSong()) {
            StringBuilder sb = new StringBuilder();
            if (artist != null && !artist.isEmpty()) sb.append(artist);
            if (mood != null && !mood.isEmpty()) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append(mood);
            }
            return sb.toString();
        }
        StringBuilder sb = new StringBuilder();
        if (frequency != null && !frequency.isEmpty()) sb.append(frequency);
        if (location != null && !location.isEmpty()) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(location);
        }
        return sb.toString();
    }

    /**
     * Returns a short description line for the card
     */
    public String getCardInfo() {
        if (isSong()) {
            return getSubtitle();
        }
        StringBuilder sb = new StringBuilder();
        if (genre != null && !genre.isEmpty()) sb.append(genre);
        if (streamType != null && !streamType.isEmpty()) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(streamType.toUpperCase());
        }
        return sb.toString();
    }

    /**
     * Returns formatted duration for songs (mm:ss)
     */
    public String getFormattedDuration() {
        if (duration <= 0) return "";
        int mins = duration / 60;
        int secs = duration % 60;
        return String.format("%d:%02d", mins, secs);
    }

    /**
     * Check if this is a demo/song (not a live radio)
     */
    public boolean isSong() {
        return audioUrl != null && !audioUrl.isEmpty();
    }

    /**
     * Check if this is a YouTube-only station
     */
    public boolean isYouTube() {
        return "youtube".equals(streamType);
    }

    /**
     * Check if this station has a playable stream
     */
    public boolean isPlayable() {
        return getPlaybackUrl() != null;
    }
}
