package com.kgh.refreshLayout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.ActionBar;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.vivo.chat.weight.refresh.smartrefresh.VRefreshRecyclerLayout;
import com.vivo.chat.weight.refresh.smartrefresh.api.VRefreshLayout;
import com.vivo.chat.weight.refresh.smartrefresh.constant.VRefreshState;
import com.vivo.chat.weight.refresh.smartrefresh.footer.VClassicsFooter;
import com.vivo.chat.weight.refresh.smartrefresh.footer.VProgressFooter;
import com.vivo.chat.weight.refresh.smartrefresh.header.VClassicsHeader;
import com.vivo.chat.weight.refresh.smartrefresh.header.VMaterialHeader;
import com.vivo.chat.weight.refresh.smartrefresh.header.VProgressHeader;
import com.vivo.chat.weight.refresh.smartrefresh.simple.VSimpleMultiListener;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(AndroidJUnit4.class)
public class RefreshRecyclerLayoutTest {

    @Test
    public void catalogShowsAllDemoEntries() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                RecyclerView demoList = activity.findViewById(R.id.demoList);
                assertEquals(3, demoList.getAdapter().getItemCount());
            });
        }
    }

    @Test
    public void demoEntriesInstallTheExpectedRefreshComponents() {
        assertDemoComponents(
                RefreshDemoActivity.DEMO_XML_PROGRESS,
                R.string.demo_xml_title,
                VProgressHeader.class,
                VProgressFooter.class
        );
        assertDemoComponents(
                RefreshDemoActivity.DEMO_CLASSICS,
                R.string.demo_classics_title,
                VClassicsHeader.class,
                VClassicsFooter.class
        );
        assertDemoComponents(
                RefreshDemoActivity.DEMO_MATERIAL,
                R.string.demo_material_title,
                VMaterialHeader.class,
                VClassicsFooter.class
        );
    }

    @Test
    public void releasingTouchNeverExpandsHeaderAgain() {
        try (ActivityScenario<RefreshDemoActivity> scenario =
                     ActivityScenario.launch(RefreshDemoActivity.class)) {
            // Stop the Demo's automatic request so this test controls the state machine itself.
            scenario.onActivity(activity -> {
                VRefreshRecyclerLayout layout = activity.findViewById(R.id.refreshLayout);
                layout.setEnableRefresh(false);
                layout.setOnRefreshListener(null);
                layout.closeHeaderOrFooter();
            });
            // Demo 的 mock 请求延迟为 700ms；等待它彻底排空，避免旧 finishRefresh 与
            // 本测试的 nested-scroll 状态机在同一时刻竞争。
            SystemClock.sleep(900L);

            AtomicReference<Float> draggedOffset = new AtomicReference<>();
            AtomicInteger pullDownStartedCount = new AtomicInteger();
            scenario.onActivity(activity -> {
                VRefreshRecyclerLayout layout = activity.findViewById(R.id.refreshLayout);
                RecyclerView recyclerView = layout.getRecyclerView();
                layout.closeHeaderOrFooter();
                layout.setEnableRefresh(true);
                layout.setOnMultiListener(new VSimpleMultiListener() {
                    @Override
                    public void onStateChanged(
                            VRefreshLayout refreshLayout,
                            VRefreshState oldState,
                            VRefreshState newState
                    ) {
                        if (newState == VRefreshState.PullDownStarted) {
                            pullDownStartedCount.incrementAndGet();
                        }
                    }
                });

                assertTrue(layout.getRefreshHeader() instanceof VProgressHeader);
                assertTrue(layout.getRefreshFooter() instanceof VProgressFooter);
                layout.setNoMoreData(true);
                assertEquals(
                        View.INVISIBLE,
                        activity.findViewById(R.id.bottomLoadProgress).getVisibility()
                );
                layout.resetNoMoreData();
                assertEquals(
                        View.VISIBLE,
                        activity.findViewById(R.id.bottomLoadProgress).getVisibility()
                );

                assertTrue(layout.onStartNestedScroll(
                        recyclerView,
                        recyclerView,
                        ViewCompat.SCROLL_AXIS_VERTICAL
                ));
                layout.onNestedScrollAccepted(
                        recyclerView,
                        recyclerView,
                        ViewCompat.SCROLL_AXIS_VERTICAL
                );
                layout.onNestedScroll(
                        recyclerView,
                        0,
                        0,
                        0,
                        -20
                );
                assertEquals(0, pullDownStartedCount.get());

                layout.onNestedScroll(recyclerView, 0, 0, 0, -200);
                assertEquals(1, pullDownStartedCount.get());
                assertEquals(VRefreshState.PullDownStarted, layout.getState());

                // Continue beyond the regular refresh threshold to verify the existing flow.
                layout.onNestedScroll(recyclerView, 0, 0, 0, -1_000);
                assertEquals(1, pullDownStartedCount.get());

                draggedOffset.set(recyclerView.getTranslationY());
                assertTrue(draggedOffset.get() > 0f);
                layout.onStopNestedScroll(recyclerView);
            });

            SystemClock.sleep(80L);
            scenario.onActivity(activity -> {
                VRefreshRecyclerLayout layout = activity.findViewById(R.id.refreshLayout);
                RecyclerView recyclerView = layout.getRecyclerView();
                float settlingOffset = recyclerView.getTranslationY();

                assertTrue(settlingOffset <= draggedOffset.get());

                assertTrue(settlingOffset >= 0f);
            });

            SystemClock.sleep(300L);
            scenario.onActivity(activity -> {
                VRefreshRecyclerLayout layout = activity.findViewById(R.id.refreshLayout);
                assertTrue(
                        layout.getState() == VRefreshState.Refreshing
                                || layout.getState() == VRefreshState.None
                );
                if (layout.isRefreshing()) {
                    layout.finishRefresh();
                }
            });
        }
    }

    @Test
    public void automaticRefreshStillInvokesRefreshListener() {
        try (ActivityScenario<RefreshDemoActivity> scenario =
                     ActivityScenario.launch(RefreshDemoActivity.class)) {
            AtomicBoolean refreshCalled = new AtomicBoolean();
            AtomicBoolean autoRefreshStarted = new AtomicBoolean();

            scenario.onActivity(activity -> {
                VRefreshRecyclerLayout layout = activity.findViewById(R.id.refreshLayout);
                layout.setEnableRefresh(false);
                layout.closeHeaderOrFooter();
                layout.setOnRefreshListener(refreshLayout -> refreshCalled.set(true));
            });
            // Let the auto-refresh Runnable posted by the Demo observe refresh=false and drain.
            SystemClock.sleep(600L);
            AtomicReference<VRefreshState> state = new AtomicReference<>();
            long idleDeadline = SystemClock.uptimeMillis() + 3_000L;
            do {
                scenario.onActivity(activity -> state.set(
                        activity.<VRefreshRecyclerLayout>findViewById(R.id.refreshLayout).getState()
                ));
                if (state.get() == VRefreshState.None) {
                    break;
                }
                SystemClock.sleep(50L);
            } while (SystemClock.uptimeMillis() < idleDeadline);
            assertEquals(VRefreshState.None, state.get());

            scenario.onActivity(activity -> {
                VRefreshRecyclerLayout layout = activity.findViewById(R.id.refreshLayout);
                layout.setEnableRefresh(true);
                // A 1 ms animation verifies the callback path without relying on emulator timing.
                autoRefreshStarted.set(layout.autoRefresh(0, 1, 1.5F, false));
            });

            long deadline = SystemClock.uptimeMillis() + 2_000L;
            while (!refreshCalled.get() && SystemClock.uptimeMillis() < deadline) {
                SystemClock.sleep(50L);
            }
            assertTrue(autoRefreshStarted.get());
            assertTrue(refreshCalled.get());

            scenario.onActivity(activity ->
                    activity.<VRefreshRecyclerLayout>findViewById(R.id.refreshLayout)
                            .finishRefresh()
            );
        }
    }

    @Test
    public void pullDownAutoHideCollapsesHeaderAndCanceledPullRestoresIt() {
        try (ActivityScenario<RefreshDemoActivity> scenario =
                     ActivityScenario.launch(RefreshDemoActivity.class)) {
            // Drain the Demo's startup auto-refresh before taking control of the gesture.
            scenario.onActivity(activity -> {
                VRefreshRecyclerLayout layout = activity.findViewById(R.id.refreshLayout);
                layout.setEnableRefresh(false);
                layout.setOnRefreshListener(null);
                layout.closeHeaderOrFooter();
            });
            SystemClock.sleep(900L);

            scenario.onActivity(activity -> {
                VRefreshRecyclerLayout layout = activity.findViewById(R.id.refreshLayout);
                RecyclerView recyclerView = layout.getRecyclerView();
                View listHeader = activity.findViewById(R.id.listHeader);

                layout.closeHeaderOrFooter();
                layout.showHeaderViews(false);
                layout.setEnableRefresh(true);
                assertTrue(listHeader.getParent() instanceof ViewGroup);
                assertTrue(((View) listHeader.getParent()).getHeight() > 0);

                assertTrue(layout.onStartNestedScroll(
                        recyclerView,
                        recyclerView,
                        ViewCompat.SCROLL_AXIS_VERTICAL
                ));
                layout.onNestedScrollAccepted(
                        recyclerView,
                        recyclerView,
                        ViewCompat.SCROLL_AXIS_VERTICAL
                );
                // This distance crosses the 1/3 start threshold but remains below refresh release.
                layout.onNestedScroll(recyclerView, 0, 0, 0, -200);
                assertEquals(VRefreshState.PullDownStarted, layout.getState());
            });

            SystemClock.sleep(250L);
            scenario.onActivity(activity -> {
                VRefreshRecyclerLayout layout = activity.findViewById(R.id.refreshLayout);
                View listHeader = activity.findViewById(R.id.listHeader);
                View headerContainer = (View) listHeader.getParent();

                assertTrue(layout.areHeaderViewsHidden());
                assertEquals(0, headerContainer.getHeight());
                assertEquals(View.INVISIBLE, headerContainer.getVisibility());
                layout.onStopNestedScroll(layout.getRecyclerView());
            });

            SystemClock.sleep(400L);
            scenario.onActivity(activity -> {
                VRefreshRecyclerLayout layout = activity.findViewById(R.id.refreshLayout);
                View listHeader = activity.findViewById(R.id.listHeader);
                View headerContainer = (View) listHeader.getParent();

                assertTrue(!layout.areHeaderViewsHidden());
                assertEquals(View.VISIBLE, headerContainer.getVisibility());
                assertTrue(headerContainer.getHeight() > 0);
            });
        }
    }

    private void assertDemoComponents(
            int demoType,
            int expectedTitleRes,
            Class<?> expectedHeaderClass,
            Class<?> expectedFooterClass
    ) {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                RefreshDemoActivity.class
        );
        intent.putExtra(RefreshDemoActivity.EXTRA_DEMO_TYPE, demoType);

        try (ActivityScenario<RefreshDemoActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                VRefreshRecyclerLayout layout = activity.findViewById(R.id.refreshLayout);
                ActionBar actionBar = activity.getSupportActionBar();

                assertTrue(actionBar != null);
                assertEquals(activity.getString(expectedTitleRes), actionBar.getTitle());
                assertTrue(expectedHeaderClass.isInstance(layout.getRefreshHeader()));
                assertTrue(expectedFooterClass.isInstance(layout.getRefreshFooter()));
            });
        }
    }

}
