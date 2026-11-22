package com.knightmarket.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.knightmarket.app.databinding.ActivityFiltersBinding;

import org.json.JSONObject;

/**
 * Price range, condition and sort for the Shopping feed (like the Flutter filter sheet).
 * Shopping opens it with its current filters and gets the new ones back.
 */
public class FiltersActivity extends AppCompatActivity {

    private static final String EXTRA_CATEGORY = "filter_category";
    private static final String EXTRA_SEARCH = "filter_search";

    private ActivityFiltersBinding binding;
    private final ListingFilters filters = new ListingFilters();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable countRunnable = this::countResults;
    private TextView[] conditionChips;
    private String[] conditionValues;

    public static Intent intent(Context context, ListingFilters current) {
        Intent intent = new Intent(context, FiltersActivity.class);
        current.putFilterExtras(intent);
        intent.putExtra(EXTRA_CATEGORY, current.category);
        intent.putExtra(EXTRA_SEARCH, current.search);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFiltersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBars.pad(binding.getRoot());

        filters.readFilterExtras(getIntent());
        filters.category = getIntent().getStringExtra(EXTRA_CATEGORY);
        filters.search = getIntent().getStringExtra(EXTRA_SEARCH);

        conditionChips = new TextView[]{binding.conditionNew, binding.conditionLikeNew, binding.conditionGood, binding.conditionFair};
        conditionValues = new String[]{"New", "Like New", "Good", "Fair"};

        binding.btnClose.setOnClickListener(v -> finish());
        binding.textReset.setOnClickListener(v -> {
            filters.condition = null;
            filters.minPrice = null;
            filters.maxPrice = null;
            filters.sortBy = ListingFilters.SORT_NEWEST;
            binding.filterMinPrice.setText("");
            binding.filterMaxPrice.setText("");
            showChoices();
            countSoon();
        });

        if (filters.minPrice != null) binding.filterMinPrice.setText(String.valueOf(filters.minPrice));
        if (filters.maxPrice != null) binding.filterMaxPrice.setText(String.valueOf(filters.maxPrice));
        watchPrice(binding.filterMinPrice, true);
        watchPrice(binding.filterMaxPrice, false);

        for (int i = 0; i < conditionChips.length; i++) {
            String value = conditionValues[i];
            conditionChips[i].setOnClickListener(v -> {
                // One condition at a time (the server filters by one); tap again to clear
                filters.condition = value.equals(filters.condition) ? null : value;
                showChoices();
                countSoon();
            });
        }

        binding.sortPriceLow.setOnClickListener(v -> {
            filters.sortBy = ListingFilters.SORT_PRICE_LOW;
            showChoices();
            countSoon();
        });
        binding.sortNewest.setOnClickListener(v -> {
            filters.sortBy = ListingFilters.SORT_NEWEST;
            showChoices();
            countSoon();
        });

        binding.btnShowResults.setOnClickListener(v -> {
            Intent result = new Intent();
            filters.putFilterExtras(result);
            setResult(RESULT_OK, result);
            finish();
        });

        showChoices();
        countResults();
    }

    private void watchPrice(EditText field, boolean isMin) {
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                Integer value = parsePrice(s.toString());
                if (isMin) {
                    filters.minPrice = value;
                } else {
                    filters.maxPrice = value;
                }
                countSoon();
            }
        });
    }

    private static Integer parsePrice(String text) {
        String digits = text.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void showChoices() {
        for (int i = 0; i < conditionChips.length; i++) {
            boolean selected = conditionValues[i].equals(filters.condition);
            conditionChips[i].setBackgroundResource(selected ? R.drawable.condition_filter_active_bg : R.drawable.condition_filter_bg);
        }
        boolean priceLow = ListingFilters.SORT_PRICE_LOW.equals(filters.sortBy);
        binding.radioPriceLow.setImageResource(priceLow ? R.drawable.radio_checked : R.drawable.radio_unchecked);
        binding.radioNewest.setImageResource(priceLow ? R.drawable.radio_unchecked : R.drawable.radio_checked);
    }

    /** Waits for typing to pause before asking the server how many listings match. */
    private void countSoon() {
        handler.removeCallbacks(countRunnable);
        handler.postDelayed(countRunnable, 400);
    }

    private void countResults() {
        binding.btnShowResults.setText("Show results");
        Api.listings(filters, Session.token(this), new Api.Callback() {
            @Override
            public void onSuccess(JSONObject data, String message) {
                JSONObject pagination = data.optJSONObject("pagination");
                int total = pagination != null ? pagination.optInt("total", 0) : 0;
                binding.btnShowResults.setText(total == 1 ? "Show 1 result" : "Show " + total + " results");
            }

            @Override
            public void onError(int status, String message) {
                binding.btnShowResults.setText("Show results");
            }
        });
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(countRunnable);
        super.onDestroy();
    }
}
