package com.vivo.chat.weight.refresh.smartrefresh.wrapper;

// Modified from SmartRefreshLayout 2.1.1 for the Vivo-prefixed AndroidX fork.

import android.annotation.SuppressLint;
import android.view.View;

import com.vivo.chat.weight.refresh.smartrefresh.api.VRefreshHeader;
import com.vivo.chat.weight.refresh.smartrefresh.simple.VSimpleComponent;

/**
 * 刷新头部包装
 * Created by scwang on 2017/5/26.
 */
@SuppressLint("ViewConstructor")
public class VRefreshHeaderWrapper extends VSimpleComponent implements VRefreshHeader {

    public VRefreshHeaderWrapper(View wrapper) {
        super(wrapper);
    }

}
