package com.example.myapplication;

import android.os.SystemClock;
import android.util.Log;

/**
 * Markdown 渲染性能埋点。
 *
 * <p>该类只在 debug 构建中输出日志，release 不产生运行时开销。埋点集中在一处，避免
 * {@link MarkdownView} 里散落大量日志判断。</p>
 */
final class MarkdownRenderProfiler {
    private static final String TAG = "MarkdownPerf";
    private static final long SLOW_STEP_MS = 8L;
    private static final long SLOW_RENDER_MS = 16L;
    private static final boolean ENABLED = BuildConfig.DEBUG;

    private MarkdownRenderProfiler() {
    }

    static long now() {
        return ENABLED ? SystemClock.uptimeMillis() : 0L;
    }

    static void logStep(String name, long startMs) {
        if (!ENABLED || startMs <= 0L) {
            return;
        }
        long cost = SystemClock.uptimeMillis() - startMs;
        if (cost >= SLOW_STEP_MS) {
            Log.d(TAG, name + " cost=" + cost + "ms");
        }
    }

    static void logRenderSummary(String source, long startMs, String markdown, MarkdownView.RenderStats stats) {
        if (!ENABLED || startMs <= 0L) {
            return;
        }
        long cost = SystemClock.uptimeMillis() - startMs;
        if (cost < SLOW_RENDER_MS && stats.viewCount < 40 && stats.tableCellCount < 80) {
            return;
        }
        Log.d(TAG,
                source
                        + " cost=" + cost + "ms"
                        + ", chars=" + (markdown == null ? 0 : markdown.length())
                        + ", blocks=" + stats.blockCount
                        + ", views=" + stats.viewCount
                        + ", tables=" + stats.tableCount
                        + ", tableCells=" + stats.tableCellCount
                        + ", images=" + stats.imageCount);
    }
}
