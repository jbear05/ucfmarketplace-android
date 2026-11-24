package com.knightmarket.app;

import android.app.Activity;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

/** A listing's status (active, pending, offMarket): its badge and the Change Status dialog. */
public final class ListingStatus {

    public interface OnChanged {
        void onChanged(String newStatus);
    }

    private static final String[] VALUES = {"active", "pending", "offMarket"};
    private static final String[] LABELS = {"Available", "Pending (someone's buying it)", "Off market (sold or removed)"};

    private ListingStatus() {}

    /** Styles a "● Available"-style badge for this status. */
    public static void showBadge(TextView badge, String status) {
        int color;
        String text;
        if ("pending".equals(status)) {
            text = "● Pending";
            color = R.color.km_gold;
        } else if ("offMarket".equals(status)) {
            text = "● Off market";
            color = R.color.km_muted;
        } else {
            text = "● Available";
            color = R.color.km_green;
        }
        badge.setText(text);
        badge.setTextColor(ContextCompat.getColor(badge.getContext(), color));
    }

    /** Lets the owner pick a new status, then saves it on the server. */
    public static void choose(Activity activity, String listingId, String current, OnChanged onChanged) {
        int checked = 0;
        for (int i = 0; i < VALUES.length; i++) {
            if (VALUES[i].equals(current)) {
                checked = i;
            }
        }
        new AlertDialog.Builder(activity)
                .setTitle("Change Status")
                .setSingleChoiceItems(LABELS, checked, (dialog, which) -> {
                    dialog.dismiss();
                    String status = VALUES[which];
                    if (status.equals(current)) {
                        return;
                    }
                    String token = Session.token(activity);
                    Api.setListingStatus(listingId, status, token, new Api.Callback() {
                        @Override
                        public void onSuccess(JSONObject data, String message) {
                            onChanged.onChanged(data.optString("status", status));
                        }

                        @Override
                        public void onError(int code, String message) {
                            if (code == 401) {
                                Session.expired(activity, token);
                                return;
                            }
                            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
