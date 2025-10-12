package com.example.citialertsph.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "CitiAlertsSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ROLE = "role";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_FIRST_NAME = "firstName";
    private static final String KEY_LAST_NAME = "lastName";
    private static final String KEY_ORGANIZATION = "organization";
    // New optional fields
    private static final String KEY_PHONE = "phone";
    private static final String KEY_PROFILE_IMAGE = "profileImage";
    private static final String KEY_IS_VERIFIED = "isVerified";

    public static final String ROLE_USER = "user";
    public static final String ROLE_MODERATOR = "moderator";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createLoginSession(int userId, String username, String role, String email, String firstName, String lastName, String organization) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_ROLE, role);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_FIRST_NAME, firstName);
        editor.putString(KEY_LAST_NAME, lastName);
        editor.putString(KEY_ORGANIZATION, organization);
        editor.commit();
    }

    // Backward compatibility method for existing code that doesn't pass organization
    public void createLoginSession(int userId, String username, String role, String email, String firstName, String lastName) {
        createLoginSession(userId, username, role, email, firstName, lastName, null);
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void logoutUser() {
        // Clear SharedPreferences
        editor.clear();
        editor.commit();

        // Clear ApiClient cookies to ensure fresh login
        ApiClient.clearCookies();
    }

    public int getUserId() { return pref.getInt(KEY_USER_ID, -1); }
    public String getUsername() { return pref.getString(KEY_USERNAME, null); }
    public String getRole() { return pref.getString(KEY_ROLE, ROLE_USER); }
    public boolean isModerator() { return ROLE_MODERATOR.equals(getRole()); }
    public boolean isResponder() { return isModerator(); }
    public boolean isRegularUser() { return ROLE_USER.equals(getRole()); }

    public String getEmail() { return pref.getString(KEY_EMAIL, null); }
    public String getFirstName() { return pref.getString(KEY_FIRST_NAME, null); }
    public String getLastName() { return pref.getString(KEY_LAST_NAME, null); }
    public String getOrganization() { return pref.getString(KEY_ORGANIZATION, null); }

    // New getters
    public String getPhone() { return pref.getString(KEY_PHONE, null); }
    public String getProfileImage() { return pref.getString(KEY_PROFILE_IMAGE, null); }
    public boolean isVerified() { return pref.getBoolean(KEY_IS_VERIFIED, false); }

    // Setters to keep session data updated after profile updates
    public void setEmail(String email) { editor.putString(KEY_EMAIL, email).apply(); }
    public void setFirstName(String firstName) { editor.putString(KEY_FIRST_NAME, firstName).apply(); }
    public void setLastName(String lastName) { editor.putString(KEY_LAST_NAME, lastName).apply(); }
    public void setPhone(String phone) { editor.putString(KEY_PHONE, phone).apply(); }
    public void setProfileImage(String url) { editor.putString(KEY_PROFILE_IMAGE, url).apply(); }
    public void setVerified(boolean verified) { editor.putBoolean(KEY_IS_VERIFIED, verified).apply(); }
}
