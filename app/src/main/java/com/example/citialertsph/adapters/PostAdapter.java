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

import com.example.citialertsph.utils.ApiClient;



public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    private List<Post> posts = new ArrayList<>();
    private Context context;
    private OnPostClickListener listener;


    public interface OnPostClickListener {
        void onPostClick(Post post);
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

        // Set post data
        holder.tvPostTitle.setText(post.getTitle());
        holder.tvPostDescription.setText(post.getDescription());
        holder.tvModeratorName.setText(post.getModeratorName());
        holder.tvViewCount.setText(String.valueOf(post.getViews()) + " views");

        // Handle moderator verification badge
        holder.ivVerifiedBadge.setVisibility(post.isModeratorVerified() ? View.VISIBLE : View.GONE);

        // Load post image if exists
        if (post.getImagePath() != null && !post.getImagePath().isEmpty()) {
            Glide.with(context)
                    .load(ApiClient.BASE_URL + post.getImagePath())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(holder.ivPostImage);
            holder.ivPostImage.setVisibility(View.VISIBLE);
        } else {
            holder.ivPostImage.setVisibility(View.GONE);
        }

        // Load moderator profile image
        if (post.getModeratorImage() != null && !post.getModeratorImage().isEmpty()) {
            Glide.with(context)
                    .load(ApiClient.BASE_URL + post.getModeratorImage())
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(holder.ivModeratorImage);
        }

        // Set relative timestamp
        holder.tvPostTime.setText(getRelativeTimeSpan(post.getCreatedAt()));

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPostClick(post);
            }
        });
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public void setPosts(List<Post> newPosts) {
        this.posts.clear();
        this.posts.addAll(newPosts);
        notifyDataSetChanged();
    }

    public void addPost(Post post) {
        this.posts.add(0, post);
        notifyItemInserted(0);
    }

    private String getRelativeTimeSpan(String timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(timestamp);
            long time = date.getTime();
            long now = System.currentTimeMillis();

            return DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS).toString();
        } catch (ParseException e) {
            return timestamp;
        }
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPostImage;
        ImageView ivModeratorImage;
        ImageView ivVerifiedBadge;
        TextView tvPostTitle;
        TextView tvPostDescription;
        TextView tvModeratorName;
        TextView tvPostTime;
        TextView tvViewCount;

        PostViewHolder(View itemView) {
            super(itemView);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            ivModeratorImage = itemView.findViewById(R.id.ivModeratorImage);
            ivVerifiedBadge = itemView.findViewById(R.id.ivVerifiedBadge);
            tvPostTitle = itemView.findViewById(R.id.tvPostTitle);
            tvPostDescription = itemView.findViewById(R.id.tvPostDescription);
            tvModeratorName = itemView.findViewById(R.id.tvModeratorName);
            tvPostTime = itemView.findViewById(R.id.tvPostTime);
            tvViewCount = itemView.findViewById(R.id.tvViewCount);
        }
    }
}
