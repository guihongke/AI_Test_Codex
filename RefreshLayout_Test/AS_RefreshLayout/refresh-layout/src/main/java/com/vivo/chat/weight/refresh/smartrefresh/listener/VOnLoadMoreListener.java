package com.vivo.chat.weight.refresh.smartrefresh.listener;

// Modified from SmartRefreshLayout 2.1.1 for the Vivo-prefixed AndroidX fork.

import androidx.annotation.NonNull;

import com.vivo.chat.weight.refresh.smartrefresh.api.VRefreshLayout;

/**
 * 加载更多监听器
 * Created by scwang on 2017/5/26.
 */
public interface VOnLoadMoreListener {
    void onLoadMore(@NonNull VRefreshLayout refreshLayout);
}
