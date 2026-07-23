package com.vivo.chat.weight.refresh.smartrefresh.drawable;

// Modified from SmartRefreshLayout 2.1.1 for the Vivo-prefixed AndroidX fork.

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;

/**
 * 旋转动画
 * Created by scwang on 2017/6/16.
 */
@SuppressWarnings("WeakerAccess")
public class VProgressDrawable extends VPaintDrawable implements Animatable , ValueAnimator.AnimatorUpdateListener{

    protected int mWidth = 0;
    protected int mHeight = 0;
    protected int mProgressDegree = 0;
    protected ValueAnimator mValueAnimator;
    protected Path mPath = new Path();

    public VProgressDrawable() {
        mValueAnimator = ValueAnimator.ofInt(30, 3600);
        mValueAnimator.setDuration(10000);
        mValueAnimator.setInterpolator(null);
        mValueAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mValueAnimator.setRepeatMode(ValueAnimator.RESTART);
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        int value = (int) animation.getAnimatedValue();
        mProgressDegree = 30 * (value / 30);
        final Drawable drawable = VProgressDrawable.this;
        drawable.invalidateSelf();
    }

    //<editor-fold desc="Drawable">
    @Override
    public void draw(@NonNull Canvas canvas) {
        final Drawable drawable = VProgressDrawable.this;
        final Rect bounds = drawable.getBounds();
        final int width = bounds.width();
        final int height = bounds.height();
        final float r = Math.max(1f, width / 22f);

        if (mWidth != width || mHeight != height) {
            mPath.reset();
            mPath.addCircle(width - r, height / 2f, r, Path.Direction.CW);
            mPath.addRect(width - 5 * r, height / 2f - r, width - r, height / 2f + r, Path.Direction.CW);
            mPath.addCircle(width - 5 * r, height / 2f, r, Path.Direction.CW);
            mWidth = width;
            mHeight = height;
        }

        canvas.save();
        canvas.rotate(mProgressDegree, (width) / 2f, (height) / 2f);
        for (int i = 0; i < 12; i++) {
            mPaint.setAlpha((i+5) * 0x11);
            canvas.rotate(30, (width) / 2f, (height) / 2f);
            canvas.drawPath(mPath, mPaint);
        }
        canvas.restore();
    }
    //</editor-fold>

    @Override
    public void start() {
        if (!mValueAnimator.isRunning()) {
            mValueAnimator.addUpdateListener(this);
            mValueAnimator.start();
        }
    }

    @Override
    public void stop() {
        if (mValueAnimator.isRunning()) {
            Animator animator = mValueAnimator;
            animator.removeAllListeners();
            mValueAnimator.removeAllUpdateListeners();
            mValueAnimator.cancel();
        }
    }

    @Override
    public boolean isRunning() {
        return mValueAnimator.isRunning();
    }

}
