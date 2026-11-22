package com.knightmarket.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.knightmarket.app.databinding.ActivityLoginBinding;

import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnLogin.setOnClickListener(v -> logIn());

        binding.textForgot.setOnClickListener(v -> forgotPassword());

        binding.btnCreateAccount.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateAccountActivity.class);
            startActivity(intent);
        });
    }

    private void logIn() {
        String email = binding.inputEmail.getText().toString().trim();
        String password = binding.inputPassword.getText().toString();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.inputEmail.setError("Enter your email");
            return;
        }
        if (password.isEmpty()) {
            binding.inputPassword.setError("Enter your password");
            return;
        }

        setBusy(true);
        Api.login(email, password, new Api.Callback() {
            @Override
            public void onSuccess(JSONObject data, String message) {
                Session.save(LoginActivity.this, data);
                Intent intent = new Intent(LoginActivity.this, ShoppingActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }

            @Override
            public void onError(int status, String message) {
                setBusy(false);
                if (status == 403 && message.toLowerCase().contains("verify")) {
                    // Account exists but the email link hasn't been opened yet
                    Intent intent = new Intent(LoginActivity.this, VerifyEmailActivity.class);
                    intent.putExtra(VerifyEmailActivity.EXTRA_EMAIL, email);
                    intent.putExtra(VerifyEmailActivity.EXTRA_PASSWORD, password);
                    startActivity(intent);
                }
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void forgotPassword() {
        String email = binding.inputEmail.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.inputEmail.setError("Enter your email first");
            return;
        }
        Api.forgotPassword(email, new Api.Callback() {
            @Override
            public void onSuccess(JSONObject data, String message) {
                new AlertDialog.Builder(LoginActivity.this)
                        .setTitle("Check your email")
                        .setMessage("If an account exists for " + email
                                + ", we sent a link to reset your password.")
                        .setPositiveButton("OK", null)
                        .show();
            }

            @Override
            public void onError(int status, String message) {
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setBusy(boolean busy) {
        binding.btnLogin.setEnabled(!busy);
        binding.btnLogin.setText(busy ? "Logging in…" : "Log in");
    }
}
