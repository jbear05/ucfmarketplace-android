package com.knightmarket.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.knightmarket.app.databinding.ActivityCreateListingBinding;

import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Screens 13-14, "List an Item": title, price, category, campus, condition, description,
 * the AI-generated tag and up to 8 photos (the first is the cover). Publishing sends it all to
 * POST /listings (through {@link ListingUpload}, so it survives rotation) and opens the new listing.
 */
public class CreateListingActivity extends AppCompatActivity implements ListingUpload.Listener {

    private static final int MAX_PHOTOS = 8;
    private static final BigDecimal MAX_PRICE = new BigDecimal("100000");
    private static final String STATE_PHOTOS = "photos";

    private static final int THUMB_MAX_SIDE = 240;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private ActivityCreateListingBinding binding;
    private ArrayList<Uri> photos = new ArrayList<>();
    private final Map<Uri, Bitmap> thumbnails = new HashMap<>();
    private ListingUpload upload;

    private ActivityResultLauncher<PickVisualMediaRequest> pickOne;
    private final List<ActivityResultLauncher<PickVisualMediaRequest>> pickMany = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateListingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBars.pad(binding.getRoot());

        if (savedInstanceState != null) {
            ArrayList<Uri> saved = savedInstanceState.getParcelableArrayList(STATE_PHOTOS, Uri.class);
            if (saved != null) {
                photos = saved;
            }
        }

        registerPickers();

        upload = new ViewModelProvider(this).get(ListingUpload.class);
        setPublishing(upload.isRunning());
        upload.attach(this); // delivers a result that arrived while the screen was rotating

