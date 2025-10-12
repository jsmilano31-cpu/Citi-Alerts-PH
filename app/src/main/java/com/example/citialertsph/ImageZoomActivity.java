package com.example.citialertsph;

import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.citialertsph.utils.ApiClient;

public class ImageZoomActivity extends AppCompatActivity {

    private static final String EXTRA_IMAGE_URL = "image_url";
    private static final String EXTRA_POST_TITLE = "post_title";

    private ImageView ivZoomedImage;
    private TextView tvPostTitle;
    private ProgressBar progressBar;
    private View topOverlay;

    private Matrix matrix = new Matrix();
    private float scale = 1f;
    private ScaleGestureDetector scaleGestureDetector;
    private boolean overlayVisible = true;

    public static Intent createIntent(Context context, String imageUrl, String postTitle) {
        Intent intent = new Intent(context, ImageZoomActivity.class);
        intent.putExtra(EXTRA_IMAGE_URL, imageUrl);
        intent.putExtra(EXTRA_POST_TITLE, postTitle);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable full screen
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_image_zoom);

        initViews();
        setupData();
        setupZoomFunctionality();
        loadImage();
    }

    private void initViews() {
        ivZoomedImage = findViewById(R.id.ivZoomedImage);
        tvPostTitle = findViewById(R.id.tvPostTitle);
        progressBar = findViewById(R.id.progressBar);
        topOverlay = findViewById(R.id.topOverlay);

        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
    }

    private void setupData() {
        Intent intent = getIntent();
        String postTitle = intent.getStringExtra(EXTRA_POST_TITLE);
        tvPostTitle.setText(postTitle != null ? postTitle : "Image");
    }

    private void setupZoomFunctionality() {
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        ivZoomedImage.setOnTouchListener(new View.OnTouchListener() {
            private float lastTouchX, lastTouchY;
            private int activePointerId = MotionEvent.INVALID_POINTER_ID;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                scaleGestureDetector.onTouchEvent(event);

                final int action = event.getActionMasked();

                switch (action) {
                    case MotionEvent.ACTION_DOWN: {
                        final float x = event.getX();
                        final float y = event.getY();

                        lastTouchX = x;
                        lastTouchY = y;
                        activePointerId = event.getPointerId(0);

                        // Toggle overlay on single tap
                        toggleOverlay();
                        break;
                    }

                    case MotionEvent.ACTION_MOVE: {
                        final int pointerIndex = event.findPointerIndex(activePointerId);

                        if (pointerIndex != -1) {
                            final float x = event.getX(pointerIndex);
                            final float y = event.getY(pointerIndex);

                            if (!scaleGestureDetector.isInProgress()) {
                                final float dx = x - lastTouchX;
                                final float dy = y - lastTouchY;

                                matrix.postTranslate(dx, dy);
                                ivZoomedImage.setImageMatrix(matrix);
                            }

                            lastTouchX = x;
                            lastTouchY = y;
                        }
                        break;
                    }

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL: {
                        activePointerId = MotionEvent.INVALID_POINTER_ID;
                        break;
                    }

                    case MotionEvent.ACTION_POINTER_UP: {
                        final int pointerIndex = event.getActionIndex();
                        final int pointerId = event.getPointerId(pointerIndex);

                        if (pointerId == activePointerId) {
                            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                            lastTouchX = event.getX(newPointerIndex);
                            lastTouchY = event.getY(newPointerIndex);
                            activePointerId = event.getPointerId(newPointerIndex);
                        }
                        break;
                    }
                }

                return true;
            }
        });
    }

    private void loadImage() {
        String imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            String fullUrl = ApiClient.BASE_URL + imageUrl; // Removed "uploads/" since imageUrl already contains the full path

            Glide.with(this)
                .load(fullUrl)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ImageZoomActivity.this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);

                        // Center the image initially
                        centerImage(resource);
                        return false;
                    }
                })
                .into(ivZoomedImage);
        } else {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "No image to display", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void centerImage(Drawable drawable) {
        if (drawable == null) return;

        int viewWidth = ivZoomedImage.getWidth();
        int viewHeight = ivZoomedImage.getHeight();
        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();

        if (viewWidth == 0 || viewHeight == 0) {
            // View not measured yet, try again later
            ivZoomedImage.post(() -> centerImage(drawable));
            return;
        }

        float scaleX = (float) viewWidth / drawableWidth;
        float scaleY = (float) viewHeight / drawableHeight;
        scale = Math.min(scaleX, scaleY);

        float dx = (viewWidth - drawableWidth * scale) / 2;
        float dy = (viewHeight - drawableHeight * scale) / 2;

        matrix.setScale(scale, scale);
        matrix.postTranslate(dx, dy);
        ivZoomedImage.setImageMatrix(matrix);
    }

    private void toggleOverlay() {
        overlayVisible = !overlayVisible;
        topOverlay.setVisibility(overlayVisible ? View.VISIBLE : View.GONE);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scale *= detector.getScaleFactor();
            scale = Math.max(0.1f, Math.min(scale, 5.0f)); // Limit scale between 0.1x and 5x

            matrix.setScale(scale, scale);
            ivZoomedImage.setImageMatrix(matrix);
            return true;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
