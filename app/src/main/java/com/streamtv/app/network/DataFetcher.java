package com.streamtv.app.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.streamtv.app.model.CategoriesResponse;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DataFetcher {
    private static final String TAG = "DataFetcher";
    private final OkHttpClient client;
    private final ExecutorService executor;
    private final Handler handler;
    private final Gson gson;

    public interface DataCallback {
        void onDataLoaded(CategoriesResponse data);
        void onError(String message);
    }

    public DataFetcher() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build();
        this.executor = Executors.newSingleThreadExecutor();
        this.handler = new Handler(Looper.getMainLooper());
        this.gson = new Gson();
    }

    public void fetchCategories(String url, DataCallback callback) {
        executor.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(url)
                        .header("Accept", "application/json")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new Exception("HTTP " + response.code());
                    }
                    String body = response.body().string();
                    Log.d(TAG, "JSON response length: " + body.length());
                    Log.d(TAG, "JSON preview: " + body.substring(0, Math.min(200, body.length())));

                    CategoriesResponse data = gson.fromJson(body, CategoriesResponse.class);

                    if (data == null || data.getCategories() == null) {
                        throw new Exception("Invalid JSON structure");
                    }

                    Log.d(TAG, "Parsed " + data.getCategories().size() + " categories");
                    for (com.streamtv.app.model.Category cat : data.getCategories()) {
                        if (cat.getStations() != null) {
                            Log.d(TAG, "  " + cat.getName() + ": " + cat.getStations().size() + " stations");
                        }
                    }

                    handler.post(() -> callback.onDataLoaded(data));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching categories", e);
                handler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
}
