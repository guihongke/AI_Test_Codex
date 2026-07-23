package com.kgh.refreshLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

final class DemoCatalogAdapter extends RecyclerView.Adapter<DemoCatalogAdapter.ItemViewHolder> {

    interface OnItemClickListener {
        void onItemClick(int demoType);
    }

    static final class Item {
        @StringRes final int titleRes;
        @StringRes final int summaryRes;
        final int demoType;

        Item(@StringRes int titleRes, @StringRes int summaryRes, int demoType) {
            this.titleRes = titleRes;
            this.summaryRes = summaryRes;
            this.demoType = demoType;
        }
    }

    private final List<Item> items;
    private final OnItemClickListener clickListener;

    DemoCatalogAdapter(
            @NonNull List<Item> items,
            @NonNull OnItemClickListener clickListener
    ) {
        this.items = items;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_demo_entry, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = items.get(position);
        holder.title.setText(item.titleRes);
        holder.summary.setText(item.summaryRes);
        holder.itemView.setOnClickListener(view -> clickListener.onItemClick(item.demoType));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class ItemViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView summary;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.demoEntryTitle);
            summary = itemView.findViewById(R.id.demoEntrySummary);
        }
    }
}
