package com.streamtv.app.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Category {
    @SerializedName("name")
    private String name;

    @SerializedName("icon")
    private String icon;

    @SerializedName("stations")
    private List<Station> stations;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public List<Station> getStations() { return stations; }
    public void setStations(List<Station> stations) { this.stations = stations; }

    /**
     * Returns the display name with icon
     */
    public String getDisplayName() {
        if (icon != null && !icon.isEmpty()) {
            return icon + " " + name;
        }
        return name;
    }
}
