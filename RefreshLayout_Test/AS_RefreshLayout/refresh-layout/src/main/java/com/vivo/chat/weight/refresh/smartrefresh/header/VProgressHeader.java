package com.vivo.chat.weight.refresh.smartrefresh.header;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vivo.chat.weight.refresh.smartrefresh.api.VRefreshHeader;
import com.vivo.chat.weight.refresh.smartrefresh.constant.VSpinnerStyle;
import com.vivo.chat.weight.refresh.smartrefresh.simple.VSimpleComponent;

/** A lightweight refresh header whose visible children are declared entirely in XML. */
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
