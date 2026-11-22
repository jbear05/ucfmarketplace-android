package com.knightmarket.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Loads listing photos from Cloudinary into ImageViews, with a small memory cache. */
public final class ImageLoader {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(16 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };

    private ImageLoader() {}

    public static void load(String url, ImageView view) {
        view.setTag(url);
        if (url == null || url.isEmpty()) {
            view.setImageDrawable(null);
            return;
        }
        String sized = thumbnail(url);
        Bitmap cached = CACHE.get(sized);
        if (cached != null) {
            view.setImageBitmap(cached);
            return;
        }
        view.setImageDrawable(null);
        EXECUTOR.execute(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(sized).openConnection();
                connection.setConnectTimeout(20_000);
                connection.setReadTimeout(20_000);
                Bitmap bitmap;
                try (InputStream in = connection.getInputStream()) {
                    bitmap = BitmapFactory.decodeStream(in);
                }
                connection.disconnect();
                if (bitmap == null) {
                    return;
                }
                CACHE.put(sized, bitmap);
                MAIN.post(() -> {
                    // The view may have been recycled for another listing meanwhile
                    if (url.equals(view.getTag())) {
                        view.setImageBitmap(bitmap);
                    }
                });
            } catch (Exception ignored) {
                // Leave the placeholder showing
            }
        });
    }

    /** Ask Cloudinary for a 400 px wide copy instead of the full photo. */
    private static String thumbnail(String url) {
        return url.contains("/upload/") && !url.contains("/upload/w_")
                ? url.replace("/upload/", "/upload/w_400,h_400,c_fill,q_auto/")
                : url;
    }
}
