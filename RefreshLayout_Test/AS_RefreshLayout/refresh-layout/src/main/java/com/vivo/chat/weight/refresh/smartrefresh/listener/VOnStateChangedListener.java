package com.vivo.chat.weight.refresh.smartrefresh.listener;

// Modified from SmartRefreshLayout 2.1.1 for the Vivo-prefixed AndroidX fork.


import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.vivo.chat.weight.refresh.smartrefresh.api.VRefreshLayout;
import com.vivo.chat.weight.refresh.smartrefresh.constant.VRefreshState;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.annotation.RestrictTo.Scope.SUBCLASSES;

/**
 * 刷新状态改变监听器
 * Created by scwang on 2017/5/26.
 */
public interface VOnStateChangedListener {
    /**
     * 【仅限框架内调用】状态改变事件 {@link VRefreshState}
     * @param refreshLayout VRefreshLayout
     * @param oldState 改变之前的状态
     * @param newState 改变之后的状态
     */
    @RestrictTo({LIBRARY,LIBRARY_GROUP,SUBCLASSES})
    void onStateChanged(@NonNull VRefreshLayout refreshLayout, @NonNull VRefreshState oldState, @NonNull VRefreshState newState);
}
