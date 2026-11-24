package com.knightmarket.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.knightmarket.app.databinding.ActivitySavedItemsBinding;

import org.json.JSONArray;
import org.json.JSONObject;

/** The listings you saved with the heart (GET /api/auth/me returns them as "favorites"). */
public class SavedItemsActivity extends AppCompatActivity {

    private ActivitySavedItemsBinding binding;
    private ListingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySavedItemsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBars.pad(binding.getRoot());

        adapter = new ListingAdapter(listing -> ListingDetailActivity.open(this, listing.optString("_id")));
        binding.savedGrid.setLayoutManager(new GridLayoutManager(this, 2));
        binding.savedGrid.setAdapter(adapter);

        binding.savedSearchesTab.setOnClickListener(v ->
                Toast.makeText(this, "Saved Searches Coming Soon!", Toast.LENGTH_SHORT).show());

        BottomNav.setup(this, binding.navHome, binding.navSaved, binding.navSell, binding.navChats, binding.navYou);
    }

    @Override
    protected void onResume() {
        super.onResume();
        load(); // a listing may have been saved or unsaved on its detail screen
    }

    private void load() {
        if (!Session.isLoggedIn(this)) {
            showEmpty("Log in to see your saved listings", "Tap here to log in");
            binding.savedEmpty.setOnClickListener(v -> startActivity(new Intent(this, LandingActivity.class)));
            return;
        }
        binding.savedEmpty.setOnClickListener(null);
        String token = Session.token(this);
        Api.me(token, new Api.Callback() {
            @Override
            public void onSuccess(JSONObject data, String message) {
                // Some saved listings may have been deleted since; keep only real ones
                JSONArray raw = data.optJSONArray("favorites");
                JSONArray favorites = new JSONArray();
                for (int i = 0; raw != null && i < raw.length(); i++) {
                    JSONObject listing = raw.optJSONObject(i);
                    if (listing != null && !"offMarket".equals(listing.optString("status"))) {
                        favorites.put(listing);
                    }
                }
                adapter.setListings(favorites);
                binding.savedTitle.setText(favorites.length() > 0 ? "Saved (" + favorites.length() + ")" : "Saved");
                if (favorites.length() == 0) {
                    showEmpty("No saved listings yet", "Tap the heart on any listing to save it");
                } else {
                    binding.savedEmpty.setVisibility(View.GONE);
                    binding.savedGrid.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(int status, String message) {
                if (status == 401) {
                    // the login expired; a slow 401 from an older login is ignored
                    Session.clearIfCurrent(SavedItemsActivity.this, token);
                    load();
                } else {
                    showEmpty("Couldn't load your saved listings", message);
                }
            }
        });
    }

    private void showEmpty(String title, String hint) {
        binding.savedGrid.setVisibility(View.GONE);
        binding.savedEmpty.setVisibility(View.VISIBLE);
        binding.savedEmptyTitle.setText(title);
        binding.savedEmptyHint.setText(hint);
    }
}
