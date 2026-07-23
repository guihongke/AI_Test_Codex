package com.vivo.chat.weight.refresh.smartrefresh.simple;

// Modified from SmartRefreshLayout 2.1.1 for the Vivo-prefixed AndroidX fork.

import androidx.annotation.NonNull;

import com.vivo.chat.weight.refresh.smartrefresh.api.VRefreshFooter;
import com.vivo.chat.weight.refresh.smartrefresh.api.VRefreshHeader;
import com.vivo.chat.weight.refresh.smartrefresh.api.VRefreshLayout;
import com.vivo.chat.weight.refresh.smartrefresh.constant.VRefreshState;
import com.vivo.chat.weight.refresh.smartrefresh.listener.VOnMultiListener;

/**
 * 多功能监听器
 * Created by scwang on 2017/5/26.
 */
public class VSimpleMultiListener implements VOnMultiListener {

    @Override
    public void onHeaderMoving(VRefreshHeader header, boolean isDragging, float percent, int offset, int headerHeight, int maxDragHeight) {

    }

    @Override
    public void onHeaderReleased(VRefreshHeader header, int headerHeight, int maxDragHeight) {

    }

    @Override
    public void onHeaderStartAnimator(VRefreshHeader header, int footerHeight, int maxDragHeight) {

    }

    @Override
    public void onHeaderFinish(VRefreshHeader header, boolean success) {

    }

    @Override
    public void onFooterMoving(VRefreshFooter footer, boolean isDragging, float percent, int offset, int footerHeight, int maxDragHeight) {

    }

    @Override
    public void onFooterReleased(VRefreshFooter footer, int footerHeight, int maxDragHeight) {

    }

    @Override
    public void onFooterStartAnimator(VRefreshFooter footer, int headerHeight, int maxDragHeight) {

    }

    @Override
    public void onFooterFinish(VRefreshFooter footer, boolean success) {

    }

    @Override
    public void onRefresh(@NonNull VRefreshLayout refreshLayout) {

    }

    @Override
    public void onLoadMore(@NonNull VRefreshLayout refreshLayout) {

    }

    @Override
    public void onStateChanged(@NonNull VRefreshLayout refreshLayout, @NonNull VRefreshState oldState, @NonNull VRefreshState newState) {

    }

}
