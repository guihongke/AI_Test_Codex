package com.vivo.chat.weight.refresh.smartrefresh.header;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vivo.chat.weight.refresh.smartrefresh.api.VRefreshHeader;
import com.vivo.chat.weight.refresh.smartrefresh.constant.VSpinnerStyle;
import com.vivo.chat.weight.refresh.smartrefresh.simple.VSimpleComponent;

/**
 * 轻量刷新 Header，本身只提供刷新生命周期和 Translate 移动方式。
 * 可见内容完全由 XML 子 View 决定，因此可以放 ProgressBar 或任意自定义布局。
 */
public class VProgressHeader extends VSimpleComponent implements VRefreshHeader {

    public VProgressHeader(@NonNull Context context) {
        this(context, null);
    }

    public VProgressHeader(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VProgressHeader(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr
    ) {
        super(context, attrs, defStyleAttr);
        mSpinnerStyle = VSpinnerStyle.Translate;
    }
}
