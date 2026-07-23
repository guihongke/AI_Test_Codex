package com.vivo.chat.weight.refresh.smartrefresh.listener;

// Modified from SmartRefreshLayout 2.1.1 for the Vivo-prefixed AndroidX fork.

import android.content.Context;
import androidx.annotation.NonNull;

import com.vivo.chat.weight.refresh.smartrefresh.api.VRefreshFooter;
import com.vivo.chat.weight.refresh.smartrefresh.api.VRefreshLayout;

/**
 * 默认Footer创建器
 * Created by scwang on 2018/1/26.
 */
public interface VDefaultRefreshFooterCreator {
    @NonNull
    VRefreshFooter createRefreshFooter(@NonNull Context context, @NonNull VRefreshLayout layout);
}
