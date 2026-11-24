package com.knightmarket.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.knightmarket.app.databinding.ActivityMapBinding;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Screen 10: every active or pending listing as a price pin on an OpenStreetMap map of campus
 * (GET /api/listings/map). Tap a pin for a preview card, tap the card to open the listing.
 * Uses osmdroid, so there is no API key to set up.
 */
public class MapActivity extends AppCompatActivity {

    private static final String EXTRA_CATEGORY = "category";
    private static final String STATE_LAT = "lat";
    private static final String STATE_LNG = "lng";
    private static final String STATE_ZOOM = "zoom";

    // Same list as the server and the category chips on Shopping
    private static final String[] CATEGORIES = {"Textbooks", "Electronics", "Furniture", "Clothing",
            "Dorm & Home", "Tickets & Events", "Vehicles", "Other"};

    private static final GeoPoint UCF_MAIN_CAMPUS = new GeoPoint(28.6024, -81.2001);
    private static final double CAMPUS_ZOOM = 16.0;

    private ActivityMapBinding binding;
    private final List<Marker> markers = new ArrayList<>();
    private String category;
    private boolean restoredCamera;
    // Bumped on every load and on destroy, so a slow response for an old category
    // (or for a screen that was closed or rotated) is ignored instead of drawn
    private int loadId;

