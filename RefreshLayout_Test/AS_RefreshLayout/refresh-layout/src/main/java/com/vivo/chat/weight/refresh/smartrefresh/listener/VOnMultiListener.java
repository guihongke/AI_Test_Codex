package com.vivo.chat.weight.refresh.smartrefresh.listener;

// Modified from SmartRefreshLayout 2.1.1 for the Vivo-prefixed AndroidX fork.

import com.vivo.chat.weight.refresh.smartrefresh.api.VRefreshFooter;
import com.vivo.chat.weight.refresh.smartrefresh.api.VRefreshHeader;

/**
 * 多功能监听器
 * Created by scwang on 2017/5/26.
 */
public interface VOnMultiListener extends VOnRefreshLoadMoreListener, VOnStateChangedListener {
    /**
     * 手指拖动下拉（会连续多次调用，添加isDragging并取代之前的onPulling、onReleasing）
     * @param header 头部
     * @param isDragging true 手指正在拖动 false 回弹动画
     * @param percent 下拉的百分比 值 = offset/footerHeight (0 - percent - (footerHeight+maxDragHeight) / footerHeight )
     * @param offset 下拉的像素偏移量  0 - offset - (footerHeight+maxDragHeight)
     * @param headerHeight 高度 HeaderHeight or FooterHeight
     * @param maxDragHeight 最大拖动高度
     */
    void onHeaderMoving(VRefreshHeader header, boolean isDragging, float percent, int offset, int headerHeight, int maxDragHeight);

    void onHeaderReleased(VRefreshHeader header, int headerHeight, int maxDragHeight);
    void onHeaderStartAnimator(VRefreshHeader header, int headerHeight, int maxDragHeight);
    void onHeaderFinish(VRefreshHeader header, boolean success);

    /**
     * 手指拖动上拉（会连续多次调用，添加isDragging并取代之前的onPulling、onReleasing）
     * @param footer 尾部
     * @param isDragging true 手指正在拖动 false 回弹动画
     * @param percent 下拉的百分比 值 = offset/footerHeight (0 - percent - (footerHeight+maxDragHeight) / footerHeight )
     * @param offset 下拉的像素偏移量  0 - offset - (footerHeight+maxDragHeight)
     * @param footerHeight 高度 HeaderHeight or FooterHeight
     * @param maxDragHeight 最大拖动高度
     */
    void onFooterMoving(VRefreshFooter footer, boolean isDragging, float percent, int offset, int footerHeight, int maxDragHeight);

    void onFooterReleased(VRefreshFooter footer, int footerHeight, int maxDragHeight);
    void onFooterStartAnimator(VRefreshFooter footer, int footerHeight, int maxDragHeight);
    void onFooterFinish(VRefreshFooter footer, boolean success);
}
