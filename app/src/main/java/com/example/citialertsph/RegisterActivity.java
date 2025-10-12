package com.example.citialertsph;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.citialertsph.utils.ApiClient;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity {
    private EditText etUsername, etPassword, etEmail, etFirstName, etLastName, etPhone, etOrganization;
    private Button btnRegister, btnUploadCredentials;
    private ImageView ivCredentials;
    private Spinner spinnerUserType;
    private String base64Image = "";
    private ApiClient apiClient;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                getContentResolver(), imageUri);
                        ivCredentials.setImageBitmap(bitmap);
                        base64Image = bitmapToBase64(bitmap);
                        Log.d("RegisterActivity", "Image size: " + base64Image.length());
                        btnUploadCredentials.setText("Change Credentials");
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        apiClient = new ApiClient();
        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etEmail = findViewById(R.id.etEmail);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etPhone = findViewById(R.id.etPhone);
        etOrganization = findViewById(R.id.etOrganization);
        btnRegister = findViewById(R.id.btnRegister);
        btnUploadCredentials = findViewById(R.id.btnUploadCredentials);
        ivCredentials = findViewById(R.id.ivCredentials);
        spinnerUserType = findViewById(R.id.spinnerUserType);

        // Setup spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.user_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUserType.setAdapter(adapter);

        // Initially hide moderator-specific fields
        btnUploadCredentials.setVisibility(View.GONE);
        ivCredentials.setVisibility(View.GONE);
        findViewById(R.id.tilOrganization).setVisibility(View.GONE);

        // Handle visibility based on user type selection
        spinnerUserType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = parent.getItemAtPosition(position).toString();
                boolean isModerator = selectedType.equals("moderator");

                // Show/hide moderator-specific fields
                btnUploadCredentials.setVisibility(isModerator ? View.VISIBLE : View.GONE);
                ivCredentials.setVisibility(isModerator ? View.VISIBLE : View.GONE);
                findViewById(R.id.tilOrganization).setVisibility(isModerator ? View.VISIBLE : View.GONE);

                // Clear fields if switching to user type
                if (!isModerator) {
                    ivCredentials.setImageResource(R.drawable.ic_upload_image);
                    base64Image = "";
                    btnUploadCredentials.setText("Upload Credentials");
                    etOrganization.setText(""); // Clear organization field
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                btnUploadCredentials.setVisibility(View.GONE);
                ivCredentials.setVisibility(View.GONE);
                findViewById(R.id.tilOrganization).setVisibility(View.GONE);
            }
        });
    }

    private void setupListeners() {
        btnUploadCredentials.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        btnRegister.setOnClickListener(v -> {
            if (validateInputs()) {
                registerUser();
            }
        });
    }

    private boolean validateInputs() {
        if (etUsername.getText().toString().trim().isEmpty() ||
            etPassword.getText().toString().trim().isEmpty() ||
            etEmail.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void registerUser() {
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("username", etUsername.getText().toString().trim());
        jsonBody.addProperty("password", etPassword.getText().toString().trim());
        jsonBody.addProperty("email", etEmail.getText().toString().trim());
        jsonBody.addProperty("first_name", etFirstName.getText().toString().trim());
        jsonBody.addProperty("last_name", etLastName.getText().toString().trim());
        jsonBody.addProperty("phone", etPhone.getText().toString().trim());
        jsonBody.addProperty("organization", etOrganization.getText().toString().trim());
        jsonBody.addProperty("user_type", spinnerUserType.getSelectedItem().toString());

        // Only add credential image if user type is moderator
        if (spinnerUserType.getSelectedItem().toString().equals("moderator") && !base64Image.isEmpty()) {
            jsonBody.addProperty("credential_image", base64Image);
        }

        apiClient.postRequest("register.php", jsonBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                    Toast.makeText(RegisterActivity.this, "Registration failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d("RegisterActivity", "Server response: " + responseBody);
                runOnUiThread(() -> {
                    try {
                        JsonObject jsonResponse = ApiClient.parseResponse(responseBody);
                        boolean success = jsonResponse.get("success").getAsBoolean();
                        String message = jsonResponse.get("message").getAsString();

                        if (success) {
                            Toast.makeText(RegisterActivity.this, "Registration successful! Please wait for admin verification.",
                                Toast.LENGTH_LONG).show();

                            // Check if verification documents were uploaded
                            JsonObject user = jsonResponse.getAsJsonObject("user");
                            if (user != null && user.has("verification_documents")) {
                                String verificationDocs = user.get("verification_documents").getAsString();
                                Log.d("RegisterActivity", "Verification documents saved as: " + verificationDocs);
                            } else {
                                Log.w("RegisterActivity", "No verification documents in response");
                            }

                            // Navigate to login
                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(RegisterActivity.this, "Registration failed: " + message,
                                Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("RegisterActivity", "Error parsing response", e);
                        Toast.makeText(RegisterActivity.this, "Error processing response: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        // Increase quality to 100 for better image clarity
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();

        // Log the image size for debugging
        Log.d("RegisterActivity", "Original image size: " + byteArray.length + " bytes");

        String base64String = Base64.encodeToString(byteArray, Base64.DEFAULT);
        Log.d("RegisterActivity", "Base64 string length: " + base64String.length());
        return base64String;
    }
}
