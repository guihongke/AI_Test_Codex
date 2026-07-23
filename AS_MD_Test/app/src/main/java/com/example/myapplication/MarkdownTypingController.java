package com.example.myapplication;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

/**
 * Markdown 打字机节奏控制器。
 *
 * <p>控制器只负责时间调度和每帧应该推进多少字符，不直接操作 View。这样 MarkdownView 可以专注于
 * 渲染状态，真实项目里也可以单独替换打字机策略。</p>
 */
final class MarkdownTypingController {
    private static final long CHAR_INTERVAL_MS = 20L;
    private static final long FRAME_INTERVAL_MS = 48L;
    private static final int MAX_CHARS_PER_FRAME = 3;

    interface Callback {
        /**
         * 推进一帧打字机。
         *
         * @param charCount 本帧建议推进的字符数。
         * @return true 表示仍有待输出内容，需要继续调度下一帧。
         */
        boolean onTypingFrame(int charCount);

        /**
         * 打字机进入空闲状态。
         */
        void onTypingIdle();
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Callback callback;
    private boolean running;
    private long lastFrameTimeMs;

    MarkdownTypingController(Callback callback) {
        this.callback = callback;
    }

    boolean isRunning() {
        return running;
    }

    void start() {
        if (running) {
            return;
        }
        running = true;
        lastFrameTimeMs = SystemClock.uptimeMillis();
        handler.post(frameRunnable);
    }

    void stop() {
        running = false;
        handler.removeCallbacks(frameRunnable);
    }

    private final Runnable frameRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            long now = SystemClock.uptimeMillis();
            long elapsed = Math.max(CHAR_INTERVAL_MS, now - lastFrameTimeMs);
            int charCount = (int) Math.max(1L, elapsed / CHAR_INTERVAL_MS);
            charCount = Math.min(charCount, MAX_CHARS_PER_FRAME);
            lastFrameTimeMs = now;
            if (callback.onTypingFrame(charCount)) {
                handler.postDelayed(this, FRAME_INTERVAL_MS);
            } else {
                running = false;
                callback.onTypingIdle();
            }
        }
    };
}
