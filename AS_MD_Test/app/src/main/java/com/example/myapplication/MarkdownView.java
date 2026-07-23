package com.example.myapplication;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.noties.markwon.Markwon;
import io.noties.markwon.image.AsyncDrawableSpan;

/**
 * 面向 AI 对话场景的 Markdown 渲染 View。
 *
 * <p>该组件基于 Markwon 渲染常见 Markdown 语法，并额外处理对话类产品常见的体验要求：
 * 流式追加数据、按 20ms/字符折算的打字机效果、表格独立横向滚动、图片/链接点击弹窗、
 * 长按 Toast、文本选择复制以及输出过程中禁用交互。</p>
 *
 * <p>为了避免每个打字 tick 都重新 inflate 全量 Markdown，本组件会先把完整已接收内容解析成
 * RenderBlock，再按 typedLength 逐块显示。普通 Markdown 块交给 Markwon；表格块使用单 View
 * Canvas 绘制，以便实现固定列宽、行级渐进展示和只让表格自身横向滚动。</p>
 */
public class MarkdownView extends LinearLayout {
    /**
     * MarkdownView 打字机状态回调。
     *
     * <p>外层列表可通过该回调持久化 typedLength，并在正文底部可见时执行自动跟随。</p>
     */
    public interface TypingStateListener {
        /**
         * 已输出字符数量变化时回调。
         *
         * @param typedLength 当前已经展示到界面的字符数。
         */
        void onTypedLengthChanged(int typedLength);

