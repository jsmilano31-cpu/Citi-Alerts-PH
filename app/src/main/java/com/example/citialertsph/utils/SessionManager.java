package com.example.citialertsph.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.example.citialertsph.models.User;
import com.google.gson.JsonObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class SessionManager {
    private static final String PREF_NAME = "CitiAlertsPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_FIRST_NAME = "firstName";
    private static final String KEY_LAST_NAME = "lastName";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_USER_TYPE = "userType";
    private static final String KEY_IS_VERIFIED = "isVerified";
    private static final String KEY_PROFILE_IMAGE = "profileImage";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ApiClient apiClient;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
        apiClient = new ApiClient();
    }

    public void createLoginSession(User user) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, user.getId());
        editor.putString(KEY_USERNAME, user.getUsername());
        editor.putString(KEY_EMAIL, user.getEmail());
        editor.putString(KEY_FIRST_NAME, user.getFirstName());
        editor.putString(KEY_LAST_NAME, user.getLastName());
        editor.putString(KEY_PHONE, user.getPhone());
        editor.putString(KEY_USER_TYPE, user.getUserType());
        editor.putBoolean(KEY_IS_VERIFIED, user.isVerified());
        editor.putString(KEY_PROFILE_IMAGE, user.getProfileImage());
        editor.apply();
    }

    public User getCurrentUser() {
        if (!isLoggedIn()) {
            return null;
        }

        User user = new User();
        user.setId(pref.getInt(KEY_USER_ID, 0));
        user.setUsername(pref.getString(KEY_USERNAME, ""));
        user.setEmail(pref.getString(KEY_EMAIL, ""));
        user.setFirstName(pref.getString(KEY_FIRST_NAME, ""));
        user.setLastName(pref.getString(KEY_LAST_NAME, ""));
        user.setPhone(pref.getString(KEY_PHONE, ""));
        user.setUserType(pref.getString(KEY_USER_TYPE, "user"));
        user.setVerified(pref.getBoolean(KEY_IS_VERIFIED, false));
        user.setProfileImage(pref.getString(KEY_PROFILE_IMAGE, ""));

        return user;
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public boolean isModerator() {
        String userType = pref.getString(KEY_USER_TYPE, "user");
        return "moderator".equals(userType);
    }

    public void logoutUser() {
        int userId = getUserId();
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("user_id", userId);

        apiClient.postRequest("logout.php", jsonBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> clearLocalSession());
            }

            @Override
            public void onResponse(Call call, Response response) {
                mainHandler.post(() -> clearLocalSession());
            }
        });
    }

    private void clearLocalSession() {
        editor.clear();
        editor.apply();
    }

    public String getUserFullName() {
        String firstName = pref.getString(KEY_FIRST_NAME, "");
        String lastName = pref.getString(KEY_LAST_NAME, "");
        return firstName + " " + lastName;
    }

    public String getUsername() {
        return pref.getString(KEY_USERNAME, "");
    }

    public String getUserEmail() {
        return pref.getString(KEY_EMAIL, "");
    }

    public int getUserId() {
        return pref.getInt(KEY_USER_ID, 0);
    }
}