package com.example.itprojek;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Base64;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProfileActivity extends AppCompatActivity {

    private static final String PREF_PROFILE_PHOTO = "PROFILE_PHOTO_B64";
    private static final String PREF_DISPLAY_NAME  = "PROFILE_DISPLAY_NAME";

    private ImageView ivProfilePhoto;
    private TextView tvUserName;
    private PrefManager pref;

    // Image picker launcher
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) saveAndDisplayPhoto(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        hideSystemNavBar();

        pref = new PrefManager(this);

        // Handle Back Press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        });

        // Status bar insets
        ViewCompat.setOnApplyWindowInsetsListener(getWindow().getDecorView(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            int statusH  = systemBars.top;
            int base60px = (int) (60 * getResources().getDisplayMetrics().density);

            View headerBg = findViewById(R.id.header_bg);
            if (headerBg != null) {
                headerBg.getLayoutParams().height = base60px + statusH;
                headerBg.requestLayout();
            }
            View btnBack = findViewById(R.id.btn_back);
            if (btnBack != null && btnBack.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
                ConstraintLayout.LayoutParams p = (ConstraintLayout.LayoutParams) btnBack.getLayoutParams();
                p.topMargin = statusH;
                btnBack.setLayoutParams(p);
            }
            View tvTitle = findViewById(R.id.tv_header_title);
            if (tvTitle != null && tvTitle.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
                ConstraintLayout.LayoutParams p = (ConstraintLayout.LayoutParams) tvTitle.getLayoutParams();
                p.topMargin = statusH;
                tvTitle.setLayoutParams(p);
            }
            return insets;
        });

        // Back button
        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        // Views
        ivProfilePhoto = findViewById(R.id.iv_profile_photo);
        tvUserName     = findViewById(R.id.tv_user_name);
        TextView tvUserEmail  = findViewById(R.id.tv_user_email);
        TextView tvUserId     = findViewById(R.id.tv_user_id);
        TextView tvUserStatus = findViewById(R.id.tv_user_status);

        // ── Muat foto tersimpan ──────────────────────────────────
        loadSavedPhoto();

        // ── Muat nama tersimpan ─────────────────────────────────
        String savedName = pref.getString(PREF_DISPLAY_NAME, null);
        if (savedName != null) tvUserName.setText(savedName);

        // ── Data Firebase Auth ──────────────────────────────────
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            tvUserEmail.setText(firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "-");
            tvUserId.setText(firebaseUser.getUid().substring(0, 8) + "...");
            tvUserStatus.setText("Aktif");
            // Nama default dari Firebase jika belum pernah diedit
            if (savedName == null && firebaseUser.getDisplayName() != null
                    && !firebaseUser.getDisplayName().isEmpty()) {
                tvUserName.setText(firebaseUser.getDisplayName());
            } else if (savedName == null) {
                tvUserName.setText(firebaseUser.getEmail() != null
                        ? firebaseUser.getEmail().split("@")[0] : "Pengguna SORA");
            }
        }

        // ── Tombol ganti foto ────────────────────────────────────
        MaterialCardView btnChangePhoto = findViewById(R.id.btn_change_photo);
        btnChangePhoto.setOnClickListener(v -> openImagePicker());

        // Klik pada foto juga membuka picker
        MaterialCardView cardAvatar = findViewById(R.id.card_avatar);
        cardAvatar.setOnClickListener(v -> openImagePicker());

        // ── Tombol edit nama ─────────────────────────────────────
        ImageView btnEditName = findViewById(R.id.btn_edit_name);
        btnEditName.setOnClickListener(v -> showEditNameDialog());

        // ── Logout ───────────────────────────────────────────────
        MaterialCardView btnLogout = findViewById(R.id.card_logout);
        btnLogout.setOnClickListener(v -> logout());

        // ── Bottom Navigation ────────────────────────────────────
        setupBottomNavigation();
    }

    // ── Ganti Foto ──────────────────────────────────────────────────────────

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void saveAndDisplayPhoto(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return;

            // Decode & compress menjadi ukuran kecil (max 512px)
            Bitmap original = BitmapFactory.decodeStream(is);
            is.close();

            int maxDim = 512;
            float scale = Math.min((float) maxDim / original.getWidth(),
                                   (float) maxDim / original.getHeight());
            int w = Math.round(original.getWidth()  * scale);
            int h = Math.round(original.getHeight() * scale);
            Bitmap scaled = Bitmap.createScaledBitmap(original, w, h, true);

            // Encode ke Base64 & simpan
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            String b64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
            pref.saveString(PREF_PROFILE_PHOTO, b64);

            // Tampilkan
            displayBitmap(scaled);
            Toast.makeText(this, "Foto profil diperbarui", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSavedPhoto() {
        String b64 = pref.getString(PREF_PROFILE_PHOTO, null);
        if (b64 == null) return;
        try {
            byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
            Bitmap bmp   = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            displayBitmap(bmp);
        } catch (Exception ignored) {}
    }

    private void displayBitmap(Bitmap bmp) {
        ivProfilePhoto.setImageBitmap(bmp);
        ivProfilePhoto.setPadding(0, 0, 0, 0);
        // Hapus tint XML hijau agar foto tampil dengan warna aslinya
        androidx.core.widget.ImageViewCompat.setImageTintList(ivProfilePhoto, null);
    }

    // ── Edit Nama ────────────────────────────────────────────────────────────

    private void showEditNameDialog() {
        EditText etName = new EditText(this);
        etName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        etName.setHint("Masukkan nama baru");
        etName.setText(tvUserName.getText());
        etName.setSelection(etName.getText().length());
        
        // Terapkan font poppins_regular ke EditText
        android.graphics.Typeface typeface = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.poppins_regular);
        if (typeface != null) {
            etName.setTypeface(typeface);
        }

        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        etName.setPadding(padding, padding / 2, padding, padding / 2);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Edit Nama")
                .setView(etName)
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        tvUserName.setText(newName);
                        pref.saveString(PREF_DISPLAY_NAME, newName);
                        Toast.makeText(this, "Nama diperbarui", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Batal", (d, w) -> d.dismiss())
                .show();
    }

    // ── Bottom Navigation ────────────────────────────────────────────────────

    private void setupBottomNavigation() {
        LinearLayout navHome          = findViewById(R.id.nav_home);
        LinearLayout navHistory       = findViewById(R.id.nav_history);
        LinearLayout navNotifications = findViewById(R.id.nav_notifications);
        LinearLayout navSettings      = findViewById(R.id.nav_settings);

        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });
        navHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });
        navNotifications.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });
        navSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });
    }

    // ── Logout ───────────────────────────────────────────────────────────────

    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle("Konfirmasi")
                .setMessage("Yakin ingin keluar dari akun?")
                .setPositiveButton("LANJUTKAN", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    finish();
                })
                .setNegativeButton("BATAL", (d, w) -> d.dismiss())
                .setCancelable(true)
                .show();
    }

    // ── System UI ────────────────────────────────────────────────────────────

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemNavBar();
    }

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
}
