package com.example.itprojek;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Random;

public class LoginActivity extends AppCompatActivity {

    // ── Firebase Auth ──
    private FirebaseAuth mAuth;

    // ── CAPTCHA ──
    private int captchaAnswer = 0;
    private int captchaA, captchaB;
    private static final int[] OPERATORS = {0, 1, 2}; // 0=+, 1=-, 2=×

    // ── Views ──
    private EditText    etEmail, etPassword, etCaptchaAnswer;
    private TextView    tvCaptchaQuestion, tvCaptchaError, tvLoginText;
    private ImageView   ivTogglePassword, btnRefreshCaptcha;
    private CardView    btnLogin;
    private View        progressLogin;

    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inisialisasi Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Jika sudah login (sesi aktif), langsung ke MainActivity
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_login);
        hideSystemNavBar();

        // Bind views
        etEmail           = findViewById(R.id.et_email);
        etPassword        = findViewById(R.id.et_password);
        etCaptchaAnswer   = findViewById(R.id.et_captcha_answer);
        tvCaptchaQuestion = findViewById(R.id.tv_captcha_question);
        tvCaptchaError    = findViewById(R.id.tv_captcha_error);
        tvLoginText       = findViewById(R.id.tv_login_text);
        ivTogglePassword  = findViewById(R.id.iv_toggle_password);
        btnRefreshCaptcha = findViewById(R.id.btn_refresh_captcha);
        btnLogin          = findViewById(R.id.btn_login);
        progressLogin     = findViewById(R.id.progress_login);

        // Generate CAPTCHA pertama
        generateCaptcha();

        // Animasi masuk untuk card login
        CardView cardLogin = findViewById(R.id.card_login);
        cardLogin.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_right));

        // Toggle show/hide password
        ivTogglePassword.setOnClickListener(v -> togglePassword());

        // Refresh CAPTCHA
        btnRefreshCaptcha.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_down));
            generateCaptcha();
            etCaptchaAnswer.setText("");
            tvCaptchaError.setVisibility(View.GONE);
        });

        // Tombol Login
        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    // ─────────────────────────────────────────────
    //  Logika Login dengan Firebase Auth
    // ─────────────────────────────────────────────
    private void attemptLogin() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String captcha  = etCaptchaAnswer.getText().toString().trim();

        // Validasi email
        if (email.isEmpty()) {
            etEmail.setError("Email tidak boleh kosong");
            etEmail.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Format email tidak valid");
            etEmail.requestFocus();
            return;
        }

        // Validasi password
        if (password.isEmpty()) {
            etPassword.setError("Password tidak boleh kosong");
            etPassword.requestFocus();
            return;
        }

        // Validasi CAPTCHA
        if (captcha.isEmpty()) {
            etCaptchaAnswer.setError("Jawab CAPTCHA terlebih dahulu");
            etCaptchaAnswer.requestFocus();
            return;
        }
        try {
            int userAnswer = Integer.parseInt(captcha);
            if (userAnswer != captchaAnswer) {
                tvCaptchaError.setVisibility(View.VISIBLE);
                generateCaptcha();
                etCaptchaAnswer.setText("");
                return;
            }
        } catch (NumberFormatException e) {
            tvCaptchaError.setVisibility(View.VISIBLE);
            return;
        }
        tvCaptchaError.setVisibility(View.GONE);

        // ── Kirim ke Firebase Auth ──
        showLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        // Login berhasil
                        goToMain();
                    } else {
                        // Login gagal — tampilkan pesan error
                        String errMsg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Login gagal. Periksa email dan password.";
                        Toast.makeText(this, errMsg, Toast.LENGTH_LONG).show();
                        // Reset CAPTCHA setelah gagal
                        generateCaptcha();
                        etCaptchaAnswer.setText("");
                        etPassword.setText("");
                    }
                });
    }

    // ─────────────────────────────────────────────
    //  Generate CAPTCHA matematika sederhana
    // ─────────────────────────────────────────────
    private void generateCaptcha() {
        Random random = new Random();
        captchaA = random.nextInt(12) + 1; // 1–12
        int op = OPERATORS[random.nextInt(OPERATORS.length)];
        String question;
        switch (op) {
            case 0: // penjumlahan
                captchaB      = random.nextInt(10) + 1;
                captchaAnswer = captchaA + captchaB;
                question      = captchaA + "  +  " + captchaB + "  = ?";
                break;
            case 1: // pengurangan (hasil ≥ 0)
                captchaB      = random.nextInt(captchaA) + 1;
                captchaAnswer = captchaA - captchaB;
                question      = captchaA + "  −  " + captchaB + "  = ?";
                break;
            default: // perkalian kecil
                captchaB      = random.nextInt(5) + 1;
                captchaAnswer = captchaA * captchaB;
                question      = captchaA + "  ×  " + captchaB + "  = ?";
                break;
        }
        tvCaptchaQuestion.setText(question);
    }

    // ─────────────────────────────────────────────
    //  Navigasi ke MainActivity
    // ─────────────────────────────────────────────
    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    // ─────────────────────────────────────────────
    //  Toggle visibility password
    // ─────────────────────────────────────────────
    private void togglePassword() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            etPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            ivTogglePassword.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        } else {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            ivTogglePassword.setImageResource(android.R.drawable.ic_menu_view);
        }
        etPassword.setSelection(etPassword.getText().length());
    }

    // ─────────────────────────────────────────────
    //  Loading state tombol login
    // ─────────────────────────────────────────────
    private void showLoading(boolean loading) {
        tvLoginText.setVisibility(loading ? View.GONE    : View.VISIBLE);
        progressLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setClickable(!loading);
    }

    // ─────────────────────────────────────────────
    //  System UI
    // ─────────────────────────────────────────────
    private void hideSystemNavBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemNavBar();
    }
}
