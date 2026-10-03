package com.mit.tushar_kaldate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.mit.tushar_kaldate.database.AppDatabase;
import com.mit.tushar_kaldate.model.Emotion;
import com.mit.tushar_kaldate.utils.SessionManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeActivity extends AppCompatActivity {

    private TextView tvGreeting;
    private TextView tvSavedCount;
    private SessionManager sessionManager;
    private AppDatabase database;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        sessionManager = new SessionManager(this);
        database = AppDatabase.getInstance(this);

        if (!sessionManager.isLoggedIn()) {
            performLogout();
            return;
        }

        tvGreeting = findViewById(R.id.tvGreeting);
        tvSavedCount = findViewById(R.id.tvSavedCount);
        ImageButton btnLogout = findViewById(R.id.btnLogout);
        MaterialCardView cardLogEmotion = findViewById(R.id.cardLogEmotion);
        MaterialCardView cardMyEmotions = findViewById(R.id.cardMyEmotions);

        String username = sessionManager.getUsername();
        if (username != null && !username.isEmpty()) {
            tvGreeting.setText("Hello, " + username + "! 👋");
        } else {
            tvGreeting.setText("Hello there! 👋");
        }

        cardLogEmotion.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, LogYourEmotionActivity.class);
            startActivity(intent);
        });

        cardMyEmotions.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, MyEmotionsActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> showLogoutConfirmation());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSavedCount();
    }

    private void loadSavedCount() {
        int userId = sessionManager.getUserId();
        if (userId <= 0) return;

        executorService.execute(() -> {
            List<Emotion> emotions = database.emotionDao().getEmotionsForUser(userId);
            runOnUiThread(() -> {
                int count = emotions != null ? emotions.size() : 0;
                if (count == 0) {
                    tvSavedCount.setText("No entries yet • Start journaling");
                } else if (count == 1) {
                    tvSavedCount.setText("1 saved journal moment");
                } else {
                    tvSavedCount.setText(count + " saved journal moments");
                }
            });
        });
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out of SoulSnap?")
                .setPositiveButton("Log Out", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        sessionManager.logout();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
