package com.vivo.chat.weight.refresh.smartrefresh.constant;

// Modified from SmartRefreshLayout 2.1.1 for the Vivo-prefixed AndroidX fork.

/**
 * 刷新状态
 */
@SuppressWarnings("unused")
public enum VRefreshState {
    None(0,false,false,false,false,false),
    PullDownToRefresh(1,true,false,false,false,false), PullUpToLoad(2,true,false,false,false,false),
    PullDownCanceled(1,false,false,false,false,false), PullUpCanceled(2,false,false,false,false,false),
    ReleaseToRefresh(1,true,false,false,false,true), ReleaseToLoad(2,true,false,false,false,true),
    ReleaseToTwoLevel(1,true,false,false,true,true), TwoLevelReleased(1,false,false,false,true,false),
    RefreshReleased(1,false,false,false,false,false), LoadReleased(2,false,false,false,false,false),
    Refreshing(1,false,true,false,false,false), Loading(2,false,true,false,false,false), TwoLevel(1, false, true,false,true,false),
    RefreshFinish(1,false,false,true,false,false), LoadFinish(2,false,false,true,false,false), TwoLevelFinish(1,false,false,true,true,false),
    /** Header 可见高度首次达到配置阈值，默认是 Header 高度的三分之一。 */
    PullDownStarted(1,true,false,false,false,false);

    public final boolean isHeader;
    public final boolean isFooter;
    public final boolean isTwoLevel;// 二级刷新 ReleaseToTwoLevel TwoLevelReleased TwoLevel
    public final boolean isDragging;// 拖动状态：PullDownToRefresh PullDownStarted PullUpToLoad ReleaseToRefresh ReleaseToLoad ReleaseToTwoLevel
    public final boolean isOpening;// 正在刷新状态：Refreshing Loading TwoLevel
    public final boolean isFinishing;//正在完成状态：RefreshFinish LoadFinish TwoLevelFinish
    public final boolean isReleaseToOpening;// 释放立马打开 ReleaseToRefresh ReleaseToLoad ReleaseToTwoLevel

    VRefreshState(int role, boolean dragging, boolean opening, boolean finishing, boolean twoLevel, boolean releaseToOpening) {
        this.isHeader = role == 1;
        this.isFooter = role == 2;
        this.isDragging = dragging;
        this.isOpening = opening;
        this.isFinishing = finishing;
        this.isTwoLevel = twoLevel;
        this.isReleaseToOpening = releaseToOpening;
    }

    public VRefreshState toFooter() {
        // 新状态只有 Header 语义；包装成 Footer 时回退到最接近的拖动状态。
        if (this == PullDownStarted) {
            return PullUpToLoad;
        }
        if (isHeader && !isTwoLevel) {
            return values()[ordinal() + 1];
        }
        return this;
    }

    public VRefreshState toHeader() {
        if (isFooter && !isTwoLevel) {
            return values()[ordinal()-1];
        }
        return this;
    }
}
