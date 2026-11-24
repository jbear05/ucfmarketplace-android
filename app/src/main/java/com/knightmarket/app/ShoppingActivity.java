package com.knightmarket.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

    private final ListingFilters filters = new ListingFilters(); // category, search, condition, price, sort

    // Bumped when another screen changes what the feed should show (e.g. a new listing),
    // so the feed reloads when you come back to it instead of staying stale.
    private static int feedVersion;
    private int shownVersion;

    /** Call after creating or changing a listing; the feed reloads next time it's shown. */
    public static void refreshOnReturn() {
        feedVersion++;
    }

    private final ActivityResultLauncher<Intent> openFilters = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    filters.readFilterExtras(result.getData());
                    int count = filters.activeCount();
                    binding.textFilters.setText(count > 0 ? "Filters (" + count + ")" : "Filters");
                    loadListings();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShoppingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adapter = new ListingAdapter(listing -> ListingDetailActivity.open(this, listing.optString("_id")));
        binding.listingGrid.setLayoutManager(new GridLayoutManager(this, 2));
        binding.listingGrid.setAdapter(adapter);

        binding.searchContainer.setOnClickListener(v -> askForSearch());
        binding.textLocation.setOnClickListener(v -> {
            if (filters.search != null) {
                filters.search = null;
                loadListings();
            }
        });

        // The chips match the server's categories ("Tech" is Electronics there)
        setUpChip(binding.chipAll, null);
        setUpChip(binding.chipTextbooks, "Textbooks");
        setUpChip(binding.chipTech, "Electronics");
        setUpChip(binding.chipFurniture, "Furniture");

        binding.btnFilters.setOnClickListener(v -> openFilters.launch(FiltersActivity.intent(this, filters)));
        BottomNav.setup(this, binding.navHome, binding.navSaved, binding.navSell, binding.navChats, binding.navYou);
        binding.navHome.setOnClickListener(v -> binding.listingGrid.smoothScrollToPosition(0)); // already home

        shownVersion = feedVersion;
        loadListings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shownVersion != feedVersion) {
            shownVersion = feedVersion;
            loadListings();
        }
    }

    private void loadListings() {
        binding.textStatus.setVisibility(View.VISIBLE);
        binding.textStatus.setText("Loading listings…");
        // Drop the retry tap from an earlier failure so it only works while the error shows.
        binding.textStatus.setOnClickListener(null);
        binding.textStatus.setClickable(false);
        Api.listings(filters, Session.token(this), new Api.Callback() {
            @Override
            public void onSuccess(JSONObject data, String message) {
                JSONArray listings = data.optJSONArray("listings");
                if (listings == null) {
                    listings = new JSONArray();
                }
                adapter.setListings(listings);
                binding.listingGrid.scrollToPosition(0);
                if (listings.length() == 0) {
                    binding.textStatus.setText(filters.search != null
                            ? "No listings match \"" + filters.search + "\"" : "No listings here yet");
                } else {
                    binding.textStatus.setVisibility(View.GONE);
                }
                int total = data.optJSONObject("pagination") != null
                        ? data.optJSONObject("pagination").optInt("total", listings.length()) : listings.length();
                binding.textLocation.setText(filters.search != null
                        ? "Results for \"" + filters.search + "\" · tap to clear"
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
            filters.category = chipCategory;
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
        if (filters.search != null) {
            input.setText(filters.search);
            input.setSelection(filters.search.length());
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
        filters.search = trimmed.isEmpty() ? null : trimmed;
        loadListings();
    }
}
