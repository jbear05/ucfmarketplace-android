package com.knightmarket.app;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.knightmarket.app.databinding.ActivityLandingBinding;

public class LandingActivity extends AppCompatActivity {

    private ActivityLandingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLandingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnLogin.setOnClickListener(v -> {
            Toast.makeText(this, "Log In Clicked - Coming Soon", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to LoginActivity
        });

        binding.btnCreateAccount.setOnClickListener(v -> {
            Toast.makeText(this, "Create Account Clicked - Coming Soon", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to CreateAccountActivity
        });
    }
}