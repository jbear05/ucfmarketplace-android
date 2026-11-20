package com.knightmarket.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.knightmarket.app.databinding.ActivityProfilePhotoBinding;

public class ProfilePhotoActivity extends AppCompatActivity {

    private ActivityProfilePhotoBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfilePhotoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.choose.setOnClickListener(v -> {
            Toast.makeText(this, "Photo picker coming soon!", Toast.LENGTH_SHORT).show();
            // TODO: Open the photo picker, then continue to Shopping
            openShopping();
        });

        binding.skip.setOnClickListener(v -> openShopping());
    }

    private void openShopping() {
        Intent intent = new Intent(this, ShoppingActivity.class);
        startActivity(intent);
        // Clear the signup screens so Back from Shopping doesn't return to them
        finishAffinity();
    }
}
