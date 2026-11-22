package com.knightmarket.app;

import android.content.Context;
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

    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }
}
