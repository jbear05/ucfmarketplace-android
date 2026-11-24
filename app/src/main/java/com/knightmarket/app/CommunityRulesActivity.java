package com.knightmarket.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.knightmarket.app.databinding.ActivityCommunityRulesBinding;

/**
 * Screen 06: the Community Rules everyone agrees to at signup.
 * Opened from Create account with {@link #agreeIntent}, it shows an "I Agree" button and
 * returns RESULT_OK when tapped, so the sign-up checkbox can be ticked.
 */
public class CommunityRulesActivity extends AppCompatActivity {

    private static final String EXTRA_ASK_TO_AGREE = "askToAgree";

    /** Read-only, for opening the rules from anywhere else in the app. */
    public static void open(Context context) {
        context.startActivity(new Intent(context, CommunityRulesActivity.class));
    }

    /** For sign-up: the screen shows "I Agree" and finishes with RESULT_OK when it is tapped. */
    public static Intent agreeIntent(Context context) {
        return new Intent(context, CommunityRulesActivity.class).putExtra(EXTRA_ASK_TO_AGREE, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCommunityRulesBinding binding = ActivityCommunityRulesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBars.pad(binding.getRoot());

        binding.back.setOnClickListener(v -> finish());

        if (getIntent().getBooleanExtra(EXTRA_ASK_TO_AGREE, false)) {
            binding.agreeBar.setVisibility(View.VISIBLE);
            binding.agree.setOnClickListener(v -> {
                setResult(Activity.RESULT_OK);
                finish();
            });
        }
    }
}
