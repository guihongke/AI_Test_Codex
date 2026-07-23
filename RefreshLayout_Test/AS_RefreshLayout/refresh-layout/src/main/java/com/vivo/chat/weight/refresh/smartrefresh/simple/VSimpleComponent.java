package com.vivo.chat.weight.refresh.smartrefresh.simple;

// Modified from SmartRefreshLayout 2.1.1 for the Vivo-prefixed AndroidX fork.

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.vivo.chat.weight.refresh.smartrefresh.VSmartRefreshLayout;
import com.vivo.chat.weight.refresh.smartrefresh.api.VRefreshComponent;
import com.vivo.chat.weight.refresh.smartrefresh.api.VRefreshFooter;
import com.vivo.chat.weight.refresh.smartrefresh.api.VRefreshHeader;
import com.vivo.chat.weight.refresh.smartrefresh.api.VRefreshKernel;
import com.vivo.chat.weight.refresh.smartrefresh.api.VRefreshLayout;
import com.vivo.chat.weight.refresh.smartrefresh.constant.VRefreshState;
import com.vivo.chat.weight.refresh.smartrefresh.constant.VSpinnerStyle;
import com.vivo.chat.weight.refresh.smartrefresh.listener.VOnStateChangedListener;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

/**
 * Component 初步实现
 * 实现 Header 和 Footer 时，继承 VSimpleComponent 的话可以少写很多接口方法
 * Created by scwang on 2018/2/6.
 */
public abstract class VSimpleComponent extends RelativeLayout implements VRefreshComponent {

    protected View mWrappedView;
    protected VSpinnerStyle mSpinnerStyle;
    protected VRefreshComponent mWrappedInternal;

    protected VSimpleComponent(@NonNull View wrapped) {
        this(wrapped, wrapped instanceof VRefreshComponent ? (VRefreshComponent) wrapped : null);
    }

    protected VSimpleComponent(@NonNull View wrappedView, @Nullable VRefreshComponent wrappedInternal) {
        super(wrappedView.getContext(), null, 0);
        this.mWrappedView = wrappedView;
        this.mWrappedInternal = wrappedInternal;
        if (this instanceof VRefreshFooter && mWrappedInternal instanceof VRefreshHeader && mWrappedInternal.getSpinnerStyle() == VSpinnerStyle.MatchLayout) {
            wrappedInternal.getView().setScaleY(-1);
        } else if (this instanceof VRefreshHeader && mWrappedInternal instanceof VRefreshFooter && mWrappedInternal.getSpinnerStyle() == VSpinnerStyle.MatchLayout) {
            wrappedInternal.getView().setScaleY(-1);
        }
    }

