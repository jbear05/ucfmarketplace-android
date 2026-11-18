package com.knightmarket.app;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.knightmarket.app.databinding.ActivityShoppingBinding;

public class ShoppingActivity extends AppCompatActivity {

    private ActivityShoppingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShoppingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.searchContainer.setOnClickListener(v -> {
            Toast.makeText(this, "Search Screen Coming Soon!", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to SearchResultsActivity
        });
    }
}