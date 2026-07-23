package com.example.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 对话演示页。
 *
 * <p>该页面保留需求指定的 {@link ListView} 作为消息容器，模拟“用户发送消息 -> 建立流式连接 ->
 * 接收 Markdown 分片 -> AI 气泡逐字输出 -> 当前回复完成后继续下一条 mock 会话”的完整链路。
 * 页面本身只负责会话编排、列表可见项刷新和自动跟随策略，Markdown 渲染与打字机节奏由
 * {@link MarkdownView} 自己维护，避免 Activity 持有过多 View 级状态。</p>
 */
public class MainActivity extends Activity {
    private static final boolean LOAD_ALL_IMMEDIATELY = true;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<ChatMessage> messages = new ArrayList<>();
    private final List<MockConversation> conversations = new ArrayList<>();
    private final MockMarkdownStreamer streamer = new MockMarkdownStreamer();
    private MarkdownPreloadController markdownPreloadController;
    private ListView listView;
    private ChatAdapter adapter;
    private int nextConversationIndex;
    private ChatMessage currentAiMessage;
    private boolean userTouchingList;
    private boolean listScrolling;

    /**
     * 初始化聊天列表、读取 assets/mock 下的 mock 数据，并启动第一轮模拟会话。
     *
     * @param savedInstanceState Activity 重建时由系统传入的状态，本示例未做额外恢复。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        listView = createChatListView();
        setContentView(listView);
        markdownPreloadController = new MarkdownPreloadController(this);
        loadMockConversations();
        if (LOAD_ALL_IMMEDIATELY) {
            loadAllConversationsImmediately();
            preloadImmediateMarkdown();
        }
        adapter = new ChatAdapter(this, messages);
        listView.setAdapter(adapter);
        if (!LOAD_ALL_IMMEDIATELY) {
            startNextConversation();
        }
    }

    /**
     * 页面销毁时停止 Activity 层延迟任务和 mock 流式推送，避免回调继续持有已销毁页面。
     */
    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        streamer.cancel();
        if (markdownPreloadController != null) {
            markdownPreloadController.shutdown();
        }
        super.onDestroy();
    }

    /**
     * 从 assets/mock 目录读取所有 .md 文件，并将每个文件包装成一轮用户问题与 AI 回复。
     *
     * <p>文件名会按数字顺序排序，例如 mock_2.md 会排在 mock_10.md 前面。读取失败时会生成
     * 一条错误 Markdown，方便界面仍能展示失败原因。</p>
     */
    private void loadMockConversations() {
        try {
            AssetManager assetManager = getAssets();
            String[] fileNames = assetManager.list("mock");
            if (fileNames == null) {
                return;
            }
            Arrays.sort(fileNames, Comparator.comparingInt(this::mockNumber));
            for (String fileName : fileNames) {
                if (!fileName.endsWith(".md")) {
                    continue;
                }
                String markdown = readAsset("mock/" + fileName);
                String title = firstHeading(markdown);
                conversations.add(new MockConversation(
                        "请基于 mock 数据生成：" + title,
                        fileName,
                        markdown,
                        "深度思考：正在分析 " + fileName + " 的结构、表格与交互元素。"));
            }
        } catch (Exception exception) {
            conversations.add(new MockConversation(
                    "加载 mock 数据",
                    "error.md",
                    "# Mock 加载失败\n\n" + exception.getMessage(),
                    "深度思考：assets/mock 读取异常。"));
        }
    }

    /**
     * 一次性把所有 mock 会话转换成聊天消息，并让 AI 消息直接持有完整 Markdown。
     *
     * <p>该模式用于压测历史消息列表和无打字机展示场景：数据会全部进入 Adapter，AI item 首次渲染后
     * 会缓存对应 View，后续回滑直接复用已渲染结果，避免重复解析同一段 Markdown。</p>
     */
    private void loadAllConversationsImmediately() {
        messages.clear();
        currentAiMessage = null;
        nextConversationIndex = conversations.size();
        for (MockConversation conversation : conversations) {
            messages.add(ChatMessage.user(conversation.userText));
            messages.add(ChatMessage.aiImmediate(conversation.fileName, conversation.thinking, conversation.markdown));
        }
    }

    /**
     * 后台预热完整展示消息的 Markdown 渲染模型。
     *
     * <p>预热不创建 View，只缓存 Markdown 分块、Spanned 和表格模型。这样首屏仍能快速进入，用户稍后
     * 滑到后续长消息时，主线程只需要把模型绑定成 View，减少边滑边解析 Markdown 的概率。</p>
     */
    private void preloadImmediateMarkdown() {
        List<String> preloadMarkdowns = new ArrayList<>();
        for (ChatMessage message : messages) {
            if (!message.isAi() || !message.immediateDisplay) {
                continue;
            }
            String markdown = message.receivedMarkdown.toString();
            preloadMarkdowns.add(markdown);
        }
        markdownPreloadController.enqueue(preloadMarkdowns);
        listView.post(() -> markdownPreloadController.startDelayed(300L));
    }

    /**
     * 根据用户触摸和 ListView 滚动状态暂停或恢复 Markdown 预热。
     *
     * <p>预热不直接操作 View，但会占用 CPU。滚动中暂停新预热任务，可以把更多时间片留给 ListView
     * 的 measure/layout/draw。</p>
     */
    private void updateMarkdownPreloadPaused() {
        if (markdownPreloadController != null) {
            markdownPreloadController.setPaused(isUserScrollingList());
        }
    }

    /**
     * 启动下一轮模拟对话。
     *
     * <p>该方法会先追加一条右侧用户消息，再追加一条左侧 AI 空消息，然后通过
     * {@link MockMarkdownStreamer} 随机分片推送 Markdown。收到分片后只刷新可见的 AI item，
     * 不对整个 ListView 频繁 {@code notifyDataSetChanged()}，降低长列表滚动时的抖动和重绑定成本。</p>
     */
    private void startNextConversation() {
        if (nextConversationIndex >= conversations.size()) {
            currentAiMessage = null;
            return;
        }
        MockConversation conversation = conversations.get(nextConversationIndex++);
        ChatMessage userMessage = ChatMessage.user(conversation.userText);
        ChatMessage aiMessage = ChatMessage.ai(conversation.fileName, conversation.thinking);
        currentAiMessage = aiMessage;
        messages.add(userMessage);
        messages.add(aiMessage);
        adapter.notifyDataSetChanged();
        listView.post(() -> listView.setSelection(messages.size() - 1));

        streamer.start(conversation.markdown, new MockMarkdownStreamer.Callback() {
            /**
             * 接收 mock 流式连接推送的 Markdown 分片。
             *
             * @param chunk 本次新增的 Markdown 内容。
             */
            @Override
            public void onChunk(String chunk) {
                if (currentAiMessage != aiMessage) {
                    return;
                }
                aiMessage.receivedMarkdown.append(chunk);
                updateAiMessageIfVisible(aiMessage);
            }

            /**
             * 接收 mock 流式连接结束信号。
             */
            @Override
            public void onComplete() {
                if (currentAiMessage != aiMessage) {
                    return;
                }
                aiMessage.streamComplete = true;
                updateAiMessageIfVisible(aiMessage);
                maybeStartNextAfterTyping();
            }
        });
    }

    /**
     * 在当前 AI 回复“流式连接已结束且打字机已追平”后，延迟启动下一轮 mock 会话。
     *
     * <p>真实项目中可在这里切换成业务侧的状态机，例如允许用户继续输入、关闭 loading 或写入数据库。</p>
     */
    private void maybeStartNextAfterTyping() {
        if (currentAiMessage == null || !currentAiMessage.streamComplete || currentAiMessage.hasTypingText()) {
            return;
        }
        currentAiMessage = null;
        handler.postDelayed(this::startNextConversation, 500);
    }

    /**
     * 按 UTF-8 读取指定 asset 文本文件。
     *
     * @param assetPath 相对于 assets 目录的路径。
     * @return 文件完整内容，行分隔统一为 {@code \n}。
     * @throws Exception 读取失败时向上抛出，由调用方决定如何展示错误。
     */
    private String readAsset(String assetPath) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (InputStream inputStream = getAssets().open(assetPath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(line);
            }
        }
        return builder.toString();
    }

    /**
     * 提取 Markdown 中第一个标题作为用户问题的摘要。
     *
     * @param markdown 原始 Markdown 文本。
     * @return 第一个以 # 开头的标题文本；没有标题时返回默认文案。
     */
    private String firstHeading(String markdown) {
        for (String line : markdown.split("\\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#")) {
                return trimmed.replaceFirst("^#+\\s*", "");
            }
        }
        return "Markdown 会话";
    }

    /**
     * 从 mock 文件名中提取数字序号，用于自然排序。
     *
     * @param name 文件名，例如 mock_12.md。
     * @return 文件名中的第一个数字；没有数字时返回 {@link Integer#MAX_VALUE} 使其排在末尾。
     */
    private int mockNumber(String name) {
        Matcher matcher = Pattern.compile("(\\d+)").matcher(name);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return Integer.MAX_VALUE;
    }

    /**
     * 创建并配置聊天 ListView。
     *
     * <p>这里集中处理列表背景、分割线、触摸状态和滚动状态。触摸/滚动状态会参与自动跟随逻辑：
     * 当用户正在拖动或 fling 时，AI 打字机撑高 item 不会强行移动列表。</p>
     *
     * @return 已完成基础样式和监听器配置的 ListView。
     */
    private ListView createChatListView() {
        ListView listView = new ListView(this);
        listView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        listView.setBackgroundColor(Color.rgb(244, 246, 250));
        listView.setClipToPadding(false);
        listView.setDivider(null);
        listView.setSelector(android.R.color.transparent);
        listView.setPadding(0, dp(12), 0, dp(16));
        listView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        listView.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                userTouchingList = true;
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                userTouchingList = false;
            }
            updateMarkdownPreloadPaused();
            return false;
        });
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            /**
             * 记录 ListView 当前滚动状态，用于控制打字机输出期间是否允许自动跟随。
             *
             * @param view 当前 ListView。
             * @param scrollState 最新滚动状态。
             */
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                listScrolling = scrollState != SCROLL_STATE_IDLE;
                updateMarkdownPreloadPaused();
            }

            /**
             * ListView 可见范围变化回调。
             *
             * <p>当前逻辑只需要滚动状态，不需要在每次可见范围变化时执行额外操作，因此方法体为空。</p>
             *
             * @param view 当前 ListView。
             * @param firstVisibleItem 第一个可见 item 的位置。
             * @param visibleItemCount 当前可见 item 数量。
             * @param totalItemCount 列表总 item 数量。
             */
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });
        return listView;
    }

    /**
     * 将 dp 转换为当前屏幕密度下的 px。
     *
     * @param value dp 值。
     * @return 四舍五入后的 px 值。
     */
    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    /**
     * 查找当前屏幕中“Markdown 正文底部可见且仍在打字”的 AI item。
     *
     * <p>只有满足该条件时，后续打字机导致 View 高度增长才会触发底部跟随。这样可避免用户已经翻到
     * 其它位置时，列表被正在输出的不可见 item 拉回去。</p>
     *
     * @return 可跟随的 item 锚点；没有符合条件的 item 或用户正在滚动时返回 null。
     */
    private TypingAnchor findVisibleTypingAnchor() {
        if (listView == null || adapter == null || isUserScrollingList()) {
            return null;
        }
        int firstVisible = listView.getFirstVisiblePosition();
        int contentBottom = listView.getHeight() - listView.getPaddingBottom();
        TypingAnchor anchor = null;
        for (int i = 0; i < listView.getChildCount(); i++) {
            int position = firstVisible + i;
            if (position < 0 || position >= messages.size()) {
                continue;
            }
            ChatMessage message = messages.get(position);
            if (!message.isAi() || !message.hasTypingText()) {
                continue;
            }
            MarkdownView markdownView = findMarkdownView(listView.getChildAt(i));
            if (markdownView != null && isMarkdownBottomVisible(markdownView, contentBottom)) {
                anchor = new TypingAnchor(position);
            }
        }
        return anchor;
    }

    /**
     * 判断 MarkdownView 的底部是否处于 ListView 当前可见内容区域内。
     *
     * @param markdownView 待检查的 MarkdownView。
     * @param contentBottom ListView 扣除底部 padding 后的可见底边。
     * @return true 表示正文底部在屏幕中，允许后续高度增长时做底部跟随。
     */
    private boolean isMarkdownBottomVisible(MarkdownView markdownView, int contentBottom) {
        Rect rect = markdownRectInList(markdownView);
        return rect.bottom >= listView.getPaddingTop() && rect.bottom <= contentBottom;
    }

    /**
     * 当正在输出的 MarkdownView 因新增文字变高时，让正文底部继续停留在屏幕底部以内。
     *
     * <p>该逻辑只在用户没有主动滚动列表时生效，并且只滚动必要的距离，不会每次都跳到 item 末尾。</p>
     *
     * @param position 需要保持底部可见的消息位置。
     */
    private void keepMarkdownBottomVisible(int position) {
        if (isUserScrollingList()) {
            return;
        }
        int childIndex = position - listView.getFirstVisiblePosition();
        if (childIndex < 0 || childIndex >= listView.getChildCount()) {
            return;
        }
        MarkdownView markdownView = findMarkdownView(listView.getChildAt(childIndex));
        if (markdownView == null) {
            return;
        }
        int contentBottom = listView.getHeight() - listView.getPaddingBottom();
        Rect rect = markdownRectInList(markdownView);
        if (rect.bottom > contentBottom) {
            listView.scrollListBy(rect.bottom - contentBottom);
        }
    }

    /**
     * 将 MarkdownView 自身坐标转换为 ListView 坐标，便于判断它与可见区域的关系。
     *
     * @param markdownView 待转换坐标的 MarkdownView。
     * @return MarkdownView 在 ListView 坐标系下的矩形。
     */
    private Rect markdownRectInList(MarkdownView markdownView) {
        Rect rect = new Rect(0, 0, markdownView.getWidth(), markdownView.getHeight());
        listView.offsetDescendantRectToMyCoords(markdownView, rect);
        return rect;
    }

    /**
     * 从 item 根 View 中递归查找 MarkdownView。
     *
     * <p>item 布局迁移到 XML 后，MarkdownView 位于多层容器内部，因此这里保留递归查找，
     * 避免上层逻辑依赖具体 XML 层级。</p>
     *
     * @param view item 根 View 或任意子 View。
     * @return 找到的 MarkdownView；不存在时返回 null。
     */
    private MarkdownView findMarkdownView(View view) {
        if (view instanceof MarkdownView) {
            return (MarkdownView) view;
        }
        if (!(view instanceof ViewGroup)) {
            return null;
        }
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            MarkdownView found = findMarkdownView(group.getChildAt(i));
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * 判断用户是否正在主动影响 ListView 滚动。
     *
     * @return true 表示手指按下、拖动或列表仍处于非 idle 滚动状态。
     */
    private boolean isUserScrollingList() {
        return userTouchingList || listScrolling;
    }

    /**
     * 接收 MarkdownView 的打字进度回调，并在适当时执行正文底部跟随。
     *
     * @param message 正在输出的 AI 消息模型。
     * @param typedLength MarkdownView 已经打出的字符数。
     */
    private void onAiTypingChanged(ChatMessage message, int typedLength) {
        message.typedLength = typedLength;
        if (!isUserScrollingList()) {
            int position = messages.indexOf(message);
            TypingAnchor anchor = findVisibleTypingAnchor();
            if (anchor != null && anchor.position == position) {
                listView.post(() -> keepMarkdownBottomVisible(position));
            }
        }
    }

    /**
     * 接收 MarkdownView 的打字空闲回调，用于判断是否可以进入下一条 mock 会话。
     *
     * @param message 当前完成打字检查的 AI 消息模型。
     */
    private void onAiTypingIdle(ChatMessage message) {
        message.typedLength = Math.min(message.typedLength, message.receivedMarkdown.length());
        maybeStartNextAfterTyping();
    }

    /**
     * 只更新当前屏幕中可见的指定 AI item。
     *
     * <p>流式分片到达频率较高，如果每个分片都全量刷新列表，会导致大量 View 重新绑定和滚动抖动。
     * 这里通过 position 找到可见 child 后直接重新绑定 holder。</p>
     *
     * @param message 收到新分片或状态变化的 AI 消息。
     */
    private void updateAiMessageIfVisible(ChatMessage message) {
        if (adapter == null || listView == null) {
            return;
        }
        int position = messages.indexOf(message);
        if (position >= 0) {
            adapter.updateVisibleAiView(listView, position);
        }
    }

    /**
     * ListView 的消息适配器。
     *
     * <p>适配器支持两种 item 类型：右侧用户消息和左侧 AI Markdown 消息。AI item 绑定时会把
     * {@link ChatMessage#receivedMarkdown} 和 {@link ChatMessage#typedLength} 一起交给
     * MarkdownView，使 recycled view 回到屏幕时能从正确的打字位置继续输出。</p>
     */
    private static final class ChatAdapter extends BaseAdapter {
        private static final int TYPE_USER = 0;
        private static final int TYPE_AI = 1;

        private final Context context;
        private final LayoutInflater inflater;
        private final List<ChatMessage> data;

        /**
         * 创建聊天适配器。
         *
         * @param context 页面上下文，用于 inflate XML 和回调 Activity。
         * @param data 消息数据源，外部追加消息后由适配器读取。
         */
        ChatAdapter(Context context, List<ChatMessage> data) {
            this.context = context;
            this.inflater = LayoutInflater.from(context);
            this.data = data;
        }

        /**
         * 返回当前消息数量。
         *
         * @return ListView 需要展示的 item 总数。
         */
        @Override
        public int getCount() {
            return data.size();
        }

        /**
         * 获取指定位置的消息模型。
         *
         * @param position 消息位置。
         * @return 对应的 ChatMessage。
         */
        @Override
        public ChatMessage getItem(int position) {
            return data.get(position);
        }

        /**
         * 返回 item id。
         *
         * <p>本示例没有稳定数据库 id，因此直接使用 position。</p>
         *
         * @param position 消息位置。
         * @return item id。
         */
        @Override
        public long getItemId(int position) {
            return position;
        }

        /**
         * 声明 ListView 中存在的 item 类型数量。
         *
         * @return 用户消息和 AI 消息两种类型。
         */
        @Override
        public int getViewTypeCount() {
            return 2;
        }

        /**
         * 根据消息类型返回对应的 ViewType，确保 ListView 不会把左右布局互相复用。
         *
         * @param position 消息位置。
         * @return {@link #TYPE_AI} 或 {@link #TYPE_USER}。
         */
        @Override
        public int getItemViewType(int position) {
            return getItem(position).isAi() ? TYPE_AI : TYPE_USER;
        }

        /**
         * 创建或复用 item，并绑定当前消息数据。
         *
         * <p>AI item 和用户 item 使用不同 XML。AI item 绑定会进入 {@link #bindAiHolder(AiHolder, ChatMessage)}
         * 来恢复 MarkdownView 的完整接收内容和当前打字位置。</p>
         *
         * @param position 当前 item 位置。
         * @param convertView ListView 传入的可复用 View。
         * @param parent ListView 父容器，用于正确 inflate layout params。
         * @return 完成绑定的 item View。
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ChatMessage message = getItem(position);
            if (message.isAi()) {
                AiHolder holder;
                if (message.immediateDisplay
                        && message.cachedAiHolder != null
                        && (message.cachedAiHolder.root.getParent() == null || message.cachedAiHolder.root == convertView)) {
                    holder = message.cachedAiHolder;
                    convertView = holder.root;
                } else if (message.immediateDisplay) {
                    holder = createAiHolder(parent);
                    message.cachedAiHolder = holder;
                    convertView = holder.root;
                    convertView.setTag(holder);
                } else if (convertView == null) {
                    holder = createAiHolder(parent);
                    convertView = holder.root;
                    convertView.setTag(holder);
                } else {
                    holder = (AiHolder) convertView.getTag();
                }
                bindAiHolder(holder, message);
                return convertView;
            }

            UserHolder holder;
            if (convertView == null) {
                holder = createUserHolder(parent);
                convertView = holder.root;
                convertView.setTag(holder);
            } else {
                holder = (UserHolder) convertView.getTag();
            }
            holder.content.setText(message.userText);
            return convertView;
        }

        /**
         * 更新屏幕中已经可见的某个 AI item。
         *
         * <p>当流式分片到达时，Activity 会调用该方法。如果目标 item 不在屏幕内，只更新数据模型；
         * 等它滚回屏幕时再由 getView 绑定最新内容。</p>
         *
         * @param listView 当前承载消息的 ListView。
         * @param position 需要更新的消息位置。
         * @return true 表示找到了可见 item 并完成绑定。
         */
        boolean updateVisibleAiView(ListView listView, int position) {
            if (position < 0 || position >= data.size()) {
                return false;
            }
            ChatMessage message = data.get(position);
            if (!message.isAi()) {
                return false;
            }
            int childIndex = position - listView.getFirstVisiblePosition();
            if (childIndex < 0 || childIndex >= listView.getChildCount()) {
                return false;
            }
            View child = listView.getChildAt(childIndex);
            Object tag = child.getTag();
            if (!(tag instanceof AiHolder)) {
                return false;
            }
            return bindAiHolder((AiHolder) tag, message);
        }

        /**
         * 绑定 AI item 的昵称、深度思考文本和 MarkdownView 内容。
         *
         * <p>为了避免同一数据状态下重复调用 MarkdownView，该方法会记录 holder 上一次绑定的消息、
         * 接收长度和打字长度。只有三者任一变化时才重新提交内容。</p>
         *
         * @param holder AI item 的 ViewHolder。
         * @param message AI 消息模型。
         * @return true 表示本次确实触发了 MarkdownView 更新。
         */
        private boolean bindAiHolder(AiHolder holder, ChatMessage message) {
            holder.nickname.setText(String.format(Locale.US, "AI 助手 · %s", message.fileName));
            holder.thinking.setText(message.thinking);
            if (holder.boundMessage != message) {
                holder.lastReceivedLength = -1;
                holder.lastTypedLength = -1;
            }
            if (message.immediateDisplay) {
                holder.markdownView.setTypingStateListener(null);
                if (holder.boundMessage != message || holder.lastReceivedLength != message.receivedMarkdown.length()) {
                    holder.markdownView.showMarkdownImmediately(message.receivedMarkdown.toString());
                    holder.boundMessage = message;
                    holder.lastReceivedLength = message.receivedMarkdown.length();
                    holder.lastTypedLength = message.receivedMarkdown.length();
                    return true;
                }
                return false;
            }
            int receivedLength = message.receivedMarkdown.length();
            int typedLength = Math.min(message.typedLength, receivedLength);
            if (holder.boundMessage != message
                    || holder.lastReceivedLength != receivedLength
                    || holder.lastTypedLength != typedLength) {
                holder.markdownView.setTypingStateListener(new MarkdownView.TypingStateListener() {
                    /**
                     * MarkdownView 每输出一个或多个字符后同步最新 typedLength 到消息模型。
                     *
                     * @param newTypedLength MarkdownView 当前已输出字符数。
                     */
                    @Override
                    public void onTypedLengthChanged(int newTypedLength) {
                        ChatAdapter.this.onTypingLengthChanged(message, newTypedLength);
                    }

                    /**
                     * MarkdownView 当前没有待输出内容时通知适配器。
                     */
                    @Override
                    public void onTypingIdle() {
                        ChatAdapter.this.onTypingIdle(message);
                    }
                });
                holder.markdownView.appendMarkdownStreaming(message.receivedMarkdown.toString(), typedLength);
                holder.boundMessage = message;
                holder.lastReceivedLength = receivedLength;
                holder.lastTypedLength = typedLength;
                return true;
            }
            return false;
        }

        /**
         * 将 MarkdownView 的打字长度变化转发给 Activity。
         *
         * @param message 正在输出的消息。
         * @param typedLength 最新已输出字符数。
         */
        private void onTypingLengthChanged(ChatMessage message, int typedLength) {
            message.typedLength = typedLength;
            if (context instanceof MainActivity) {
                ((MainActivity) context).onAiTypingChanged(message, typedLength);
            }
        }

        /**
         * 将 MarkdownView 打字空闲事件转发给 Activity。
         *
         * @param message 当前已经没有待输出字符的消息。
         */
        private void onTypingIdle(ChatMessage message) {
            if (context instanceof MainActivity) {
                ((MainActivity) context).onAiTypingIdle(message);
            }
        }

        /**
         * 从 XML 创建 AI item holder。
         *
         * @param parent ListView 父容器，用于获取正确的 LayoutParams。
         * @return 包含 AI item 关键控件引用的 holder。
         */
        private AiHolder createAiHolder(ViewGroup parent) {
            View root = inflater.inflate(R.layout.item_chat_ai, parent, false);
            AiHolder holder = new AiHolder();
            holder.root = root;
            holder.nickname = root.findViewById(R.id.aiNickname);
            holder.thinking = root.findViewById(R.id.aiThinking);
            holder.markdownView = root.findViewById(R.id.aiMarkdownView);
            return holder;
        }

        /**
         * 从 XML 创建用户 item holder。
         *
         * <p>用户气泡最大宽度仍根据屏幕宽度动态计算，XML 中的 maxWidth 仅作为编辑预览和兜底值。</p>
         *
         * @param parent ListView 父容器，用于获取正确的 LayoutParams。
         * @return 包含用户文本控件引用的 holder。
         */
        private UserHolder createUserHolder(ViewGroup parent) {
            View root = inflater.inflate(R.layout.item_chat_user, parent, false);
            TextView content = root.findViewById(R.id.userContent);
            content.setMaxWidth((int) (context.getResources().getDisplayMetrics().widthPixels * 0.72f));

            UserHolder holder = new UserHolder();
            holder.root = root;
            holder.content = content;
            return holder;
        }
    }

    /**
     * AI item 的 ViewHolder。
     *
     * <p>除控件引用外，还保存上一次绑定状态，避免流式分片期间同一可见 item 被重复无意义渲染。</p>
     */
    private static final class AiHolder {
        View root;
        TextView nickname;
        TextView thinking;
        MarkdownView markdownView;
        ChatMessage boundMessage;
        int lastReceivedLength = -1;
        int lastTypedLength = -1;
    }

    /**
     * 用户 item 的 ViewHolder。
     *
     * <p>右侧布局目前只有一段普通文本，因此 holder 只保存根 View 和内容 TextView。</p>
     */
    private static final class UserHolder {
        View root;
        TextView content;
    }

    /**
     * 自动跟随打字机输出时使用的列表锚点。
     *
     * <p>当前只需要记录 position，后续如果要做更精细的像素级恢复，可继续扩展偏移量字段。</p>
     */
    private static final class TypingAnchor {
        final int position;

        /**
         * 创建一个跟随锚点。
         *
         * @param position 正在输出且底部可见的消息位置。
         */
        TypingAnchor(int position) {
            this.position = position;
        }
    }

    /**
     * 聊天消息模型。
     *
     * <p>用户消息使用 {@link #userText}；AI 消息使用 {@link #fileName}、{@link #thinking}、
     * {@link #receivedMarkdown} 和 {@link #typedLength}。将“已接收长度”和“已打出长度”分离，
     * 可以模拟真实流式场景中网络数据到达快于界面打字机展示的情况。</p>
     */
    private static final class ChatMessage {
        final String userText;
        final String fileName;
        final StringBuilder receivedMarkdown = new StringBuilder();
        final String thinking;
        AiHolder cachedAiHolder;
        int typedLength;
        boolean streamComplete;
        boolean immediateDisplay;

        /**
         * 创建消息模型。
         *
         * @param userText 用户消息文本；AI 消息为空字符串。
         * @param fileName AI 消息来源 mock 文件名；用户消息为空字符串。
         * @param thinking AI 深度思考文案；用户消息为空字符串。
         */
        private ChatMessage(String userText, String fileName, String thinking) {
            this.userText = userText;
            this.fileName = fileName;
            this.thinking = thinking;
        }

        /**
         * 创建右侧用户消息。
         *
         * @param text 用户发送的文本。
         * @return 用户消息模型。
         */
        static ChatMessage user(String text) {
            return new ChatMessage(text, "", "");
        }

        /**
         * 创建左侧 AI 消息。
         *
         * @param fileName 当前 AI 回复对应的 mock 文件名。
         * @param thinking 深度思考文案。
         * @return AI 消息模型，Markdown 内容会在流式回调中逐步追加。
         */
        static ChatMessage ai(String fileName, String thinking) {
            return new ChatMessage("", fileName, thinking);
        }

        /**
         * 创建直接完整展示的 AI 消息。
         *
         * @param fileName 当前 AI 回复对应的 mock 文件名。
         * @param thinking 深度思考文案。
         * @param markdown 完整 Markdown 回复。
         * @return 已经持有完整 Markdown 且无需打字机的 AI 消息模型。
         */
        static ChatMessage aiImmediate(String fileName, String thinking, String markdown) {
            ChatMessage message = new ChatMessage("", fileName, thinking);
            message.receivedMarkdown.append(markdown == null ? "" : markdown);
            message.typedLength = message.receivedMarkdown.length();
            message.streamComplete = true;
            message.immediateDisplay = true;
            return message;
        }

        /**
         * 判断当前消息是否为 AI 消息。
         *
         * @return true 表示左侧 AI 消息；false 表示右侧用户消息。
         */
        boolean isAi() {
            return fileName != null && !fileName.isEmpty();
        }

        /**
         * 判断当前 AI 消息是否还有已接收但未打出的内容。
         *
         * @return true 表示打字机仍需继续输出。
         */
        boolean hasTypingText() {
            return typedLength < receivedMarkdown.length();
        }
    }

    /**
     * 从单个 mock Markdown 文件派生出的模拟会话。
     *
     * <p>每个会话包含一条用户问题、一份完整 AI Markdown 回复和一个深度思考展示文案。</p>
     */
    private static final class MockConversation {
        final String userText;
        final String fileName;
        final String markdown;
        final String thinking;

        /**
         * 创建 mock 会话对象。
         *
         * @param userText 右侧用户消息。
         * @param fileName mock 文件名。
         * @param markdown AI 完整 Markdown 回复。
         * @param thinking AI 深度思考文案。
         */
        MockConversation(String userText, String fileName, String markdown, String thinking) {
            this.userText = userText;
            this.fileName = fileName;
            this.markdown = markdown;
            this.thinking = thinking;
        }
    }
}
