package com.example.fruits;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageView;

@SuppressLint("ViewConstructor")
public class DrawRectImageView extends AppCompatImageView {
    private Paint paint;
    private RectF rect;
    private boolean isDrawing;
    private RectF drawnRect;

    public DrawRectImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        rect = new RectF();
        isDrawing = false;
        drawnRect = null;

        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Start drawing rectangle
                        rect.left = event.getX();
                        rect.top = event.getY();
                        isDrawing = true;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // Update rectangle as user moves finger
                        if (isDrawing) {
                            rect.right = event.getX();
                            rect.bottom = event.getY();
                            invalidate();
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        // Finish drawing rectangle
                        isDrawing = false;
                        drawnRect = new RectF(rect);
                        invalidate();
                        break;
                }
                return true;
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Draw the stored rectangle
        if (drawnRect != null) {
            canvas.drawRect(drawnRect, paint);
        }
        // Draw the currently drawn rectangle
        if (isDrawing) {
            canvas.drawRect(rect, paint);
        }
    }
    public RectF getDrawnRect() {
        return drawnRect;
    }
    // Method to clear the drawn rectangle
    public void clearDrawnRect() {
        drawnRect = null;
        invalidate();
    }
}


