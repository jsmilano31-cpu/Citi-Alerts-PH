package com.example.citialertsph.utils;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class ApiClient {
    public static final String BASE_URL = "https://jsmkj.space/citialerts/app/api/"; // For Android emulator
    // Use "http://192.168.1.xxx/citialerts/app/api/" for physical device (replace xxx with your IP)

    private final OkHttpClient client;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public ApiClient() {
        this.client = new OkHttpClient();
    }

    public void postRequest(String endpoint, JsonObject jsonData, Callback callback) {
        String url = BASE_URL + endpoint;

        RequestBody body = RequestBody.create(jsonData.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        Call call = client.newCall(request);
        call.enqueue(callback);
    }

    public void getRequest(String endpoint, Callback callback) {
        String url = BASE_URL + endpoint;

        Request request = new Request.Builder()
                .url(url)
                .build();

        Call call = client.newCall(request);
        call.enqueue(callback);
    }

    public static JsonObject parseResponse(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            Log.e("ApiClient", "Empty response body");
            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("success", false);
            errorJson.addProperty("message", "Empty response from server");
            return errorJson;
        }

        try {
            return JsonParser.parseString(responseBody).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            Log.e("ApiClient", "Invalid JSON response: " + responseBody);
            Log.e("ApiClient", "Parse error: " + e.getMessage());
            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("success", false);
            errorJson.addProperty("message", "Invalid server response: " + e.getMessage());
            return errorJson;
        }
    }
}