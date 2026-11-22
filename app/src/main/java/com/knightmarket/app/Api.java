package com.knightmarket.app;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Talks to the KnightMarket backend, the same REST API the web app uses.
 * Every response is { success, message, data } (or { success: false, message, errors }).
 * Calls run on a background thread; callbacks come back on the main thread.
 */
public final class Api {

    public static final String BASE_URL = "https://knightmarket.onrender.com/api";

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    // The free server can take up to a minute to wake up after a quiet spell
    private static final int TIMEOUT_MS = 60_000;

    public interface Callback {
        void onSuccess(JSONObject data, String message);

        void onError(int status, String message);
    }

    private Api() {}

    // ── Auth ─────────────────────────────────────────────────────────────────

    public static void register(String name, String email, String password, Callback callback) {
        JSONObject body = new JSONObject();
        put(body, "name", name);
        put(body, "email", email);
        put(body, "password", password);
        request("POST", "/auth/register", body, null, callback);
    }

    public static void login(String email, String password, Callback callback) {
        JSONObject body = new JSONObject();
        put(body, "email", email);
        put(body, "password", password);
        request("POST", "/auth/login", body, null, callback);
    }

    public static void resendVerification(String email, Callback callback) {
        JSONObject body = new JSONObject();
        put(body, "email", email);
        request("POST", "/auth/resend-verification", body, null, callback);
    }

    public static void forgotPassword(String email, Callback callback) {
        JSONObject body = new JSONObject();
        put(body, "email", email);
        request("POST", "/auth/forgot-password", body, null, callback);
    }

    public static void me(String token, Callback callback) {
        request("GET", "/auth/me", null, token, callback);
    }

    // ── Listings ─────────────────────────────────────────────────────────────

    /** Newest first; category and search may be null. */
    public static void listings(String category, String search, String token, Callback callback) {
        StringBuilder path = new StringBuilder("/listings?limit=50");
        if (category != null) {
            path.append("&category=").append(encode(category));
        }
        if (search != null) {
            path.append("&search=").append(encode(search));
        }
        request("GET", path.toString(), null, token, callback);
    }

    // ── Plumbing ─────────────────────────────────────────────────────────────

    private static void request(String method, String path, JSONObject body, String token, Callback callback) {
        EXECUTOR.execute(() -> {
            int status = 0;
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + path).openConnection();
                connection.setRequestMethod(method);
                connection.setConnectTimeout(TIMEOUT_MS);
                connection.setReadTimeout(TIMEOUT_MS);
                connection.setRequestProperty("Accept", "application/json");
                if (token != null) {
                    connection.setRequestProperty("Authorization", "Bearer " + token);
                }
                if (body != null) {
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    try (OutputStream out = connection.getOutputStream()) {
                        out.write(body.toString().getBytes(StandardCharsets.UTF_8));
                    }
                }

                status = connection.getResponseCode();
                InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
                String text = stream == null ? "" : read(stream);
                connection.disconnect();

                JSONObject json = text.isEmpty() ? new JSONObject() : new JSONObject(text);
                String message = json.optString("message", "");
                if (status < 400 && json.optBoolean("success", false)) {
                    JSONObject data = json.optJSONObject("data");
                    deliverSuccess(callback, data != null ? data : new JSONObject(), message);
                } else {
                    deliverError(callback, status, errorText(json, message));
                }
            } catch (IOException e) {
                deliverError(callback, status, "Can't reach KnightMarket. Check your connection and try again.");
            } catch (JSONException e) {
                deliverError(callback, status, "Unexpected response from the server.");
            }
        });
    }

    /** "Validation failed" alone isn't helpful, so show the first specific reason. */
    private static String errorText(JSONObject json, String message) {
        JSONArray errors = json.optJSONArray("errors");
        if (errors != null && errors.length() > 0) {
            return errors.optString(0, message);
        }
        return message.isEmpty() ? "Something went wrong. Please try again." : message;
    }

    private static void deliverSuccess(Callback callback, JSONObject data, String message) {
        MAIN.post(() -> callback.onSuccess(data, message));
    }

    private static void deliverError(Callback callback, int status, String message) {
        MAIN.post(() -> callback.onError(status, message));
    }

    private static String read(InputStream stream) throws IOException {
        try (InputStream in = stream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            return value;
        }
    }

    private static void put(JSONObject json, String key, String value) {
        try {
            json.put(key, value);
        } catch (JSONException ignored) {
            // only fails for a null key
        }
    }
}
