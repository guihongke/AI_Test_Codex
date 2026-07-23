package com.vivo.chat.weight.refresh.smartrefresh.wrapper;

// Modified from SmartRefreshLayout 2.1.1 for the Vivo-prefixed AndroidX fork.

import android.annotation.SuppressLint;
import android.view.View;

import com.vivo.chat.weight.refresh.smartrefresh.api.VRefreshFooter;
import com.vivo.chat.weight.refresh.smartrefresh.simple.VSimpleComponent;

/**
 * 刷新底部包装
 * Created by scwang on 2017/5/26.
 */
@SuppressLint("ViewConstructor")
public class VRefreshFooterWrapper extends VSimpleComponent implements VRefreshFooter {

    public VRefreshFooterWrapper(View wrapper) {
        super(wrapper);
    }

}
