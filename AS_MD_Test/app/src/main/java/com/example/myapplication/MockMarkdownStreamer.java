package com.example.myapplication;

import android.os.Handler;
import android.os.Looper;

import java.util.Random;

/**
 * Markdown 流式数据模拟器。
 *
 * <p>真实项目中这一层通常由 SSE、WebSocket 或 HTTP chunked response 驱动。本类用 Handler 在主线程
 * 随机延迟推送 Markdown 分片，用来模拟“连接建立后不断接收增量数据，最后通过连接断开判断完成”的流程。</p>
 */
public final class MockMarkdownStreamer {
    /**
     * 流式推送回调。
     */
    public interface Callback {
        /**
         * 收到一个新的 Markdown 分片时回调。
         *
         * @param chunk 本次新增的 Markdown 文本。
         */
        void onChunk(String chunk);

        /**
         * 模拟连接断开或服务端输出结束时回调。
         */
        void onComplete();
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private boolean cancelled;

    /**
     * 开始推送一段完整 Markdown。
     *
     * <p>调用后会从 offset 0 开始随机拆分并延迟推送。每次 start 都会清除取消标记，但不会主动取消
     * 之前已排队的任务；当前项目保证同一时间只启动一条流。真实项目建议为每条连接维护独立 token。</p>
     *
     * @param markdown 要模拟推送的完整 Markdown，可为 null。
     * @param callback 分片和完成事件回调。
     */
    public void start(String markdown, Callback callback) {
        cancelled = false;
        pushNext(markdown == null ? "" : markdown, 0, callback);
    }

    /**
     * 取消当前模拟流，并移除尚未执行的延迟任务。
     */
    public void cancel() {
        cancelled = true;
        handler.removeCallbacksAndMessages(null);
    }

    /**
     * 递归安排下一次分片推送。
     *
     * @param markdown 完整 Markdown。
     * @param offset 当前已经推送到的字符位置。
     * @param callback 推送回调。
     */
    private void pushNext(String markdown, int offset, Callback callback) {
        if (cancelled) {
            return;
        }
        if (offset >= markdown.length()) {
            handler.postDelayed(() -> {
                if (!cancelled) {
                    callback.onComplete();
                }
            }, randomDelayMs());
            return;
        }

        int chunkSize = randomChunkSize(markdown, offset);
        String chunk = markdown.substring(offset, Math.min(markdown.length(), offset + chunkSize));
        handler.postDelayed(() -> {
            if (cancelled) {
                return;
            }
            callback.onChunk(chunk);
            pushNext(markdown, offset + chunk.length(), callback);
        }, randomDelayMs());
    }

    /**
     * 生成一次模拟网络推送的随机延迟。
     *
     * @return 50ms 到 1000ms 之间的随机值。
     */
    private int randomDelayMs() {
        return 50 + random.nextInt(951);
    }

    /**
     * 生成下一次分片大小。
     *
     * <p>普通文本分片在 8 到 37 个字符之间；如果当前位置是换行，会倾向于推送更短分片，
     * 让段落、表格行等结构的出现更接近真实流式输出。</p>
     *
     * @param markdown 完整 Markdown。
     * @param offset 当前分片起始位置。
     * @return 本次要推送的字符数量。
     */
    private int randomChunkSize(String markdown, int offset) {
        int remaining = markdown.length() - offset;
        if (remaining <= 0) {
            return 0;
        }
        int base = 8 + random.nextInt(30);
        if (markdown.charAt(offset) == '\n') {
            base = Math.min(base, 3);
        }
        return Math.min(remaining, base);
    }
}
