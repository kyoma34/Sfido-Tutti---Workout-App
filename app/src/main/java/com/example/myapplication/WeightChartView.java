package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class WeightChartView extends View {
    private List<Float> weights = new ArrayList<>();
    private List<String> labels = new ArrayList<>();
    
    private Paint linePaint;
    private Paint pointPaint;
    private Paint textPaint;
    private Paint axisPaint;
    private Paint weightTextPaint;
    private Paint emptyPaint;

    public WeightChartView(Context context) {
        super(context);
        init();
    }

    public WeightChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#FF3B3B"));
        linePaint.setStrokeWidth(dpToPx(3));
        linePaint.setStyle(Paint.Style.STROKE);

        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setColor(Color.parseColor("#FF3B3B"));
        pointPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#9CA3AF"));
        textPaint.setTextSize(spToPx(10));

        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setColor(Color.parseColor("#1F2937"));
        axisPaint.setStrokeWidth(dpToPx(1));

        weightTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        weightTextPaint.setColor(Color.WHITE);
        weightTextPaint.setTextSize(spToPx(10));
        weightTextPaint.setTextAlign(Paint.Align.CENTER);

        emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        emptyPaint.setColor(Color.parseColor("#9CA3AF"));
        emptyPaint.setTextSize(spToPx(14));
        emptyPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(List<Float> weights, List<String> labels) {
        this.weights = weights != null ? weights : new ArrayList<>();
        this.labels = labels != null ? labels : new ArrayList<>();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (weights.isEmpty()) {
            canvas.drawText("No weight data yet", getWidth() / 2f, getHeight() / 2f, emptyPaint);
            return;
        }

        float padding = dpToPx(40);
        float width = getWidth() - 2 * padding;
        float height = getHeight() - 2 * padding;

        float minWeight = Float.MAX_VALUE;
        float maxWeight = Float.MIN_VALUE;
        for (float w : weights) {
            if (w < minWeight) minWeight = w;
            if (w > maxWeight) maxWeight = w;
        }

        // Add 10% padding above and below
        float range = maxWeight - minWeight;
        if (range == 0) range = 10; 
        minWeight -= range * 0.1f;
        maxWeight += range * 0.1f;
        range = maxWeight - minWeight;

        // Draw axes
        canvas.drawLine(padding, padding, padding, padding + height, axisPaint); // Y
        canvas.drawLine(padding, padding + height, padding + width, padding + height, axisPaint); // X

        float xStep = weights.size() > 1 ? width / (weights.size() - 1) : 0;
        
        for (int i = 0; i < weights.size(); i++) {
            float x = (weights.size() > 1) ? (padding + i * xStep) : (padding + width / 2f);
            float y = padding + height - ((weights.get(i) - minWeight) / range * height);

            // Draw line to next point
            if (i < weights.size() - 1) {
                float nextX = padding + (i + 1) * xStep;
                float nextY = padding + height - ((weights.get(i + 1) - minWeight) / range * height);
                canvas.drawLine(x, y, nextX, nextY, linePaint);
            }

            // Draw point
            canvas.drawCircle(x, y, dpToPx(5), pointPaint);

            // Draw weight text above
            canvas.drawText(String.format("%.1f", weights.get(i)), x, y - dpToPx(8), weightTextPaint);

            // Draw date label below (rotated)
            if (i < labels.size()) {
                canvas.save();
                canvas.rotate(-45, x, padding + height + dpToPx(10));
                canvas.drawText(labels.get(i), x, padding + height + dpToPx(15), textPaint);
                canvas.restore();
            }
        }
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private float spToPx(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }
}
