package com.example.itprojek;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    // ── SharedPreferences Keys for Brute Force Protection ──
    private static final String PREFS_NAME          = "sora_login_prefs";
    private static final String KEY_FAILED_ATTEMPTS = "failed_attempts";
    private static final String KEY_LOCKOUT_COUNT   = "lockout_count";
    private static final String KEY_LOCKOUT_UNTIL   = "lockout_until";

    private Handler  lockoutHandler  = new Handler(Looper.getMainLooper());
    private Runnable lockoutRunnable = new Runnable() {
        @Override
        public void run() {
            checkLockoutStatus();
        }
    };

    // ── Firebase Auth ──
    private FirebaseAuth mAuth;

    // ── Views ──
    private EditText     etEmail, etPassword;
    private TextView     tvLoginText, tvTapHint, tvLoginError;
    private ImageView    ivTogglePassword;
    private CardView     btnLogin, cardLogin;
    private View         progressLogin;
    private LinearLayout heroSection;

    private boolean formVisible      = false;
    private boolean passwordVisible  = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        // Jika sesi masih aktif, langsung ke MainActivity
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_login);
        hideSystemNavBar();

        // Bind views
        etEmail          = findViewById(R.id.et_email);
        etPassword       = findViewById(R.id.et_password);
        tvLoginText      = findViewById(R.id.tv_login_text);
        tvTapHint        = findViewById(R.id.tv_tap_hint);
        tvLoginError     = findViewById(R.id.tv_login_error);
        ivTogglePassword = findViewById(R.id.iv_toggle_password);
        btnLogin         = findViewById(R.id.btn_login);
        progressLogin    = findViewById(R.id.progress_login);
        cardLogin        = findViewById(R.id.card_login);
        heroSection      = findViewById(R.id.hero_section);

        // Posisikan card JAUH di bawah layar sehingga tidak terlihat sama sekali
        // (dilakukan setelah layout selesai diukur)
        cardLogin.post(() -> cardLogin.setTranslationY(cardLogin.getHeight() + 400f));

        // Mulai animasi denyut pada teks petunjuk
        startPulseAnimation(tvTapHint);

        // Klik logo → tampilkan / sembunyikan form (toggle)
        heroSection.setOnClickListener(v -> toggleLoginForm());

        // Toggle show/hide password
        ivTogglePassword.setOnClickListener(v -> togglePassword());

        // Tombol Login
        btnLogin.setOnClickListener(v -> attemptLogin());

        // Cek status blokir saat pertama kali dibuka
        checkLockoutStatus();
    }

    // ─────────────────────────────────────────────
    //  Toggle form: tampilkan atau sembunyikan
    // ─────────────────────────────────────────────
    private void toggleLoginForm() {
        if (!formVisible) {
            // ── TAMPILKAN FORM ──
            formVisible = true;

            // Sembunyikan teks petunjuk
            tvTapHint.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> tvTapHint.setVisibility(View.GONE))
                    .start();

            // Geser hero section ke atas
            float moveUpPx = getResources().getDisplayMetrics().density * 155f;
            heroSection.animate()
                    .translationY(-moveUpPx)
                    .setDuration(450)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();

            // Slide card naik dari bawah
            cardLogin.setVisibility(View.VISIBLE);
            cardLogin.animate()
                    .translationY(0f)
                    .setDuration(480)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();

        } else {
            // ── SEMBUNYIKAN FORM ──
            formVisible = false;

            // Slide card turun ke bawah layar
            float cardOffScreenY = cardLogin.getHeight() + 400f;
            cardLogin.animate()
                    .translationY(cardOffScreenY)
                    .setDuration(400)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(() -> cardLogin.setVisibility(View.INVISIBLE))
                    .start();

            // Kembalikan hero ke posisi tengah
            heroSection.animate()
                    .translationY(0f)
                    .setDuration(420)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();

            // Tampilkan kembali teks petunjuk
            tvTapHint.setVisibility(View.VISIBLE);
            tvTapHint.animate()
                    .alpha(0.80f)
                    .setDuration(300)
                    .withEndAction(() -> startPulseAnimation(tvTapHint))
                    .start();
        }
    }

    // ─────────────────────────────────────────────
    //  Animasi denyut (pulse) pada teks petunjuk
    // ─────────────────────────────────────────────
    private void startPulseAnimation(View view) {
        view.animate()
                .alpha(0.25f)
                .setDuration(850)
                .withEndAction(() -> view.animate()
                        .alpha(0.85f)
                        .setDuration(850)
                        .withEndAction(() -> {
                            if (view.getVisibility() == View.VISIBLE) {
                                startPulseAnimation(view);
                            }
                        })
                        .start())
                .start();
    }

    // ─────────────────────────────────────────────
    //  Logika Login dengan Firebase Auth
    // ─────────────────────────────────────────────
    private void attemptLogin() {
        if (checkLockoutStatus()) {
            return;
        }
        // Sembunyikan pesan error sebelumnya setiap kali mencoba login
        tvLoginError.setVisibility(View.GONE);

        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

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
        if (password.isEmpty()) {
            etPassword.setError("Password tidak boleh kosong");
            etPassword.requestFocus();
            return;
        }

        showLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        // Reset status brute force
                        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                        editor.putInt(KEY_FAILED_ATTEMPTS, 0);
                        editor.putInt(KEY_LOCKOUT_COUNT, 0);
                        editor.putLong(KEY_LOCKOUT_UNTIL, 0);
                        editor.apply();

                        goToMain();
                    } else {
                        handleFailedLogin();
                        etPassword.setText("");

                        // Tentukan pesan berdasarkan jenis error Firebase
                        Exception exception = task.getException();
                        String errorMessage;
                        if (exception != null) {
                            String errCode = exception.getMessage() != null
                                    ? exception.getMessage().toLowerCase() : "";
                            if (errCode.contains("no user record") ||
                                errCode.contains("user-not-found") ||
                                errCode.contains("there is no user")) {
                                errorMessage = "Akun tidak ditemukan. Periksa kembali email Anda.";
                            } else if (errCode.contains("password is invalid") ||
                                       errCode.contains("wrong-password") ||
                                       errCode.contains("incorrect") ||
                                       errCode.contains("invalid credential") ||
                                       errCode.contains("malformed")) {
                                errorMessage = "Password salah, silakan coba lagi.";
                            } else if (errCode.contains("badly formatted") ||
                                       errCode.contains("invalid-email")) {
                                errorMessage = "Format email tidak valid.";
                            } else if (errCode.contains("too many requests") ||
                                       errCode.contains("blocked")) {
                                errorMessage = "Terlalu banyak percobaan. Coba lagi nanti.";
                            } else {
                                errorMessage = "Login gagal. Periksa email dan password Anda.";
                            }
                        } else {
                            errorMessage = "Login gagal. Periksa email dan password Anda.";
                        }
                        tvLoginError.setText(errorMessage);
                        tvLoginError.setVisibility(View.VISIBLE);
                    }
                });
    }

    // ─────────────────────────────────────────────
    //  Proteksi Brute Force & Lockout
    // ─────────────────────────────────────────────
    private boolean checkLockoutStatus() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        long lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0);
        long now = System.currentTimeMillis();

        if (now < lockoutUntil) {
            long remainingSeconds = (lockoutUntil - now) / 1000;
            btnLogin.setClickable(false);
            btnLogin.setAlpha(0.6f);
            tvLoginText.setText("Terkunci (" + remainingSeconds + "s)");

            lockoutHandler.removeCallbacks(lockoutRunnable);
            lockoutHandler.postDelayed(lockoutRunnable, 1000);
            return true;
        } else {
            if (progressLogin.getVisibility() != View.VISIBLE) {
                btnLogin.setClickable(true);
                btnLogin.setAlpha(1.0f);
                tvLoginText.setText("Masuk");
            }
            return false;
        }
    }

    private void handleFailedLogin() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int failedAttempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1;
        int lockoutCount = prefs.getInt(KEY_LOCKOUT_COUNT, 0);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_FAILED_ATTEMPTS, failedAttempts);

        if (failedAttempts >= 5) {
            lockoutCount++;
            // Rumus durasi blokir eksponensial: 60 * 2^(lockoutCount - 1) detik
            long durationSeconds = 60L * (long) Math.pow(2, lockoutCount - 1);
            long lockoutUntil = System.currentTimeMillis() + (durationSeconds * 1000);

            editor.putInt(KEY_LOCKOUT_COUNT, lockoutCount);
            editor.putLong(KEY_LOCKOUT_UNTIL, lockoutUntil);
            editor.apply();

            checkLockoutStatus();
        } else {
            editor.apply();
        }
    }

    @Override
    protected void onDestroy() {
        if (lockoutHandler != null) {
            lockoutHandler.removeCallbacks(lockoutRunnable);
        }
        super.onDestroy();
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
    //  System UI (sembunyikan nav bar)
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