    /** category may be null for all listings. */
    public static void open(Context context, String category) {
        Intent intent = new Intent(context, MapActivity.class);
        intent.putExtra(EXTRA_CATEGORY, category);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // osmdroid needs these before the map is inflated. The OpenStreetMap tile servers
        // refuse apps without their own user agent, and the cache goes in app storage
        // so no storage permission is needed.
        Configuration.getInstance().setUserAgentValue(getPackageName());
        File osmdroidDir = new File(getCacheDir(), "osmdroid");
        Configuration.getInstance().setOsmdroidBasePath(osmdroidDir);
        Configuration.getInstance().setOsmdroidTileCache(new File(osmdroidDir, "tiles"));

        binding = ActivityMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBars.pad(binding.getRoot());

        category = savedInstanceState != null
                ? savedInstanceState.getString(EXTRA_CATEGORY)
                : getIntent().getStringExtra(EXTRA_CATEGORY);

        binding.map.setTileSource(TileSourceFactory.MAPNIK);
        binding.map.setMultiTouchControls(true);
        binding.map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        binding.map.setMinZoomLevel(4.0);
        binding.map.setMaxZoomLevel(19.0);
        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_ZOOM)) {
            // Keep the same view after a rotation instead of jumping back to fit all pins
            binding.map.getController().setZoom(savedInstanceState.getDouble(STATE_ZOOM));
            binding.map.getController().setCenter(new GeoPoint(
                    savedInstanceState.getDouble(STATE_LAT), savedInstanceState.getDouble(STATE_LNG)));
            restoredCamera = true;
        } else {
            binding.map.getController().setZoom(CAMPUS_ZOOM);
            binding.map.getController().setCenter(UCF_MAIN_CAMPUS);
        }

        binding.back.setOnClickListener(v -> finish());
        binding.categoryPill.setOnClickListener(v -> chooseCategory());
        binding.previewClose.setOnClickListener(v -> binding.preview.setVisibility(View.GONE));
        BottomNav.setup(this, binding.navHome, binding.navSaved, binding.navSell, binding.navChats, binding.navYou);
        binding.navHome.setOnClickListener(v -> finish()); // the map opens from Home

        loadPins();
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.map.onResume();
    }

    @Override
    protected void onPause() {
        binding.map.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        loadId++; // drop any response still on its way
        binding.map.onDetach();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_CATEGORY, category);
        IGeoPoint center = binding.map.getMapCenter();
        outState.putDouble(STATE_LAT, center.getLatitude());
        outState.putDouble(STATE_LNG, center.getLongitude());
        outState.putDouble(STATE_ZOOM, binding.map.getZoomLevelDouble());
    }

    private void loadPins() {
        binding.categoryName.setText(category != null ? category : "All listings");
        binding.preview.setVisibility(View.GONE);
        showStatus("Loading listings…");
        binding.mapStatus.setOnClickListener(null);

        final int thisLoad = ++loadId;
        Api.mapPins(category, Session.token(this), new Api.Callback() {
            @Override
            public void onSuccess(JSONObject data, String message) {
                if (isStale(thisLoad)) {
                    return;
                }
                JSONArray pins = data.optJSONArray("pins");
                int shown = showPins(pins != null ? pins : new JSONArray());
                binding.pinCount.setText(String.valueOf(shown));
                if (shown == 0) {
                    showStatus(category != null ? "No " + category + " listings on the map yet"
                            : "No listings on the map yet");
                } else {
                    binding.mapStatus.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(int status, String message) {
                if (isStale(thisLoad)) {
                    return;
                }
                binding.pinCount.setText("0");
                showStatus(message + "\nTap to try again.");
                binding.mapStatus.setOnClickListener(v -> loadPins());
            }
        });
    }

    /** True when the screen is gone or a newer load has started since this one. */
    private boolean isStale(int load) {
        return load != loadId || isFinishing() || isDestroyed();
    }

    /** Puts one pin per listing on the map and returns how many had a location. */
    private int showPins(JSONArray pins) {
        binding.map.getOverlays().removeAll(markers);
        markers.clear();

        // Many listings share a meetup spot (e.g. Student Union). Fan those out in a small
        // circle so every pin can still be tapped.
        Map<String, Integer> seen = new HashMap<>();
        List<GeoPoint> points = new ArrayList<>();
        for (int i = 0; i < pins.length(); i++) {
            JSONObject pin = pins.optJSONObject(i);
            GeoPoint point = pin != null ? location(pin) : null;
            if (point == null) {
                continue;
            }
            String key = String.format(Locale.US, "%.5f,%.5f", point.getLatitude(), point.getLongitude());
            int repeat = seen.containsKey(key) ? seen.get(key) : 0;
            seen.put(key, repeat + 1);
            if (repeat > 0) {
                double angle = repeat * 2.4; // radians, spreads the pins evenly around the spot
                double distance = 0.00012 * Math.sqrt(repeat); // about 13 m per step
                point = new GeoPoint(point.getLatitude() + distance * Math.sin(angle),
                        point.getLongitude() + distance * Math.cos(angle));
            }
            points.add(point);

            Marker marker = new Marker(binding.map);
            marker.setPosition(point);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setIcon(pricePin(ListingAdapter.price(pin.optDouble("price", 0))));
            marker.setTitle(pin.optString("title"));
            marker.setInfoWindow(null); // the preview card replaces osmdroid's bubble
            marker.setOnMarkerClickListener((m, mapView) -> {
                showPreview(pin);
                mapView.getController().animateTo(m.getPosition());
                return true;
            });
            markers.add(marker);
        }
        binding.map.getOverlays().addAll(markers);
        binding.map.invalidate();

        if (!restoredCamera) {
            fitToPins(points);
        }
        restoredCamera = false; // only skip the fit once, right after a rotation
        return markers.size();
    }

    /** The server stores GeoJSON { type: "Point", coordinates: [lng, lat] }. */
    private static GeoPoint location(JSONObject pin) {
        JSONArray lngLat;
        JSONObject geo = pin.optJSONObject("coordinates");
        lngLat = geo != null ? geo.optJSONArray("coordinates") : pin.optJSONArray("coordinates");
        if (lngLat == null || lngLat.length() < 2) {
            return null;
        }
        double lng = lngLat.optDouble(0, Double.NaN);
        double lat = lngLat.optDouble(1, Double.NaN);
        if (Double.isNaN(lat) || Double.isNaN(lng) || (lat == 0 && lng == 0)) {
            return null;
        }
        return new GeoPoint(lat, lng);
    }

    private void fitToPins(List<GeoPoint> points) {
        if (points.isEmpty()) {
            binding.map.getController().setZoom(CAMPUS_ZOOM);
            binding.map.getController().setCenter(UCF_MAIN_CAMPUS);
        } else if (points.size() == 1) {
            binding.map.getController().setZoom(17.0);
            binding.map.getController().setCenter(points.get(0));
        } else {
            BoundingBox box = BoundingBox.fromGeoPointsSafe(points);
            int padding = dp(64);
            // zoomToBoundingBox needs the map's size, so wait for its first layout
            final int thisLoad = loadId;
            binding.map.post(() -> {
                if (isStale(thisLoad)) {
                    return;
                }
                binding.map.zoomToBoundingBox(box, false, padding);
                if (binding.map.getZoomLevelDouble() > 18.0) {
                    binding.map.getController().setZoom(18.0); // pins all on one block
                }
            });
        }
    }

    private void showPreview(JSONObject pin) {
        binding.previewPrice.setText(ListingAdapter.price(pin.optDouble("price", 0)));
        binding.previewTitle.setText(pin.optString("title"));
        String text = pin.optString("category");
        if ("pending".equals(pin.optString("status"))) {
            text = text.isEmpty() ? "Pending" : text + " · Pending";
        }
        binding.previewCategory.setText(text);
        JSONArray images = pin.optJSONArray("images");
        ImageLoader.load(images != null && images.length() > 0 ? images.optString(0) : null, binding.previewImage);
        binding.preview.setOnClickListener(v -> ListingDetailActivity.open(this, pin.optString("_id")));
        binding.preview.setVisibility(View.VISIBLE);
    }

    private void chooseCategory() {
        String[] options = new String[CATEGORIES.length + 1];
        options[0] = "All listings";
        System.arraycopy(CATEGORIES, 0, options, 1, CATEGORIES.length);
        int checked = 0;
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (CATEGORIES[i].equals(category)) {
                checked = i + 1;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle("Show on the map")
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    dialog.dismiss();
                    String chosen = which == 0 ? null : CATEGORIES[which - 1];
                    if (chosen == null ? category != null : !chosen.equals(category)) {
                        category = chosen;
                        loadPins();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showStatus(String text) {
        binding.mapStatus.setText(text);
        binding.mapStatus.setVisibility(View.VISIBLE);
    }

    /** A gold pill with the price, drawn once into a bitmap for the marker icon. */
    private BitmapDrawable pricePin(String price) {
        TextView label = new TextView(this);
        label.setText(price);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextColor(getColor(R.color.km_black));
        label.setPadding(dp(10), dp(4), dp(10), dp(4));
        GradientDrawable background = new GradientDrawable();
        background.setColor(getColor(R.color.km_gold));
        background.setCornerRadius(dp(14));
        background.setStroke(dp(2), Color.WHITE);
        label.setBackground(background);

        int unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        label.measure(unspecified, unspecified);
        label.layout(0, 0, label.getMeasuredWidth(), label.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(label.getMeasuredWidth(), label.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        label.draw(new Canvas(bitmap));
        return new BitmapDrawable(getResources(), bitmap);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
