package com.knightmarket.app;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.knightmarket.app.databinding.ItemMyListingBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** The user's own listings in My Listings, with Change Status and Delete. */
public class MyListingAdapter extends RecyclerView.Adapter<MyListingAdapter.Holder> {

    public interface Actions {
        void open(JSONObject listing);

        void changeStatus(JSONObject listing, int position);

        void delete(JSONObject listing, int position);
    }

    private final List<JSONObject> listings = new ArrayList<>();
    private final Actions actions;

    public MyListingAdapter(Actions actions) {
        this.actions = actions;
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

    public void remove(int position) {
        listings.remove(position);
        notifyItemRemoved(position);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemMyListingBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        JSONObject listing = listings.get(position);
        ItemMyListingBinding b = holder.binding;

        b.myTitle.setText(listing.optString("title"));
        b.myMeta.setText(ListingAdapter.price(listing.optDouble("price", 0)) + " · " + listing.optString("category"));
        ListingStatus.showBadge(b.myStatus, listing.optString("status"));

        JSONArray images = listing.optJSONArray("images");
        ImageLoader.load(images != null && images.length() > 0 ? images.optString(0) : null, b.myImage);

        b.myCard.setOnClickListener(v -> actions.open(listing));
        b.myChangeStatus.setOnClickListener(v -> actions.changeStatus(listing, holder.getBindingAdapterPosition()));
        b.myDelete.setOnClickListener(v -> actions.delete(listing, holder.getBindingAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return listings.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemMyListingBinding binding;

        Holder(ItemMyListingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
