package com.knightmarket.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.knightmarket.app.databinding.ActivityCreateAccountBinding;

import org.json.JSONObject;

import java.util.Locale;

public class CreateAccountActivity extends AppCompatActivity {

    private static final String RULES_LINK_TEXT = "Community Rules";

    private ActivityCreateAccountBinding binding;

    // "I Agree" on the Community Rules screen ticks the checkbox
    private final ActivityResultLauncher<Intent> rulesLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    binding.checkboxRules.setChecked(true);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnContinue.setOnClickListener(v -> createAccount());

        linkCommunityRules();
    }

    /**
     * Turns "Community Rules" in the checkbox label into a link to the rules screen.
     * Tapping the rest of the label still ticks the box as usual.
     */
    private void linkCommunityRules() {
        String label = binding.checkboxRules.getText().toString();
        int start = label.indexOf(RULES_LINK_TEXT);
        if (start < 0) return;

        SpannableString text = new SpannableString(label);
        text.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                // Stop the tap from also toggling the checkbox
                widget.cancelPendingInputEvents();
                rulesLauncher.launch(CommunityRulesActivity.agreeIntent(CreateAccountActivity.this));
            }

            @Override
            public void updateDrawState(@NonNull TextPaint paint) {
                super.updateDrawState(paint); // keeps the underline
                // Dark text, since gold is hard to read on the cream background
                paint.setColor(ContextCompat.getColor(CreateAccountActivity.this, R.color.text_main));
                paint.setFakeBoldText(true);
            }
        }, start, start + RULES_LINK_TEXT.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        binding.checkboxRules.setText(text);
        binding.checkboxRules.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void createAccount() {
        String name = binding.inputFullName.getText().toString().trim();
        String email = binding.inputEmail.getText().toString().trim().toLowerCase(Locale.US);
        String password = binding.inputPassword.getText().toString();

        if (name.isEmpty()) {
            showFieldError(binding.inputFullName, "Enter your full name");
            return;
        }
        // Same rule as the hint under the field
        if (!email.matches("^[^@\\s]+@(knights\\.)?ucf\\.edu$")) {
            showFieldError(binding.inputEmail, "Use your ucf.edu or knights.ucf.edu email");
            return;
        }
        // Same rule as the server: 8+ characters with a number
        if (password.length() < 8 || !password.matches(".*\\d.*")) {
            showFieldError(binding.inputPassword, "At least 8 characters, including a number");
            return;
        }
        if (!binding.checkboxRules.isChecked()) {
            Toast.makeText(this, "Please read and agree to the Community Rules", Toast.LENGTH_SHORT).show();
            rulesLauncher.launch(CommunityRulesActivity.agreeIntent(this));
            return;
        }

        setBusy(true);
        Api.register(name, email, password, new Api.Callback() {
            @Override
            public void onSuccess(JSONObject data, String message) {
                setBusy(false);
                Intent intent = new Intent(CreateAccountActivity.this, VerifyEmailActivity.class);
                intent.putExtra(VerifyEmailActivity.EXTRA_EMAIL, email);
                intent.putExtra(VerifyEmailActivity.EXTRA_PASSWORD, password);
                startActivity(intent);
            }

            @Override
            public void onError(int status, String message) {
                setBusy(false);
                Toast.makeText(CreateAccountActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setBusy(boolean busy) {
        binding.btnContinue.setEnabled(!busy);
        binding.btnContinue.setText(busy ? "Creating account…" : "Continue");
    }

    /** Marks the field and says why, since the error bubble only shows while the field has focus. */
    private void showFieldError(EditText field, String message) {
        field.setError(message);
        field.requestFocus();
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
