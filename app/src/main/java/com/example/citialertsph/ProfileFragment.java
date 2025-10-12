package com.example.citialertsph;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.citialertsph.utils.ApiClient;
import com.example.citialertsph.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ProfileFragment extends Fragment {
    private SessionManager sessionManager;
    private ApiClient apiClient;

    private ImageView ivAvatar;
    private TextView tvFullName;
    private TextView tvRole;
    private View chipVerified;
    private TextInputEditText etUsername, etFirstName, etLastName, etPhone, etEmail;
    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private MaterialButton btnChangePhoto, btnSave, btnChangePassword, btnLogout;
    private ProgressBar progressBar;

    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                        ivAvatar.setImageBitmap(bitmap);
                        uploadProfileImage(bitmap);
                    } catch (IOException e) {
                        Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        sessionManager = new SessionManager(requireContext());
        apiClient = new ApiClient();

        bindViews(view);
        populateFromSession();
        fetchLatestProfile();
        wireActions();
        return view;
    }

    private void bindViews(View view) {
        ivAvatar = view.findViewById(R.id.ivAvatar);
        tvFullName = view.findViewById(R.id.tvFullName);
        tvRole = view.findViewById(R.id.tvRole);
        chipVerified = view.findViewById(R.id.chipVerified);
        etUsername = view.findViewById(R.id.etUsername);
        etFirstName = view.findViewById(R.id.etFirstName);
        etLastName = view.findViewById(R.id.etLastName);
        etPhone = view.findViewById(R.id.etPhone);
        etEmail = view.findViewById(R.id.etEmail);
        etCurrentPassword = view.findViewById(R.id.etCurrentPassword);
        etNewPassword = view.findViewById(R.id.etNewPassword);
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
        btnChangePhoto = view.findViewById(R.id.btnChangePhoto);
        btnSave = view.findViewById(R.id.btnSave);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnLogout = view.findViewById(R.id.btnLogout);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void populateFromSession() {
        String first = safe(sessionManager.getFirstName());
        String last = safe(sessionManager.getLastName());
        tvFullName.setText((first + " " + last).trim());
        tvRole.setText(sessionManager.isModerator() ? "Moderator" : "User");
        etUsername.setText(safe(sessionManager.getUsername()));
        etFirstName.setText(first);
        etLastName.setText(safe(last));
        etPhone.setText(safe(sessionManager.getPhone()));
        etEmail.setText(safe(sessionManager.getEmail()));
        chipVerified.setVisibility(sessionManager.isVerified() ? View.VISIBLE : View.GONE);

        String profileUrl = sessionManager.getProfileImage();
        if (!TextUtils.isEmpty(profileUrl)) {
            loadImageAsync(absoluteUrl(profileUrl));
        }
    }

    private void wireActions() {
        btnChangePhoto.setOnClickListener(v -> selectImage());
        btnSave.setOnClickListener(v -> updateProfile());
        btnChangePassword.setOnClickListener(v -> changePassword());
        btnLogout.setOnClickListener(v -> doLogout());
    }

    private void fetchLatestProfile() {
        // Optional: ask server for latest data
        JsonObject json = new JsonObject();
        json.addProperty("user_id", sessionManager.getUserId());

        apiClient.postRequest("get_profile.php", json, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Ignore silently; we already show session data
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                JsonObject res = ApiClient.parseResponse(body);
                if (response.isSuccessful() && res.has("success") && res.get("success").getAsBoolean()) {
                    JsonObject u = res.getAsJsonObject("user");
                    requireActivity().runOnUiThread(() -> {
                        sessionManager.setFirstName(u.get("first_name").getAsString());
                        sessionManager.setLastName(u.get("last_name").getAsString());
                        sessionManager.setEmail(u.get("email").getAsString());
                        if (u.has("phone") && !u.get("phone").isJsonNull()) sessionManager.setPhone(u.get("phone").getAsString());
                        if (u.has("profile_image") && !u.get("profile_image").isJsonNull()) sessionManager.setProfileImage(u.get("profile_image").getAsString());
                        if (u.has("is_verified")) sessionManager.setVerified(u.get("is_verified").getAsBoolean());
                        populateFromSession();
                    });
                }
            }
        });
    }

    private void updateProfile() {
        String first = textOf(etFirstName);
        String last = textOf(etLastName);
        String phone = textOf(etPhone);
        String email = textOf(etEmail);
        if (first.isEmpty() || last.isEmpty() || email.isEmpty()) {
            Toast.makeText(getContext(), "First, Last and Email are required", Toast.LENGTH_SHORT).show();
            return;
        }
        showLoading(true);
        JsonObject json = new JsonObject();
        json.addProperty("user_id", sessionManager.getUserId());
        json.addProperty("first_name", first);
        json.addProperty("last_name", last);
        json.addProperty("phone", phone);
        json.addProperty("email", email);

        apiClient.postRequest("update_profile.php", json, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                JsonObject res = ApiClient.parseResponse(body);
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    boolean ok = response.isSuccessful() && res.has("success") && res.get("success").getAsBoolean();
                    String msg = res.has("message") ? res.get("message").getAsString() : (ok ? "Profile updated" : "Update failed");
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                    if (ok) {
                        sessionManager.setFirstName(first);
                        sessionManager.setLastName(last);
                        sessionManager.setPhone(phone);
                        sessionManager.setEmail(email);
                        populateFromSession();
                    }
                });
            }
        });
    }

    private void changePassword() {
        String current = textOf(etCurrentPassword);
        String npw = textOf(etNewPassword);
        String confirm = textOf(etConfirmPassword);
        if (current.isEmpty() || npw.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(getContext(), "Fill all password fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!npw.equals(confirm)) {
            Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }
        showLoading(true);
        JsonObject json = new JsonObject();
        json.addProperty("user_id", sessionManager.getUserId());
        json.addProperty("current_password", current);
        json.addProperty("new_password", npw);

        apiClient.postRequest("update_password.php", json, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(getContext(), "Failed to update password", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                JsonObject res = ApiClient.parseResponse(body);
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    boolean ok = response.isSuccessful() && res.has("success") && res.get("success").getAsBoolean();
                    String msg = res.has("message") ? res.get("message").getAsString() : (ok ? "Password updated" : "Update failed");
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                    if (ok) {
                        etCurrentPassword.setText("");
                        etNewPassword.setText("");
                        etConfirmPassword.setText("");
                    }
                });
            }
        });
    }

    private void uploadProfileImage(Bitmap bitmap) {
        showLoading(true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
        String base64 = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.DEFAULT);

        JsonObject json = new JsonObject();
        json.addProperty("user_id", sessionManager.getUserId());
        json.addProperty("image", base64);

        apiClient.postRequest("update_profile_image.php", json, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(getContext(), "Image upload failed", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                JsonObject res = ApiClient.parseResponse(body);
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    boolean ok = response.isSuccessful() && res.has("success") && res.get("success").getAsBoolean();
                    String msg = res.has("message") ? res.get("message").getAsString() : (ok ? "Profile image updated" : "Upload failed");
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                    if (ok && res.has("profile_image")) {
                        String path = res.get("profile_image").getAsString();
                        sessionManager.setProfileImage(path);
                        loadImageAsync(absoluteUrl(path));
                    }
                });
            }
        });
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void loadImageAsync(String url) {
        imageExecutor.execute(() -> {
            try {
                URL u = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                conn.setDoInput(true);
                conn.connect();
                try (InputStream is = conn.getInputStream()) {
                    Bitmap bmp = BitmapFactory.decodeStream(is);
                    requireActivity().runOnUiThread(() -> ivAvatar.setImageBitmap(bmp));
                }
            } catch (Exception ignored) { }
        });
    }

    private String absoluteUrl(String maybeRelative) {
        if (maybeRelative == null) return null;
        if (maybeRelative.startsWith("http")) return maybeRelative;
        return ApiClient.BASE_URL + maybeRelative;
    }

    private void doLogout() {
        sessionManager.logoutUser();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static String textOf(TextInputEditText et) { return et.getText() == null ? "" : et.getText().toString().trim(); }
}