        /**
         * 打字机没有待输出字符时回调。
         */
        void onTypingIdle();
    }

    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[([^]]*)]\\(([^)\\s]+)(?:\\s+\"[^\"]*\")?\\)");
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+.+$");
    private static final Pattern LIST_PATTERN = Pattern.compile("^\\s*(?:[-+*]\\s+|\\d+\\.\\s+).+$");
    private static final int TEXT_SIZE_DP = 14;
    private static final int TABLE_HEADER_COLOR = Color.rgb(232, 238, 247);
    private static final int TABLE_ROW_ALT_COLOR = Color.rgb(248, 250, 252);
    private static final int TABLE_BORDER_COLOR = Color.rgb(224, 231, 240);
    private static final int TABLE_CELL_HORIZONTAL_PADDING_DP = 8;
    private static final int TABLE_CELL_VERTICAL_PADDING_DP = 5;
    private static final int TABLE_MIN_COLUMN_WIDTH_DP = 72;
    private static final int TABLE_MAX_COLUMN_WIDTH_DP = 220;
    private static final int MAX_RENDER_MODEL_CACHE_SIZE = 80;
    private static final MarkdownRenderCache<String, RenderModel> RENDER_MODEL_CACHE = new MarkdownRenderCache<>(MAX_RENDER_MODEL_CACHE_SIZE);

    private final Markwon markwon;
    private final MarkdownTypingController typewriterController = new MarkdownTypingController(new MarkdownTypingController.Callback() {
        @Override
        public boolean onTypingFrame(int charCount) {
            return advanceTypingFrame(charCount);
        }

        @Override
        public void onTypingIdle() {
            finishTypingIfIdle();
        }
    });
    private final List<String> fullImageUrls = new ArrayList<>();
    private final List<RenderBlock> renderBlocks = new ArrayList<>();
    private final StringBuilder streamingMarkdown = new StringBuilder();
    private String currentFullMarkdown = "";
    private int typedLength;
    private int lastAppliedVisibleLength = -1;
    private boolean interactionEnabled = true;
    private TypingStateListener typingStateListener;
    private InteractionListener interactionListener;

    /**
     * Markdown 交互回调。
     *
     * <p>返回 true 表示外部已经消费事件；返回 false 时组件执行默认 demo 行为。</p>
     */
    public interface InteractionListener {
        boolean onImageClick(MarkdownView markdownView, View view, String imageUrl, int imageIndex, List<String> allImageUrls);

        boolean onLinkClick(MarkdownView markdownView, View view, String link);

        boolean onContentLongClick(MarkdownView markdownView, View view);

        boolean onTextDoubleClick(MarkdownView markdownView, TextView textView, String selectedText);
    }

    /**
     * 代码创建 MarkdownView 时使用的构造函数。
     *
     * @param context View 所在上下文。
     */
    public MarkdownView(Context context) {
        this(context, null);
    }

    /**
     * XML inflate MarkdownView 时使用的构造函数。
     *
     * @param context View 所在上下文。
     * @param attrs XML 中声明的属性集合。
     */
    public MarkdownView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * 完整构造函数，统一初始化方向、长按行为和 Markwon 实例。
     *
     * @param context View 所在上下文。
     * @param attrs XML 属性集合。
     * @param defStyleAttr 默认样式属性。
     */
    public MarkdownView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        setLongClickable(true);
        setOnLongClickListener(v -> {
            if (dispatchContentLongClick(this)) {
                return true;
            }
            Toast.makeText(getContext(), "md长按", Toast.LENGTH_SHORT).show();
            return true;
        });
        markwon = MarkdownMarkwonProvider.get(context);
    }

    /**
     * 设置 Markdown 内容，并指定当前已经可见的 Markdown 前缀。
     *
     * <p>该接口适合列表恢复场景：外部已经知道完整已接收内容和当前 typedLength 对应的可见内容，
     * 可以直接恢复到指定进度。调用后不会继续启动打字机。</p>
     *
     * @param visibleMarkdown 当前应立即显示的 Markdown 前缀。
     * @param fullMarkdown 当前已接收的完整 Markdown。
     */
    public void setMarkdown(String visibleMarkdown, String fullMarkdown) {
        stopTypewriter();
        String normalizedFullMarkdown = normalizeMarkdown(fullMarkdown);
        String normalizedVisibleMarkdown = normalizeMarkdown(visibleMarkdown);
        streamingMarkdown.setLength(0);
        streamingMarkdown.append(normalizedFullMarkdown);
        typedLength = Math.min(normalizedVisibleMarkdown.length(), normalizedFullMarkdown.length());
        if (!normalizedFullMarkdown.equals(currentFullMarkdown)) {
            rebuildBlocks(normalizedFullMarkdown);
        }
        updateVisibleLength(typedLength);
    }

    /**
     * 一次性展示完整 Markdown，不启用打字机效果。
     *
     * <p>该接口用于历史消息、调试预览或业务上需要直接展开完整内容的场景。</p>
     *
     * @param markdown 要完整展示的 Markdown 文本。
     */
    public void showMarkdownImmediately(String markdown) {
        long startMs = MarkdownRenderProfiler.now();
        stopTypewriter();
        String normalizedMarkdown = normalizeMarkdown(markdown);
        streamingMarkdown.setLength(0);
        streamingMarkdown.append(normalizedMarkdown);
        typedLength = normalizedMarkdown.length();
        if (!normalizedMarkdown.equals(currentFullMarkdown)) {
            rebuildBlocks(normalizedMarkdown, true, true);
        } else {
            showAllBlocksImmediately();
        }
        MarkdownRenderProfiler.logRenderSummary("showMarkdownImmediately", startMs, normalizedMarkdown, renderStats());
    }

    /**
     * 预热指定 Markdown 的渲染模型。
     *
     * <p>该方法不创建 Android View，只解析 Markdown、计算表格模型并缓存 Markwon 渲染后的 Spanned。
     * 列表全量加载历史消息时可以提前调用，降低用户首次滑到某条长消息时的主线程渲染成本。</p>
     *
     * @param context 上下文。
     * @param markdown 需要预热的 Markdown。
     */
    public static void preloadMarkdown(Context context, String markdown) {
        long startMs = MarkdownRenderProfiler.now();
        String normalizedMarkdown = normalizeMarkdownStatic(markdown);
        getRenderModel(context, normalizedMarkdown, true);
        MarkdownRenderProfiler.logStep("preloadMarkdown", startMs);
    }

    /**
     * 提交当前已接收到的完整 Markdown，并从指定打字位置继续输出。
     *
     * <p>ListView 复用 item 时推荐调用该接口。外部数据模型保存 typedLength，View 重新绑定后即可
     * 继续打字，而不会从头播放或瞬间显示全部内容。</p>
     *
     * @param receivedMarkdown 当前流式连接已经收到的全部 Markdown。
     * @param typedLength 外部保存的已输出字符数。
     */
    public void appendMarkdownStreaming(String receivedMarkdown, int typedLength) {
        String normalizedMarkdown = normalizeMarkdown(receivedMarkdown);
        boolean contentChanged = !normalizedMarkdown.equals(currentFullMarkdown);
        int normalizedTypedLength = Math.min(Math.max(typedLength, 0), normalizedMarkdown.length());
        if (normalizedTypedLength < this.typedLength || !contentChanged && normalizedTypedLength != this.typedLength) {
            stopTypewriter();
        }
        streamingMarkdown.setLength(0);
        streamingMarkdown.append(normalizedMarkdown);
        this.typedLength = normalizedTypedLength;
        if (shouldRebuildForStreaming(normalizedMarkdown, normalizedTypedLength)) {
            rebuildBlocks(normalizedMarkdown, false);
        }
        updateVisibleLength(Math.min(this.typedLength, currentFullMarkdown.length()));
        ensureTypewriterRunning();
    }

    /**
     * 追加一个新的流式 Markdown 分片，并启动或继续打字机输出。
     *
     * <p>该接口适合 View 直接承接网络流回调的场景。本项目当前在 Adapter 中使用完整内容重绑接口，
     * 以便更好地兼容 ListView 的 item 回收。</p>
     *
     * @param chunk 本次新增的 Markdown 分片。
     */
    public void appendMarkdownStreaming(String chunk) {
        appendMarkdownStreaming(streamingMarkdown.toString() + normalizeMarkdown(chunk), typedLength);
    }

    /**
     * 设置打字机状态监听器。
     *
     * @param typingStateListener 用于接收已输出长度和空闲状态的监听器；传 null 可取消监听。
     */
    public void setTypingStateListener(TypingStateListener typingStateListener) {
        this.typingStateListener = typingStateListener;
    }

    /**
     * 设置 Markdown 交互监听。
     *
     * @param interactionListener 图片、链接、长按、双击文字等交互回调；传 null 恢复默认行为。
     */
    public void setInteractionListener(InteractionListener interactionListener) {
        this.interactionListener = interactionListener;
    }

    /**
     * 获取当前已输出字符数量。
     *
     * @return 当前 typedLength。
     */
    public int getTypedLength() {
        return typedLength;
    }

    /**
     * 判断是否仍有已接收但未展示的内容。
     *
     * @return true 表示打字机还有待输出字符。
     */
    public boolean hasPendingTyping() {
        return typedLength < streamingMarkdown.length();
    }

    /**
     * 确保打字机处于运行状态。
     *
     * <p>当 typedLength 已经追平已接收内容时会保持或恢复交互；否则禁用图片、链接、长按和选择等操作，
     * 并交给 {@link MarkdownTypingController} 按帧推进。</p>
     */
    private void ensureTypewriterRunning() {
        if (typewriterController.isRunning() || typedLength >= streamingMarkdown.length()) {
            setContentInteractionEnabled(typedLength >= streamingMarkdown.length());
            return;
        }
        setContentInteractionEnabled(false);
        typewriterController.start();
    }

    /**
     * 执行一帧打字机渲染。
     *
     * <p>时间控制器会根据 20ms/字符的速度折算本帧推进字符数，MarkdownView 只负责推进状态和刷新
     * 可见内容。这样可以减少 TextView 重新排版次数，同时保持整体输出速度稳定。</p>
     *
     * @param charCount 本帧需要推进的字符数。
     * @return true 表示还有待输出内容。
     */
    private boolean advanceTypingFrame(int charCount) {
        if (typedLength >= streamingMarkdown.length()) {
            return false;
        }
        ensureRenderableContentForTyping();
        setContentInteractionEnabled(false);
        typedLength = Math.min(streamingMarkdown.length(), typedLength + Math.max(1, charCount));
        updateVisibleLength(Math.min(typedLength, currentFullMarkdown.length()));
        if (typingStateListener != null) {
            typingStateListener.onTypedLengthChanged(typedLength);
        }
        return typedLength < streamingMarkdown.length();
    }

    /**
     * 打字机没有待输出内容后恢复交互并通知外层。
     */
    private void finishTypingIfIdle() {
        setContentInteractionEnabled(true);
        if (typingStateListener != null) {
            typingStateListener.onTypingIdle();
        }
    }

    /**
     * 判断流式内容变化后是否需要立刻重建 View 树。
     *
     * <p>流式连接可能高频推送分片，但 UI 打字机通常落后于网络接收。如果当前已构建内容还能覆盖
     * typedLength 附近的展示范围，就先只更新缓冲，不立刻 remove/add 子 View；等打字进度追到
     * 已构建内容末尾时再重建下一段，避免每个 chunk 都触发主线程 View 树重建。</p>
     *
     * @param markdown 最新已接收完整 Markdown。
     * @param nextTypedLength 外部恢复的已输出长度。
     * @return true 表示需要立即重建。
     */
    private boolean shouldRebuildForStreaming(String markdown, int nextTypedLength) {
        if (markdown.equals(currentFullMarkdown)) {
            return false;
        }
        if (currentFullMarkdown.isEmpty()) {
            return true;
        }
        if (!markdown.startsWith(currentFullMarkdown)) {
            return true;
        }
        return nextTypedLength >= currentFullMarkdown.length();
    }

    /**
     * 确保下一次打字机推进的位置已经有对应的渲染块。
     */
    private void ensureRenderableContentForTyping() {
        if (typedLength >= currentFullMarkdown.length() && currentFullMarkdown.length() < streamingMarkdown.length()) {
            rebuildBlocks(streamingMarkdown.toString(), false);
        }
    }

    /**
     * 停止打字机并清理未执行的调度回调。
     *
     * <p>该方法用于直接展示、View detach 或进度回退等场景，避免旧任务继续修改新内容。</p>
     */
    private void stopTypewriter() {
        typewriterController.stop();
        setContentInteractionEnabled(true);
    }

    /**
     * View 从窗口移除时停止打字机，避免 ListView 回收 item 后仍继续持有回调。
     */
    @Override
    protected void onDetachedFromWindow() {
        stopTypewriter();
        super.onDetachedFromWindow();
    }

    /**
     * 统一 Markdown 换行格式。
     *
     * @param markdown 原始 Markdown，可为 null。
     * @return 非 null 且换行统一为 \n 的文本。
     */
    private String normalizeMarkdown(String markdown) {
        return normalizeMarkdownStatic(markdown);
    }

    private static String normalizeMarkdownStatic(String markdown) {
        return markdown == null ? "" : markdown.replace("\r\n", "\n").replace('\r', '\n');
    }

    /**
     * 根据最新完整 Markdown 重建渲染块。
     *
     * <p>只有已接收内容变化时才调用该方法。它会清空旧 View、重新解析表格/图片/普通块，
     * 并收集完整图片 URL 列表供点击弹窗使用。</p>
     *
     * @param fullMarkdown 当前已接收的完整 Markdown。
     */
    private void rebuildBlocks(String fullMarkdown) {
        rebuildBlocks(fullMarkdown, false);
    }

    private void rebuildBlocks(String fullMarkdown, boolean cacheable) {
        rebuildBlocks(fullMarkdown, cacheable, false);
    }

    private void rebuildBlocks(String fullMarkdown, boolean cacheable, boolean showImmediately) {
        long startMs = MarkdownRenderProfiler.now();
        currentFullMarkdown = fullMarkdown;
        lastAppliedVisibleLength = -1;
        removeAllViews();
        renderBlocks.clear();
        fullImageUrls.clear();
        RenderModel renderModel = getRenderModel(getContext(), fullMarkdown, cacheable);
        fullImageUrls.addAll(renderModel.imageUrls);
        bindRenderModel(renderModel);
        for (RenderBlock block : renderBlocks) {
            addView(block.view, block.params);
            setBlockVisibility(block, GONE);
        }
        if (showImmediately) {
            showAllBlocksImmediately();
        }
        setContentInteractionEnabled(!hasPendingTyping(), true);
        MarkdownRenderProfiler.logRenderSummary("rebuildBlocks", startMs, fullMarkdown, renderStats());
    }

    /**
     * 将所有渲染块直接切到完整展示状态。
     *
     * <p>历史消息或一次性加载场景不需要模拟打字机可见长度。直接使用已缓存的完整 Spanned 和表格模型，
     * 可以少一次全量 {@link #updateVisibleLength(int)} 扫描，也避免普通块走 substring 判断。</p>
     */
    private void showAllBlocksImmediately() {
        for (RenderBlock block : renderBlocks) {
            if (block.type == RenderBlock.TYPE_MARKDOWN) {
                setBlockVisibility(block, VISIBLE);
                if (block.plainText && !block.sourceMarkdown.equals(block.lastRenderedMarkdown)) {
                    ((TextView) block.view).setText(block.sourceMarkdown);
                    block.lastRenderedMarkdown = block.sourceMarkdown;
                } else if (block.preRenderedMarkdown != null && !block.sourceMarkdown.equals(block.lastRenderedMarkdown)) {
                    markwon.setParsedMarkdown((TextView) block.view, block.preRenderedMarkdown);
                    block.lastRenderedMarkdown = block.sourceMarkdown;
                }
            } else if (block.type == RenderBlock.TYPE_TABLE) {
                setBlockVisibility(block, VISIBLE);
                showAllTableRows(block);
            } else {
                setBlockVisibility(block, VISIBLE);
            }
        }
        lastAppliedVisibleLength = currentFullMarkdown.length();
        setContentInteractionEnabled(true);
    }

    private void showAllTableRows(RenderBlock block) {
        MarkdownTableView table = block.tableView;
        if (table == null || block.tableBlock == null) {
            return;
        }
        int rowCount = block.tableBlock.rowRanges.size();
        if (block.lastVisibleTableRows == rowCount) {
            return;
        }
        table.setVisibleRowCount(rowCount);
        block.lastVisibleTableRows = rowCount;
    }

    private RenderStats renderStats() {
        RenderStats stats = new RenderStats();
        stats.finish(renderBlocks, fullImageUrls.size());
        return stats;
    }

    private static RenderModel getRenderModel(Context context, String markdown, boolean cacheable) {
        if (cacheable) {
            RenderModel cached = RENDER_MODEL_CACHE.get(markdown);
            if (cached != null) {
                return cached;
            }
        }
        RenderModel model = buildRenderModel(context.getApplicationContext(), MarkdownMarkwonProvider.get(context), markdown);
        if (cacheable) {
            RENDER_MODEL_CACHE.put(markdown, model);
        }
        return model;
    }

    private void bindRenderModel(RenderModel renderModel) {
        long startMs = MarkdownRenderProfiler.now();
        for (BlockModel block : renderModel.blocks) {
            if (block.type == RenderBlock.TYPE_MARKDOWN) {
                SelectableMarkwonTextView textView = baseMarkwonTextView();
                renderBlocks.add(RenderBlock.markdown(block.start, block.end, textView, blockParams(2, 6), block.markdown, block.spanned, block.plainText));
            } else if (block.type == RenderBlock.TYPE_TABLE) {
                addTableBlock(block.start, block.end, block.tableBlock);
            } else if (block.type == RenderBlock.TYPE_STATIC) {
                addImageBlock(block.start, block.end, block.alt, block.url, block.spanned);
            }
        }
        MarkdownRenderProfiler.logStep("bindRenderModel", startMs);
    }

    private static RenderModel buildRenderModel(Context context, Markwon markwon, String markdown) {
        long startMs = MarkdownRenderProfiler.now();
        RenderModel model = new RenderModel(markdown, extractImageUrls(markdown));
        int lineStart = 0;
        while (lineStart < markdown.length()) {
            int lineEnd = markdown.indexOf('\n', lineStart);
            if (lineEnd < 0) {
                lineEnd = markdown.length();
            }
            int nextLineStart = lineEnd < markdown.length() ? lineEnd + 1 : lineEnd;
            String line = markdown.substring(lineStart, lineEnd);

            if (isTableStart(markdown, lineStart, lineEnd)) {
                int tableEnd = nextLineStart;
                List<String> tableLines = new ArrayList<>();
                tableLines.add(line);
                LineInfo separator = lineAt(markdown, nextLineStart);
                tableLines.add(separator.text);
                tableEnd = separator.nextStart;
                LineInfo row = lineAt(markdown, tableEnd);
                while (row != null && row.text.contains("|") && !row.text.trim().isEmpty()) {
                    tableLines.add(row.text);
                    tableEnd = row.nextStart;
                    row = lineAt(markdown, tableEnd);
                }
                model.blocks.add(BlockModel.table(lineStart, tableEnd, buildTableBlock(context, markwon, lineStart, tableLines)));
                lineStart = tableEnd;
                continue;
            }

            if (isImageOnly(line)) {
                Matcher matcher = IMAGE_PATTERN.matcher(line.trim());
                while (matcher.find()) {
                    String alt = matcher.group(1);
                    String url = matcher.group(2);
                    model.blocks.add(BlockModel.staticBlock(lineStart, nextLineStart, alt, url, markwon.toMarkdown("![" + escapeImageAltStatic(alt) + "](" + url + ")")));
                }
                lineStart = nextLineStart;
                continue;
            }

            if (line.trim().isEmpty()) {
                lineStart = nextLineStart;
                continue;
            }

            int blockEnd = findMarkdownBlockEnd(markdown, lineStart);
            addMarkdownBlockModel(model, markwon, markdown, lineStart, blockEnd);
            lineStart = blockEnd;
        }
        model.stats.finish(model.blocks, model.imageUrls.size());
        MarkdownRenderProfiler.logRenderSummary("buildRenderModel", startMs, markdown, model.stats);
        return model;
    }

    private static void addMarkdownBlockModel(RenderModel model, Markwon markwon, String markdown, int start, int end) {
        if (start < 0 || end <= start) {
            return;
        }
        int trimmedStart = start;
        int trimmedEnd = end;
        while (trimmedStart < trimmedEnd && Character.isWhitespace(markdown.charAt(trimmedStart))) {
            trimmedStart++;
        }
        while (trimmedEnd > trimmedStart && Character.isWhitespace(markdown.charAt(trimmedEnd - 1))) {
            trimmedEnd--;
        }
        if (trimmedEnd <= trimmedStart) {
            return;
        }
        String blockMarkdown = markdown.substring(trimmedStart, trimmedEnd);
        boolean plainText = isPlainTextMarkdown(blockMarkdown);
        model.blocks.add(BlockModel.markdown(trimmedStart, trimmedEnd, blockMarkdown, plainText ? null : markwon.toMarkdown(blockMarkdown), plainText));
    }

    /**
     * 从指定位置开始查找一个普通 Markdown 块的结束位置。
     *
     * <p>代码块、标题、列表、引用和普通段落的结束规则不同。该方法尽量把属于同一语义块的连续行
     * 合并渲染，减少 Markwon TextView 数量，同时在遇到表格或独占图片时停止。</p>
     *
     * @param markdown 完整 Markdown 文本。
     * @param start 当前块起始字符位置。
     * @return 当前普通 Markdown 块的结束字符位置。
     */
    private static int findMarkdownBlockEnd(String markdown, int start) {
        LineInfo first = lineAt(markdown, start);
        if (first == null) {
            return markdown.length();
        }
        String trimmed = first.text.trim();
        if (trimmed.startsWith("```")) {
            int cursor = first.nextStart;
            LineInfo line = lineAt(markdown, cursor);
            while (line != null) {
                cursor = line.nextStart;
                if (line.text.trim().startsWith("```")) {
                    break;
                }
                line = lineAt(markdown, cursor);
            }
            return cursor;
        }
        if (isSingleLineMarkdownBlock(first.text)) {
            return first.nextStart;
        }
        if (LIST_PATTERN.matcher(first.text).matches()) {
            return collectConsecutiveLines(markdown, first.nextStart, line -> LIST_PATTERN.matcher(line).matches());
        }
        if (trimmed.startsWith(">")) {
            return collectConsecutiveLines(markdown, first.nextStart, line -> line.trim().startsWith(">"));
        }

        int cursor = first.nextStart;
        LineInfo line = lineAt(markdown, cursor);
        while (line != null
                && !line.text.trim().isEmpty()
                && !isTableStart(markdown, cursor, cursor + line.text.length())
                && !isImageOnly(line.text)
                && !isSingleLineMarkdownBlock(line.text)
                && !line.text.trim().startsWith("```")
                && !LIST_PATTERN.matcher(line.text).matches()
                && !line.text.trim().startsWith(">")) {
            cursor = line.nextStart;
            line = lineAt(markdown, cursor);
        }
        return cursor;
    }

    /**
     * 收集满足条件的连续行。
     *
     * @param markdown 完整 Markdown 文本。
     * @param cursor 第一行的起始位置。
     * @param predicate 行匹配条件。
     * @return 第一行不满足条件时的位置，也就是连续块结束位置。
     */
    private static int collectConsecutiveLines(String markdown, int cursor, LinePredicate predicate) {
        LineInfo line = lineAt(markdown, cursor);
        while (line != null && predicate.matches(line.text)) {
            cursor = line.nextStart;
            line = lineAt(markdown, cursor);
        }
        return cursor;
    }

    /**
     * 判断一行是否天然独立成块。
     *
     * @param line 待判断的 Markdown 行。
     * @return true 表示标题或分割线等单行块。
     */
    private static boolean isSingleLineMarkdownBlock(String line) {
        String trimmed = line.trim();
        return HEADING_PATTERN.matcher(line).matches()
                || trimmed.matches("(-\\s*){3,}")
                || trimmed.matches("(\\*\\s*){3,}")
                || trimmed.matches("(_\\s*){3,}");
    }

    /**
     * 判断一个普通块是否可以按纯文本快速渲染。
     *
     * <p>该判断故意保守：只要包含 Markdown 控制符、HTML 或 URL，就继续交给 Markwon，避免破坏格式。
     * 纯文本块在打字机阶段可以直接 {@link TextView#setText(CharSequence)}，少一次 Markdown 前缀解析。</p>
     *
     * @param markdown 块级 Markdown 文本。
     * @return true 表示可以按纯文本处理。
     */
    private static boolean isPlainTextMarkdown(String markdown) {
        if (markdown.contains("://") || markdown.contains("www.")) {
            return false;
        }
        for (int i = 0; i < markdown.length(); i++) {
            char c = markdown.charAt(i);
            if (c == '#'
                    || c == '*'
                    || c == '_'
                    || c == '~'
                    || c == '`'
                    || c == '['
                    || c == ']'
                    || c == '!'
                    || c == '&'
                    || c == '<'
                    || c == '>'
                    || c == '|'
                    || c == '\\') {
                return false;
            }
        }
        return true;
    }

    /**
     * 按当前可见字符数刷新所有渲染块。
     *
     * <p>普通 Markdown 块会截取可见前缀重新交给 Markwon；表格块按行控制可见性；
     * 图片等静态块只有完整到达后才显示，避免半截 Markdown 造成错误图片请求。</p>
     *
     * @param visibleLength 当前允许展示到界面的字符位置。
     */
    private void updateVisibleLength(int visibleLength) {
        boolean visibleLengthDecreased = lastAppliedVisibleLength >= 0 && visibleLength < lastAppliedVisibleLength;
        for (RenderBlock block : renderBlocks) {
            if (visibleLength <= block.start) {
                setBlockVisibility(block, GONE);
                if (!visibleLengthDecreased) {
                    break;
                }
                continue;
            }
            if (block.type == RenderBlock.TYPE_MARKDOWN) {
                updateMarkdownBlock(block, visibleLength);
            } else if (block.type == RenderBlock.TYPE_TABLE) {
                updateTableBlock(block, visibleLength);
            } else {
                setBlockVisibility(block, visibleLength >= block.end ? VISIBLE : GONE);
            }
        }
        lastAppliedVisibleLength = visibleLength;
        setContentInteractionEnabled(!hasPendingTyping());
    }

    private void setBlockVisibility(RenderBlock block, int visibility) {
        if (block.lastVisibility == visibility) {
            return;
        }
        block.view.setVisibility(visibility);
        block.lastVisibility = visibility;
    }

    /**
     * 开启或关闭 Markdown 内容区交互。
     *
     * <p>打字机输出过程中禁用点击、长按、双击和选择，输出结束后恢复。这样可以避免用户点击尚未稳定的
     * 图片/链接，或在内容持续变化时进入选择态。</p>
     *
     * @param enabled true 表示允许交互。
     */
    private void setContentInteractionEnabled(boolean enabled) {
        setContentInteractionEnabled(enabled, false);
    }

    private void setContentInteractionEnabled(boolean enabled, boolean force) {
        if (!force && interactionEnabled == enabled) {
            return;
        }
        interactionEnabled = enabled;
        setEnabled(enabled);
        setLongClickable(enabled);
        for (RenderBlock block : renderBlocks) {
            setInteractionEnabledRecursive(block.view, enabled);
        }
    }

    /**
     * 递归设置渲染块内部所有子 View 的交互状态。
     *
     * @param view 当前要设置的 View。
     * @param enabled true 表示允许交互。
     */
    private void setInteractionEnabledRecursive(View view, boolean enabled) {
        view.setEnabled(enabled);
        view.setClickable(enabled);
        view.setLongClickable(enabled);
        if (view instanceof SelectableMarkwonTextView) {
            ((SelectableMarkwonTextView) view).setMarkdownInteractionEnabled(enabled);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                setInteractionEnabledRecursive(group.getChildAt(i), enabled);
            }
        }
    }

    /**
     * 更新普通 Markwon 文本块的可见内容。
     *
     * @param block 需要更新的 Markdown 类型渲染块。
     * @param visibleLength 当前全局可见字符位置。
     */
    private void updateMarkdownBlock(RenderBlock block, int visibleLength) {
        int end = Math.min(visibleLength, block.end);
        if (end <= block.start) {
            setBlockVisibility(block, GONE);
            return;
        }
        String markdown = currentFullMarkdown.substring(block.start, end);
        if (markdown.trim().isEmpty()) {
            setBlockVisibility(block, GONE);
            return;
        }
        setBlockVisibility(block, VISIBLE);
        if (!markdown.equals(block.lastRenderedMarkdown)) {
            if (block.plainText) {
                ((TextView) block.view).setText(markdown);
            } else if (end == block.end && block.preRenderedMarkdown != null) {
                markwon.setParsedMarkdown((TextView) block.view, block.preRenderedMarkdown);
            } else {
                markwon.setMarkdown((TextView) block.view, markdown);
            }
            block.lastRenderedMarkdown = markdown;
        }
    }

    /**
     * 更新表格块的行级显示状态。
     *
     * <p>表格整体 View 在表格开始输出后就显示，行会根据对应 Markdown 原文范围逐步出现。</p>
     *
     * @param block 表格类型渲染块。
     * @param visibleLength 当前全局可见字符位置。
     */
    private void updateTableBlock(RenderBlock block, int visibleLength) {
        if (visibleLength <= block.start) {
            setBlockVisibility(block, GONE);
            return;
        }
        setBlockVisibility(block, VISIBLE);
        MarkdownTableView table = block.tableView;
        if (table == null || block.tableBlock == null) {
            return;
        }
        int rowCount = block.tableBlock.rowRanges.size();
        int visibleRows = 0;
        for (int i = 0; i < rowCount; i++) {
            RowRange range = block.tableBlock.rowRanges.get(i);
            if (visibleLength > range.start) {
                visibleRows++;
            }
        }
        if (block.lastVisibleTableRows == visibleRows) {
            return;
        }
        table.setVisibleRowCount(visibleRows);
        block.lastVisibleTableRows = visibleRows;
    }

    /**
     * 创建一个表格渲染块。
     *
     * <p>表格内容仍使用 Markwon 渲染单元格 Markdown，因此单元格内可支持链接、粗体、代码等格式。
     * 外层包裹 HorizontalScrollView，使只有表格本身可以横向滑动，而整个 MarkdownView 不会横滑。</p>
     *
     * @param start 表格原文起始字符位置。
     * @param end 表格原文结束字符位置。
     * @param tableLines 表格所有原始行，包含分隔行。
     */
    private static TableBlock buildTableBlock(Context context, Markwon markwon, int start, List<String> tableLines) {
        List<List<Spanned>> renderedRows = new ArrayList<>();
        for (String tableLine : tableLines) {
            if (isTableSeparator(tableLine)) {
                continue;
            }
            List<String> cells = parseTableCells(tableLine);
            List<Spanned> renderedCells = new ArrayList<>();
            for (String cell : cells) {
                renderedCells.add(markwon.toMarkdown(normalizeTableCellMarkdown(cell)));
            }
            renderedRows.add(renderedCells);
        }
        return new TableBlock(tableLines, computeColumnWidthsDp(context, tableLines), buildTableRowRanges(start, tableLines), renderedRows);
    }

    private void addTableBlock(int start, int end, TableBlock tableBlock) {
        MarkdownTableView table = new MarkdownTableView(getContext(), tableBlock);

        HorizontalScrollView scrollView = new HorizontalScrollView(getContext());
        scrollView.setHorizontalScrollBarEnabled(true);
        scrollView.setOverScrollMode(OVER_SCROLL_IF_CONTENT_SCROLLS);
        scrollView.setFillViewport(false);
        scrollView.addView(table, new HorizontalScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        scrollView.setOnTouchListener((view, event) -> {
            view.getParent().requestDisallowInterceptTouchEvent(true);
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                view.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });
        renderBlocks.add(RenderBlock.table(start, end, scrollView, blockParams(8, 8), tableBlock, table));
    }

    /**
     * 创建独占图片渲染块。
     *
     * <p>当某一行只包含一张 Markdown 图片时，会使用卡片式布局展示图片和地址/alt 文案。点击卡片会
     * 弹出当前 Markdown 内所有图片地址，并标记当前点击的是第几张。</p>
     *
     * @param start 图片原文起始字符位置。
     * @param end 图片原文结束字符位置。
     * @param alt Markdown 图片 alt 文本。
     * @param url 图片地址。
     */
    private void addImageBlock(int start, int end, String alt, String url, Spanned imageMarkdown) {
        SelectableMarkwonTextView imageView = baseMarkwonTextView();
        imageView.setGravity(Gravity.START);
        markwon.setParsedMarkdown(imageView, imageMarkdown);
        renderBlocks.add(RenderBlock.staticBlock(start, end, imageView, blockParams(8, 8)));
    }

    /**
     * 创建用于渲染 Markwon 文本的基础 TextView。
     *
     * @return 已配置颜色、字号、行距和图片点击回调的 SelectableMarkwonTextView。
     */
    private SelectableMarkwonTextView baseMarkwonTextView() {
        SelectableMarkwonTextView textView = new SelectableMarkwonTextView(getContext(), this::handleImageClick, this::dispatchContentLongClick, this::dispatchTextDoubleClick);
        textView.setTextColor(MarkdownMarkwonProvider.TEXT_COLOR);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DP);
        textView.setLineSpacing(dp(3), 1f);
        textView.setIncludeFontPadding(true);
        return textView;
    }

    /**
     * 创建渲染块在 MarkdownView 内部使用的布局参数。
     *
     * @param topDp 顶部外边距 dp。
     * @param bottomDp 底部外边距 dp。
     * @return LinearLayout.LayoutParams。
     */
    private LinearLayout.LayoutParams blockParams(int topDp, int bottomDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(topDp), 0, dp(bottomDp));
        return params;
    }

    /**
     * 转义图片 alt 中会破坏 Markdown 图片语法的中括号。
     *
     * @param alt 原始 alt 文本。
     * @return 可安全拼回 Markdown 图片语法的 alt。
     */
    private String escapeImageAlt(String alt) {
        return escapeImageAltStatic(alt);
    }

    private static String escapeImageAltStatic(String alt) {
        return alt == null ? "" : alt.replace("[", "\\[").replace("]", "\\]");
    }

    /**
     * 判断数组形式的某一行是否为表格起始行。
     *
     * <p>当前主流程使用字符串位置版本；该方法保留用于后续如果改成按行数组解析时复用。</p>
     *
     * @param lines Markdown 行数组。
     * @param index 待判断行下标。
     * @return true 表示当前行和下一行构成表格头。
     */
    private static boolean isTableStart(String[] lines, int index) {
        return index + 1 < lines.length
                && index + 1 < lines.length - 1
                && lines[index].contains("|")
                && isTableSeparator(lines[index + 1]);
    }

    /**
     * 判断指定字符范围对应的行是否为表格起始行。
     *
     * @param markdown 完整 Markdown 文本。
     * @param lineStart 当前行起始位置。
     * @param lineEnd 当前行结束位置。
     * @return true 表示当前行包含管道符且下一行是表格分隔行。
     */
    private static boolean isTableStart(String markdown, int lineStart, int lineEnd) {
        String line = markdown.substring(lineStart, lineEnd);
        LineInfo nextLine = lineAt(markdown, lineEnd < markdown.length() ? lineEnd + 1 : lineEnd);
        LineInfo firstRow = nextLine == null ? null : lineAt(markdown, nextLine.nextStart);
        return line.contains("|")
                && nextLine != null
                && firstRow != null
                && isTableSeparator(nextLine.text);
    }

    /**
     * 获取指定字符位置开始的一行文本。
     *
     * @param markdown 完整 Markdown 文本。
     * @param start 行起始位置。
     * @return 行文本和下一行起始位置；start 越界时返回 null。
     */
    private static LineInfo lineAt(String markdown, int start) {
        if (start >= markdown.length()) {
            return null;
        }
        int end = markdown.indexOf('\n', start);
        if (end < 0) {
            end = markdown.length();
        }
        return new LineInfo(markdown.substring(start, end), end < markdown.length() ? end + 1 : end);
    }

    /**
     * 判断一行是否为 Markdown 表格分隔行。
     *
     * @param line 待判断行。
     * @return true 表示形如 {@code |---|:---:|---:|} 的分隔行。
     */
    private static boolean isTableSeparator(String line) {
        String normalized = trimTablePipe(line).trim();
        if (normalized.isEmpty() || !normalized.contains("-")) {
            return false;
        }
        String[] cells = normalized.split("\\|");
        for (String cell : cells) {
            if (!cell.trim().matches(":?-{2,}:?")) {
                return false;
            }
        }
        return true;
    }

    /**
     * 解析表格行中的单元格文本。
     *
     * @param line 原始表格行。
     * @return 去除首尾管道符并 trim 后的单元格列表。
     */
    private static List<String> parseTableCells(String line) {
        String[] parts = trimTablePipe(line).split("\\|", -1);
        List<String> cells = new ArrayList<>();
        for (String part : parts) {
            cells.add(part.trim());
        }
        return cells;
    }

    /**
     * 预估表格每一列的固定宽度。
     *
     * <p>列宽会在表格创建时一次性计算，避免打字机输出过程中随着单元格内容变化反复测量导致表格抖动。</p>
     *
     * @param tableLines 表格所有原始行。
     * @return 每一列对应的 px 宽度数组，变量名保留 Dp 是历史命名。
     */
    private static int[] computeColumnWidthsDp(Context context, List<String> tableLines) {
        int columnCount = 0;
        for (String line : tableLines) {
            if (!isTableSeparator(line)) {
                columnCount = Math.max(columnCount, parseTableCells(line).size());
            }
        }
        int[] widthsDp = new int[columnCount];
        TextPaint paint = new TextPaint();
        paint.setTextSize(dp(context, 14));
        int minWidth = dp(context, TABLE_MIN_COLUMN_WIDTH_DP);
        int maxWidth = dp(context, TABLE_MAX_COLUMN_WIDTH_DP);
        int horizontalPadding = dp(context, TABLE_CELL_HORIZONTAL_PADDING_DP * 2);
        for (String line : tableLines) {
            if (isTableSeparator(line)) {
                continue;
            }
            List<String> cells = parseTableCells(line);
            for (int i = 0; i < cells.size(); i++) {
                int textWidth = (int) Math.ceil(paint.measureText(plainCellText(normalizeTableCellMarkdown(cells.get(i)))));
                int preferredWidth = Math.max(minWidth, textWidth + horizontalPadding);
                widthsDp[i] = Math.max(widthsDp[i], Math.min(maxWidth, preferredWidth));
            }
        }
        return widthsDp;
    }

    /**
     * 归一化表格单元格 Markdown。
     *
     * <p>表格内主要支持 inline Markdown。若单元格里误放了标题语法，例如 {@code ## text ###}，
     * 这里会降级成普通 inline 文本，避免 Markwon 按块级标题渲染后把整行行高撑大。</p>
     *
     * @param cell 原始单元格文本。
     * @return 更适合表格单元格渲染的 Markdown。
     */
    private static String normalizeTableCellMarkdown(String cell) {
        String normalized = cell == null ? "" : cell.trim();
        Matcher headingMatcher = Pattern.compile("^#{1,6}\\s+(.+?)\\s*#*$").matcher(normalized);
        if (headingMatcher.matches()) {
            return headingMatcher.group(1).trim();
        }
        return normalized;
    }

    /**
     * 构建表格每个可见行对应的 Markdown 原文范围。
     *
     * @param tableStart 表格在完整 Markdown 中的起始字符位置。
     * @param tableLines 表格所有原始行。
     * @return 排除分隔行后的可见行范围列表。
     */
    private static List<RowRange> buildTableRowRanges(int tableStart, List<String> tableLines) {
        List<RowRange> ranges = new ArrayList<>();
        int cursor = tableStart;
        for (String line : tableLines) {
            int lineStart = cursor;
            int lineEnd = lineStart + line.length();
            if (!isTableSeparator(line)) {
                ranges.add(new RowRange(lineStart, lineEnd));
            }
            cursor = lineEnd + 1;
        }
        return ranges;
    }

    /**
     * 将单元格 Markdown 粗略转换成纯文本，用于宽度预估。
     *
     * <p>真实渲染仍由 Markwon 完成；这里仅去掉常见 Markdown 标记，避免宽度估算被语法字符放大。</p>
     *
     * @param markdown 单元格 Markdown 文本。
     * @return 去除常见 Markdown 标记后的文本。
     */
    private static String plainCellText(String markdown) {
        String text = markdown == null ? "" : markdown.trim();
        text = text.replaceAll("!\\[([^]]*)]\\(([^)]*)\\)", "$1");
        text = text.replaceAll("\\[([^]]+)]\\(([^)]*)\\)", "$1");
        text = text.replaceAll("`([^`]*)`", "$1");
        text = text.replace("**", "")
                .replace("__", "")
                .replace("~~", "")
                .replace("*", "")
                .replace("_", "");
        return text;
    }

    /**
     * 去除表格行首尾的管道符。
     *
     * @param line 原始表格行。
     * @return 去除首尾管道符后的内容。
     */
    private static String trimTablePipe(String line) {
        String value = line.trim();
        if (value.startsWith("|")) {
            value = value.substring(1);
        }
        if (value.endsWith("|")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    /**
     * 判断某行是否只包含一张 Markdown 图片。
     *
     * @param line 待判断行。
     * @return true 表示该行可以使用独占图片卡片渲染。
     */
    private static boolean isImageOnly(String line) {
        String trimmed = line.trim();
        Matcher matcher = IMAGE_PATTERN.matcher(trimmed);
        return matcher.find() && matcher.start() == 0 && matcher.end() == trimmed.length();
    }

    /**
     * 提取完整 Markdown 中所有图片地址。
     *
     * @param markdown 完整 Markdown 文本。
     * @return 按出现顺序排列的图片 URL 列表。
     */
    private static List<String> extractImageUrls(String markdown) {
        List<String> urls = new ArrayList<>();
        Matcher matcher = IMAGE_PATTERN.matcher(markdown == null ? "" : markdown);
        while (matcher.find()) {
            urls.add(matcher.group(2));
        }
        return urls;
    }

    /**
     * 展示图片地址弹窗。
     *
     * <p>如果传入的 clickedUrl 能在当前 Markdown 图片列表中找到，弹窗标题会显示当前点击的是第几张。</p>
     *
     * @param clickedUrl 当前点击的图片地址。
     */
    private void showImagesDialog(String clickedUrl) {
        if (!interactionEnabled) {
            return;
        }
        int clickedIndex = imageIndex(clickedUrl);
        String title = clickedIndex >= 0
                ? String.format(Locale.CHINA, "图片地址（第 %d/%d 张）", clickedIndex + 1, fullImageUrls.size())
                : "图片地址";
        String message = fullImageUrls.isEmpty() ? "当前 Markdown 没有图片地址" : buildImageMessage(clickedIndex);
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
    }

    void handleImageClick(View view, String clickedUrl) {
        if (!interactionEnabled) {
            return;
        }
        int clickedIndex = imageIndex(clickedUrl);
        if (interactionListener != null
                && interactionListener.onImageClick(this, view, clickedUrl, clickedIndex, new ArrayList<>(fullImageUrls))) {
            return;
        }
        showImagesDialog(clickedUrl);
    }

    /**
     * 查找图片地址在当前 Markdown 图片列表中的位置。
     *
     * @param clickedUrl 待查找的图片地址。
     * @return 从 0 开始的图片下标；找不到时返回 -1。
     */
    private int imageIndex(String clickedUrl) {
        if (clickedUrl == null || clickedUrl.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < fullImageUrls.size(); i++) {
            if (clickedUrl.equals(fullImageUrls.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 构建图片地址弹窗正文。
     *
     * @param clickedIndex 当前点击图片下标；-1 表示没有当前点击项。
     * @return 每行一张图片地址的弹窗文本。
     */
    private String buildImageMessage(int clickedIndex) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < fullImageUrls.size(); i++) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            if (i == clickedIndex) {
                builder.append("当前点击：");
            }
            builder.append(i + 1).append(". ").append(fullImageUrls.get(i));
        }
        return builder.toString();
    }

    /**
     * 展示链接点击弹窗。
     *
     * @param url 当前点击的链接地址。
     */
    private void showLinkDialog(String url) {
        if (!interactionEnabled) {
            return;
        }
        new AlertDialog.Builder(getContext())
                .setTitle("链接")
                .setMessage(url)
                .setPositiveButton("确定", null)
                .show();
    }

    void handleLinkClick(View view, String url) {
        if (!interactionEnabled) {
            return;
        }
        if (interactionListener != null && interactionListener.onLinkClick(this, view, url)) {
            return;
        }
        showLinkDialog(url);
    }

    private boolean dispatchContentLongClick(View view) {
        return interactionListener != null && interactionListener.onContentLongClick(this, view);
    }

    private boolean dispatchTextDoubleClick(TextView textView, String selectedText) {
        return interactionListener != null && interactionListener.onTextDoubleClick(this, textView, selectedText);
    }

    /**
     * 将 dp 转换成当前屏幕密度下的 px。
     *
     * @param value dp 值。
     * @return 四舍五入后的 px 值。
     */
    private int dp(int value) {
        return dp(getContext(), value);
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    /**
     * 支持 Markdown 点击与选择复制的 TextView。
     *
     * <p>该 TextView 开启系统文本选择，但只保留“复制”菜单；同时在单击时优先识别链接和图片 span。
     * 当父级 MarkdownView 正在输出时，会通过 {@link #setMarkdownInteractionEnabled(boolean)}
     * 禁用点击、长按和选择。</p>
     */
    private static final class SelectableMarkwonTextView extends TextView {
        private interface ImageClickAction {
            void onImageClick(View view, String url);
        }

        private interface LongClickAction {
            boolean onLongClick(View view);
        }

        private interface TextDoubleClickAction {
            boolean onDoubleClick(TextView textView, String selectedText);
        }

        private final Handler handler = new Handler(Looper.getMainLooper());
        private final ImageClickAction imageClickAction;
        private final LongClickAction longClickAction;
        private final TextDoubleClickAction textDoubleClickAction;
        private final Runnable longPressParentBlocker = () -> requestParentsDisallowTouch(true);
        private long downTime;
        private long lastUpTime;
        private float lastUpX;
        private float lastUpY;
        private boolean selectionModeActive;
        private boolean markdownInteractionEnabled = true;

        /**
         * 创建可选择的 Markwon TextView。
         *
         * @param context View 上下文。
         * @param imageClickAction 图片 span 点击时的回调。
         */
        SelectableMarkwonTextView(Context context, ImageClickAction imageClickAction, LongClickAction longClickAction, TextDoubleClickAction textDoubleClickAction) {
            super(context);
            this.imageClickAction = imageClickAction;
            this.longClickAction = longClickAction;
            this.textDoubleClickAction = textDoubleClickAction;
            setTextIsSelectable(true);
            setHighlightColor(Color.argb(90, 56, 139, 226));
            setCustomSelectionActionModeCallback(new CopyOnlyActionModeCallback(this));
        }

        /**
         * 处理触摸事件，协调链接/图片单击、长按选择以及父级 ListView 拦截。
         *
         * @param event 当前触摸事件。
         * @return true 表示事件已消费。
         */
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (!markdownInteractionEnabled) {
                handler.removeCallbacks(longPressParentBlocker);
                requestParentsDisallowTouch(false);
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                downTime = event.getEventTime();
                handler.postDelayed(longPressParentBlocker, ViewConfiguration.getLongPressTimeout());
            } else if (event.getAction() == MotionEvent.ACTION_UP
                    && event.getEventTime() - downTime < ViewConfiguration.getLongPressTimeout()) {
                if (isDoubleTap(event) && selectWordForDoubleTap(event)) {
                    handler.removeCallbacks(longPressParentBlocker);
                    lastUpTime = 0L;
                    requestParentsDisallowTouch(true);
                    return true;
                }
                if (openClickableContent(event)) {
                    handler.removeCallbacks(longPressParentBlocker);
                    requestParentsDisallowTouch(false);
                    rememberTap(event);
                    return true;
                }
                rememberTap(event);
            } else if (event.getAction() == MotionEvent.ACTION_MOVE
                    && (selectionModeActive || event.getEventTime() - downTime >= ViewConfiguration.getLongPressTimeout())) {
                requestParentsDisallowTouch(true);
            } else if (event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP) {
                handler.removeCallbacks(longPressParentBlocker);
                if (!selectionModeActive) {
                    requestParentsDisallowTouch(false);
                }
            }
            return super.onTouchEvent(event);
        }

        /**
         * 处理长按事件，先满足需求弹出“md长按”Toast，再进入系统文本选择流程。
         *
         * @return 父类长按处理结果。
         */
        @Override
        public boolean performLongClick() {
            if (!markdownInteractionEnabled) {
                return true;
            }
            if (longClickAction != null && longClickAction.onLongClick(this)) {
                return true;
            }
            Toast.makeText(getContext(), "md长按", Toast.LENGTH_SHORT).show();
            return super.performLongClick();
        }

        /**
         * 在短按时尝试打开点击位置上的链接或图片。
         *
         * @param event ACTION_UP 触摸事件。
         * @return true 表示命中了链接或图片并完成处理。
         */
        private boolean openClickableContent(MotionEvent event) {
            CharSequence text = getText();
            if (!(text instanceof Spanned) || getLayout() == null) {
                return false;
            }
            int x = (int) event.getX() - getTotalPaddingLeft() + getScrollX();
            int y = (int) event.getY() - getTotalPaddingTop() + getScrollY();
            if (y < 0 || y > getLayout().getHeight()) {
                return false;
            }
            int line = getLayout().getLineForVertical(y);
            int offset = getLayout().getOffsetForHorizontal(line, x);
            Spanned spanned = (Spanned) text;

            ClickableSpan[] links = spanned.getSpans(offset, offset, ClickableSpan.class);
            if (links.length > 0) {
                links[0].onClick(this);
                return true;
            }

            AsyncDrawableSpan[] images = spanned.getSpans(offset, offset, AsyncDrawableSpan.class);
            if (images.length > 0) {
                imageClickAction.onImageClick(this, images[0].getDrawable().getDestination());
                return true;
            }
            return false;
        }

        private boolean isDoubleTap(MotionEvent event) {
            long delta = event.getEventTime() - lastUpTime;
            if (delta <= 0 || delta > ViewConfiguration.getDoubleTapTimeout()) {
                return false;
            }
            int slop = ViewConfiguration.get(getContext()).getScaledDoubleTapSlop();
            float dx = event.getX() - lastUpX;
            float dy = event.getY() - lastUpY;
            return dx * dx + dy * dy <= slop * slop;
        }

        private void rememberTap(MotionEvent event) {
            lastUpTime = event.getEventTime();
            lastUpX = event.getX();
            lastUpY = event.getY();
        }

        private boolean selectWordForDoubleTap(MotionEvent event) {
            CharSequence text = getText();
            if (!(text instanceof Spannable) || getLayout() == null) {
                return false;
            }
            int offset = touchOffset(event);
            if (offset < 0 || offset >= text.length()) {
                return false;
            }
            int start = offset;
            int end = offset;
            while (start > 0 && !Character.isWhitespace(text.charAt(start - 1))) {
                start--;
            }
            while (end < text.length() && !Character.isWhitespace(text.charAt(end))) {
                end++;
            }
            if (end <= start) {
                return false;
            }
            Selection.setSelection((Spannable) text, start, end);
            setSelectionModeActive(true);
            String selected = text.subSequence(start, end).toString();
            if (textDoubleClickAction != null) {
                textDoubleClickAction.onDoubleClick(this, selected);
            }
            return true;
        }

        private int touchOffset(MotionEvent event) {
            int x = (int) event.getX() - getTotalPaddingLeft() + getScrollX();
            int y = (int) event.getY() - getTotalPaddingTop() + getScrollY();
            if (y < 0 || y > getLayout().getHeight()) {
                return -1;
            }
            int line = getLayout().getLineForVertical(y);
            return getLayout().getOffsetForHorizontal(line, x);
        }

        /**
         * 标记当前是否处于文本选择模式，并同步父容器事件拦截状态。
         *
         * @param active true 表示正在选择文本。
         */
        private void setSelectionModeActive(boolean active) {
            selectionModeActive = active;
            handler.removeCallbacks(longPressParentBlocker);
            requestParentsDisallowTouch(active);
        }

        /**
         * 开启或关闭 Markdown 文本交互。
         *
         * @param enabled true 表示允许点击、长按和选择。
         */
        private void setMarkdownInteractionEnabled(boolean enabled) {
            markdownInteractionEnabled = enabled;
            setTextIsSelectable(enabled);
            setFocusable(enabled);
            setFocusableInTouchMode(enabled);
            if (!enabled) {
                handler.removeCallbacks(longPressParentBlocker);
                selectionModeActive = false;
                requestParentsDisallowTouch(false);
                CharSequence text = getText();
                if (text instanceof Spannable) {
                    Selection.removeSelection((Spannable) text);
                }
            }
        }

        /**
         * 向所有父 View 请求是否禁止拦截触摸事件。
         *
         * <p>进入文本选择或表格内部横滑时，需要阻止 ListView 抢走手势。</p>
         *
         * @param disallow true 表示请求父容器不要拦截。
         */
        private void requestParentsDisallowTouch(boolean disallow) {
            View view = this;
            while (view.getParent() != null) {
                view.getParent().requestDisallowInterceptTouchEvent(disallow);
                if (!(view.getParent() instanceof View)) {
                    break;
                }
                view = (View) view.getParent();
            }
        }
    }

    /**
     * 文本选择 ActionMode 回调。
     *
     * <p>系统默认选择菜单可能包含全选、分享、网页搜索等选项；当前需求只保留“复制”，并在复制时直接
     * 使用 TextView 已渲染的纯文本内容，因此复制结果不包含 Markdown 标记。</p>
     */
    private static final class CopyOnlyActionModeCallback implements ActionMode.Callback {
        private final TextView textView;

        /**
         * 创建只保留复制菜单的 ActionMode 回调。
         *
         * @param textView 正在选择文本的 TextView。
         */
        CopyOnlyActionModeCallback(TextView textView) {
            this.textView = textView;
        }

        /**
         * ActionMode 创建时进入选择态并重置菜单。
         *
         * @param mode 系统 ActionMode。
         * @param menu 系统菜单。
         * @return true 表示允许显示 ActionMode。
         */
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            if (textView instanceof SelectableMarkwonTextView) {
                ((SelectableMarkwonTextView) textView).setSelectionModeActive(true);
            }
            resetMenu(menu);
            return true;
        }

        /**
         * ActionMode 准备显示前再次清理菜单，防止系统或 ROM 注入额外选项。
         *
         * @param mode 系统 ActionMode。
         * @param menu 系统菜单。
         * @return true 表示菜单已更新。
         */
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            resetMenu(menu);
            return true;
        }

        /**
         * 处理复制菜单点击。
         *
         * @param mode 当前 ActionMode。
         * @param item 被点击的菜单项。
         * @return true 表示复制动作已处理。
         */
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() != android.R.id.copy) {
                return false;
            }
            int start = Math.max(0, Math.min(textView.getSelectionStart(), textView.getSelectionEnd()));
            int end = Math.max(0, Math.max(textView.getSelectionStart(), textView.getSelectionEnd()));
            if (end > start) {
                String selected = textView.getText().subSequence(start, end).toString();
                ClipboardManager clipboardManager = (ClipboardManager) textView.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                clipboardManager.setPrimaryClip(ClipData.newPlainText("markdown", selected));
                Toast.makeText(textView.getContext(), "已复制", Toast.LENGTH_SHORT).show();
            }
            if (textView.getText() instanceof Spannable) {
                Selection.removeSelection((Spannable) textView.getText());
            }
            mode.finish();
            return true;
        }

        /**
         * ActionMode 销毁时退出选择态并恢复父容器事件拦截。
         *
         * @param mode 当前 ActionMode。
         */
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            if (textView instanceof SelectableMarkwonTextView) {
                ((SelectableMarkwonTextView) textView).setSelectionModeActive(false);
            }
        }

        /**
         * 将系统选择菜单重置为单一“复制”选项。
         *
         * @param menu 需要重置的菜单。
         */
        private void resetMenu(Menu menu) {
            menu.clear();
            menu.add(0, android.R.id.copy, 0, "复制").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
    }

    /**
     * 表格渲染块的元数据。
     *
     * <p>保存原始行、预估列宽和每个可见行在 Markdown 原文中的字符范围，用于表格稳定布局和打字机
     * 行级显示。</p>
     */
    private static final class TableBlock {
        final List<String> lines;
        final int[] columnWidthsDp;
        final List<RowRange> rowRanges;
        final List<List<Spanned>> renderedRows;

        /**
         * 创建表格元数据。
         *
         * @param lines 表格原始行。
         * @param columnWidthsDp 每列固定宽度，单位实际为 px。
         * @param rowRanges 可见行对应的原文范围。
         */
        TableBlock(List<String> lines, int[] columnWidthsDp, List<RowRange> rowRanges, List<List<Spanned>> renderedRows) {
            this.lines = lines;
            this.columnWidthsDp = columnWidthsDp;
            this.rowRanges = rowRanges;
            this.renderedRows = renderedRows;
        }

        /**
         * 获取表格列数。
         *
         * @return 列宽数组长度。
         */
        int columnCount() {
            return columnWidthsDp.length;
        }
    }

    /**
     * 表格可见行在完整 Markdown 中的字符范围。
     */
    private static final class RowRange {
        final int start;
        final int end;

        /**
         * 创建行范围。
         *
         * @param start 行起始字符位置。
         * @param end 行结束字符位置。
         */
        RowRange(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    private static final class RenderModel {
        final String markdown;
        final List<String> imageUrls;
        final List<BlockModel> blocks = new ArrayList<>();
        final RenderStats stats = new RenderStats();

        RenderModel(String markdown, List<String> imageUrls) {
            this.markdown = markdown;
            this.imageUrls = imageUrls;
        }
    }

    private static final class BlockModel {
        final int type;
        final int start;
        final int end;
        final String markdown;
        final String alt;
        final String url;
        final Spanned spanned;
        final TableBlock tableBlock;
        final boolean plainText;

        private BlockModel(int type, int start, int end, String markdown, String alt, String url, Spanned spanned, TableBlock tableBlock, boolean plainText) {
            this.type = type;
            this.start = start;
            this.end = end;
            this.markdown = markdown;
            this.alt = alt;
            this.url = url;
            this.spanned = spanned;
            this.tableBlock = tableBlock;
            this.plainText = plainText;
        }

        static BlockModel markdown(int start, int end, String markdown, Spanned spanned, boolean plainText) {
            return new BlockModel(RenderBlock.TYPE_MARKDOWN, start, end, markdown, "", "", spanned, null, plainText);
        }

        static BlockModel staticBlock(int start, int end, String alt, String url, Spanned spanned) {
            return new BlockModel(RenderBlock.TYPE_STATIC, start, end, "", alt, url, spanned, null, false);
        }

        static BlockModel table(int start, int end, TableBlock tableBlock) {
            return new BlockModel(RenderBlock.TYPE_TABLE, start, end, "", "", "", null, tableBlock, false);
        }
    }

    static final class RenderStats {
        int blockCount;
        int viewCount;
        int tableCount;
        int tableCellCount;
        int imageCount;

        void finish(List<?> blocks, int imageCount) {
            this.blockCount = blocks.size();
            this.imageCount = imageCount;
            this.viewCount = 0;
            this.tableCount = 0;
            this.tableCellCount = 0;
            for (Object block : blocks) {
                if (block instanceof BlockModel) {
                    collect((BlockModel) block);
                } else if (block instanceof RenderBlock) {
                    collect((RenderBlock) block);
                }
            }
        }

        private void collect(BlockModel block) {
            if (block.type == RenderBlock.TYPE_TABLE && block.tableBlock != null) {
                tableCount++;
                int rowCount = block.tableBlock.rowRanges.size();
                int cellCount = rowCount * block.tableBlock.columnCount();
                tableCellCount += cellCount;
                viewCount += 2;
            } else {
                viewCount++;
            }
        }

        private void collect(RenderBlock block) {
            if (block.type == RenderBlock.TYPE_TABLE && block.tableBlock != null) {
                tableCount++;
                int rowCount = block.tableBlock.rowRanges.size();
                int cellCount = rowCount * block.tableBlock.columnCount();
                tableCellCount += cellCount;
                viewCount += 2;
            } else {
                viewCount++;
            }
        }
    }

    /**
     * 行匹配函数接口。
     *
     * <p>用于收集连续列表行、引用行等简单场景，避免为每种块类型重复写循环。</p>
     */
    private interface LinePredicate {
        /**
         * 判断当前行是否匹配。
         *
         * @param line 当前行文本。
         * @return true 表示该行属于正在收集的连续块。
         */
        boolean matches(String line);
    }

    /**
     * Canvas 绘制版 Markdown 表格。
     *
     * <p>相比 {@code TableLayout + TableRow + cell TextView}，该实现整张表格只有一个 View。
     * 行高、列宽和网格线由同一个 View 统一计算和绘制，可以显著减少长表格的 View 数量，并天然保证
     * 横线、竖线像真实表格一样对齐。</p>
     */
    private final class MarkdownTableView extends View {
        private final TableBlock tableBlock;
        private final TextPaint bodyPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint headerPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int horizontalPadding;
        private final int verticalPadding;
        private final int borderWidth;
        private StaticLayout[][] cellLayouts;
        private int[] rowHeights;
        private int[] columnLefts;
        private int tableWidth;
        private int tableHeight;
        private int visibleRowCount;

        MarkdownTableView(Context context, TableBlock tableBlock) {
            super(context);
            this.tableBlock = tableBlock;
            this.horizontalPadding = dp(TABLE_CELL_HORIZONTAL_PADDING_DP);
            this.verticalPadding = dp(TABLE_CELL_VERTICAL_PADDING_DP);
            this.borderWidth = dp(1);
            this.visibleRowCount = tableBlock.rowRanges.size();
            bodyPaint.setColor(MarkdownMarkwonProvider.TEXT_COLOR);
            bodyPaint.setTextSize(dp(14));
            headerPaint.set(bodyPaint);
            headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            headerPaint.setColor(Color.rgb(25, 36, 52));
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(borderWidth);
            borderPaint.setColor(TABLE_BORDER_COLOR);
            setClickable(true);
            setLongClickable(true);
            setOnLongClickListener(view -> {
                if (dispatchContentLongClick(view)) {
                    return true;
                }
                Toast.makeText(getContext(), "md长按", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        void setVisibleRowCount(int visibleRowCount) {
            int normalized = Math.max(0, Math.min(visibleRowCount, tableBlock.rowRanges.size()));
            if (this.visibleRowCount == normalized) {
                return;
            }
            this.visibleRowCount = normalized;
            requestLayout();
            invalidate();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            ensureLayouts();
            int height = 0;
            int rows = Math.min(visibleRowCount, rowHeights.length);
            for (int i = 0; i < rows; i++) {
                height += rowHeights[i];
            }
            tableHeight = height;
            setMeasuredDimension(tableWidth, resolveSize(height, heightMeasureSpec));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            ensureLayouts();
            int rows = Math.min(visibleRowCount, rowHeights.length);
            if (rows <= 0 || tableWidth <= 0) {
                return;
            }
            int y = 0;
            for (int row = 0; row < rows; row++) {
                int rowHeight = rowHeights[row];
                backgroundPaint.setColor(row == 0 ? TABLE_HEADER_COLOR : row % 2 == 0 ? Color.WHITE : TABLE_ROW_ALT_COLOR);
                canvas.drawRect(0, y, tableWidth, y + rowHeight, backgroundPaint);
                drawRowText(canvas, row, y, rowHeight);
                y += rowHeight;
            }
            drawGrid(canvas, rows, y);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (!interactionEnabled || !isEnabled()) {
                return false;
            }
            if (event.getAction() == MotionEvent.ACTION_UP && handleCellSpanClick(event.getX(), event.getY())) {
                return true;
            }
            return super.onTouchEvent(event);
        }

        private void ensureLayouts() {
            if (cellLayouts != null) {
                return;
            }
            int rowCount = tableBlock.renderedRows.size();
            int columnCount = tableBlock.columnCount();
            cellLayouts = new StaticLayout[rowCount][columnCount];
            rowHeights = new int[rowCount];
            columnLefts = new int[columnCount + 1];
            tableWidth = 0;
            for (int column = 0; column < columnCount; column++) {
                columnLefts[column] = tableWidth;
                tableWidth += tableBlock.columnWidthsDp[column];
            }
            columnLefts[columnCount] = tableWidth;
            int minRowHeight = dp(34);
            for (int row = 0; row < rowCount; row++) {
                int rowHeight = minRowHeight;
                for (int column = 0; column < columnCount; column++) {
                    CharSequence text = cellText(row, column);
                    int contentWidth = Math.max(1, tableBlock.columnWidthsDp[column] - horizontalPadding * 2);
                    StaticLayout layout = createCellLayout(text, row == 0 ? headerPaint : bodyPaint, contentWidth);
                    cellLayouts[row][column] = layout;
                    rowHeight = Math.max(rowHeight, layout.getHeight() + verticalPadding * 2);
                }
                rowHeights[row] = rowHeight;
            }
        }

        private StaticLayout createCellLayout(CharSequence text, TextPaint paint, int width) {
            return StaticLayout.Builder.obtain(text, 0, text.length(), paint, width)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setIncludePad(false)
                    .setLineSpacing(0f, 1f)
                    .build();
        }

        private CharSequence cellText(int row, int column) {
            if (row >= tableBlock.renderedRows.size()) {
                return "";
            }
            List<Spanned> rowCells = tableBlock.renderedRows.get(row);
            if (column >= rowCells.size()) {
                return "";
            }
            return rowCells.get(column);
        }

        private void drawRowText(Canvas canvas, int row, int rowTop, int rowHeight) {
            for (int column = 0; column < tableBlock.columnCount(); column++) {
                StaticLayout layout = cellLayouts[row][column];
                int left = columnLefts[column] + horizontalPadding;
                int top = rowTop + Math.max(verticalPadding, (rowHeight - layout.getHeight()) / 2);
                canvas.save();
                canvas.translate(left, top);
                layout.draw(canvas);
                canvas.restore();
            }
        }

        private void drawGrid(Canvas canvas, int rows, int drawnHeight) {
            float half = borderWidth / 2f;
            float left = half;
            float right = tableWidth - half;
            float top = half;
            float bottom = Math.max(half, drawnHeight - half);
            canvas.drawLine(left, top, right, top, borderPaint);
            int y = 0;
            for (int row = 0; row < rows; row++) {
                y += rowHeights[row];
                float lineY = Math.min(y - half, bottom);
                canvas.drawLine(left, lineY, right, lineY, borderPaint);
            }
            for (int column = 0; column < columnLefts.length; column++) {
                float x = column == 0 ? half : columnLefts[column];
                if (column == columnLefts.length - 1) {
                    x = tableWidth - half;
                }
                canvas.drawLine(x, top, x, bottom, borderPaint);
            }
        }

        private boolean handleCellSpanClick(float x, float y) {
            ensureLayouts();
            int row = rowAt(y);
            int column = columnAt(x);
            if (row < 0 || column < 0 || row >= cellLayouts.length || column >= cellLayouts[row].length) {
                return false;
            }
            StaticLayout layout = cellLayouts[row][column];
            float localX = x - columnLefts[column] - horizontalPadding;
            float localY = y - rowTop(row) - Math.max(verticalPadding, (rowHeights[row] - layout.getHeight()) / 2);
            if (localX < 0 || localY < 0 || localY > layout.getHeight()) {
                return false;
            }
            int line = layout.getLineForVertical((int) localY);
            int offset = layout.getOffsetForHorizontal(line, localX);
            CharSequence text = layout.getText();
            if (!(text instanceof Spanned)) {
                return false;
            }
            Spanned spanned = (Spanned) text;
            AsyncDrawableSpan[] images = spanned.getSpans(offset, offset, AsyncDrawableSpan.class);
            if (images.length > 0) {
                handleImageClick(this, images[0].getDrawable().getDestination());
                return true;
            }
            ClickableSpan[] links = spanned.getSpans(offset, offset, ClickableSpan.class);
            if (links.length > 0) {
                links[0].onClick(this);
                return true;
            }
            return false;
        }

        private int rowAt(float y) {
            int top = 0;
            int rows = Math.min(visibleRowCount, rowHeights.length);
            for (int row = 0; row < rows; row++) {
                int bottom = top + rowHeights[row];
                if (y >= top && y < bottom) {
                    return row;
                }
                top = bottom;
            }
            return -1;
        }

        private int rowTop(int targetRow) {
            int top = 0;
            for (int row = 0; row < targetRow && row < rowHeights.length; row++) {
                top += rowHeights[row];
            }
            return top;
        }

        private int columnAt(float x) {
            for (int column = 0; column < tableBlock.columnCount(); column++) {
                if (x >= columnLefts[column] && x < columnLefts[column + 1]) {
                    return column;
                }
            }
            return -1;
        }
    }

    /**
     * 单行文本及下一行起点信息。
     */
    private static final class LineInfo {
        final String text;
        final int nextStart;

        /**
         * 创建行信息。
         *
         * @param text 当前行文本，不包含换行符。
         * @param nextStart 下一行起始字符位置，或文本末尾位置。
         */
        LineInfo(String text, int nextStart) {
            this.text = text;
            this.nextStart = nextStart;
        }
    }

    /**
     * MarkdownView 内部渲染块。
     *
     * <p>每个块记录自己在完整 Markdown 中的字符范围、对应 View 和块类型。打字机推进时会根据
     * typedLength 判断块是否显示，以及普通 Markdown 块需要渲染到哪个前缀。</p>
     */
    private static final class RenderBlock {
        static final int TYPE_MARKDOWN = 1;
        static final int TYPE_STATIC = 2;
        static final int TYPE_TABLE = 3;

        final int type;
        final int start;
        final int end;
        final View view;
        final LinearLayout.LayoutParams params;
        final TableBlock tableBlock;
        final MarkdownTableView tableView;
        final String sourceMarkdown;
        final Spanned preRenderedMarkdown;
        final boolean plainText;
        String lastRenderedMarkdown = "";
        int lastVisibility = Integer.MIN_VALUE;
        int lastVisibleTableRows = -1;

        /**
         * 创建非表格渲染块。
         *
         * @param type 块类型。
         * @param start 原文起始字符位置。
         * @param end 原文结束字符位置。
         * @param view 块对应的 View。
         * @param params 块在 MarkdownView 中的布局参数。
         */
        private RenderBlock(int type, int start, int end, View view, LinearLayout.LayoutParams params) {
            this(type, start, end, view, params, null, null, "", null, false);
        }

        /**
         * 创建完整渲染块。
         *
         * @param type 块类型。
         * @param start 原文起始字符位置。
         * @param end 原文结束字符位置。
         * @param view 块对应的 View。
         * @param params 块在 MarkdownView 中的布局参数。
         * @param tableBlock 表格元数据；非表格块为 null。
         */
        private RenderBlock(int type, int start, int end, View view, LinearLayout.LayoutParams params, TableBlock tableBlock) {
            this(type, start, end, view, params, tableBlock, null, "", null, false);
        }

        private RenderBlock(int type, int start, int end, View view, LinearLayout.LayoutParams params, TableBlock tableBlock, MarkdownTableView tableView, String sourceMarkdown, Spanned preRenderedMarkdown, boolean plainText) {
            this.type = type;
            this.start = start;
            this.end = end;
            this.view = view;
            this.params = params;
            this.tableBlock = tableBlock;
            this.tableView = tableView;
            this.sourceMarkdown = sourceMarkdown;
            this.preRenderedMarkdown = preRenderedMarkdown;
            this.plainText = plainText;
        }

        /**
         * 创建普通 Markdown 文本块。
         *
         * @param start 原文起始字符位置。
         * @param end 原文结束字符位置。
         * @param view Markwon 文本 View。
         * @param params 布局参数。
         * @return Markdown 类型 RenderBlock。
         */
        static RenderBlock markdown(int start, int end, View view, LinearLayout.LayoutParams params) {
            return new RenderBlock(TYPE_MARKDOWN, start, end, view, params);
        }

        static RenderBlock markdown(int start, int end, View view, LinearLayout.LayoutParams params, String markdown, Spanned spanned, boolean plainText) {
            return new RenderBlock(TYPE_MARKDOWN, start, end, view, params, null, null, markdown, spanned, plainText);
        }

        /**
         * 创建静态块，例如独占图片卡片。
         *
         * @param start 原文起始字符位置。
         * @param end 原文结束字符位置。
         * @param view 静态块 View。
         * @param params 布局参数。
         * @return 静态类型 RenderBlock。
         */
        static RenderBlock staticBlock(int start, int end, View view, LinearLayout.LayoutParams params) {
            return new RenderBlock(TYPE_STATIC, start, end, view, params);
        }

        /**
         * 创建表格块。
         *
         * @param start 原文起始字符位置。
         * @param end 原文结束字符位置。
         * @param view 表格横滑容器 View。
         * @param params 布局参数。
         * @param tableBlock 表格元数据。
         * @return 表格类型 RenderBlock。
         */
        static RenderBlock table(int start, int end, View view, LinearLayout.LayoutParams params, TableBlock tableBlock, MarkdownTableView tableView) {
            return new RenderBlock(TYPE_TABLE, start, end, view, params, tableBlock, tableView, "", null, false);
        }
    }
}
