package com.knightmarket.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.knightmarket.app.databinding.ActivityVerifyEmailBinding;

import org.json.JSONObject;

/**
 * The KnightMarket server verifies email with a link (not a 6-digit code), so this screen
 * asks the student to open the link, then logs them in when they come back.
 */
public class VerifyEmailActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "email";
    public static final String EXTRA_PASSWORD = "password";

    private static final long RESEND_WAIT_MS = 60_000;

    private ActivityVerifyEmailBinding binding;
    private String email;
    private String password;
    private CountDownTimer resendTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVerifyEmailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        email = getIntent().getStringExtra(EXTRA_EMAIL);
        password = getIntent().getStringExtra(EXTRA_PASSWORD);
        if (email == null || password == null) {
            finish(); // only reachable from sign-up or login
            return;
        }

        binding.btnBack.setOnClickListener(v -> finish());

        binding.otpContainer.setVisibility(View.GONE);
        binding.subtitleVerify.setText("We sent a verification link to\n" + email
                + "\nOpen it, then come back and tap Continue.");
        binding.btnVerify.setText("I've verified, continue");
        binding.btnVerify.setOnClickListener(v -> continueIfVerified());

        binding.resendText.setOnClickListener(v -> resend());
        startResendCountdown();
    }

    private void continueIfVerified() {
        setBusy(true);
        Api.login(email, password, new Api.Callback() {
            @Override
            public void onSuccess(JSONObject data, String message) {
                Session.save(VerifyEmailActivity.this, data);
                startActivity(new Intent(VerifyEmailActivity.this, ProfilePhotoActivity.class));
            }

            @Override
            public void onError(int status, String message) {
                setBusy(false);
                Toast.makeText(VerifyEmailActivity.this, status == 403
                        ? "Not verified yet. Open the link in your email first." : message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void resend() {
        binding.resendText.setEnabled(false);
        Api.resendVerification(email, new Api.Callback() {
            @Override
            public void onSuccess(JSONObject data, String message) {
                Toast.makeText(VerifyEmailActivity.this, "New link sent to " + email, Toast.LENGTH_SHORT).show();
                startResendCountdown();
            }

            @Override
            public void onError(int status, String message) {
                binding.resendText.setEnabled(true);
                Toast.makeText(VerifyEmailActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startResendCountdown() {
        binding.resendText.setEnabled(false);
        if (resendTimer != null) {
            resendTimer.cancel();
        }
        resendTimer = new CountDownTimer(RESEND_WAIT_MS, 1000) {
            @Override
            public void onTick(long millisLeft) {
                long seconds = (millisLeft + 999) / 1000;
                binding.resendText.setText(String.format("Didn't get it? Resend in %d:%02d", seconds / 60, seconds % 60));
            }

            @Override
            public void onFinish() {
                binding.resendText.setText("Didn't get it? Tap to resend");
                binding.resendText.setEnabled(true);
            }
        }.start();
    }

    private void setBusy(boolean busy) {
        binding.btnVerify.setEnabled(!busy);
        binding.btnVerify.setText(busy ? "Checking…" : "I've verified, continue");
    }

    @Override
    protected void onDestroy() {
        if (resendTimer != null) {
            resendTimer.cancel();
        }
        super.onDestroy();
    }
}
