package com.example.citialertsph.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.citialertsph.R;
import com.example.citialertsph.models.Post;
import com.example.citialertsph.utils.ApiClient;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    private List<Post> posts = new ArrayList<>();
    private Context context;
    private OnPostClickListener listener;

    public interface OnPostClickListener {
        void onPostClick(Post post);
        void onImageClick(Post post, ImageView imageView);
    }

    public PostAdapter(Context context, OnPostClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = posts.get(position);

        // Bind data to views
        holder.tvPostTitle.setText(post.getTitle());
        holder.tvPostDescription.setText(post.getDescription());
        holder.tvModeratorName.setText(post.getModeratorName());
        holder.tvModeratorOrganization.setText(post.getModeratorOrganization());
        holder.tvViewCount.setText(post.getViews() + " views");
        holder.tvStatus.setText(post.getStatus());

        // Show/hide verified badge
        holder.ivVerifiedBadge.setVisibility(post.isModeratorVerified() ? View.VISIBLE : View.GONE);

        // Set status badge color based on status
        setStatusBadgeColor(holder, post.getStatus());

        // Format and set time
        holder.tvPostTime.setText(formatTimeAgo(post.getCreatedAt()));

        // Load moderator image (supports absolute URL or API-relative path)
        String modImg = post.getModeratorImage();
        if (modImg != null && !modImg.isEmpty()) {
            String url = modImg.startsWith("http") ? modImg : (ApiClient.BASE_URL + modImg);
            Glide.with(context)
                .load(url)
                .placeholder(R.drawable.default_profile)
                .error(R.drawable.default_profile)
                .circleCrop()
                .into(holder.ivModeratorImage);
        } else {
            Glide.with(context)
                .load(R.drawable.default_profile)
                .circleCrop()
                .into(holder.ivModeratorImage);
        }

        // Load post image with rounded corners
        if (post.getImagePath() != null && !post.getImagePath().isEmpty()) {
            holder.imageCardView.setVisibility(View.VISIBLE);

            RequestOptions requestOptions = new RequestOptions()
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .transform(new RoundedCorners(32));

            Glide.with(context)
                .load(ApiClient.BASE_URL + post.getImagePath()) // Removed "uploads/" since it's already in getImagePath()
                .apply(requestOptions)
                .into(holder.ivPostImage);
        } else {
            holder.imageCardView.setVisibility(View.GONE);
        }

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPostClick(post);
            }
        });

        // Image click for zoom functionality
        holder.ivPostImage.setOnClickListener(v -> {
            if (listener != null) {
                listener.onImageClick(post, holder.ivPostImage);
            }
        });
    }

    private void setStatusBadgeColor(PostViewHolder holder, String status) {
        // Handle null status by providing a default value
        if (status == null) {
            status = "pending"; // Default status if null
        }

        int colorRes;
        switch (status.toLowerCase()) {
            case "completed":
                colorRes = R.color.status_completed;
                break;
            case "help_coming":
                colorRes = R.color.status_help_coming;
                break;
            case "pending":
            default:
                colorRes = R.color.status_pending;
                break;
        }
        // Cast to CardView to access setCardBackgroundColor method
        if (holder.statusBadge instanceof androidx.cardview.widget.CardView) {
            ((androidx.cardview.widget.CardView) holder.statusBadge).setCardBackgroundColor(context.getResources().getColor(colorRes));
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts;
        notifyDataSetChanged();
    }

    public void addPost(Post post) {
        posts.add(0, post);
        notifyItemInserted(0);
    }

    private String formatTimeAgo(String dateString) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(dateString);

            if (date != null) {
                return DateUtils.getRelativeTimeSpanString(
                    date.getTime(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                ).toString();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "Just now";
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvPostTitle, tvPostDescription, tvModeratorName, tvModeratorOrganization, tvPostTime;
        TextView tvViewCount, tvStatus;
        ImageView ivPostImage, ivModeratorImage, ivVerifiedBadge;
        View imageCardView;
        androidx.cardview.widget.CardView statusBadge; // Changed from View to CardView

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize views
            tvPostTitle = itemView.findViewById(R.id.tvPostTitle);
            tvPostDescription = itemView.findViewById(R.id.tvPostDescription);
            tvModeratorName = itemView.findViewById(R.id.tvModeratorName);
            tvModeratorOrganization = itemView.findViewById(R.id.tvModeratorOrganization);
            tvPostTime = itemView.findViewById(R.id.tvPostTime);
            tvViewCount = itemView.findViewById(R.id.tvViewCount);
            tvStatus = itemView.findViewById(R.id.tvStatus);

            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            ivModeratorImage = itemView.findViewById(R.id.ivModeratorImage);
            ivVerifiedBadge = itemView.findViewById(R.id.ivVerifiedBadge);

            imageCardView = itemView.findViewById(R.id.imageCardView);
            statusBadge = itemView.findViewById(R.id.statusBadge); // Now properly typed as CardView
        }
    }
}
