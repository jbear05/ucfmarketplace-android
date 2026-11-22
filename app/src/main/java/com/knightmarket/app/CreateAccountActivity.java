package com.knightmarket.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.knightmarket.app.databinding.ActivityCreateAccountBinding;

import org.json.JSONObject;

import java.util.Locale;

public class CreateAccountActivity extends AppCompatActivity {

    private ActivityCreateAccountBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnContinue.setOnClickListener(v -> createAccount());
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
            Toast.makeText(this, "Please agree to the Community Rules", Toast.LENGTH_SHORT).show();
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
