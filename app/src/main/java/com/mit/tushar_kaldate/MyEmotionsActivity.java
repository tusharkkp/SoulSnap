package com.mit.tushar_kaldate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mit.tushar_kaldate.adapter.EmotionAdapter;
import com.mit.tushar_kaldate.database.AppDatabase;
import com.mit.tushar_kaldate.model.Emotion;
import com.mit.tushar_kaldate.utils.SessionManager;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyEmotionsActivity extends AppCompatActivity implements EmotionAdapter.OnEmotionItemListener {

    private RecyclerView rvEmotions;
    private View layoutEmptyState;
    private ProgressBar pbLoading;
    private TextView tvEntryCount;
    private EmotionAdapter adapter;

    private AppDatabase database;
    private SessionManager sessionManager;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_emotions);

        sessionManager = new SessionManager(this);
        database = AppDatabase.getInstance(this);

        rvEmotions = findViewById(R.id.rvEmotions);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        pbLoading = findViewById(R.id.pbLoading);
        tvEntryCount = findViewById(R.id.tvEntryCount);

        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageButton btnAddEmotion = findViewById(R.id.btnAddEmotion);
        View btnEmptyLogEmotion = findViewById(R.id.btnEmptyLogEmotion);

        btnBack.setOnClickListener(v -> finish());

        btnAddEmotion.setOnClickListener(v -> {
            Intent intent = new Intent(MyEmotionsActivity.this, LogYourEmotionActivity.class);
            startActivity(intent);
        });

        btnEmptyLogEmotion.setOnClickListener(v -> {
            Intent intent = new Intent(MyEmotionsActivity.this, LogYourEmotionActivity.class);
            startActivity(intent);
        });

        rvEmotions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EmotionAdapter(this, this);
        rvEmotions.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEmotions();
    }

    private void loadEmotions() {
        int userId = sessionManager.getUserId();
        if (userId <= 0) {
            Toast.makeText(this, "Session invalid, please log in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        pbLoading.setVisibility(View.VISIBLE);

        executorService.execute(() -> {
            List<Emotion> emotions = database.emotionDao().getEmotionsForUser(userId);

            runOnUiThread(() -> {
                pbLoading.setVisibility(View.GONE);
                if (emotions == null || emotions.isEmpty()) {
                    layoutEmptyState.setVisibility(View.VISIBLE);
                    rvEmotions.setVisibility(View.GONE);
                    tvEntryCount.setText("No entries yet");
                } else {
                    layoutEmptyState.setVisibility(View.GONE);
                    rvEmotions.setVisibility(View.VISIBLE);
                    adapter.setEmotions(emotions);
                    int count = emotions.size();
                    tvEntryCount.setText(count == 1 ? "1 journal moment recorded" : count + " journal moments recorded");
                }
            });
        });
    }

    @Override
    public void onDeleteClick(Emotion emotion) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Emotion Entry")
                .setMessage("Are you sure you want to remove this emotion record from your journal?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    executorService.execute(() -> {
                        // Delete private image file if exists
                        if (emotion.getPhotoPath() != null) {
                            File file = new File(emotion.getPhotoPath());
                            if (file.exists()) {
                                file.delete();
                            }
                        }
                        // Delete from database
                        database.emotionDao().deleteEmotion(emotion);

                        runOnUiThread(() -> {
                            Toast.makeText(MyEmotionsActivity.this, "Entry removed", Toast.LENGTH_SHORT).show();
                            loadEmotions();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
