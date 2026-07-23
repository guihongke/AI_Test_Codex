package com.vivo.chat.weight.refresh.smartrefresh.simple;

// Modified from SmartRefreshLayout 2.1.1 for the Vivo-prefixed AndroidX fork.

import android.graphics.PointF;
import android.view.View;

import com.vivo.chat.weight.refresh.smartrefresh.listener.VScrollBoundaryDecider;
import com.vivo.chat.weight.refresh.smartrefresh.util.VSmartUtil;

/**
 * 滚动边界
 * Created by scwang on 2017/7/8.
 */
public class VSimpleBoundaryDecider implements VScrollBoundaryDecider {

    //<editor-fold desc="Internal">
    public PointF mActionEvent;
    public VScrollBoundaryDecider boundary;
    public boolean mEnableLoadMoreWhenContentNotFull = true;
    //</editor-fold>

    //<editor-fold desc="VScrollBoundaryDecider">
    @Override
    public boolean canRefresh(View content) {
        if (boundary != null) {
            return boundary.canRefresh(content);
        }
        //mActionEvent == null 时 canRefresh 不会动态递归搜索
        return VSmartUtil.canRefresh(content, mActionEvent);
    }

    @Override
    public boolean canLoadMore(View content) {
        if (boundary != null) {
            return boundary.canLoadMore(content);
        }
        //mActionEvent == null 时 canLoadMore 不会动态递归搜索
        return VSmartUtil.canLoadMore(content, mActionEvent, mEnableLoadMoreWhenContentNotFull);
    }
    //</editor-fold>
}
