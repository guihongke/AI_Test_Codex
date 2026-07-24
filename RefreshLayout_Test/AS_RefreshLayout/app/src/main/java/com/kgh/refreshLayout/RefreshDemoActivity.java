package com.kgh.refreshLayout;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.vivo.chat.weight.refresh.smartrefresh.VRefreshRecyclerLayout;
import com.vivo.chat.weight.refresh.smartrefresh.api.VRefreshLayout;
import com.vivo.chat.weight.refresh.smartrefresh.constant.VRefreshState;
import com.vivo.chat.weight.refresh.smartrefresh.footer.VClassicsFooter;
import com.vivo.chat.weight.refresh.smartrefresh.header.VClassicsHeader;
import com.vivo.chat.weight.refresh.smartrefresh.header.VMaterialHeader;
import com.vivo.chat.weight.refresh.smartrefresh.simple.VSimpleMultiListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RefreshDemoActivity extends AppCompatActivity {

    public static final String EXTRA_DEMO_TYPE = "demo_type";
    public static final int DEMO_XML_PROGRESS = 0;
    public static final int DEMO_CLASSICS = 1;
    public static final int DEMO_MATERIAL = 2;

    private static final int PAGE_SIZE = 15;
    private static final int MAX_PAGE = 4;
    private static final long MOCK_DELAY_MS = 700L;
    private static final float MIN_DRAG_RATE = 0.1F;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private VRefreshRecyclerLayout refreshLayout;
    private MockDataAdapter dataAdapter;
    private SwitchCompat failNextSwitch;
    private TextView stateText;
    private int currentPage;
    private int requestGeneration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_refresh_demo);

        int demoType = getIntent().getIntExtra(EXTRA_DEMO_TYPE, DEMO_XML_PROGRESS);
        configureToolbar(demoType);

        refreshLayout = findViewById(R.id.refreshLayout);
        failNextSwitch = findViewById(R.id.failNextSwitch);
        stateText = findViewById(R.id.stateText);
        Button autoRefreshButton = findViewById(R.id.autoRefreshButton);
        Button resetButton = findViewById(R.id.resetButton);

        configureRefreshComponents(demoType);
        configureRecyclerView();
        configureRefreshCallbacks();
        configureDragRateControl();

        autoRefreshButton.setOnClickListener(view -> refreshLayout.autoRefresh());
        resetButton.setOnClickListener(view -> resetDemo());
        updateState(VRefreshState.None);
        refreshLayout.post(refreshLayout::autoRefresh);
    }

    private void configureToolbar(int demoType) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getTitleRes(demoType));
        }
    }

    private void configureRefreshComponents(int demoType) {
        // XML Progress 是布局中的默认组件；另外两种样式在运行时替换，数据逻辑保持一致。
        if (demoType == DEMO_CLASSICS) {
            refreshLayout.setRefreshHeader(new VClassicsHeader(this));
            refreshLayout.setRefreshFooter(new VClassicsFooter(this));
            refreshLayout.setPrimaryColorsId(R.color.refresh_accent, android.R.color.white);
        } else if (demoType == DEMO_MATERIAL) {
            VMaterialHeader materialHeader = new VMaterialHeader(this)
                    .setShowBezierWave(true)
                    .setColorSchemeResources(
                            R.color.refresh_accent,
                            R.color.load_accent,
                            R.color.demo_warning
                    );
            refreshLayout.setRefreshHeader(materialHeader);
            refreshLayout.setRefreshFooter(new VClassicsFooter(this));
            refreshLayout.setPrimaryColorsId(R.color.material_wave, android.R.color.white);
        }
    }

    private void configureRecyclerView() {
        dataAdapter = new MockDataAdapter();
        refreshLayout.getRecyclerView().setLayoutManager(new LinearLayoutManager(this));
        refreshLayout.getRecyclerView().setHasFixedSize(true);
        refreshLayout.setAdapter(dataAdapter);

        // Header 是 RecyclerView 的真实 Adapter 条目，会随列表滚动，不是刷新 Header。
        View listHeader = LayoutInflater.from(this)
                .inflate(R.layout.view_list_header, refreshLayout.getRecyclerView(), false);
        refreshLayout.addHeaderView(listHeader);
    }

    private void configureRefreshCallbacks() {
        refreshLayout.setOnRefreshListener(layout -> refreshFirstPage());
        refreshLayout.setOnLoadMoreListener(layout -> loadNextPage());
        refreshLayout.setOnMultiListener(new VSimpleMultiListener() {
            @Override
            public void onStateChanged(
                    @NonNull VRefreshLayout layout,
                    @NonNull VRefreshState oldState,
                    @NonNull VRefreshState newState
            ) {
                // Header 露出 1/3 时，这里会收到新增的 PullDownStarted 状态。
                updateState(newState);
            }
        });
    }

    private void configureDragRateControl() {
        TextView dragRateText = findViewById(R.id.dragRateText);
        SeekBar dragRateSeekBar = findViewById(R.id.dragRateSeekBar);

        // progress 0..90 映射为 0.1..1.0；默认 progress=40，即组件默认阻尼 0.5。
        dragRateSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float dragRate = MIN_DRAG_RATE + progress / 100F;
                refreshLayout.setDragRate(dragRate);
                dragRateText.setText(getString(R.string.demo_drag_rate_format, dragRate));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // No-op.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // No-op. Drag rate is applied continuously while the thumb moves.
            }
        });
        // SeekBar 不会因注册监听器而主动回调，初始化时显式同步一次显示和参数。
        float initialDragRate = MIN_DRAG_RATE + dragRateSeekBar.getProgress() / 100F;
        refreshLayout.setDragRate(initialDragRate);
        dragRateText.setText(getString(R.string.demo_drag_rate_format, initialDragRate));
    }

    private void refreshFirstPage() {
        // generation 令牌让重置或退出页面前发出的旧请求无法覆盖新数据。
        final int generation = ++requestGeneration;
        final boolean shouldFail = consumeFailureFlag();
        mainHandler.postDelayed(() -> {
            if (generation != requestGeneration || isFinishing()) {
                return;
            }
            if (shouldFail) {
                refreshLayout.finishRefresh(false);
            } else {
                currentPage = 1;
                dataAdapter.replaceData(createMockData(currentPage));
                refreshLayout.setNoMoreData(false);
                refreshLayout.finishRefresh(true);
            }
            updateState(refreshLayout.getState());
        }, MOCK_DELAY_MS);
    }

    private void loadNextPage() {
        final int generation = ++requestGeneration;
        final boolean shouldFail = consumeFailureFlag();
        mainHandler.postDelayed(() -> {
            if (generation != requestGeneration || isFinishing()) {
                return;
            }
            if (shouldFail) {
                refreshLayout.finishLoadMore(0, false, false);
            } else {
                currentPage++;
                dataAdapter.appendData(createMockData(currentPage));
                refreshLayout.finishLoadMore(0, true, currentPage >= MAX_PAGE);
            }
            updateState(refreshLayout.getState());
        }, MOCK_DELAY_MS);
    }

    private boolean consumeFailureFlag() {
        boolean shouldFail = failNextSwitch.isChecked();
        failNextSwitch.setChecked(false);
        return shouldFail;
    }

    private void resetDemo() {
        // 同时取消 Mock 回调和“没有更多数据”状态，再走一次完整刷新流程。
        requestGeneration++;
        mainHandler.removeCallbacksAndMessages(null);
        currentPage = 0;
        dataAdapter.replaceData(new ArrayList<>());
        // 手动恢复列表 Header，便于再次体验“真实下拉时平滑隐藏”的完整过程。
        refreshLayout.showHeaderViews(true);
        refreshLayout.resetNoMoreData();
        failNextSwitch.setChecked(false);
        refreshLayout.autoRefresh();
    }

    private void updateState(@NonNull VRefreshState state) {
        stateText.setText(getString(
                R.string.demo_state_format,
                state.name(),
                currentPage,
                MAX_PAGE
        ));
    }

    private int getTitleRes(int demoType) {
        if (demoType == DEMO_CLASSICS) {
            return R.string.demo_classics_title;
        }
        if (demoType == DEMO_MATERIAL) {
            return R.string.demo_material_title;
        }
        return R.string.demo_xml_title;
    }

    @NonNull
    private List<String> createMockData(int page) {
        int firstNumber = (page - 1) * PAGE_SIZE + 1;
        List<String> result = new ArrayList<>(PAGE_SIZE);
        for (int index = 0; index < PAGE_SIZE; index++) {
//        for (int index = 0; index < 0; index++) {
            result.add(String.format(
                    Locale.CHINA,
                    "第 %d 条模拟数据",
                    firstNumber + index
            ));
        }
        return result;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        requestGeneration++;
        mainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
