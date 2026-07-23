package com.vivo.chat.weight.refresh.smartrefresh.classics;

// Modified from SmartRefreshLayout 2.1.1 for the Vivo-prefixed AndroidX fork.

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;

import com.vivo.chat.weight.refresh.smartrefresh.drawable.VPaintDrawable;

/**
 * 箭头图像
 * Created by scwang on 2018/2/5.
 */
public class VArrowDrawable extends VPaintDrawable {

    private int mWidth = 0;
    private int mHeight = 0;
    private final Path mPath = new Path();

    @Override
    public void draw(@NonNull Canvas canvas) {
        final Drawable drawable = VArrowDrawable.this;
        final Rect bounds = drawable.getBounds();
        final int width = bounds.width();
        final int height = bounds.height();
        if (mWidth != width || mHeight != height) {
            int lineWidth = width * 30 / 225;
            mPath.reset();

            float vector1 = (lineWidth * 0.70710678118654752440084436210485f);//Math.sin(Math.PI/4));
            float vector2 = (lineWidth / 0.70710678118654752440084436210485f);//Math.sin(Math.PI/4));
            mPath.moveTo(width / 2f, height);
            mPath.lineTo(0, height / 2f);
            mPath.lineTo(vector1, height / 2f - vector1);
            mPath.lineTo(width / 2f - lineWidth / 2f, height - vector2 - lineWidth / 2f);
            mPath.lineTo(width / 2f - lineWidth / 2f, 0);
            mPath.lineTo(width / 2f + lineWidth / 2f, 0);
            mPath.lineTo(width / 2f + lineWidth / 2f, height - vector2 - lineWidth / 2f);
            mPath.lineTo(width - vector1, height / 2f - vector1);
            mPath.lineTo(width, height / 2f);
            mPath.close();

            mWidth = width;
            mHeight = height;
        }
        canvas.drawPath(mPath, mPaint);
    }
}
