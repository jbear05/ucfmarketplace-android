package com.knightmarket.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.knightmarket.app.databinding.ActivityMyListingsBinding;

import org.json.JSONArray;
import org.json.JSONObject;

/** Your own listings (all statuses): open, change status (Available / Pending / Off market), delete. */
public class MyListingsActivity extends AppCompatActivity {

    private ActivityMyListingsBinding binding;
    private MyListingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyListingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBars.pad(binding.getRoot());

        binding.back.setOnClickListener(v -> finish());
        binding.newOwnedListing.setOnClickListener(v ->
                Toast.makeText(this, "Create Listing Coming Soon!", Toast.LENGTH_SHORT).show());
        // TODO: open CreateListingActivity once it's built

        adapter = new MyListingAdapter(new MyListingAdapter.Actions() {
            @Override
            public void open(JSONObject listing) {
                ListingDetailActivity.open(MyListingsActivity.this, listing.optString("_id"));
            }

            @Override
            public void changeStatus(JSONObject listing, int position) {
                ListingStatus.choose(MyListingsActivity.this, listing.optString("_id"), listing.optString("status"),
                        newStatus -> {
                            try {
                                listing.put("status", newStatus);
                            } catch (Exception ignored) {
                                // only fails for a null key
                            }
                            adapter.notifyItemChanged(position);
                        });
            }

            @Override
            public void delete(JSONObject listing, int position) {
                confirmDelete(listing, position);
            }
        });
        binding.myList.setLayoutManager(new LinearLayoutManager(this));
        binding.myList.setAdapter(adapter);

        BottomNav.setup(this, binding.navHome, binding.navSaved, binding.navSell, binding.navChats, binding.navYou);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Session.isLoggedIn(this)) {
            startActivity(new Intent(this, LandingActivity.class));
            finish();
            return;
        }
        load(); // its status may have changed on the detail screen
    }

    private void load() {
        Api.myListings(Session.token(this), new Api.Callback() {
            @Override
            public void onSuccess(JSONObject data, String message) {
                JSONArray listings = data.optJSONArray("listings");
                adapter.setListings(listings != null ? listings : new JSONArray());
                showEmptyIfNeeded();
            }

            @Override
            public void onError(int status, String message) {
                if (status == 401) {
                    Session.expired(MyListingsActivity.this);
                    return;
                }
                binding.myList.setVisibility(View.GONE);
                binding.myEmpty.setVisibility(View.VISIBLE);
                binding.myEmpty.setText(message);
            }
        });
    }

    private void showEmptyIfNeeded() {
        boolean empty = adapter.getItemCount() == 0;
        binding.myList.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.myEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.myEmpty.setText("You haven't listed anything yet\nTap + to list your first item");
    }

    private void confirmDelete(JSONObject listing, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Listing?")
                .setMessage("\"" + listing.optString("title") + "\" will be removed for good. This can't be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) ->
                        Api.deleteListing(listing.optString("_id"), Session.token(this), new Api.Callback() {
                            @Override
                            public void onSuccess(JSONObject data, String message) {
                                adapter.remove(position);
                                showEmptyIfNeeded();
                                Toast.makeText(MyListingsActivity.this, "Listing deleted", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(int status, String message) {
                                if (status == 401) {
                                    Session.expired(MyListingsActivity.this);
                                    return;
                                }
                                Toast.makeText(MyListingsActivity.this, message, Toast.LENGTH_LONG).show();
                            }
                        }))
                .show();
    }
}
