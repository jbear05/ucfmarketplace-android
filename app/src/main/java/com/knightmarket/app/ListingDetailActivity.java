package com.knightmarket.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.knightmarket.app.databinding.ActivityListingDetailBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/** One listing: photos, price, status, details, seller, save (heart) and the owner's Change Status. */
public class ListingDetailActivity extends AppCompatActivity {

    private static final String EXTRA_ID = "listing_id";

    private ActivityListingDetailBinding binding;
    private String listingId;
    private JSONObject listing;
    private JSONArray images = new JSONArray();
    private int photoIndex;
    private boolean favorited;

    public static void open(Context context, String listingId) {
        Intent intent = new Intent(context, ListingDetailActivity.class);
        intent.putExtra(EXTRA_ID, listingId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityListingDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBars.pad(binding.getRoot());

        listingId = getIntent().getStringExtra(EXTRA_ID);
        if (listingId == null) {
            finish();
            return;
        }

        binding.back.setOnClickListener(v -> finish());
        binding.favorite.setOnClickListener(v -> toggleFavorite());
        binding.detailImage.setOnClickListener(v -> nextPhoto());
        binding.directions.setOnClickListener(v -> openDirections());
        binding.main.setVisibility(View.INVISIBLE); // until it loads

        load();
    }

    private void load() {
        Api.listing(listingId, Session.token(this), new Api.Callback() {
            @Override
            public void onSuccess(JSONObject data, String message) {
                listing = data;
                show();
                binding.main.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(int status, String message) {
                Toast.makeText(ListingDetailActivity.this,
                        status == 404 ? "This listing is no longer available" : message, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void show() {
        binding.detailPrice.setText(ListingAdapter.price(listing.optDouble("price", 0)));
        ListingStatus.showBadge(binding.detailStatus, listing.optString("status"));
        binding.detailTitle.setText(listing.optString("title"));
        binding.detailArea.setText(listing.optString("meetupArea", "UCF"));
        binding.detailPosted.setText(postedAgo(listing.optString("createdAt")));
        binding.detailCategory.setText(listing.optString("category"));
        binding.detailCondition.setText(listing.optString("condition"));
        binding.detailDescription.setText(listing.optString("description"));

        JSONObject owner = listing.optJSONObject("owner");
        String ownerName = owner != null ? owner.optString("name", "Student") : "Student";
        binding.sellerName.setText(ownerName);
        binding.sellerInitial.setText(ownerName.isEmpty() ? "?" : ownerName.substring(0, 1).toUpperCase(Locale.US));
        StringBuilder badge = new StringBuilder();
        if (owner != null && owner.optBoolean("isVerifiedStudent")) {
            badge.append("✓ Verified Student");
        }
        if (owner != null && owner.optInt("ratingCount", 0) > 0) {
            if (badge.length() > 0) badge.append(" · ");
            badge.append(String.format(Locale.US, "★ %.1f (%d)", owner.optDouble("ratingAvg"), owner.optInt("ratingCount")));
        }
        binding.sellerBadge.setText(badge.length() > 0 ? badge : "No ratings yet");

        images = listing.optJSONArray("images") != null ? listing.optJSONArray("images") : new JSONArray();
        photoIndex = 0;
        showPhoto();

        favorited = listing.optBoolean("isFavorited");
        showFavorite();

        if (listing.optBoolean("isOwnListing")) {
            // Your own listing: manage it instead of messaging yourself (as in the Flutter app)
            binding.favorite.setVisibility(View.GONE);
            binding.contact.setText("Change Status");
            binding.contact.setOnClickListener(v -> ListingStatus.choose(this, listingId,
                    listing.optString("status"), newStatus -> {
                        putQuietly(listing, "status", newStatus);
                        ListingStatus.showBadge(binding.detailStatus, newStatus);
                    }));
        } else {
            binding.contact.setText("Message Seller");
            binding.contact.setOnClickListener(v ->
                    Toast.makeText(this, "Messages Coming Soon!", Toast.LENGTH_SHORT).show());
            // TODO: open the chat with the seller (POST /api/threads) once Messages is built
        }
    }

    private void showPhoto() {
        ImageLoader.load(images.length() > 0 ? images.optString(photoIndex) : null, binding.detailImage);
        binding.detailPhotoCount.setVisibility(images.length() > 1 ? View.VISIBLE : View.GONE);
        binding.detailPhotoCount.setText((photoIndex + 1) + " / " + images.length());
    }

    /** Tap the photo to see the next one. */
    private void nextPhoto() {
        if (images.length() > 1) {
            photoIndex = (photoIndex + 1) % images.length();
            showPhoto();
        }
    }

    private void toggleFavorite() {
        if (!Session.isLoggedIn(this)) {
            Toast.makeText(this, "Log in to save listings", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LandingActivity.class));
            return;
        }
        favorited = !favorited; // show it right away, then confirm with the server
        showFavorite();
        Api.toggleFavorite(listingId, Session.token(this), new Api.Callback() {
            @Override
            public void onSuccess(JSONObject data, String message) {
                favorited = data.optBoolean("isFavorited", favorited);
                showFavorite();
            }

            @Override
            public void onError(int status, String message) {
                favorited = !favorited;
                showFavorite();
                Toast.makeText(ListingDetailActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showFavorite() {
        binding.favorite.setImageResource(favorited ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border);
        binding.favorite.setContentDescription(favorited ? "Unsave listing" : "Save listing");
    }

    /** Opens the maps app at the listing's meetup area. */
    private void openDirections() {
        JSONObject point = listing != null ? listing.optJSONObject("coordinates") : null;
        JSONArray lngLat = point != null ? point.optJSONArray("coordinates") : null;
        if (lngLat == null || lngLat.length() < 2) {
            Toast.makeText(this, "No location for this listing", Toast.LENGTH_SHORT).show();
            return;
        }
        double lng = lngLat.optDouble(0);
        double lat = lngLat.optDouble(1);
        Uri uri = Uri.parse(String.format(Locale.US, "geo:%f,%f?q=%f,%f(%s)", lat, lng, lat, lng,
                Uri.encode(listing.optString("meetupArea", "Meetup area"))));
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "No maps app installed", Toast.LENGTH_SHORT).show();
        }
    }

    /** "Posted 4d ago" from the server's ISO date. */
    static String postedAgo(String isoDate) {
        try {
            Duration age = Duration.between(Instant.parse(isoDate), Instant.now());
            long minutes = Math.max(0, age.toMinutes());
            if (minutes < 60) return "Posted " + Math.max(1, minutes) + "m ago";
            if (minutes < 60 * 24) return "Posted " + (minutes / 60) + "h ago";
            return "Posted " + (minutes / (60 * 24)) + "d ago";
        } catch (Exception e) {
            return "";
        }
    }

    private static void putQuietly(JSONObject json, String key, String value) {
        try {
            json.put(key, value);
        } catch (Exception ignored) {
            // only fails for a null key
        }
    }
}
