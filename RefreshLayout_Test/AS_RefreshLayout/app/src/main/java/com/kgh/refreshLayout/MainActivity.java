package com.kgh.refreshLayout;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.vivo.chat.weight.refresh.smartrefresh.VRefreshRecyclerLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PAGE_SIZE = 15;
    private static final int MAX_PAGE = 4;
    private static final long MOCK_DELAY_MS = 700L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private VRefreshRecyclerLayout refreshLayout;
    private MockDataAdapter dataAdapter;
    private int currentPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        refreshLayout = findViewById(R.id.refreshLayout);
        dataAdapter = new MockDataAdapter();

        refreshLayout.getRecyclerView().setLayoutManager(new LinearLayoutManager(this));
        refreshLayout.getRecyclerView().setHasFixedSize(true);
        refreshLayout.setAdapter(dataAdapter);

        View listHeader = LayoutInflater.from(this)
                .inflate(R.layout.view_list_header, refreshLayout.getRecyclerView(), false);
        refreshLayout.addHeaderView(listHeader);

        refreshLayout.setOnRefreshListener(layout -> refreshFirstPage());
        refreshLayout.setOnLoadMoreListener(layout -> loadNextPage());
        refreshLayout.post(refreshLayout::autoRefresh);
    }

    private void refreshFirstPage() {
        mainHandler.postDelayed(() -> {
            currentPage = 1;
            dataAdapter.replaceData(createMockData(currentPage));
            refreshLayout.setNoMoreData(false);
            refreshLayout.finishRefresh();
        }, MOCK_DELAY_MS);
    }

    private void loadNextPage() {
        mainHandler.postDelayed(() -> {
            currentPage++;
            dataAdapter.appendData(createMockData(currentPage));
            refreshLayout.finishLoadMore(0, true, currentPage >= MAX_PAGE);
        }, MOCK_DELAY_MS);
    }

    @NonNull
    private List<String> createMockData(int page) {
        int firstNumber = (page - 1) * PAGE_SIZE + 1;
        List<String> result = new ArrayList<>(PAGE_SIZE);
        for (int index = 0; index < PAGE_SIZE; index++) {
            result.add(String.format(
                    Locale.CHINA,
                    "第 %d 条模拟数据",
                    firstNumber + index
            ));
        }
        return result;
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
