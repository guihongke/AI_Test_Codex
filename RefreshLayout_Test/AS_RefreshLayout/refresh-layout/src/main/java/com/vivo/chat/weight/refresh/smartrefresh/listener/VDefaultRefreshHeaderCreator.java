package com.vivo.chat.weight.refresh.smartrefresh.listener;

// Modified from SmartRefreshLayout 2.1.1 for the Vivo-prefixed AndroidX fork.

import android.content.Context;
import androidx.annotation.NonNull;

import com.vivo.chat.weight.refresh.smartrefresh.api.VRefreshHeader;
import com.vivo.chat.weight.refresh.smartrefresh.api.VRefreshLayout;

/**
 * 默认Header创建器
 * Created by scwang on 2018/1/26.
 */
public interface VDefaultRefreshHeaderCreator {
    @NonNull
    VRefreshHeader createRefreshHeader(@NonNull Context context, @NonNull VRefreshLayout layout);
}
