package com.streamtv.app.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CategoriesResponse {
    @SerializedName("categories")
    private List<Category> categories;

    @SerializedName("meta")
    private Meta meta;

    public List<Category> getCategories() { return categories; }
    public void setCategories(List<Category> categories) { this.categories = categories; }

    public Meta getMeta() { return meta; }
    public void setMeta(Meta meta) { this.meta = meta; }

    public static class Meta {
        @SerializedName("exportedAt")
        private String exportedAt;

        @SerializedName("source")
        private String source;

        @SerializedName("version")
        private String version;

        @SerializedName("totalRadios")
        private int totalRadios;

        @SerializedName("totalDemos")
        private int totalDemos;

        @SerializedName("totalEscena")
        private int totalEscena;

        public String getExportedAt() { return exportedAt; }
        public String getSource() { return source; }
        public String getVersion() { return version; }
        public int getTotalRadios() { return totalRadios; }
        public int getTotalDemos() { return totalDemos; }
        public int getTotalEscena() { return totalEscena; }
    }
}
