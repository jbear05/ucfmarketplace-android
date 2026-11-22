package com.knightmarket.app;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;

/**
 * The bottom bar shared by Shopping, Saved, Messages, Profile and My Listings.
 * Each tab brings its screen to the front instead of stacking a new copy.
 */
public final class BottomNav {

    private BottomNav() {}

    public static void setup(Activity activity, View home, View saved, View sell, View chats, View you) {
        home.setOnClickListener(v -> open(activity, ShoppingActivity.class));
        saved.setOnClickListener(v -> open(activity, SavedItemsActivity.class));
        sell.setOnClickListener(v -> comingSoon(activity, "Selling"));   // TODO: CreateListingActivity
        chats.setOnClickListener(v -> comingSoon(activity, "Chats"));    // TODO: MessagesActivity
        you.setOnClickListener(v -> open(activity, UserProfileActivity.class));
    }

    private static void open(Activity activity, Class<? extends Activity> screen) {
        if (activity.getClass() == screen) {
            return; // already on this tab
        }
        Intent intent = new Intent(activity, screen);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        activity.startActivity(intent);
    }

    private static void comingSoon(Activity activity, String what) {
        Toast.makeText(activity, what + " Coming Soon!", Toast.LENGTH_SHORT).show();
    }
}
