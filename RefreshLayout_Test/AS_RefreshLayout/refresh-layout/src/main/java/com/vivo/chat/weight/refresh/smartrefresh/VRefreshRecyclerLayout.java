package com.vivo.chat.weight.refresh.smartrefresh;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.RecyclerView;

/** VSmartRefreshLayout convenience container with ListView-style RecyclerView headers. */
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

    public void addHeaderView(@NonNull View view) {
        headerViewAdapter.addHeaderView(view);
    }

    public boolean removeHeaderView(@NonNull View view) {
        return headerViewAdapter.removeHeaderView(view);
    }

    @Nullable
    public RecyclerView.Adapter<?> getDataAdapter() {
        return dataAdapter;
    }

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
