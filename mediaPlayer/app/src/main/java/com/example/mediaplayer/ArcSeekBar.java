package com.example.mediaplayer;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class ArcSeekBar extends View {

    private Paint backgroundPaint;
    private Paint progressPaint;

    private float progress = 0f; // 0 to 100

    public ArcSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.GRAY);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(8f);
        backgroundPaint.setAntiAlias(true);

        progressPaint = new Paint();
        progressPaint.setColor(Color.parseColor("#2c5364"));
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(10f);
        progressPaint.setAntiAlias(true);
    }

    public void setProgress(float progress) {
        this.progress = progress;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();

        RectF rect = new RectF(50, 50, width - 50, height - 50);

        // Background arc
        canvas.drawArc(rect, 180, 180, false, backgroundPaint);

        // Progress arc
        canvas.drawArc(rect, 180, (progress / 100) * 180, false, progressPaint);
    }
}