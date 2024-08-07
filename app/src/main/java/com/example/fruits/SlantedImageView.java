package com.example.fruits;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

public class SlantedImageView extends AppCompatImageView {

    private Matrix matrix;

    public SlantedImageView(Context context) {
        super(context);
        init();
    }

    public SlantedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SlantedImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        matrix = new Matrix();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float originalWidth = getDrawable().getIntrinsicWidth();
        float originalHeight = getDrawable().getIntrinsicHeight();
        float viewWidth = getWidth();

        float scale =  viewWidth / (originalWidth * (1 + 0.9f * 0.5f)); // Adjust the scaling factor to make the picture bigger

        matrix.reset();
        matrix.setSkew(0f, -0.2f); // Adjust the slant angle here (negative value for slanting from the bottom)
        matrix.postScale(scale * 2.5f, 1f); // Increase the scaling factor to make the picture bigger

        // Calculate the amount of cropping required from the bottom
        float cropAmount = originalHeight * 0.3f * (1 - (1 / (scale * 1.2f)));

        // Set translation to 0 to make the image cover the entire width of the phone
        matrix.postTranslate(-860f, -cropAmount); // Adjust -200f to move the image left or right

        canvas.concat(matrix);
        super.onDraw(canvas);
    }


}
