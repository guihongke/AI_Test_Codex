package com.vivo.chat.weight.refresh.smartrefresh;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.vivo.chat.weight.refresh.smartrefresh.constant.VRefreshState;

/**
 * 带 RecyclerView Header 能力的刷新容器。
 *
 * <p>刷新 Header/Footer 仍由父类管理；列表 Header 则通过 ConcatAdapter 插入数据
 * Adapter 之前，两者是完全不同的概念。</p>
 */
public class VRefreshRecyclerLayout extends VSmartRefreshLayout {

    private final VHeaderViewAdapter headerViewAdapter = new VHeaderViewAdapter();
    private RecyclerView recyclerView;
    private RecyclerView.Adapter<?> dataAdapter;
    private ConcatAdapter concatAdapter;
    private boolean autoHideHeaderViewOnPullDown;
    private boolean pendingAutoHideCommit;

    public VRefreshRecyclerLayout(@NonNull Context context) {
        this(context, null);
    }

    public VRefreshRecyclerLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VRefreshRecyclerLayout(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr
    ) {
        super(context, attrs);
        TypedArray attributes = context.obtainStyledAttributes(
                attrs,
                R.styleable.VRefreshRecyclerLayout,
                defStyleAttr,
                0
        );
        autoHideHeaderViewOnPullDown = attributes.getBoolean(
                R.styleable.VRefreshRecyclerLayout_vrlAutoHideHeaderViewOnPullDown,
                false
        );
        headerViewAdapter.setAnimationDuration(attributes.getInt(
                R.styleable.VRefreshRecyclerLayout_vrlHeaderViewHideDuration,
                180
        ));
        attributes.recycle();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        resolveRecyclerView();
    }

    /**
     * 设置业务数据 Adapter，并自动在它前面拼接列表 Header Adapter。
     * 请使用本方法代替 recyclerView.setAdapter()，否则 addHeaderView 不会生效。
     */
    public void setAdapter(@NonNull RecyclerView.Adapter<?> adapter) {
        dataAdapter = adapter;
        concatAdapter = new ConcatAdapter(
                new ConcatAdapter.Config.Builder()
                        .setIsolateViewTypes(true)
                        .setStableIdMode(ConcatAdapter.Config.StableIdMode.NO_STABLE_IDS)
                        .build(),
                headerViewAdapter,
                adapter
        );
        getRecyclerView().setAdapter(concatAdapter);
    }

    /** 添加一个随 RecyclerView 内容滚动的 Header View。重复添加同一实例会被忽略。 */
    public void addHeaderView(@NonNull View view) {
        headerViewAdapter.addHeaderView(view);
    }

    /** 删除指定 Header View；找到并删除时返回 true。 */
    public boolean removeHeaderView(@NonNull View view) {
        return headerViewAdapter.removeHeaderView(view);
    }

    /**
     * 开启后，用户下拉达到 {@link VRefreshState#PullDownStarted} 阈值时平滑折叠所有列表
     * Header。取消下拉会恢复；真正进入刷新后则保持隐藏。
     *
     * <p>只响应触摸和 RecyclerView 嵌套滚动，不响应 {@code autoRefresh()}，避免页面初始化
     * 的自动刷新意外隐藏业务 Header。</p>
     */
    @NonNull
    public VRefreshRecyclerLayout setHeaderViewAutoHideOnPullDown(boolean enabled) {
        autoHideHeaderViewOnPullDown = enabled;
        if (!enabled) {
            pendingAutoHideCommit = false;
        }
        return this;
    }

    /** 设置列表 Header 折叠/恢复动画时长，单位毫秒；传 0 表示立即切换。 */
    @NonNull
    public VRefreshRecyclerLayout setHeaderViewHideDuration(int durationMillis) {
        headerViewAdapter.setAnimationDuration(durationMillis);
        return this;
    }

    /**
     * 隐藏所有列表 Header。内部只将容器折叠为 0 高度，不删除 Adapter 条目，因此不会改变
     * 数据位置；需要永久删除单个 Header 时仍使用 {@link #removeHeaderView(View)}。
     */
    @NonNull
    public VRefreshRecyclerLayout hideHeaderViews(boolean animate) {
        pendingAutoHideCommit = false;
        headerViewAdapter.setHeadersHidden(true, animate);
        return this;
    }

    /** 恢复之前被折叠的所有列表 Header。 */
    @NonNull
    public VRefreshRecyclerLayout showHeaderViews(boolean animate) {
        pendingAutoHideCommit = false;
        headerViewAdapter.setHeadersHidden(false, animate);
        return this;
    }

    /** 返回列表 Header 当前是否处于折叠隐藏状态。 */
    public boolean areHeaderViewsHidden() {
        return headerViewAdapter.areHeadersHidden();
    }

    @Override
    protected void notifyStateChanged(VRefreshState state) {
        VRefreshState oldState = mState;
        super.notifyStateChanged(state);
        if (oldState == state) {
            return;
        }

        if (state == VRefreshState.PullDownStarted
                && autoHideHeaderViewOnPullDown
                && (mIsBeingDragged || mNestedInProgress)
                && !headerViewAdapter.areHeadersHidden()) {
            // 先保留 Adapter 条目，只折叠 holder；这样下拉期间所有数据 position 都稳定。
            pendingAutoHideCommit = true;
            headerViewAdapter.setHeadersHidden(true, true);
        } else if (state == VRefreshState.Refreshing) {
            // 刷新已真正发生，隐藏结果提交，不再在 None 状态恢复。
            pendingAutoHideCommit = false;
        } else if (state == VRefreshState.PullDownCanceled
                || (state == VRefreshState.None && pendingAutoHideCommit)) {
            // 阈值已达到但刷新未发生，恢复 Header，避免一次试探性下拉永久改变页面。
            pendingAutoHideCommit = false;
            headerViewAdapter.setHeadersHidden(false, true);
        }
    }

    /** 返回业务数据 Adapter，不包含内部 Header Adapter。 */
    @Nullable
    public RecyclerView.Adapter<?> getDataAdapter() {
        return dataAdapter;
    }

    /**
     * 返回 XML 内容区域中的 RecyclerView。
     * 若子树中不存在 RecyclerView，会抛出 IllegalStateException 以尽早暴露布局错误。
     */
    @NonNull
    public RecyclerView getRecyclerView() {
        resolveRecyclerView();
        if (recyclerView == null) {
            throw new IllegalStateException(
                    "VRefreshRecyclerLayout requires a RecyclerView content child"
            );
        }
        return recyclerView;
    }

    private void resolveRecyclerView() {
        if (recyclerView == null) {
            recyclerView = findRecyclerView(this);
        }
    }

    @Nullable
    private RecyclerView findRecyclerView(@NonNull View view) {
        // 允许 RecyclerView 位于一层或多层内容容器中，而不限制必须是直接子 View。
        if (view instanceof RecyclerView) {
            return (RecyclerView) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                RecyclerView found = findRecyclerView(group.getChildAt(index));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
