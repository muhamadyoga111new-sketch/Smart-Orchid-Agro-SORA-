package com.example.itprojek;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class SoilGaugeView extends View {

    private Paint trackPaint, redPaint, yellowPaint, greenPaint;
    private Paint textPaint;
    private int percentage = 0;
    private final RectF oval = new RectF();

    public SoilGaugeView(Context c) { super(c); init(); }
    public SoilGaugeView(Context c, AttributeSet a) { super(c, a); init(); }
    public SoilGaugeView(Context c, AttributeSet a, int s) { super(c, a, s); init(); }

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }

    private void init() {
        trackPaint  = arc(Color.parseColor("#E0E0E0"));
        redPaint    = arc(Color.parseColor("#F44336"));
        yellowPaint = arc(Color.parseColor("#FFC107"));
        greenPaint  = arc(Color.parseColor("#4CAF50"));


        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#263238"));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    }

    private Paint arc(int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(color);
        return p;
    }

    public void setPercentage(int pct) {
        percentage = Math.min(100, Math.max(0, pct));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx   = getWidth() / 2f;
        float h    = getHeight();

        // cy = titik tengah lingkaran (di 68% tinggi view, sisanya untuk teks %)
        float cy = h * 0.68f;

        // Radius dibatasi agar tidak keluar view
        float sw     = Math.min(dp(26), cx * 0.18f);
        float radius = Math.min(cx - sw, cy - sw) * 0.90f;

        // Update stroke width semua arc secara proporsional
        float arcSW = sw;
        trackPaint.setStrokeWidth(arcSW);
        redPaint.setStrokeWidth(arcSW);
        yellowPaint.setStrokeWidth(arcSW);
        greenPaint.setStrokeWidth(arcSW);

        oval.set(cx - radius, cy - radius, cx + radius, cy + radius);

        // 1. Background track (abu-abu)
        canvas.drawArc(oval, 180f, 180f, false, trackPaint);

        // 2. Zona warna: merah 0-30%, kuning 30-60%, hijau 60-100%
        canvas.drawArc(oval, 180f,  54f, false, redPaint);
        canvas.drawArc(oval, 234f,  54f, false, yellowPaint);
        canvas.drawArc(oval, 288f,  72f, false, greenPaint);

        // 3. Teks % - di tengah bawah busur
        textPaint.setTextSize(radius * 0.42f);
        canvas.drawText(percentage + "%", cx, cy + radius * 0.08f, textPaint);
    }
}
