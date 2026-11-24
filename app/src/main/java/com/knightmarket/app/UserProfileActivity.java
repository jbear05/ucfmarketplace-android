package com.knightmarket.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.knightmarket.app.databinding.ActivityUserProfileBinding;

import org.json.JSONObject;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Your profile from GET /api/auth/me, like the Flutter Profile tab. */
public class UserProfileActivity extends AppCompatActivity {

    private ActivityUserProfileBinding binding;
    private JSONObject user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBars.pad(binding.getRoot());

        binding.profileListings.setOnClickListener(v -> startActivity(new Intent(this, MyListingsActivity.class)));
        // The Settings row edits your name (the only profile setting the server stores so far)
        binding.profileSettings.setOnClickListener(v -> editName());
        binding.profileName.setOnClickListener(v -> editName());
        binding.contactAdmin.setOnClickListener(v -> contactAdmin());
        binding.logout.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Log Out?")
                .setMessage("You can always log back in.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Log Out", (dialog, which) -> logOut())
                .show());

        BottomNav.setup(this, binding.navHome, binding.navSaved, binding.navSell, binding.navChats, binding.navYou);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Session.isLoggedIn(this)) {
            startActivity(new Intent(this, LandingActivity.class));
            finish();
            return;
        }
        // Show what's saved on the phone right away, then the server's latest
        binding.profileName.setText(Session.name(this));
        binding.profileEmail.setText(Session.email(this));
        String token = Session.token(this);
        Api.me(token, new Api.Callback() {
            @Override
            public void onSuccess(JSONObject data, String message) {
                user = data;
                show();
            }

            @Override
            public void onError(int status, String message) {
                if (status == 401) {
                    Session.expired(UserProfileActivity.this, token);
                } else {
                    Toast.makeText(UserProfileActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void show() {
        binding.profileName.setText(user.optString("name"));
        binding.profileEmail.setText(user.optString("email"));

        boolean verified = user.optBoolean("isVerifiedStudent");
        binding.profileVerified.setVisibility(verified ? View.VISIBLE : View.GONE);
        binding.profileBadgeDot.setVisibility(verified ? View.VISIBLE : View.GONE);

        int ratings = user.optInt("ratingCount", 0);
        binding.profileRating.setText(ratings == 0 ? "No ratings yet"
                : String.format(Locale.US, "★ %.1f · %d rating%s", user.optDouble("ratingAvg"), ratings, ratings == 1 ? "" : "s"));

        try {
            String since = DateTimeFormatter.ofPattern("MMM yyyy", Locale.US)
                    .format(Instant.parse(user.optString("createdAt")).atZone(ZoneId.systemDefault()));
            binding.profileSince.setText("Member since " + since);
        } catch (Exception e) {
            binding.profileSince.setVisibility(View.GONE);
        }
    }

    private void editName() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setText(binding.profileName.getText());
        input.setSelection(input.getText().length());
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad / 2, pad, pad / 2);

        new AlertDialog.Builder(this)
                .setTitle("Your name")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Name can't be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String token = Session.token(this);
                    Api.updateName(name, token, new Api.Callback() {
                        @Override
                        public void onSuccess(JSONObject data, String message) {
                            JSONObject updated = data.optJSONObject("user");
                            String saved = updated != null ? updated.optString("name", name) : name;
                            binding.profileName.setText(saved);
                            Session.updateName(UserProfileActivity.this, saved);
                            Toast.makeText(UserProfileActivity.this, "Name saved", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(int status, String message) {
                            if (status == 401) {
                                Session.expired(UserProfileActivity.this, token);
                                return;
                            }
                            Toast.makeText(UserProfileActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .show();
    }

    /** Sends a message to the KnightMarket admin (it starts a chat thread with them). */
    private void contactAdmin() {
        EditText input = new EditText(this);
        input.setHint("How can we help?");
        input.setMinLines(3);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad / 2, pad, pad / 2);

        new AlertDialog.Builder(this)
                .setTitle("Contact Admin")
                .setMessage("Send us a message and we'll reply in your messages.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Send Message", (dialog, which) -> {
                    String message = input.getText().toString().trim();
                    if (message.isEmpty()) {
                        return;
                    }
                    String token = Session.token(this);
                    Api.contactAdmin(message, token, new Api.Callback() {
                        @Override
                        public void onSuccess(JSONObject data, String msg) {
                            Toast.makeText(UserProfileActivity.this, "Message sent to admin", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(int status, String msg) {
                            if (status == 401) {
                                Session.expired(UserProfileActivity.this, token);
                                return;
                            }
                            Toast.makeText(UserProfileActivity.this, msg, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .show();
    }

    private void logOut() {
        Session.clear(this);
        Intent intent = new Intent(this, LandingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