    protected VSimpleComponent(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            if (obj instanceof VRefreshComponent) {
                final VRefreshComponent thisView = this;
                return thisView.getView() == ((VRefreshComponent)obj).getView();
            }
            return false;
        }
        return true;
    }

    @NonNull
    public View getView() {
        return mWrappedView == null ? this : mWrappedView;
    }

    @Override
    public int onFinish(@NonNull VRefreshLayout refreshLayout, boolean success) {
        if (mWrappedInternal != null && mWrappedInternal != this) {
            return mWrappedInternal.onFinish(refreshLayout, success);
        }
        return 0;
    }

    @Override
    public void setPrimaryColors(@ColorInt int ... colors) {
        if (mWrappedInternal != null && mWrappedInternal != this) {
            mWrappedInternal.setPrimaryColors(colors);
        }
    }

    @NonNull
    @Override
    public VSpinnerStyle getSpinnerStyle() {
        if (mSpinnerStyle != null) {
            return mSpinnerStyle;
        }
        if (mWrappedInternal != null && mWrappedInternal != this) {
            return mWrappedInternal.getSpinnerStyle();
        }
        if (mWrappedView != null) {
            ViewGroup.LayoutParams params = mWrappedView.getLayoutParams();
            if (params instanceof VSmartRefreshLayout.LayoutParams) {
                mSpinnerStyle = ((VSmartRefreshLayout.LayoutParams) params).spinnerStyle;
                if (mSpinnerStyle != null) {
                    return mSpinnerStyle;
                }
            }
            if (params != null) {
                if (params.height == 0 || params.height == MATCH_PARENT) {
                    for (VSpinnerStyle style : VSpinnerStyle.values) {
                        if (style.scale) {
                            return mSpinnerStyle = style;
                        }
                    }
                }
            }
        }
        return mSpinnerStyle = VSpinnerStyle.Translate;
    }

    @Override
    public void onInitialized(@NonNull VRefreshKernel kernel, int height, int maxDragHeight) {
        if (mWrappedInternal != null && mWrappedInternal != this) {
            mWrappedInternal.onInitialized(kernel, height, maxDragHeight);
        } else if (mWrappedView != null) {
            ViewGroup.LayoutParams params = mWrappedView.getLayoutParams();
            if (params instanceof VSmartRefreshLayout.LayoutParams) {
                kernel.requestDrawBackgroundFor(this, ((VSmartRefreshLayout.LayoutParams) params).backgroundColor);
            }
        }
    }

    @Override
    public boolean isSupportHorizontalDrag() {
        return mWrappedInternal != null && mWrappedInternal != this && mWrappedInternal.isSupportHorizontalDrag();
    }

    @Override
    public void onHorizontalDrag(float percentX, int offsetX, int offsetMax) {
        if (mWrappedInternal != null && mWrappedInternal != this) {
            mWrappedInternal.onHorizontalDrag(percentX, offsetX, offsetMax);
        }
    }

    @Override
    public void onMoving(boolean isDragging, float percent, int offset, int height, int maxDragHeight) {
        if (mWrappedInternal != null && mWrappedInternal != this) {
            mWrappedInternal.onMoving(isDragging, percent, offset, height, maxDragHeight);
        }
    }

    @Override
    public void onReleased(@NonNull VRefreshLayout refreshLayout, int height, int maxDragHeight) {
        if (mWrappedInternal != null && mWrappedInternal != this) {
            mWrappedInternal.onReleased(refreshLayout, height, maxDragHeight);
        }
    }

    @Override
    public void onStartAnimator(@NonNull VRefreshLayout refreshLayout, int height, int maxDragHeight) {
        if (mWrappedInternal != null && mWrappedInternal != this) {
            mWrappedInternal.onStartAnimator(refreshLayout, height, maxDragHeight);
        }
    }

    @Override
    public void onStateChanged(@NonNull VRefreshLayout refreshLayout, @NonNull VRefreshState oldState, @NonNull VRefreshState newState) {
        if (mWrappedInternal != null && mWrappedInternal != this) {
            if (this instanceof VRefreshFooter && mWrappedInternal instanceof VRefreshHeader) {
                if (oldState.isFooter) {
                    oldState = oldState.toHeader();
                }
                if (newState.isFooter) {
                    newState = newState.toHeader();
                }
            } else if (this instanceof VRefreshHeader && mWrappedInternal instanceof VRefreshFooter) {
                if (oldState.isHeader) {
                    oldState = oldState.toFooter();
                }
                if (newState.isHeader) {
                    newState = newState.toFooter();
                }
            }
            final VOnStateChangedListener listener = mWrappedInternal;
            if (listener != null) {
                listener.onStateChanged(refreshLayout, oldState, newState);
            }
        }
    }

    @SuppressLint("RestrictedApi")
    public boolean setNoMoreData(boolean noMoreData) {
        return mWrappedInternal instanceof VRefreshFooter && ((VRefreshFooter) mWrappedInternal).setNoMoreData(noMoreData);
    }

    @Override
    public boolean autoOpen(int duration, float dragRate, boolean animationOnly) {
        return false;
    }
}
