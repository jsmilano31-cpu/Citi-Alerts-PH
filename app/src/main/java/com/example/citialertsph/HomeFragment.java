package com.example.citialertsph;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.citialertsph.adapters.PostAdapter;
import com.example.citialertsph.models.Post;
import com.example.citialertsph.databinding.FragmentHomeBinding;
import com.example.citialertsph.utils.ApiClient;
import com.example.citialertsph.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private SessionManager sessionManager;
    private ApiClient apiClient;
    private Dialog addPostDialog;
    private String base64Image = "";
    private ImageView dialogImageView;
    private PostAdapter postAdapter;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                requireActivity().getContentResolver(), imageUri);
                        dialogImageView.setImageBitmap(bitmap);
                        base64Image = bitmapToBase64(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        sessionManager = new SessionManager(requireContext());
        apiClient = new ApiClient();

        setupViews();
        loadPosts();

        return binding.getRoot();
    }

    private void setupViews() {
        // Show moderator panel if user is moderator
        if (sessionManager.isModerator()) {
            binding.moderatorPanel.setVisibility(View.VISIBLE);
            binding.btnAddPost.setOnClickListener(v -> showAddPostDialog());
        }

        // Setup SwipeRefreshLayout
        binding.swipeRefresh.setOnRefreshListener(this::loadPosts);

        // Setup RecyclerView and Adapter
        postAdapter = new PostAdapter(requireContext(), post -> {
            // TODO: Handle post click - open detail view
            Toast.makeText(getContext(), "Post clicked: " + post.getTitle(), Toast.LENGTH_SHORT).show();
        });
        binding.recyclerPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerPosts.setAdapter(postAdapter);
    }

    private void showAddPostDialog() {
        addPostDialog = new Dialog(requireContext());
        addPostDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        addPostDialog.setContentView(R.layout.dialog_add_post);

        // Initialize dialog views
        dialogImageView = addPostDialog.findViewById(R.id.ivPostImage);
        EditText etTitle = addPostDialog.findViewById(R.id.etPostTitle);
        EditText etDescription = addPostDialog.findViewById(R.id.etPostDescription);
        MaterialButton btnSelectImage = addPostDialog.findViewById(R.id.btnSelectImage);
        MaterialButton btnCancel = addPostDialog.findViewById(R.id.btnCancel);
        MaterialButton btnPost = addPostDialog.findViewById(R.id.btnPost);

        // Setup click listeners
        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        btnCancel.setOnClickListener(v -> addPostDialog.dismiss());

        btnPost.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (title.isEmpty() || description.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            createPost(title, description);
        });

        addPostDialog.show();
    }

    private void createPost(String title, String description) {
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("title", title);
        jsonBody.addProperty("description", description);
        jsonBody.addProperty("moderator_id", sessionManager.getUserId());

        if (!base64Image.isEmpty()) {
            jsonBody.addProperty("image", base64Image);
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        apiClient.postRequest("create_post.php", jsonBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                requireActivity().runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Failed to create post", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                requireActivity().runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    try {
                        JsonObject jsonResponse = ApiClient.parseResponse(responseBody);
                        boolean success = jsonResponse.get("success").getAsBoolean();
                        String message = jsonResponse.get("message").getAsString();

                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        if (success) {
                            addPostDialog.dismiss();
                            loadPosts(); // Refresh posts list
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Error processing response",
                            Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void loadPosts() {
        binding.progressBar.setVisibility(View.VISIBLE);
        apiClient.getRequest("get_posts.php", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                requireActivity().runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                    Toast.makeText(getContext(), "Failed to load posts", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                requireActivity().runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                    try {
                        JsonObject jsonResponse = ApiClient.parseResponse(responseBody);
                        if (jsonResponse.get("success").getAsBoolean()) {
                            JsonArray postsArray = jsonResponse.getAsJsonArray("posts");
                            List<Post> posts = new ArrayList<>();

                            for (JsonElement element : postsArray) {
                                JsonObject postObj = element.getAsJsonObject();
                                Post post = new Post();
                                post.setId(postObj.get("id").getAsInt());
                                post.setModeratorId(postObj.get("moderator_id").getAsInt());
                                post.setTitle(postObj.get("title").getAsString());
                                post.setDescription(postObj.get("description").getAsString());
                                if (!postObj.get("image_path").isJsonNull()) {
                                    post.setImagePath(postObj.get("image_path").getAsString());
                                }
                                post.setViews(postObj.get("views").getAsInt());
                                post.setCreatedAt(postObj.get("created_at").getAsString());
                                post.setModeratorName(postObj.get("moderator_name").getAsString());
                                if (!postObj.get("moderator_image").isJsonNull()) {
                                    post.setModeratorImage(postObj.get("moderator_image").getAsString());
                                }
                                post.setModeratorVerified(postObj.get("moderator_verified").getAsBoolean());
                                posts.add(post);
                            }

                            postAdapter.setPosts(posts);
                        } else {
                            Toast.makeText(getContext(), "Error: " + jsonResponse.get("message").getAsString(),
                                Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Error processing response",
                            Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (addPostDialog != null && addPostDialog.isShowing()) {
            addPostDialog.dismiss();
        }
    }
}
