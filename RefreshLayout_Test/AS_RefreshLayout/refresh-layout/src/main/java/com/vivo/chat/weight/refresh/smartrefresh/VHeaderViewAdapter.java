package com.vivo.chat.weight.refresh.smartrefresh;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import java.util.ArrayList;
import java.util.List;

/** Internal adapter used to expose ListView-style header views on RecyclerView. */
final class VHeaderViewAdapter extends RecyclerView.Adapter<VHeaderViewAdapter.VHeaderViewHolder> {

    private final List<View> headerViews = new ArrayList<>();
    private GridLayoutManager gridLayoutManager;
    private GridLayoutManager.SpanSizeLookup previousSpanSizeLookup;
    private GridLayoutManager.SpanSizeLookup headerSpanSizeLookup;

    void addHeaderView(@NonNull View view) {
        if (headerViews.contains(view)) {
            return;
        }
        headerViews.add(view);
        notifyItemInserted(headerViews.size() - 1);
    }

    boolean removeHeaderView(@NonNull View view) {
        int position = headerViews.indexOf(view);
        if (position < 0) {
            return false;
        }
        headerViews.remove(position);
        notifyItemRemoved(position);
        return true;
    }

    @NonNull
    @Override
    public VHeaderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        FrameLayout container = new FrameLayout(parent.getContext());
        container.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return new VHeaderViewHolder(container);
    }

    @Override
    public void onBindViewHolder(@NonNull VHeaderViewHolder holder, int position) {
        View header = headerViews.get(position);
        ViewParent oldParent = header.getParent();
        if (oldParent instanceof ViewGroup) {
            ((ViewGroup) oldParent).removeView(header);
        }
        holder.container.removeAllViews();
        holder.container.addView(header, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
    }

    @Override
    public void onViewAttachedToWindow(@NonNull VHeaderViewHolder holder) {
        // 瀑布流中 Header 必须占满所有列，行为才与 ListView.addHeaderView 一致。
        ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
        if (params instanceof StaggeredGridLayoutManager.LayoutParams) {
            ((StaggeredGridLayoutManager.LayoutParams) params).setFullSpan(true);
        }
    }

    @Override
    public void onViewRecycled(@NonNull VHeaderViewHolder holder) {
        holder.container.removeAllViews();
    }

    @Override
    public int getItemCount() {
        return headerViews.size();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
        if (manager instanceof GridLayoutManager) {
            // 保留业务原有 SpanSizeLookup，仅让 Header 返回整个 spanCount。
            gridLayoutManager = (GridLayoutManager) manager;
            previousSpanSizeLookup = gridLayoutManager.getSpanSizeLookup();
            headerSpanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (position < VHeaderViewAdapter.this.getItemCount()) {
                        return gridLayoutManager.getSpanCount();
                    }
                    return previousSpanSizeLookup.getSpanSize(position);
                }
            };
            gridLayoutManager.setSpanSizeLookup(headerSpanSizeLookup);
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        if (gridLayoutManager != null
                && gridLayoutManager.getSpanSizeLookup() == headerSpanSizeLookup) {
            gridLayoutManager.setSpanSizeLookup(previousSpanSizeLookup);
        }
        gridLayoutManager = null;
        previousSpanSizeLookup = null;
        headerSpanSizeLookup = null;
    }

    static final class VHeaderViewHolder extends RecyclerView.ViewHolder {
        final FrameLayout container;

        VHeaderViewHolder(@NonNull FrameLayout container) {
            super(container);
            this.container = container;
        }
    }
}
