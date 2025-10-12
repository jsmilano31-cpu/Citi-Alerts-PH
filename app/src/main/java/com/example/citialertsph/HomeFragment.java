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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
                        if (dialogImageView != null) {
                            dialogImageView.setImageBitmap(bitmap);
                        }
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
        // Always hide fragment-level FAB to avoid duplication with Activity FAB
        if (binding.fabAddPost != null) binding.fabAddPost.setVisibility(View.GONE);

        // Setup SwipeRefreshLayout
        binding.swipeRefresh.setOnRefreshListener(this::loadPosts);

        // Setup RecyclerView and Adapter with image zoom functionality
        postAdapter = new PostAdapter(requireContext(), new PostAdapter.OnPostClickListener() {
            @Override
            public void onPostClick(Post post) {
                Toast.makeText(getContext(), "Post clicked: " + post.getTitle(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onImageClick(Post post, ImageView imageView) {
                if (post.getImagePath() != null && !post.getImagePath().isEmpty()) {
                    Intent zoomIntent = ImageZoomActivity.createIntent(
                            requireContext(),
                            post.getImagePath(),
                            post.getTitle()
                    );
                    startActivity(zoomIntent);
                }
            }
        });
        binding.recyclerPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerPosts.setAdapter(postAdapter);
    }

    // Public method for Activity to open the bottom sheet composer
    public void openCreatePostComposer() {
        showAddPostDialog();
    }

    private void showAddPostDialog() {
        // Use a Material BottomSheet for modern UX
        BottomSheetDialog bottomSheet = new BottomSheetDialog(requireContext());
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_post, null, false);
        bottomSheet.setContentView(content);
        addPostDialog = bottomSheet; // Keep reference for lifecycle dismissal

        // Initialize dialog views from the content view
        dialogImageView = content.findViewById(R.id.ivPostImage);
        View imageOverlay = content.findViewById(R.id.imageOverlay);
        View imageCard = content.findViewById(R.id.imageCard);
        ImageView btnClose = content.findViewById(R.id.btnCloseDialog);
        EditText etTitle = content.findViewById(R.id.etPostTitle);
        EditText etDescription = content.findViewById(R.id.etPostDescription);
        MaterialButton btnSelectImage = content.findViewById(R.id.btnSelectImage);
        MaterialButton btnCancel = content.findViewById(R.id.btnCancel);
        MaterialButton btnPost = content.findViewById(R.id.btnPost);

        // Unified image selection triggers
        View.OnClickListener selectImage = v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        };
        if (btnSelectImage != null) btnSelectImage.setOnClickListener(selectImage);
        if (imageOverlay != null) imageOverlay.setOnClickListener(selectImage);
        if (imageCard != null) imageCard.setOnClickListener(selectImage);
        if (dialogImageView != null) dialogImageView.setOnClickListener(selectImage);

        // Close and cancel actions
        if (btnClose != null) btnClose.setOnClickListener(v -> {
            base64Image = "";
            addPostDialog.dismiss();
        });
        if (btnCancel != null) btnCancel.setOnClickListener(v -> {
            base64Image = "";
            addPostDialog.dismiss();
        });

        // Post action
        if (btnPost != null) {
            btnPost.setOnClickListener(v -> {
                String title = etTitle != null ? etTitle.getText().toString().trim() : "";
                String description = etDescription != null ? etDescription.getText().toString().trim() : "";

                if (title.isEmpty() || description.isEmpty()) {
                    Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                createPost(title, description);
            });
        }

        bottomSheet.show();

        // Expand by default for better visibility
        View sheet = bottomSheet.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet != null) {
            com.google.android.material.bottomsheet.BottomSheetBehavior<View> behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet);
            behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
        }
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
                String responseBody = response.body() != null ? response.body().string() : "";
                requireActivity().runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    try {
                        JsonObject jsonResponse = ApiClient.parseResponse(responseBody);
                        boolean success = jsonResponse.has("success") && jsonResponse.get("success").getAsBoolean();
                        String message = jsonResponse.has("message") ? jsonResponse.get("message").getAsString() : "";

                        if (!message.isEmpty()) {
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        }
                        if (success) {
                            if (addPostDialog != null && addPostDialog.isShowing()) {
                                addPostDialog.dismiss();
                            }
                            base64Image = ""; // reset after success
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
                String responseBody = response.body() != null ? response.body().string() : "";
                requireActivity().runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                    try {
                        JsonObject jsonResponse = ApiClient.parseResponse(responseBody);
                        if (jsonResponse.has("success") && jsonResponse.get("success").getAsBoolean()) {
                            JsonArray postsArray = jsonResponse.getAsJsonArray("posts");
                            List<Post> posts = new ArrayList<>();

                            for (JsonElement element : postsArray) {
                                JsonObject postObj = element.getAsJsonObject();
                                Post post = new Post();
                                post.setId(postObj.get("id").getAsInt());
                                post.setModeratorId(postObj.get("moderator_id").getAsInt());
                                post.setTitle(postObj.get("title").getAsString());
                                post.setDescription(postObj.get("description").getAsString());
                                if (postObj.has("image_path") && !postObj.get("image_path").isJsonNull()) {
                                    post.setImagePath(postObj.get("image_path").getAsString());
                                }
                                post.setViews(postObj.get("views").getAsInt());
                                post.setCreatedAt(postObj.get("created_at").getAsString());
                                if (postObj.has("moderator_name") && !postObj.get("moderator_name").isJsonNull()) {
                                    post.setModeratorName(postObj.get("moderator_name").getAsString());
                                }
                                if (postObj.has("moderator_organization") && !postObj.get("moderator_organization").isJsonNull()) {
                                    post.setModeratorOrganization(postObj.get("moderator_organization").getAsString());
                                }
                                if (postObj.has("moderator_image") && !postObj.get("moderator_image").isJsonNull()) {
                                    post.setModeratorImage(postObj.get("moderator_image").getAsString());
                                }
                                if (postObj.has("moderator_verified") && !postObj.get("moderator_verified").isJsonNull()) {
                                    post.setModeratorVerified(postObj.get("moderator_verified").getAsBoolean());
                                }
                                posts.add(post);
                            }

                            postAdapter.setPosts(posts);
                        } else {
                            String msg = jsonResponse.has("message") ? jsonResponse.get("message").getAsString() : "Unknown error";
                            Toast.makeText(getContext(), "Error: " + msg,
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
        base64Image = "";
        dialogImageView = null;
    }
}
