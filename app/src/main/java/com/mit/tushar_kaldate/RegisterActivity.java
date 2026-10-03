package com.mit.tushar_kaldate;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
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

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private TextView tvErrorMessage;
    private ProgressBar pbRegister;
    private View btnRegister;

    private AppDatabase database;
    private SessionManager sessionManager;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        database = AppDatabase.getInstance(this);
        sessionManager = new SessionManager(this);

        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        pbRegister = findViewById(R.id.pbRegister);
        btnRegister = findViewById(R.id.btnRegister);
        View tvGoToLogin = findViewById(R.id.tvGoToLogin);

        btnRegister.setOnClickListener(v -> performRegistration());

        tvGoToLogin.setOnClickListener(v -> finish());
    }

    private void performRegistration() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        tvErrorMessage.setVisibility(View.GONE);

        if (TextUtils.isEmpty(username)) {
            showError("Please enter a username.");
            etUsername.requestFocus();
            return;
        }

        if (username.length() < 3) {
            showError("Username must be at least 3 characters.");
            etUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            showError("Please enter your email address.");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address.");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            showError("Please enter a password.");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters.");
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match.");
            etConfirmPassword.requestFocus();
            return;
        }

        setLoading(true);

        executorService.execute(() -> {
            User existingUser = database.userDao().findByUsernameOrEmail(username, email);
            if (existingUser != null) {
                runOnUiThread(() -> {
                    setLoading(false);
                    if (existingUser.getUsername().equalsIgnoreCase(username)) {
                        showError("Username is already taken. Please choose another.");
                    } else {
                        showError("An account with this email already exists.");
                    }
                });
                return;
            }

            User newUser = new User(username, email, password);
            long newUserId = database.userDao().insertUser(newUser);

            runOnUiThread(() -> {
                setLoading(false);
                if (newUserId > 0) {
                    sessionManager.login((int) newUserId, username);
                    Toast.makeText(RegisterActivity.this, "Account created successfully! Welcome ❤️", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    showError("Could not save account. Please try again.");
                }
            });
        });
    }

    private void showError(String message) {
        tvErrorMessage.setText(message);
        tvErrorMessage.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean isLoading) {
        pbRegister.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!isLoading);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
