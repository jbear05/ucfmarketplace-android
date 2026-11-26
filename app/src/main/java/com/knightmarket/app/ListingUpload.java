package com.knightmarket.app;

import android.app.Application;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Runs the New listing upload outside the Activity, so rotating the phone mid-upload doesn't
 * start a second upload or deliver the result to a screen that no longer exists. The new
 * Activity re-attaches and gets the result when it arrives (or right away if it already did).
 * Everything here is called on the main thread except the photo work on the executor.
 */
public class ListingUpload extends AndroidViewModel {

    public interface Listener {
        /** listing is the new listing on success; otherwise status and message describe the error. */
        void onUploadDone(JSONObject listing, int status, String message);
    }

    // Photos are resized before upload: the server keeps at most 1200 px and takes 5 MB per file
    private static final int UPLOAD_MAX_SIDE = 1600;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private boolean running;
    private Listener listener;
    private String token; // the login the last upload was sent with

    // A result that arrived while no screen was attached (mid-rotation)
    private boolean hasResult;
    private JSONObject resultListing;
    private int resultStatus;
    private String resultMessage;

    public ListingUpload(@NonNull Application application) {
        super(application);
    }

    public boolean isRunning() {
        return running;
    }

    /** The token the last upload used, for Session.expired() on a 401. */
    public String token() {
        return token;
    }

    public void attach(Listener listener) {
        this.listener = listener;
        if (hasResult) {
            hasResult = false;
            listener.onUploadDone(resultListing, resultStatus, resultMessage);
        }
    }

    public void detach(Listener listener) {
        if (this.listener == listener) {
            this.listener = null;
        }
    }

    /** Resizes the photos, then POSTs or PUTs the listing. Ignored if an upload is already running. */
    public void start(String listingId, Map<String, String> fields, List<Uri> photos, String token) {
        if (running) {
            return;
        }
        running = true;
        this.token = token;
        ContentResolver resolver = getApplication().getContentResolver();
        List<Uri> toUpload = new ArrayList<>(photos);
        worker.execute(() -> {
            List<byte[]> files = new ArrayList<>();
            try {
                for (Uri uri : toUpload) {
                    files.add(jpeg(resolver, uri));
                }
            } catch (IOException | OutOfMemoryError e) {
                main.post(() -> done(null, 0, "Couldn't read one of the photos. Remove it and try again."));
                return;
            }
            main.post(() -> {
                Api.Callback callback = new Api.Callback() {
                    @Override
                    public void onSuccess(JSONObject data, String message) {
                        done(data.optJSONObject("listing"), 200, message);
                    }

                    @Override
                    public void onError(int status, String message) {
                        done(null, status, message);
                    }
                };
                if (listingId != null) {
                    Api.editListing(listingId, fields, files, token, callback);
                } else {
                    Api.createListing(fields, files, token, callback);
                }
            });
        });
    }

    private void done(JSONObject listing, int status, String message) {
        running = false;
        if (listener != null) {
            listener.onUploadDone(listing, status, message);
        } else {
            hasResult = true;
            resultListing = listing;
            resultStatus = status;
            resultMessage = message;
        }
    }

    @Override
    protected void onCleared() {
        listener = null;
        worker.shutdownNow();
    }

    /** Decodes a picked photo upright (EXIF applied), scaled so its longest side is at most maxSide. */
    static Bitmap decode(ContentResolver resolver, Uri uri, int maxSide) throws IOException {
        ImageDecoder.Source source = ImageDecoder.createSource(resolver, uri);
        return ImageDecoder.decodeBitmap(source, (decoder, info, src) -> {
            int width = info.getSize().getWidth();
            int height = info.getSize().getHeight();
            int longest = Math.max(width, height);
            if (longest > maxSide) {
                float scale = (float) maxSide / longest;
                decoder.setTargetSize(Math.max(1, Math.round(width * scale)), Math.max(1, Math.round(height * scale)));
            }
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE); // so it can be compressed
        });
    }

    private static byte[] jpeg(ContentResolver resolver, Uri uri) throws IOException {
        Bitmap bitmap = decode(resolver, uri, UPLOAD_MAX_SIDE);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
        bitmap.recycle();
        return out.toByteArray();
    }
}
