package com.vivo.chat.weight.refresh.smartrefresh.listener;

// Modified from SmartRefreshLayout 2.1.1 for the Vivo-prefixed AndroidX fork.

import android.content.Context;
import androidx.annotation.NonNull;

import com.vivo.chat.weight.refresh.smartrefresh.api.VRefreshLayout;

/**
 * 默认全局初始化器
 * Created by scwang on 2018/5/29 0029.
 */
public interface VDefaultRefreshInitializer {
    void initialize(@NonNull Context context, @NonNull VRefreshLayout layout);
}
