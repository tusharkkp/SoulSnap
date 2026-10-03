package com.mit.tushar_kaldate;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.mit.tushar_kaldate.utils.SessionManager;

/**
 * MainActivity is the application's entry point and router.
 * Inspects session via SharedPreferences and immediately routes:
 * - isLoggedIn == true  -> HomeActivity
 * - isLoggedIn == false -> LoginActivity
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SessionManager sessionManager = new SessionManager(this);

        // Brief delay for branding splash feel, then route
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent targetIntent;
            if (sessionManager.isLoggedIn()) {
                targetIntent = new Intent(MainActivity.this, HomeActivity.class);
            } else {
                targetIntent = new Intent(MainActivity.this, LoginActivity.class);
            }
            startActivity(targetIntent);
            finish();
        }, 600);
    }
}
