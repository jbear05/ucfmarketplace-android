package com.knightmarket.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.knightmarket.app.databinding.ActivitySellerProfileBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Screen 12: a seller's public profile, opened from the seller card on Listing detail.
 * Shows their name, rating, sales, member since, active listings and reviews.
 */
public class SellerProfileActivity extends AppCompatActivity {

    private static final String EXTRA_OWNER = "owner_json";

    private ActivitySellerProfileBinding binding;
    private ListingAdapter adapter;
    private String sellerId;

    /** owner is the listing's "owner" object (it has _id, name, rating). */
    public static void open(Context context, JSONObject owner) {
        if (owner == null || owner.optString("_id").isEmpty()) {
            Toast.makeText(context, "Seller profile isn't available", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(context, SellerProfileActivity.class);
        intent.putExtra(EXTRA_OWNER, owner.toString());
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySellerProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBars.pad(binding.getRoot());

        JSONObject owner = parse(getIntent().getStringExtra(EXTRA_OWNER));
        sellerId = owner != null ? owner.optString("_id") : "";
        if (sellerId.isEmpty()) {
            finish();
            return;
        }

        binding.back.setOnClickListener(v -> finish());

        adapter = new ListingAdapter(listing -> ListingDetailActivity.open(this, listing.optString("_id")));
        binding.sellerGrid.setLayoutManager(new GridLayoutManager(this, 2));
        binding.sellerGrid.setAdapter(adapter);

        // Show what the listing already told us right away, then fill in the rest
        showProfile(owner);
        loadProfile();
        loadReviews();
    }

    // ── Profile and listings ────────────────────────────────────────────────

    private void loadProfile() {
        Api.userProfile(sellerId, Session.token(this), new Api.Callback() {
            @Override
            public void onSuccess(JSONObject data, String message) {
                JSONObject profile = data.optJSONObject("profile");
                if (profile != null) showProfile(profile);
                showListings(data.optJSONArray("listings"));
            }

            @Override
            public void onError(int status, String message) {
                if (status == 404 && "User not found".equals(message)) {
                    Toast.makeText(SellerProfileActivity.this,
                            "This seller's account is no longer available", Toast.LENGTH_LONG).show();
                    finish();
                } else if (status == 404) {
                    // Older server without GET /users/:id: find their listings in the public feed
                    loadListingsFromFeed();
                } else {
                    binding.listingsEmpty.setText(message);
                }
            }
        });
    }

    private void loadListingsFromFeed() {
        Api.listings(new ListingFilters(), Session.token(this), new Api.Callback() {
            @Override
            public void onSuccess(JSONObject data, String message) {
                JSONArray all = data.optJSONArray("listings");
                JSONArray mine = new JSONArray();
                for (int i = 0; all != null && i < all.length(); i++) {
                    JSONObject listing = all.optJSONObject(i);
                    JSONObject owner = listing != null ? listing.optJSONObject("owner") : null;
                    if (owner != null && sellerId.equals(owner.optString("_id"))
                            && "active".equals(listing.optString("status"))) {
                        mine.put(listing);
                    }
                }
                showListings(mine);
            }

            @Override
            public void onError(int status, String message) {
                binding.listingsEmpty.setText(message);
            }
        });
    }

    private void showProfile(JSONObject profile) {
        String name = profile.optString("name", "Student");
        if (name.isEmpty()) name = "Student";
        binding.topTitle.setText(name);
        binding.sellerName.setText(name);
        binding.sellerInitial.setText(name.substring(0, 1).toUpperCase(Locale.US));

        String avatar = profile.optString("avatarUrl", "");
        if (!avatar.isEmpty() && !"null".equals(avatar)) {
            binding.sellerAvatar.setVisibility(View.VISIBLE);
            ImageLoader.load(avatar, binding.sellerAvatar);
        }

        int ratingCount = profile.optInt("ratingCount", 0);
        StringBuilder badge = new StringBuilder();
        if (profile.optBoolean("isVerifiedStudent")) badge.append("✓ Verified Student");
        String classYear = profile.optString("classYear", "");
        if (!classYear.isEmpty() && !"null".equals(classYear)) {
            if (badge.length() > 0) badge.append(" · ");
            badge.append("Class of ").append(classYear);
        }
        if (ratingCount == 0) {
            if (badge.length() > 0) badge.append(" · ");
            badge.append("No ratings yet");
        }
        binding.sellerBadge.setText(badge);
        binding.sellerBadge.setTextColor(getColor(profile.optBoolean("isVerifiedStudent") ? R.color.km_green : R.color.km_muted));

        binding.statRating.setText(ratingCount == 0 ? "—"
                : String.format(Locale.US, "★ %.1f (%d)", profile.optDouble("ratingAvg", 0), ratingCount));
        if (profile.has("salesCount")) {
            binding.statSales.setText(String.valueOf(profile.optInt("salesCount", 0)));
        }
        try {
            binding.statSince.setText(DateTimeFormatter.ofPattern("MMM yyyy", Locale.US)
                    .format(Instant.parse(profile.optString("createdAt")).atZone(ZoneId.systemDefault())));
        } catch (Exception ignored) {
            // the listing's owner object has no join date; keep the dash
        }
    }

    private void showListings(JSONArray listings) {
        int count = listings != null ? listings.length() : 0;
        binding.listingsTitle.setText(count > 0 ? "Active Listings (" + count + ")" : "Active Listings");
        if (count == 0) {
            binding.sellerGrid.setVisibility(View.GONE);
            binding.listingsEmpty.setVisibility(View.VISIBLE);
            binding.listingsEmpty.setText("No active listings right now");
            return;
        }
        adapter.setListings(listings);
        binding.listingsEmpty.setVisibility(View.GONE);
        binding.sellerGrid.setVisibility(View.VISIBLE);
    }

    // ── Reviews ─────────────────────────────────────────────────────────────

    private void loadReviews() {
        Api.userRatings(sellerId, new Api.Callback() {
            @Override
            public void onSuccess(JSONObject data, String message) {
                showReviews(data.optJSONArray("ratings"));
            }

            @Override
            public void onError(int status, String message) {
                binding.reviewsEmpty.setText("Couldn't load reviews");
            }
        });
    }

    private void showReviews(JSONArray ratings) {
        binding.reviews.removeAllViews();
        int count = ratings != null ? ratings.length() : 0;
        binding.reviewsEmpty.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        binding.reviewsEmpty.setText("No reviews yet");
        for (int i = 0; i < count; i++) {
            JSONObject rating = ratings.optJSONObject(i);
            if (rating != null) binding.reviews.addView(reviewView(rating));
        }
    }

    private View reviewView(JSONObject rating) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.km_card);
        int pad = dp(12);
        card.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(8);
        card.setLayoutParams(params);

        int score = Math.max(0, Math.min(5, rating.optInt("score", 0)));
        JSONObject from = rating.optJSONObject("fromUser");
        String fromName = from != null ? from.optString("name", "Student") : "Student";
        String date = ListingDetailActivity.postedAgo(rating.optString("createdAt")).replace("Posted ", "");

        TextView header = text("★★★★★".substring(0, score) + "☆☆☆☆☆".substring(score)
                + "  " + fromName + (date.isEmpty() ? "" : " · " + date), 13, R.color.km_gold);
        card.addView(header);

        String feedback = rating.optString("feedback", "").trim();
        if (!feedback.isEmpty() && !"null".equals(feedback)) {
            TextView body = text(feedback, 13, R.color.km_text);
            body.setPadding(0, dp(6), 0, 0);
            card.addView(body);
        }
        return card;
    }

    private TextView text(String value, int sp, int colorRes) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        view.setTextColor(getColor(colorRes));
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static JSONObject parse(String json) {
        if (json == null) return null;
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            return null;
        }
    }
}
