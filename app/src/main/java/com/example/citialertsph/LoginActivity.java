package com.example.citialertsph;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.citialertsph.databinding.ActivityLoginBinding;
import com.example.citialertsph.models.User;
import com.example.citialertsph.utils.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity"; // Add tag for logging
    private ActivityLoginBinding binding;
    private SessionManager sessionManager;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        sessionManager = new SessionManager(this);

        binding.btnLogin.setOnClickListener(v -> loginUser());
        binding.tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String username = binding.etUsername.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnLogin.setEnabled(false);
        binding.btnLogin.setText("Logging in...");

        // Create JSON data
        JSONObject loginData = new JSONObject();
        try {
            loginData.put("username", username);
            loginData.put("password", password);
            Log.d(TAG, "Request data: " + loginData.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON request: " + e.getMessage());
            e.printStackTrace();
        }

        // Make API call in background thread
        executor.execute(() -> {
            try {
                String response = makePostRequest("https://jsmkj.space/citialerts/app/api/login.php", loginData.toString());
                Log.d(TAG, "Raw response from server: " + response);

                JSONObject result = new JSONObject(response);
                Log.d(TAG, "Parsed JSON response: " + result.toString());

                mainHandler.post(() -> {
                    binding.btnLogin.setEnabled(true);
                    binding.btnLogin.setText("Login");

                    try {
                        boolean success = result.getBoolean("success");
                        String message = result.getString("message");
                        Log.d(TAG, "Success: " + success + ", Message: " + message);

                        if (success) {
                            // Check if 'user' field exists
                            if (!result.has("user")) {
                                Log.e(TAG, "Response is missing 'user' field: " + result.toString());
                                Toast.makeText(LoginActivity.this, "Invalid server response", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // Parse user data
                            JSONObject userJson = result.getJSONObject("user");
                            Log.d(TAG, "User data: " + userJson.toString());

                            User user = new User();
                            user.setId(userJson.getInt("id"));
                            user.setUsername(userJson.getString("username"));
                            user.setEmail(userJson.getString("email"));
                            user.setFirstName(userJson.getString("first_name"));
                            user.setLastName(userJson.getString("last_name"));
                            user.setUserType(userJson.getString("user_type"));

                            // Handle is_verified as either boolean or int
                            if (userJson.get("is_verified") instanceof Boolean) {
                                user.setVerified(userJson.getBoolean("is_verified"));
                            } else {
                                user.setVerified(userJson.getInt("is_verified") == 1);
                            }

                            if (!userJson.isNull("phone")) {
                                user.setPhone(userJson.getString("phone"));
                            }
                            if (!userJson.isNull("profile_image")) {
                                user.setProfileImage(userJson.getString("profile_image"));
                            }
                            if (!userJson.isNull("created_at")) {
                                user.setCreatedAt(userJson.getString("created_at"));
                            }

                            // Save user session
                            sessionManager.createLoginSession(user);
                            Log.d(TAG, "User session created successfully");

                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing JSON response: " + e.getMessage());
                        Log.e(TAG, "Response that caused error: " + response);
                        Toast.makeText(LoginActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Network error: " + e.getMessage());
                Log.e(TAG, "Stack trace: ", e);
                mainHandler.post(() -> {
                    binding.btnLogin.setEnabled(true);
                    binding.btnLogin.setText("Login");
                    Toast.makeText(LoginActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private String makePostRequest(String urlString, String jsonData) throws IOException {
        Log.d(TAG, "Making POST request to: " + urlString);
        Log.d(TAG, "Request body: " + jsonData);

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Send POST data
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonData.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Log response code and headers
            int responseCode = conn.getResponseCode();
            Log.d(TAG, "Response code: " + responseCode);
            Log.d(TAG, "Response headers: " + conn.getHeaderFields());

            // Read response
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            String responseData = response.toString();
            Log.d(TAG, "Response data: " + responseData);
            return responseData;

        } finally {
            conn.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        if (executor != null) {
            executor.shutdown();
        }
    }
}