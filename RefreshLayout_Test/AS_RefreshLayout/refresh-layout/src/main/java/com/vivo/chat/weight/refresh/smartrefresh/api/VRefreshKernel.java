package com.vivo.chat.weight.refresh.smartrefresh.api;

// Modified from SmartRefreshLayout 2.1.1 for the Vivo-prefixed AndroidX fork.

import android.animation.Animator;
import android.animation.ValueAnimator;
import androidx.annotation.NonNull;

import com.vivo.chat.weight.refresh.smartrefresh.constant.VRefreshState;

/**
 * 刷新布局核心功能接口
 * 为功能复杂的 Header 或者 Footer 开放的接口
 * Created by scwang on 2017/5/26.
 */
@SuppressWarnings({"unused", "UnusedReturnValue", "SameParameterValue"})
public interface VRefreshKernel {

    @NonNull
    VRefreshLayout getRefreshLayout();
    @NonNull
    VRefreshContent getRefreshContent();

    VRefreshKernel setState(@NonNull VRefreshState state);

    //<editor-fold desc="视图位移 Spinner">

    /**
     * 开始执行二极刷新
     * @param open 是否展开
     * @return VRefreshKernel
     */
    VRefreshKernel startTwoLevel(boolean open);

    /**
     * 结束关闭二极刷新
     * @return VRefreshKernel
     */
    VRefreshKernel finishTwoLevel();

    /**
     * 移动视图到指定位置
     * moveSpinner 的取名来自 谷歌官方的 {@link androidx.core.widget.SwipeRefreshLayout}
     * @param spinner 位置 (px)
     * @param isDragging true 手指正在拖动 false 回弹动画执行
     * @return VRefreshKernel
     */
    VRefreshKernel moveSpinner(int spinner, boolean isDragging);

    /**
     * 执行动画使视图位移到指定的 位置
     * moveSpinner 的取名来自 谷歌官方的 {@link androidx.core.widget.SwipeRefreshLayout}
     * @param endSpinner 指定的结束位置 (px)
     * @return ValueAnimator 如果没有执行动画 null
     */
    ValueAnimator animSpinner(int endSpinner);
    //</editor-fold>

    //<editor-fold desc="请求事件">

    /**
     * 指定在下拉时候为 Header 或 Footer 绘制背景
     * @param internal Header Footer 调用时传 this
     * @param backgroundColor 背景颜色
     * @return VRefreshKernel
     */
    VRefreshKernel requestDrawBackgroundFor(@NonNull VRefreshComponent internal, int backgroundColor);
    /**
     * 请求事件
     * @param internal Header Footer 调用时传 this
     * @param request 请求
     * @return VRefreshKernel
     */
    VRefreshKernel requestNeedTouchEventFor(@NonNull VRefreshComponent internal, boolean request);
    /**
     * 请求设置默认内容滚动设置
     * @param internal Header Footer 调用时传 this
     * @param translation 移动
     * @return VRefreshKernel
     */
    VRefreshKernel requestDefaultTranslationContentFor(@NonNull VRefreshComponent internal, boolean translation);
    /**
     * 请求重新测量 headerHeight 或 footerHeight , 要求 height 高度为 WRAP_CONTENT
     * @param internal Header Footer 调用时传 this
     * @return VRefreshKernel
     */
    VRefreshKernel requestRemeasureHeightFor(@NonNull VRefreshComponent internal);
    /**
     * 设置二楼回弹时长
     * @param duration 二楼回弹时长
     * @return VRefreshKernel
     */
    VRefreshKernel requestFloorDuration(int duration);
    /**
     * 设置二楼底部上划关闭所占高度的比率
     * @return VRefreshKernel
     */
    VRefreshKernel requestFloorBottomPullUpToCloseRate(float rate);
    /**
     * 当 autoRefresh 动画结束时，处理刷新状态的事件
     * @param animation 动画对象
     * @param animationOnly 是否只播放动画，不通知事件
     * @return VRefreshKernel
     */
    VRefreshKernel onAutoRefreshAnimationEnd(Animator animation, boolean animationOnly);
    /**
     * 当 autoLoadMore 动画结束时，处理刷新状态的事件
     * @param animation 动画对象
     * @param animationOnly 是否只播放动画，不通知事件
     * @return VRefreshKernel
     */
    VRefreshKernel onAutoLoadMoreAnimationEnd(Animator animation, boolean animationOnly);
    //</editor-fold>
}
