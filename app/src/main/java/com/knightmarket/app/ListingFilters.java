package com.knightmarket.app;

import android.content.Intent;

/** What the Shopping feed is filtered by; null means "any". Matches GET /api/listings. */
public class ListingFilters {

    public static final String SORT_NEWEST = null; // the server's default
    public static final String SORT_PRICE_LOW = "price_asc";

    private static final String EXTRA_CONDITION = "filter_condition";
    private static final String EXTRA_MIN_PRICE = "filter_min_price";
    private static final String EXTRA_MAX_PRICE = "filter_max_price";
    private static final String EXTRA_SORT = "filter_sort";

    public String category;
    public String search;
    public String condition;   // New, Like New, Good, Fair, Poor
    public Integer minPrice;
    public Integer maxPrice;
    public String sortBy;

    /** How many of the Filters screen's options are set (for the Filters button). */
    public int activeCount() {
        int count = 0;
        if (condition != null) count++;
        if (minPrice != null || maxPrice != null) count++;
        if (sortBy != null) count++;
        return count;
    }

    String toQuery() {
        StringBuilder query = new StringBuilder();
        append(query, "category", category);
        append(query, "search", search);
        append(query, "condition", condition);
        append(query, "minPrice", minPrice == null ? null : String.valueOf(minPrice));
        append(query, "maxPrice", maxPrice == null ? null : String.valueOf(maxPrice));
        append(query, "sortBy", sortBy);
        return query.toString();
    }

    private static void append(StringBuilder query, String key, String value) {
        if (value != null && !value.isEmpty()) {
            query.append('&').append(key).append('=').append(Api.encode(value));
        }
    }

    /** The Filters screen's part (condition, price, sort) travels between screens in an Intent. */
    public void putFilterExtras(Intent intent) {
        intent.putExtra(EXTRA_CONDITION, condition);
        if (minPrice != null) intent.putExtra(EXTRA_MIN_PRICE, (int) minPrice);
        if (maxPrice != null) intent.putExtra(EXTRA_MAX_PRICE, (int) maxPrice);
        intent.putExtra(EXTRA_SORT, sortBy);
    }

    public void readFilterExtras(Intent intent) {
        condition = intent.getStringExtra(EXTRA_CONDITION);
        minPrice = intent.hasExtra(EXTRA_MIN_PRICE) ? intent.getIntExtra(EXTRA_MIN_PRICE, 0) : null;
        maxPrice = intent.hasExtra(EXTRA_MAX_PRICE) ? intent.getIntExtra(EXTRA_MAX_PRICE, 0) : null;
        sortBy = intent.getStringExtra(EXTRA_SORT);
    }
}
