package com.example.citialertsph;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.citialertsph.databinding.ActivitySplashBinding;
import com.example.citialertsph.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {
    private ActivitySplashBinding binding;
    private SessionManager sessionManager;
    private static final int SPLASH_DURATION = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        sessionManager = new SessionManager(this);

        // Check if user is already logged in after splash duration
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            checkUserSession();
        }, SPLASH_DURATION);
    }

    private void checkUserSession() {
        Intent intent;
        if (sessionManager.isLoggedIn()) {
            // User is logged in, go to MainActivity
            intent = new Intent(SplashActivity.this, MainActivity.class);
        } else {
            // User is not logged in, go to LoginActivity
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}