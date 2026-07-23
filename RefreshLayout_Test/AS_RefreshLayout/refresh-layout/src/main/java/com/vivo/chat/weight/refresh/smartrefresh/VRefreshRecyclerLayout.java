package com.vivo.chat.weight.refresh.smartrefresh;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.RecyclerView;

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
