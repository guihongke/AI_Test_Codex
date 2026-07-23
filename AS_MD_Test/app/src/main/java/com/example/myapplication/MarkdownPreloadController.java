package com.example.myapplication;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Markdown 渲染模型预热调度器。
 *
 * <p>预热本身虽然在后台线程执行，但大量长 Markdown 连续解析仍会抢占 CPU，影响首屏绘制和滑动手感。
 * 该调度器把预热拆成小任务：首屏绘制后延迟启动，每次只预热一条，列表滚动时暂停，空闲后再继续。</p>
 */
final class MarkdownPreloadController {
    private static final long PRELOAD_GAP_MS = 48L;

    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Queue<String> pendingMarkdowns = new ArrayDeque<>();

    private boolean running;
    private boolean paused;
    private boolean shutdown;

    /**
     * 创建预热调度器。
     *
     * @param context 任意 Context，内部会转成 Application Context。
     */
    MarkdownPreloadController(Context context) {
        this.appContext = context.getApplicationContext();
    }

    /**
     * 添加需要预热的 Markdown 数据。
     *
     * @param markdowns 待预热 Markdown 集合。
     */
    void enqueue(Collection<String> markdowns) {
        if (shutdown || markdowns == null || markdowns.isEmpty()) {
            return;
        }
        pendingMarkdowns.addAll(markdowns);
    }

    /**
     * 延迟启动预热，让首屏布局和首次绘制先完成。
     *
     * @param delayMs 延迟毫秒数。
     */
    void startDelayed(long delayMs) {
        if (shutdown) {
            return;
        }
        mainHandler.postDelayed(this::scheduleNext, delayMs);
    }

    /**
     * 设置是否暂停预热。
     *
     * @param paused true 表示列表正在滑动或用户正在触摸，暂时不启动新的预热任务。
     */
    void setPaused(boolean paused) {
        if (shutdown || this.paused == paused) {
            return;
        }
        this.paused = paused;
        if (!paused) {
            scheduleNext();
        }
    }

    /**
     * 停止预热并释放线程。
     */
    void shutdown() {
        shutdown = true;
        pendingMarkdowns.clear();
        mainHandler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
    }

    private void scheduleNext() {
        if (shutdown || paused || running || pendingMarkdowns.isEmpty()) {
            return;
        }
        String markdown = pendingMarkdowns.poll();
        running = true;
        executor.execute(() -> {
            try {
                MarkdownView.preloadMarkdown(appContext, markdown);
            } finally {
                mainHandler.post(() -> {
                    running = false;
                    if (!shutdown && !paused) {
                        mainHandler.postDelayed(this::scheduleNext, PRELOAD_GAP_MS);
                    }
                });
            }
        });
    }
}
