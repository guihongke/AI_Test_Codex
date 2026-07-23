package com.vivo.chat.weight.refresh.smartrefresh.listener;

// Modified from SmartRefreshLayout 2.1.1 for the Vivo-prefixed AndroidX fork.

public interface VCoordinatorLayoutListener {
    void onCoordinatorUpdate(boolean enableRefresh, boolean enableLoadMore);
}