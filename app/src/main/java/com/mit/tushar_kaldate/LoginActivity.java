package com.mit.tushar_kaldate;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mit.tushar_kaldate.database.AppDatabase;
import com.mit.tushar_kaldate.model.User;
import com.mit.tushar_kaldate.utils.SessionManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private EditText etIdentifier;
    private EditText etPassword;
    private TextView tvErrorMessage;
    private ProgressBar pbLogin;
    private View btnLogin;

    private AppDatabase database;
    private SessionManager sessionManager;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        database = AppDatabase.getInstance(this);
        sessionManager = new SessionManager(this);

        etIdentifier = findViewById(R.id.etIdentifier);
        etPassword = findViewById(R.id.etPassword);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        pbLogin = findViewById(R.id.pbLogin);
        btnLogin = findViewById(R.id.btnLogin);
        View tvGoToRegister = findViewById(R.id.tvGoToRegister);

        btnLogin.setOnClickListener(v -> performLogin());

        tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void performLogin() {
        String identifier = etIdentifier.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        tvErrorMessage.setVisibility(View.GONE);

        if (TextUtils.isEmpty(identifier)) {
            showError("Please enter your username or email address.");
            etIdentifier.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            showError("Please enter your password.");
            etPassword.requestFocus();
            return;
        }

        setLoading(true);

        executorService.execute(() -> {
            User user = database.userDao().authenticate(identifier, password);

            runOnUiThread(() -> {
                setLoading(false);
                if (user != null) {
                    sessionManager.login(user.getId(), user.getUsername());
                    Toast.makeText(LoginActivity.this, "Welcome back, " + user.getUsername() + "! ❤️", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    showError("Invalid credentials. Please verify your username/email and password.");
                }
            });
        });
    }

    private void showError(String message) {
        tvErrorMessage.setText(message);
        tvErrorMessage.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean isLoading) {
        pbLogin.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
