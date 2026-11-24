package com.knightmarket.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import android.content.SharedPreferences;

import org.json.JSONObject;

/** Keeps the logged-in user's token and basic profile on the phone. */
public final class Session {

    private static final String PREFS = "knightmarket_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";

    private Session() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Saves the { token, user } returned by login. */
    public static void save(Context context, JSONObject loginData) {
        JSONObject user = loginData.optJSONObject("user");
        prefs(context).edit()
                .putString(KEY_TOKEN, loginData.optString("token", null))
                .putString(KEY_NAME, user != null ? user.optString("name", "") : "")
                .putString(KEY_EMAIL, user != null ? user.optString("email", "") : "")
                .apply();
    }

    public static String token(Context context) {
        return prefs(context).getString(KEY_TOKEN, null);
    }

    public static boolean isLoggedIn(Context context) {
        return token(context) != null;
    }

    public static String name(Context context) {
        return prefs(context).getString(KEY_NAME, "");
    }

    public static String email(Context context) {
        return prefs(context).getString(KEY_EMAIL, "");
    }

    public static void updateName(Context context, String name) {
        prefs(context).edit().putString(KEY_NAME, name).apply();
    }

    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }

    /**
     * Call this when the server answers 401 to a logged-in request: the 7-day login ran out
     * (or the account is gone). Logs out on the phone too and goes back to the start screen.
     */
    public static void expired(Activity activity) {
        clear(activity);
        Toast.makeText(activity, "Your login expired. Please log in again.", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(activity, LandingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
    }
}
