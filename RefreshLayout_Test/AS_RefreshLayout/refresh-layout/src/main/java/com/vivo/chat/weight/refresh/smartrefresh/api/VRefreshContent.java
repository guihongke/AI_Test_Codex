package com.vivo.chat.weight.refresh.smartrefresh.api;

// Modified from SmartRefreshLayout 2.1.1 for the Vivo-prefixed AndroidX fork.

import android.animation.ValueAnimator.AnimatorUpdateListener;
import androidx.annotation.NonNull;
import android.view.MotionEvent;
import android.view.View;

import com.vivo.chat.weight.refresh.smartrefresh.listener.VScrollBoundaryDecider;

/**
 * 刷新内容组件
 * Created by scwang on 2017/5/26.
 */
public interface VRefreshContent {

    @NonNull
    View getView();
    @NonNull
    View getScrollableView();

    void onActionDown(MotionEvent e);

    void setUpComponent(VRefreshKernel kernel, View fixedHeader, View fixedFooter);
    void setScrollBoundaryDecider(VScrollBoundaryDecider boundary);

    void setEnableLoadMoreWhenContentNotFull(boolean enable);

    void moveSpinner(int spinner, int headerTranslationViewId, int footerTranslationViewId);

    boolean canRefresh();
    boolean canLoadMore();

    AnimatorUpdateListener scrollContentWhenFinished(int spinner);
}
