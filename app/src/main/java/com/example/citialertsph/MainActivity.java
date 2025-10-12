package com.example.citialertsph;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.citialertsph.databinding.ActivityMainBinding;
import com.example.citialertsph.utils.SessionManager;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        sessionManager = new SessionManager(this);

        replaceFragment(new HomeFragment());
        binding.bottomNavigation.setBackground(null);

        // Profile FAB click handler
        binding.fabProfile.setOnClickListener(view -> replaceFragment(new ProfileFragment()));

        // Emergency FAB click handler
        binding.fabEmergency.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, EmergencyActivity.class);
            startActivity(intent);
        });

        // Add Post FAB: show only for moderators/responders
        if (sessionManager.isResponder()) {
            binding.fabAddPost.setVisibility(View.VISIBLE);
            binding.fabAddPost.setOnClickListener(v -> openCreatePostComposerOnHome());
        } else {
            binding.fabAddPost.setVisibility(View.GONE);
        }

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                replaceFragment(new HomeFragment());
            } else if (itemId == R.id.nav_hotlines) {
                replaceFragment(new HotlinesFragment());
            } else if (itemId == R.id.nav_locations) {
                replaceFragment(new LocationsFragment());
            } else if (itemId == R.id.nav_feedback) {
                replaceFragment(new FeedbackFragment());
            }
            return true;
        });
    }

    private void openCreatePostComposerOnHome() {
        FragmentManager fm = getSupportFragmentManager();
        Fragment current = fm.findFragmentById(R.id.frameLayout);
        if (!(current instanceof HomeFragment)) {
            replaceFragment(new HomeFragment());
            fm.executePendingTransactions();
            current = fm.findFragmentById(R.id.frameLayout);
        }
        if (current instanceof HomeFragment) {
            ((HomeFragment) current).openCreatePostComposer();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        sessionManager.logoutUser();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frameLayout, fragment);
        fragmentTransaction.commit();
    }
}