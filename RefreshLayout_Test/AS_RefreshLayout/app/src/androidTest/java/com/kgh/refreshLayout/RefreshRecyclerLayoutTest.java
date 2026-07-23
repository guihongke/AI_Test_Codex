package com.kgh.refreshLayout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.os.SystemClock;
import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.vivo.chat.weight.refresh.smartrefresh.VRefreshRecyclerLayout;
import com.vivo.chat.weight.refresh.smartrefresh.constant.VRefreshState;
import com.vivo.chat.weight.refresh.smartrefresh.footer.VProgressFooter;
import com.vivo.chat.weight.refresh.smartrefresh.header.VProgressHeader;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class RefreshRecyclerLayoutTest {

    @Test
    public void releasingTouchNeverExpandsHeaderAgain() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Let the demo's initial automatic refresh finish before driving the container directly.
            SystemClock.sleep(1_500L);

            AtomicReference<Float> draggedOffset = new AtomicReference<>();
            scenario.onActivity(activity -> {
                VRefreshRecyclerLayout layout = activity.findViewById(R.id.refreshLayout);
                RecyclerView recyclerView = layout.getRecyclerView();
                layout.setOnRefreshListener(null);

                assertTrue(layout.getRefreshHeader() instanceof VProgressHeader);
                assertTrue(layout.getRefreshFooter() instanceof VProgressFooter);
                assertEquals(16, recyclerView.getAdapter().getItemCount());

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
                        -1_000
                );

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
}
