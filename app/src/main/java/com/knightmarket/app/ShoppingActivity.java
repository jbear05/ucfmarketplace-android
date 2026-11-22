package com.knightmarket.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.knightmarket.app.databinding.ActivityShoppingBinding;

import org.json.JSONArray;
import org.json.JSONObject;

public class ShoppingActivity extends AppCompatActivity {

    private ActivityShoppingBinding binding;
    private ListingAdapter adapter;

    private String category; // null = All
    private String search;   // null = no search

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShoppingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adapter = new ListingAdapter(this::showListing);
        binding.listingGrid.setLayoutManager(new GridLayoutManager(this, 2));
        binding.listingGrid.setAdapter(adapter);

        binding.searchContainer.setOnClickListener(v -> askForSearch());
        binding.textLocation.setOnClickListener(v -> {
            if (search != null) {
                search = null;
                loadListings();
            }
        });

        // The chips match the server's categories ("Tech" is Electronics there)
        setUpChip(binding.chipAll, null);
        setUpChip(binding.chipTextbooks, "Textbooks");
        setUpChip(binding.chipTech, "Electronics");
        setUpChip(binding.chipFurniture, "Furniture");

        binding.btnFilters.setOnClickListener(v -> comingSoon("Filters"));
        binding.navHome.setOnClickListener(v -> binding.listingGrid.smoothScrollToPosition(0));
        binding.navSaved.setOnClickListener(v -> comingSoon("Saved items"));
        binding.navSell.setOnClickListener(v -> comingSoon("Selling"));
        binding.navChats.setOnClickListener(v -> comingSoon("Chats"));
        binding.navYou.setOnClickListener(v -> showAccount());

        loadListings();
    }

    private void loadListings() {
        binding.textStatus.setVisibility(View.VISIBLE);
        binding.textStatus.setText("Loading listings…");
        Api.listings(category, search, Session.token(this), new Api.Callback() {
            @Override
            public void onSuccess(JSONObject data, String message) {
                JSONArray listings = data.optJSONArray("listings");
                if (listings == null) {
                    listings = new JSONArray();
                }
                adapter.setListings(listings);
                binding.listingGrid.scrollToPosition(0);
                if (listings.length() == 0) {
                    binding.textStatus.setText(search != null
                            ? "No listings match \"" + search + "\"" : "No listings here yet");
                } else {
                    binding.textStatus.setVisibility(View.GONE);
                }
                int total = data.optJSONObject("pagination") != null
                        ? data.optJSONObject("pagination").optInt("total", listings.length()) : listings.length();
                binding.textLocation.setText(search != null
                        ? "Results for \"" + search + "\" · tap to clear"
                        : total + (total == 1 ? " listing" : " listings"));
            }

            @Override
            public void onError(int status, String message) {
                adapter.setListings(new JSONArray());
                binding.textStatus.setVisibility(View.VISIBLE);
                binding.textStatus.setText(message + "\nTap to try again.");
                binding.textStatus.setOnClickListener(v -> loadListings());
            }
        });
    }

    private void setUpChip(TextView chip, String chipCategory) {
        chip.setOnClickListener(v -> {
            category = chipCategory;
            for (TextView other : new TextView[]{binding.chipAll, binding.chipTextbooks, binding.chipTech, binding.chipFurniture}) {
                boolean selected = other == chip;
                other.setBackgroundResource(selected ? R.drawable.filter_chip_selected_bg : R.drawable.filter_chip_bg);
                other.setTextColor(ContextCompat.getColor(this, selected ? R.color.white : R.color.text_main));
            }
            loadListings();
        });
    }

    private void askForSearch() {
        EditText input = new EditText(this);
        input.setHint("Search textbooks, tech, furniture...");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        input.setSingleLine(true);
        if (search != null) {
            input.setText(search);
            input.setSelection(search.length());
        }
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad / 2, pad, pad / 2);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Search")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Search", (d, which) -> runSearch(input.getText().toString()))
                .create();
        input.setOnEditorActionListener((v, actionId, event) -> {
            runSearch(input.getText().toString());
            dialog.dismiss();
            return true;
        });
        dialog.show();
    }

    private void runSearch(String text) {
        String trimmed = text.trim();
        search = trimmed.isEmpty() ? null : trimmed;
        loadListings();
    }

    /** A simple details view until the Listing Detail screen is built. */
    private void showListing(JSONObject listing) {
        JSONObject owner = listing.optJSONObject("owner");
        StringBuilder details = new StringBuilder()
                .append(ListingAdapter.price(listing.optDouble("price", 0)))
                .append(" · ").append(listing.optString("condition"))
                .append(" · ").append(listing.optString("category"))
                .append("\n\n").append(listing.optString("description"));
        if (!listing.optString("meetupArea").isEmpty()) {
            details.append("\n\nMeetup area: ").append(listing.optString("meetupArea"));
        }
        if (owner != null) {
            details.append("\nSeller: ").append(owner.optString("name"));
            if (owner.optBoolean("isVerifiedStudent")) {
                details.append(" ✓ Verified Student");
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(listing.optString("title"))
                .setMessage(details.toString())
                .setPositiveButton("Close", null)
                .show();
    }

    private void showAccount() {
        if (!Session.isLoggedIn(this)) {
            startActivity(new Intent(this, LandingActivity.class));
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(Session.name(this))
                .setMessage(Session.email(this))
                .setNegativeButton("Close", null)
                .setPositiveButton("Log out", (d, which) -> {
                    Session.clear(this);
                    Intent intent = new Intent(this, LandingActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .show();
    }

    private void comingSoon(String what) {
        Toast.makeText(this, what + " Coming Soon!", Toast.LENGTH_SHORT).show();
    }
}