        binding.back.setOnClickListener(v -> leave());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                leave();
            }
        });
        binding.addPhoto.setOnClickListener(v -> pickPhotos());
        binding.publish.setOnClickListener(v -> publish());

        showPhotos();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Session.isLoggedIn(this)) {
            startActivity(new Intent(this, LandingActivity.class));
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(STATE_PHOTOS, photos);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        upload.detach(this);
        worker.shutdownNow();
    }

    // ── Photos ───────────────────────────────────────────────────────────────

    /**
     * The multi-photo picker needs its limit when it's registered, so there's one launcher per
     * "photos left" count (2..8). With one slot left the single picker is used instead.
     */
    private void registerPickers() {
        pickOne = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                addPhotos(List.of(uri));
            }
        });
        for (int max = 2; max <= MAX_PHOTOS; max++) {
            pickMany.add(registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(max),
                    this::addPhotos));
        }
    }

    private void pickPhotos() {
        int left = MAX_PHOTOS - photos.size();
        if (left <= 0) {
            Toast.makeText(this, "You can add up to " + MAX_PHOTOS + " photos", Toast.LENGTH_SHORT).show();
            return;
        }
        PickVisualMediaRequest request = new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build();
        if (left == 1) {
            pickOne.launch(request);
        } else {
            pickMany.get(left - 2).launch(request);
        }
    }

    private void addPhotos(List<Uri> picked) {
        for (Uri uri : picked) {
            if (photos.size() >= MAX_PHOTOS) {
                break;
            }
            if (!photos.contains(uri)) {
                photos.add(uri);
            }
        }
        showPhotos();
    }

    /** Rebuilds the row: the Add tile, then each photo with a ✕, the first one marked Cover. */
    private void showPhotos() {
        LinearLayout row = binding.photoRow;
        row.removeViews(1, row.getChildCount() - 1); // keep the Add tile
        for (int i = 0; i < photos.size(); i++) {
            row.addView(photoTile(photos.get(i), i));
        }
        binding.photoCount.setText(photos.size() + "/" + MAX_PHOTOS);
        binding.addPhoto.setVisibility(photos.size() >= MAX_PHOTOS ? View.GONE : View.VISIBLE);
    }

    private View photoTile(Uri uri, int index) {
        int size = dp(80);
        FrameLayout tile = new FrameLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMarginStart(dp(8));
        tile.setLayoutParams(params);
        tile.setBackgroundResource(R.drawable.km_card);
        tile.setClipToOutline(true);

        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setContentDescription(index == 0 ? "Cover photo" : "Photo " + (index + 1) + ", tap to make it the cover");
        tile.addView(image, new FrameLayout.LayoutParams(size, size));
        loadThumbnail(uri, image);

        if (index == 0) {
            TextView cover = new TextView(this);
            cover.setText("Cover");
            cover.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            cover.setTextColor(ContextCompat.getColor(this, R.color.km_black));
            cover.setBackgroundColor(ContextCompat.getColor(this, R.color.km_gold));
            cover.setGravity(Gravity.CENTER);
            tile.addView(cover, new FrameLayout.LayoutParams(size, FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM));
        }

        ImageView remove = new ImageView(this);
        remove.setImageResource(R.drawable.ic_close);
        remove.setColorFilter(ContextCompat.getColor(this, R.color.km_text));
        remove.setBackgroundResource(R.drawable.km_circle);
        remove.setPadding(dp(4), dp(4), dp(4), dp(4));
        remove.setContentDescription("Remove photo");
        FrameLayout.LayoutParams removeParams = new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.TOP | Gravity.END);
        removeParams.setMargins(dp(4), dp(4), dp(4), dp(4));
        tile.addView(remove, removeParams);

        remove.setOnClickListener(v -> {
            photos.remove(uri);
            thumbnails.remove(uri);
            showPhotos();
        });
        tile.setOnClickListener(v -> {
            if (photos.indexOf(uri) > 0) {
                photos.remove(uri);
                photos.add(0, uri);
                showPhotos();
                Toast.makeText(this, "Cover photo set", Toast.LENGTH_SHORT).show();
            }
        });
        return tile;
    }

    private void loadThumbnail(Uri uri, ImageView view) {
        Bitmap cached = thumbnails.get(uri);
        if (cached != null) {
            view.setImageBitmap(cached);
            return;
        }
        worker.execute(() -> {
            try {
                Bitmap bitmap = ListingUpload.decode(getContentResolver(), uri, THUMB_MAX_SIDE);
                main.post(() -> {
                    thumbnails.put(uri, bitmap);
                    view.setImageBitmap(bitmap);
                });
            } catch (IOException e) {
                main.post(() -> {
                    // The photo can't be read anymore (deleted, or access ended): drop it
                    if (photos.remove(uri)) {
                        showPhotos();
                        Toast.makeText(this, "Couldn't open one of the photos", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    // ── Publish ──────────────────────────────────────────────────────────────

    private void publish() {
        if (upload.isRunning()) {
            return;
        }
        String title = text(binding.listingTitle);
        String priceText = text(binding.price);
        String description = text(binding.description);

        // Check every field and point at the first problem
        EditText firstError = null;
        if (title.isEmpty()) {
            binding.listingTitle.setError("Add a title");
            firstError = binding.listingTitle;
        }
        BigDecimal price = parsePrice(priceText);
        if (price == null) {
            binding.price.setError(priceText.isEmpty() ? "Add a price (0 for free)" : "Enter a price like 25 or 19.99");
            if (firstError == null) firstError = binding.price;
        } else if (price.compareTo(MAX_PRICE) > 0) {
            binding.price.setError("Price can't be more than $100,000");
            if (firstError == null) firstError = binding.price;
        }
        if (firstError != null) {
            firstError.requestFocus();
            return;
        }
        if (photos.isEmpty()) {
            Toast.makeText(this, "Add at least one photo", Toast.LENGTH_SHORT).show();
            binding.addPhoto.requestFocus();
            return;
        }

        String campus = campusValue(chosen(binding.campusGroup));
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("title", title);
        fields.put("description", description);
        fields.put("price", price.stripTrailingZeros().toPlainString());
        fields.put("category", chosen(binding.categoryGroup));
        fields.put("condition", chosen(binding.conditionGroup));
        fields.put("campus", campus);
        // Older servers want a meetup area instead of a campus; newer ones set it from the campus
        fields.put("meetupArea", campus);
        fields.put("isAiGenerated", binding.aiGenerated.isChecked() ? "true" : "false");

        setPublishing(true);
        upload.start(fields, photos, Session.token(this));
    }

    /** Called by ListingUpload on the main thread, on whichever screen is showing now. */
    @Override
    public void onUploadDone(JSONObject listing, int status, String message) {
        setPublishing(false);
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (listing != null) {
            Toast.makeText(this, "Your listing is live!", Toast.LENGTH_SHORT).show();
            ShoppingActivity.refreshOnReturn();
            ListingDetailActivity.open(this, listing.optString("_id"));
            finish();
            return;
        }
        if (status == 401) {
            // Ignored if it came from an older login (the user logged out and back in meanwhile)
            Session.expired(this, upload.token());
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Couldn't publish")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void setPublishing(boolean on) {
        binding.publish.setEnabled(!on);
        binding.publish.setText(on ? "Publishing…" : "Publish Listing");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Asks before throwing away a half-filled form. */
    private void leave() {
        if (upload.isRunning()) {
            Toast.makeText(this, "Hang on, your listing is uploading", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean started = !photos.isEmpty() || !text(binding.listingTitle).isEmpty()
                || !text(binding.price).isEmpty() || !text(binding.description).isEmpty();
        if (!started) {
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Discard this listing?")
                .setMessage("What you've entered so far won't be saved.")
                .setNegativeButton("Keep Editing", null)
                .setPositiveButton("Discard", (dialog, which) -> finish())
                .show();
    }

    /** null unless it's a number ≥ 0 with at most 2 decimals. */
    static BigDecimal parsePrice(String text) {
        if (TextUtils.isEmpty(text)) {
            return null;
        }
        try {
            BigDecimal price = new BigDecimal(text.replace("$", "").replace(",", "").trim());
            return price.signum() < 0 || price.scale() > 2 ? null : price;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** The chips say "Rosen Campus"; the server's campus names are Main Campus, Downtown, Rosen, Lake Nona. */
    static String campusValue(String chip) {
        return chip.equals("Main Campus") ? chip : chip.replace(" Campus", "");
    }

    private String chosen(ChipGroup group) {
        Chip chip = findViewById(group.getCheckedChipId());
        return chip != null ? chip.getText().toString() : "";
    }

    private static String text(EditText field) {
        return field.getText().toString().trim();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
