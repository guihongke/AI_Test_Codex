package com.vivo.chat.weight.refresh.smartrefresh;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import java.util.ArrayList;
import java.util.List;

/** Internal adapter used to expose ListView-style header views on RecyclerView. */
final class VHeaderViewAdapter extends RecyclerView.Adapter<VHeaderViewAdapter.VHeaderViewHolder> {

    private static final PathInterpolator HEADER_ANIMATOR_INTERPOLATOR =
            new PathInterpolator(0.2F, 0F, 0F, 1F);

    /**
     * HeaderEntry keeps the measured expanded height outside the ViewHolder. RecyclerView may
     * recycle a holder during an animation, while the Header's hidden state must survive rebinding.
     */
    private static final class HeaderEntry {
        final View view;
        int expandedHeight;
        boolean hidden;

        HeaderEntry(@NonNull View view, boolean hidden) {
            this.view = view;
            this.hidden = hidden;
        }
    }

    private final List<HeaderEntry> headerEntries = new ArrayList<>();
    private GridLayoutManager gridLayoutManager;
    private GridLayoutManager.SpanSizeLookup previousSpanSizeLookup;
    private GridLayoutManager.SpanSizeLookup headerSpanSizeLookup;
    private RecyclerView recyclerView;
    private boolean headersHidden;
    private int animationDuration = 180;

    void addHeaderView(@NonNull View view) {
        if (indexOf(view) >= 0) {
            return;
        }
        headerEntries.add(new HeaderEntry(view, headersHidden));
        notifyItemInserted(headerEntries.size() - 1);
    }

    boolean removeHeaderView(@NonNull View view) {
        int position = indexOf(view);
        if (position < 0) {
            return false;
        }
        headerEntries.remove(position);
        notifyItemRemoved(position);
        return true;
    }

    void setAnimationDuration(int duration) {
        animationDuration = Math.max(0, duration);
    }

    boolean areHeadersHidden() {
        return headersHidden;
    }

    /**
     * Collapses all Header holders without removing adapter items. Keeping positions stable avoids
     * RecyclerView relayout jumps while a nested pull gesture is still in progress.
     */
    void setHeadersHidden(boolean hidden, boolean animate) {
        if (headersHidden == hidden) {
            return;
        }
        headersHidden = hidden;
        for (HeaderEntry entry : headerEntries) {
            entry.hidden = hidden;
        }
        applyStateToAttachedHolders(animate && animationDuration > 0);
    }

    private void applyStateToAttachedHolders(boolean animate) {
        RecyclerView attachedRecyclerView = recyclerView;
        if (attachedRecyclerView == null) {
            return;
        }
        Runnable update = () -> {
            for (int position = 0; position < headerEntries.size(); position++) {
                RecyclerView.ViewHolder holder =
                        attachedRecyclerView.findViewHolderForAdapterPosition(position);
                if (holder instanceof VHeaderViewHolder) {
                    HeaderEntry entry = headerEntries.get(position);
                    animateHolder((VHeaderViewHolder) holder, entry, animate);
                }
            }
        };
        // A state callback can be emitted from RecyclerView's nested-scroll dispatch. Defer the
        // first layout mutation when RecyclerView is computing a layout to avoid an illegal update.
        if (attachedRecyclerView.isComputingLayout()) {
            attachedRecyclerView.post(update);
        } else {
            update.run();
        }
    }

