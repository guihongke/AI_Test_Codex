package com.kgh.refreshLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public final class MockDataAdapter extends RecyclerView.Adapter<MockDataAdapter.ItemViewHolder> {

    private final List<String> items = new ArrayList<>();

    public void replaceData(@NonNull List<String> newItems) {
        int oldSize = items.size();
        items.clear();
        if (oldSize > 0) {
            notifyItemRangeRemoved(0, oldSize);
        }
        items.addAll(newItems);
        if (!newItems.isEmpty()) {
            notifyItemRangeInserted(0, newItems.size());
        }
    }

    public void appendData(@NonNull List<String> newItems) {
        int start = items.size();
        items.addAll(newItems);
        notifyItemRangeInserted(start, newItems.size());
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mock_data, parent, false);
        return new ItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        holder.title.setText(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class ItemViewHolder extends RecyclerView.ViewHolder {
        final TextView title;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.itemTitle);
        }
    }
}
