package com.streamtv.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.streamtv.app.App;
import com.streamtv.app.R;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvError;
    private ProgressBar progressBar;

    private static final String VALID_USER = "Francervino12";
    private static final String VALID_PASS = "1276";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvError = findViewById(R.id.tvError);
        progressBar = findViewById(R.id.progressBar);

        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });

        btnLogin.setOnClickListener(v -> attemptLogin());
        btnLogin.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) btnLogin.setScaleX(1.05f);
            else btnLogin.setScaleX(1.0f);
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            View focused = getCurrentFocus();
            if (focused == btnLogin) {
                attemptLogin();
                return true;
            } else if (focused == etUsername || focused == etPassword) {
                // Let the default IME action handle it
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void attemptLogin() {
        String user = etUsername.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        tvError.setVisibility(View.GONE);

        if (user.isEmpty() || pass.isEmpty()) {
            showError("Completá usuario y contraseña");
            return;
        }

        if (user.equals(VALID_USER) && pass.equals(VALID_PASS)) {
            btnLogin.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
            App.getInstance().setLoggedIn(true);

            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }, 500);
        } else {
            showError("Usuario o contraseña incorrectos");
        }
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }
}