    private void animateHolder(
            @NonNull VHeaderViewHolder holder,
            @NonNull HeaderEntry entry,
            boolean animate
    ) {
        holder.cancelAnimator();
        FrameLayout container = holder.container;
        ViewGroup.LayoutParams params = container.getLayoutParams();

        int currentHeight = container.getHeight();
        if (!entry.hidden && currentHeight == 0) {
            currentHeight = Math.max(0, params.height);
        }
        if (entry.hidden && currentHeight > 0) {
            entry.expandedHeight = currentHeight;
        }
        int targetHeight = entry.hidden ? 0 : resolveExpandedHeight(holder, entry);

        if (!animate || currentHeight == targetHeight) {
            applyFinalState(holder, entry);
            return;
        }

        container.setVisibility(View.VISIBLE);
        final int startHeight = currentHeight;
        final float startAlpha = container.getAlpha();
        final float targetAlpha = entry.hidden ? 0F : 1F;
        ValueAnimator animator = ValueAnimator.ofFloat(0F, 1F);
        holder.animator = animator;
        animator.setDuration(animationDuration);
        animator.setInterpolator(HEADER_ANIMATOR_INTERPOLATOR);
        animator.addUpdateListener(valueAnimator -> {
            float fraction = (float) valueAnimator.getAnimatedValue();
            params.height = Math.round(startHeight + (targetHeight - startHeight) * fraction);
            container.setAlpha(startAlpha + (targetAlpha - startAlpha) * fraction);
            container.requestLayout();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            private boolean canceled;

            @Override
            public void onAnimationCancel(Animator animation) {
                canceled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (holder.animator == animation) {
                    holder.animator = null;
                }
                if (!canceled && holder.boundEntry == entry) {
                    applyFinalState(holder, entry);
                }
            }
        });
        animator.start();
    }

    private int resolveExpandedHeight(
            @NonNull VHeaderViewHolder holder,
            @NonNull HeaderEntry entry
    ) {
        if (entry.expandedHeight > 0) {
            return entry.expandedHeight;
        }
        FrameLayout container = holder.container;
        int parentWidth = recyclerView == null ? container.getWidth() : recyclerView.getWidth();
        if (parentWidth <= 0) {
            return 0;
        }
        int width = Math.max(0, parentWidth - container.getPaddingLeft()
                - container.getPaddingRight());
        container.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        entry.expandedHeight = container.getMeasuredHeight();
        return entry.expandedHeight;
    }

    private void applyFinalState(
            @NonNull VHeaderViewHolder holder,
            @NonNull HeaderEntry entry
    ) {
        FrameLayout container = holder.container;
        ViewGroup.LayoutParams params = container.getLayoutParams();
        if (entry.hidden) {
            params.height = 0;
            container.setAlpha(0F);
            container.setVisibility(View.INVISIBLE);
        } else {
            // WRAP_CONTENT is restored after the animation so rotation, font scale, and dynamic
            // Header content can be measured again instead of being locked to a stale pixel value.
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            container.setAlpha(1F);
            container.setVisibility(View.VISIBLE);
        }
        container.requestLayout();
    }

    private int indexOf(@NonNull View view) {
        for (int index = 0; index < headerEntries.size(); index++) {
            if (headerEntries.get(index).view == view) {
                return index;
            }
        }
        return -1;
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
        HeaderEntry entry = headerEntries.get(position);
        holder.cancelAnimator();
        holder.boundEntry = entry;
        ViewParent oldParent = entry.view.getParent();
        if (oldParent instanceof ViewGroup) {
            ((ViewGroup) oldParent).removeView(entry.view);
        }
        holder.container.removeAllViews();
        holder.container.addView(entry.view, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        applyFinalState(holder, entry);
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
        holder.cancelAnimator();
        holder.boundEntry = null;
        holder.container.removeAllViews();
    }

    @Override
    public int getItemCount() {
        return headerEntries.size();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
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
        if (this.recyclerView == recyclerView) {
            this.recyclerView = null;
        }
        gridLayoutManager = null;
        previousSpanSizeLookup = null;
        headerSpanSizeLookup = null;
    }

    static final class VHeaderViewHolder extends RecyclerView.ViewHolder {
        final FrameLayout container;
        @Nullable HeaderEntry boundEntry;
        @Nullable ValueAnimator animator;

        VHeaderViewHolder(@NonNull FrameLayout container) {
            super(container);
            this.container = container;
            container.addOnLayoutChangeListener((view, left, top, right, bottom,
                    oldLeft, oldTop, oldRight, oldBottom) -> {
                HeaderEntry entry = boundEntry;
                int height = bottom - top;
                if (entry != null && !entry.hidden && height > 0) {
                    entry.expandedHeight = height;
                }
            });
        }

        void cancelAnimator() {
            if (animator != null) {
                ValueAnimator runningAnimator = animator;
                animator = null;
                runningAnimator.cancel();
            }
        }
    }
}
