package com.notebook.notes.checklist.Advertisement;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IPAddressHelper {

    private static final String API_URL =
            "https://pro.ip-api.com/json/?key=RrtTUs6AdrbN4q0&fields=status,countryCode,city,query,proxy,hosting,regionName,country";
    private static final String TAG = "IPAddressHelper";

    public interface IPCallback {
        void onResponse(String countryName);
        void onFailure(Exception e);
    }

    public static void getCountryName(Context context, IPCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setConnectTimeout(10000);
                urlConnection.setReadTimeout(10000);

                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    JSONObject jsonObject = new JSONObject(response.toString());
                    if ("success".equalsIgnoreCase(jsonObject.optString("status"))
                            && jsonObject.has("country")) {
                        String countryName = jsonObject.optString("country", "");
                        handler.post(() -> callback.onResponse(countryName));
                    } else {
                        handler.post(() -> callback.onFailure(new Exception("Country data not found in response")));
                    }
                } else {
                    handler.post(() -> callback.onFailure(new Exception("HTTP error code: " + responseCode)));
                }
                urlConnection.disconnect();
            } catch (Exception e) {
//                Log.e(TAG, "Error fetching IP data: " + e.getMessage());
                handler.post(() -> callback.onFailure(e));
            }
        });
    }
}
