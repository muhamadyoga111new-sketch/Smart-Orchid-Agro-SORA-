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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    // ── Firebase Auth ──
    private FirebaseAuth mAuth;

    // ── Views ──
    private EditText     etEmail, etPassword;
    private TextView     tvLoginText, tvTapHint;
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

        // Klik logo → tampilkan form
        heroSection.setOnClickListener(v -> showLoginForm());

        // Toggle show/hide password
        ivTogglePassword.setOnClickListener(v -> togglePassword());

        // Tombol Login
        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    // ─────────────────────────────────────────────
    //  Tampilkan form (animasi logo naik + card naik)
    // ─────────────────────────────────────────────
    private void showLoginForm() {
        if (formVisible) return;
        formVisible = true;

        // Sembunyikan teks petunjuk dengan fade-out
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

        // Tampilkan card dan slide ke atas dari bawah layar
        cardLogin.setVisibility(View.VISIBLE);
        cardLogin.animate()
                .translationY(0f)
                .setDuration(480)
                .setInterpolator(new DecelerateInterpolator())
                .start();
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
                        goToMain();
                    } else {
                        String errMsg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Login gagal. Periksa email dan password.";
                        Toast.makeText(this, errMsg, Toast.LENGTH_LONG).show();
                        etPassword.setText("");
                    }
                });
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
