package com.vivo.chat.weight.refresh.smartrefresh.footer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vivo.chat.weight.refresh.smartrefresh.api.VRefreshFooter;
import com.vivo.chat.weight.refresh.smartrefresh.api.VRefreshLayout;
import com.vivo.chat.weight.refresh.smartrefresh.constant.VSpinnerStyle;
import com.vivo.chat.weight.refresh.smartrefresh.simple.VSimpleComponent;

/** A lightweight load footer whose visible children are declared entirely in XML. */
public class VProgressFooter extends VSimpleComponent implements VRefreshFooter {

    private boolean noMoreData;

    public VProgressFooter(@NonNull Context context) {
        this(context, null);
    }

    public VProgressFooter(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VProgressFooter(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr
    ) {
        super(context, attrs, defStyleAttr);
        mSpinnerStyle = VSpinnerStyle.Translate;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        updateProgressVisibility(this);
    }

    @Override
    public void onStartAnimator(
            @NonNull VRefreshLayout refreshLayout,
            int height,
            int maxDragHeight
    ) {
        updateProgressVisibility(this);
    }

    @Override
    public boolean setNoMoreData(boolean noMoreData) {
        this.noMoreData = noMoreData;
        updateProgressVisibility(this);
        return true;
    }

    private void updateProgressVisibility(@NonNull View view) {
        if (view instanceof ProgressBar) {
            view.setVisibility(noMoreData ? INVISIBLE : VISIBLE);
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                updateProgressVisibility(group.getChildAt(index));
            }
        }
    }
}
