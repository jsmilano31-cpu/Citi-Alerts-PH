package com.example.citialertsph.utils;

import android.util.Log;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    private static final String TAG = "ApiClient";
    public static final String BASE_URL = "https://jsmkj.space/citialerts/app/api/";
    private final OkHttpClient client;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // SHARED STATIC COOKIE JAR - all ApiClient instances use the same cookies
    private static final PersistentCookieJar cookieJar = new PersistentCookieJar();

    public ApiClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        this.client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(chain -> {
                    Request originalRequest = chain.request();
                    Request.Builder builder = originalRequest.newBuilder();

                    // Add session handling headers
                    builder.addHeader("Content-Type", "application/json");
                    builder.addHeader("X-Requested-With", "XMLHttpRequest");
                    builder.addHeader("Accept", "application/json");

                    return chain.proceed(builder.build());
                })
                .cookieJar(cookieJar)  // All instances share the same cookie jar
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    // Add method to clear cookies when user logs out
    public static void clearCookies() {
        Log.d(TAG, "Clearing all cookies from shared cookie jar");
        cookieJar.clear();
    }

    public void postRequest(String endpoint, JsonObject jsonData, Callback callback) {
        String url = BASE_URL + endpoint;
        Log.d(TAG, "Making POST request to: " + url);
        Log.d(TAG, "Request body: " + jsonData.toString());

        RequestBody body = RequestBody.create(jsonData.toString(), JSON);
        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(body);

        // Get current session cookie if available
        HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl != null) {
            List<Cookie> cookies = cookieJar.loadForRequest(httpUrl);
            Log.d(TAG, "Current cookies before request: " + cookies);
            for (Cookie cookie : cookies) {
                if (cookie.name().equals("PHPSESSID")) {
                    String cookieHeader = String.format("%s=%s", cookie.name(), cookie.value());
                    builder.addHeader("Cookie", cookieHeader);
                    Log.d(TAG, "Added session cookie to request: " + cookieHeader);
                    break;
                }
            }
        }

        Request request = builder.build();

        // Add an interceptor specifically for this request to capture the response cookies
        OkHttpClient clientWithCookieLogging = client.newBuilder()
                .addInterceptor(chain -> {
                    okhttp3.Response response = chain.proceed(chain.request());
                    List<String> cookieHeaders = response.headers("Set-Cookie");
                    if (!cookieHeaders.isEmpty()) {
                        Log.d(TAG, "Received new cookies: " + cookieHeaders);
                    }
                    return response;
                })
                .build();

        Call call = clientWithCookieLogging.newCall(request);
        call.enqueue(callback);
    }

    public void getRequest(String endpoint, Callback callback) {
        String url = BASE_URL + endpoint;
        Log.d(TAG, "Making GET request to: " + url);

        Request.Builder builder = new Request.Builder()
                .url(url);

        // Get current session cookie if available
        HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl != null) {
            List<Cookie> cookies = cookieJar.loadForRequest(httpUrl);
            for (Cookie cookie : cookies) {
                if (cookie.name().equals("PHPSESSID")) {
                    builder.addHeader("Cookie", "PHPSESSID=" + cookie.value());
                    break;
                }
            }
        }

        Request request = builder.build();
        Call call = client.newCall(request);
        call.enqueue(callback);
    }

    public static JsonObject parseResponse(String responseBody) {
        try {
            if (responseBody == null || responseBody.isEmpty()) {
                Log.e(TAG, "Empty response body");
                JsonObject error = new JsonObject();
                error.addProperty("error", "Empty response from server");
                return error;
            }

            Log.d(TAG, "Parsing response: " + responseBody);
            return JsonParser.parseString(responseBody).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "Error parsing JSON response: " + e.getMessage());
            JsonObject error = new JsonObject();
            error.addProperty("error", "Invalid JSON response: " + e.getMessage());
            return error;
        }
    }

    private static class PersistentCookieJar implements CookieJar {
        private final List<Cookie> cookieStore = Collections.synchronizedList(new ArrayList<>());

        @Override
        public synchronized void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            Log.d(TAG, "Saving cookies from response for " + url.host());
            synchronized (cookieStore) {
                for (Cookie cookie : cookies) {
                    // Keep track of old cookie before removal
                    Cookie oldCookie = null;
                    for (Cookie stored : cookieStore) {
                        if (stored.name().equals(cookie.name())) {
                            oldCookie = stored;
                            break;
                        }
                    }

                    // Remove old cookie
                    if (oldCookie != null) {
                        cookieStore.remove(oldCookie);
                        Log.d(TAG, "Removed old cookie: " + oldCookie);
                    }

                    // Add new cookie with proper domain and path
                    Cookie.Builder cookieBuilder = new Cookie.Builder()
                        .name(cookie.name())
                        .value(cookie.value())
                        .path(cookie.path() != null ? cookie.path() : "/");

                    // Handle domain properly
                    String domain = cookie.domain();
                    if (domain == null) {
                        domain = url.host();
                    }
                    // Ensure domain starts with a dot for proper subdomain matching
                    if (!domain.startsWith(".") && !domain.equals(url.host())) {
                        domain = "." + domain;
                    }
                    cookieBuilder.domain(domain);

                    // Add expiration if present
                    if (cookie.expiresAt() != Long.MIN_VALUE) {
                        cookieBuilder.expiresAt(cookie.expiresAt());
                    }

                    // Add secure if needed
                    if (cookie.secure()) {
                        cookieBuilder.secure();
                    }

                    // Add httpOnly if needed
                    if (cookie.httpOnly()) {
                        cookieBuilder.httpOnly();
                    }

                    Cookie newCookie = cookieBuilder.build();
                    cookieStore.add(newCookie);
                    Log.d(TAG, "Added new cookie: " + newCookie);
                }
            }
        }

        @Override
        public synchronized List<Cookie> loadForRequest(HttpUrl url) {
            List<Cookie> validCookies = new ArrayList<>();
            Log.d(TAG, "Loading cookies for URL: " + url.toString());

            synchronized (cookieStore) {
                for (Cookie cookie : cookieStore) {
                    if (cookie.matches(url) && !isExpired(cookie)) {
                        validCookies.add(cookie);
                        Log.d(TAG, "Found valid cookie for request: " + cookie);
                    } else {
                        Log.d(TAG, "Skipping cookie (expired or not matching): " + cookie);
                    }
                }
            }
            return validCookies;
        }

        private boolean isExpired(Cookie cookie) {
            return cookie.expiresAt() != Long.MIN_VALUE &&
                   cookie.expiresAt() < System.currentTimeMillis();
        }

        public synchronized void clear() {
            cookieStore.clear();
        }
    }
}