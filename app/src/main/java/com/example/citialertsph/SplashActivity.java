package com.example.citialertsph;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.citialertsph.databinding.ActivitySplashBinding;
import com.example.citialertsph.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {
    private ActivitySplashBinding binding;
    private SessionManager sessionManager;
    private static final int SPLASH_DURATION = 3000; // 3 seconds for better UX

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Hide status bar and navigation bar for immersive experience
        hideSystemUI();

        sessionManager = new SessionManager(this);

        // Start animations
        startAnimations();

        // Navigate after splash duration
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (sessionManager.isLoggedIn()) {
                startActivity(new Intent(this, MainActivity.class));
            } else {
                startActivity(new Intent(this, LoginActivity.class));
            }
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, SPLASH_DURATION);
    }

    private void hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    private void startAnimations() {
        // Initial state - all views invisible
        binding.logoContainer.setAlpha(0f);
        binding.logoContainer.setScaleX(0.3f);
        binding.logoContainer.setScaleY(0.3f);

        binding.appNameText.setAlpha(0f);
        binding.appNameText.setTranslationY(50f);

        binding.taglineText.setAlpha(0f);
        binding.taglineText.setTranslationY(30f);

        binding.subtitleText.setAlpha(0f);
        binding.subtitleText.setTranslationY(20f);

        binding.loadingContainer.setAlpha(0f);

        // Logo animation
        ObjectAnimator logoAlpha = ObjectAnimator.ofFloat(binding.logoContainer, "alpha", 0f, 1f);
        ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(binding.logoContainer, "scaleX", 0.3f, 1f);
        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(binding.logoContainer, "scaleY", 0.3f, 1f);

        AnimatorSet logoAnimSet = new AnimatorSet();
        logoAnimSet.playTogether(logoAlpha, logoScaleX, logoScaleY);
        logoAnimSet.setDuration(800);
        logoAnimSet.setInterpolator(new OvershootInterpolator(1.2f));
        logoAnimSet.setStartDelay(200);

        // App name animation
        ObjectAnimator nameAlpha = ObjectAnimator.ofFloat(binding.appNameText, "alpha", 0f, 1f);
        ObjectAnimator nameTransY = ObjectAnimator.ofFloat(binding.appNameText, "translationY", 50f, 0f);

        AnimatorSet nameAnimSet = new AnimatorSet();
        nameAnimSet.playTogether(nameAlpha, nameTransY);
        nameAnimSet.setDuration(600);
        nameAnimSet.setInterpolator(new AccelerateDecelerateInterpolator());
        nameAnimSet.setStartDelay(800);

        // Tagline animation
        ObjectAnimator taglineAlpha = ObjectAnimator.ofFloat(binding.taglineText, "alpha", 0f, 1f);
        ObjectAnimator taglineTransY = ObjectAnimator.ofFloat(binding.taglineText, "translationY", 30f, 0f);

        AnimatorSet taglineAnimSet = new AnimatorSet();
        taglineAnimSet.playTogether(taglineAlpha, taglineTransY);
        taglineAnimSet.setDuration(500);
        taglineAnimSet.setInterpolator(new AccelerateDecelerateInterpolator());
        taglineAnimSet.setStartDelay(1200);

        // Subtitle animation
        ObjectAnimator subtitleAlpha = ObjectAnimator.ofFloat(binding.subtitleText, "alpha", 0f, 1f);
        ObjectAnimator subtitleTransY = ObjectAnimator.ofFloat(binding.subtitleText, "translationY", 20f, 0f);

        AnimatorSet subtitleAnimSet = new AnimatorSet();
        subtitleAnimSet.playTogether(subtitleAlpha, subtitleTransY);
        subtitleAnimSet.setDuration(500);
        subtitleAnimSet.setInterpolator(new AccelerateDecelerateInterpolator());
        subtitleAnimSet.setStartDelay(1500);

        // Loading animation
        ObjectAnimator loadingAlpha = ObjectAnimator.ofFloat(binding.loadingContainer, "alpha", 0f, 1f);
        loadingAlpha.setDuration(400);
        loadingAlpha.setStartDelay(1800);

        // Start all animations
        logoAnimSet.start();
        nameAnimSet.start();
        taglineAnimSet.start();
        subtitleAnimSet.start();
        loadingAlpha.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}