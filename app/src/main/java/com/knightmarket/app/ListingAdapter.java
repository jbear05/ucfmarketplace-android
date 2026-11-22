package com.knightmarket.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.knightmarket.app.databinding.ItemListingBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shows listings from GET /api/listings as cards in the Shopping grid. */
public class ListingAdapter extends RecyclerView.Adapter<ListingAdapter.Holder> {

    public interface OnListingClick {
        void onClick(JSONObject listing);
    }

    private final List<JSONObject> listings = new ArrayList<>();
    private final OnListingClick onClick;

    public ListingAdapter(OnListingClick onClick) {
        this.onClick = onClick;
    }

    public void setListings(JSONArray array) {
        listings.clear();
        for (int i = 0; i < array.length(); i++) {
            JSONObject listing = array.optJSONObject(i);
            if (listing != null) {
                listings.add(listing);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemListingBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        JSONObject listing = listings.get(position);
        ItemListingBinding b = holder.binding;

        b.listingPrice.setText(price(listing.optDouble("price", 0)));
        b.listingTitle.setText(listing.optString("title", ""));
        b.listingTag.setText(listing.optString("condition", ""));

        JSONObject owner = listing.optJSONObject("owner");
        int ratingCount = owner != null ? owner.optInt("ratingCount", 0) : 0;
        boolean hasRating = ratingCount > 0;
        b.listingStar.setVisibility(hasRating ? View.VISIBLE : View.GONE);
        b.listingRating.setVisibility(hasRating ? View.VISIBLE : View.GONE);
        if (hasRating) {
            b.listingRating.setText(String.format(Locale.US, "%.1f", owner.optDouble("ratingAvg", 0)));
        }

        JSONArray images = listing.optJSONArray("images");
        ImageLoader.load(images != null && images.length() > 0 ? images.optString(0) : null, b.listingImage);

        b.listingCard.setOnClickListener(v -> onClick.onClick(listing));
    }

    @Override
    public int getItemCount() {
        return listings.size();
    }

    /** $18, or $18.50 when there are cents. */
    public static String price(double value) {
        return value == Math.floor(value)
                ? String.format(Locale.US, "$%.0f", value)
                : String.format(Locale.US, "$%.2f", value);
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemListingBinding binding;

        Holder(ItemListingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
