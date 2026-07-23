# MarkdownView 组件使用文档

## 1. 组件定位

`MarkdownView` 是一个面向 AI 对话场景的自定义 Markdown 渲染组件，位于：

`app/src/main/java/com/example/myapplication/MarkdownView.java`

它负责把 AI 流式返回的 Markdown 文本渲染成 Android 原生 View，并提供以下能力：

- 常见 Markdown 格式渲染：标题、段落、粗体、斜体、删除线、引用、列表、任务列表、代码块、HTML、链接、图片等。
- 表格渲染：表格使用独立 `HorizontalScrollView`，只有表格可以横向滑动，整体 MarkdownView 不横滑。
- 表格单元格 Markdown：单元格内容仍交给 Markwon 渲染，可支持链接、代码、加粗等常见格式。
- 图片点击：点击图片后弹窗展示当前 Markdown 内所有图片地址，并标记点击的是第几张。
- 链接点击：点击链接后弹窗展示链接地址。
- 长按：长按 Markdown 内容弹出 `md长按` Toast。
- 文本选择复制：支持跨行选择普通 Markdown 文本，复制内容来自渲染后的纯文本，不包含 Markdown 标记。
- 打字机效果：流式展示时按 20ms/字符折算输出速度，UI 层按帧合并刷新，减少 TextView 重排。
- 输出中禁用交互：打字机输出过程中禁用点击、长按、双击、文本选择等操作。

## 2. 项目结构

核心类如下：

- `MainActivity`
  - 管理 `ListView` 聊天界面。
  - 从 `assets/mock` 读取 Markdown mock 数据。
  - 模拟“用户发送 -> AI 流式响应 -> 当前回复完成 -> 下一条会话”的流程。
  - 负责可见 item 局部刷新和打字机撑高时的底部跟随。

- `MarkdownView`
  - Markdown 渲染核心组件。
  - 持有 Markwon 实例。
  - 提供直接展示和流式追加接口。

- `MarkdownTypingController`
  - 负责打字机时间调度。
  - 按 20ms/字符计算输出进度，并将多个字符合并到同一 UI 帧刷新。
  - 不直接操作 View，方便后续替换打字机策略。

- `MockMarkdownStreamer`
  - 模拟真实网络流式连接。
  - 每隔 50ms 到 1000ms 随机推送一个 Markdown 分片。
  - 推送完成后通过 `onComplete()` 模拟连接断开。

- `PrismBundleConfig`
  - Prism4j 代码高亮语法配置。
  - 注解处理器会根据配置生成 `GrammarLocatorDef`。

## 3. 依赖说明

项目使用 Java 11，配置在：

`app/build.gradle`

主要 Markdown 相关依赖：

```gradle
implementation "io.noties.markwon:core:$markwonVersion"
implementation "io.noties.markwon:ext-strikethrough:$markwonVersion"
implementation "io.noties.markwon:ext-tables:$markwonVersion"
implementation "io.noties.markwon:ext-tasklist:$markwonVersion"
implementation "io.noties.markwon:html:$markwonVersion"
implementation "io.noties.markwon:image:$markwonVersion"
implementation "io.noties.markwon:linkify:$markwonVersion"
implementation "io.noties.markwon:syntax-highlight:$markwonVersion"
implementation "io.noties:prism4j:2.0.0"
compileOnly "io.noties:prism4j-bundler:2.0.0"
annotationProcessor "io.noties:prism4j-bundler:2.0.0"
implementation "com.caverock:androidsvg-aar:1.4"
```

如果要支持新的代码语言，需要在 `PrismBundleConfig` 的 `@PrismBundle(include = {...})` 中追加语言名，然后重新编译。

## 4. XML 接入方式

AI item 布局在：

`app/src/main/res/layout/item_chat_ai.xml`

核心写法：

```xml
<com.example.myapplication.MarkdownView
    android:id="@+id/aiMarkdownView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@drawable/bg_ai_bubble"
    android:paddingStart="12dp"
    android:paddingTop="10dp"
    android:paddingEnd="12dp"
    android:paddingBottom="10dp" />
```

注意：自定义 View 放到 XML 中时，必须提供 `MarkdownView(Context, AttributeSet)` 构造函数。当前组件已经支持 XML inflate。

## 5. MarkdownView API

### 5.1 直接展示完整 Markdown

```java
markdownView.showMarkdownImmediately(markdown);
```

适用场景：

- 历史消息。
- 离线预览。
- 不需要打字机效果的内容。

特点：

- 不启动打字机。
- 走完整展示快路径，直接使用完整 `Spanned` 和表格模型。
- 立即渲染完整 Markdown。
- 图片、链接、长按、选择复制等交互立即可用。

### 5.2 恢复流式内容和打字位置

```java
markdownView.appendMarkdownStreaming(receivedMarkdown, typedLength);
```

适用场景：

- `ListView` 或 `RecyclerView` item 被回收后重新绑定。
- 外部数据模型已经保存了“当前已接收内容”和“当前已输出字符数”。

参数说明：

- `receivedMarkdown`：当前流式连接已经收到的完整 Markdown。
- `typedLength`：当前界面已经通过打字机展示到的位置。

特点：

- 可以从指定位置继续打字。
- 不会因为 item 回收而从头播放。
- 适合列表场景，当前项目主要使用这个接口。

### 5.3 追加流式分片

```java
markdownView.appendMarkdownStreaming(chunk);
```

适用场景：

- View 直接接网络流回调。
- 不需要外层 Adapter 保存完整流式状态的简单页面。

特点：

- 每次调用追加一个新分片。
- 自动启动或继续打字机。
- 打字机输出过程中会禁用 Markdown 内容交互。

### 5.4 监听打字机状态

```java
markdownView.setTypingStateListener(new MarkdownView.TypingStateListener() {
    @Override
    public void onTypedLengthChanged(int typedLength) {
        message.typedLength = typedLength;
    }

    @Override
    public void onTypingIdle() {
        // 当前已接收内容已经全部展示完
    }
});
```

用途：

- 保存 `typedLength`，便于列表 item 复用后恢复。
- 判断当前 AI 回复是否真正展示完成。
- 配合列表实现“正文底部可见时自动跟随”。

### 5.5 查询当前状态

```java
int typedLength = markdownView.getTypedLength();
boolean pending = markdownView.hasPendingTyping();
```

`hasPendingTyping()` 返回 true 表示仍有已接收但未展示的内容。

## 6. 流式数据推荐接入方式

真实项目中的典型流程：

1. 用户发送右侧消息。
2. 立即插入一条空 AI 消息。
3. 建立 SSE、WebSocket 或 HTTP chunked 连接。
4. 每收到一个分片，追加到 AI 消息模型的 `receivedMarkdown`。
5. 如果该 AI item 当前可见，只刷新该可见 item。
6. 连接结束时标记 `streamComplete = true`。
7. 等 `typedLength` 追平 `receivedMarkdown.length()` 后，认为该条 AI 回复展示完成。

当前项目中的模拟实现：

```java
streamer.start(conversation.markdown, new MockMarkdownStreamer.Callback() {
    @Override
    public void onChunk(String chunk) {
        aiMessage.receivedMarkdown.append(chunk);
        updateAiMessageIfVisible(aiMessage);
    }

    @Override
    public void onComplete() {
        aiMessage.streamComplete = true;
        updateAiMessageIfVisible(aiMessage);
        maybeStartNextAfterTyping();
    }
});
```

注意：真实流式连接中，通常不能依赖“完整数据长度”判断结束，只能依赖连接结束、服务端 done 事件或业务结束包。本项目也按这个思路模拟，`onComplete()` 表示流结束。

## 7. ListView 绑定建议

当前需求要求使用 `ListView`，因此项目使用 `BaseAdapter` 和 ViewHolder。

AI item 绑定核心逻辑：

```java
holder.markdownView.appendMarkdownStreaming(
        message.receivedMarkdown.toString(),
        typedLength
);
```

为了避免高频刷新造成卡顿，当前实现只更新屏幕中可见的 AI item：

```java
adapter.updateVisibleAiView(listView, position);
```

如果 item 不可见，只更新数据模型；等用户滚动回来时，由 `getView()` 根据模型恢复。

## 8. 自动跟随策略

需求中要求：

当 item 的 Markdown 正文底部在屏幕内时，打字机撑高视图后，item 底部需要始终在屏幕中显示。

当前策略：

- 用户没有触摸或滚动列表时，允许自动跟随。
- 当前 AI item 的 MarkdownView 底部可见时，允许自动跟随。
- 打字机导致高度增加后，只滚动必要距离，让 MarkdownView 底部保持在可见区域内。
- 如果用户正在拖动或 fling，禁止自动滚动，避免打断用户操作。

对应核心方法：

- `findVisibleTypingAnchor()`
- `isMarkdownBottomVisible(...)`
- `keepMarkdownBottomVisible(...)`
- `isUserScrollingList()`

## 9. 表格渲染说明

表格没有直接使用一个整体 Markwon TextView，而是被解析成专用表格块：

- 外层：`HorizontalScrollView`
- 内层：`MarkdownTableView`
- 绘制：`Canvas + StaticLayout`

这样做的原因：

- 可以让表格独立横向滑动。
- 可以预计算列宽，减少输出时抖动。
- 可以按行控制打字机可见性。
- 单元格内容先使用 Markwon 转成 `Spanned`，再由 `StaticLayout` 绘制。
- 整张表格只有一个自定义 View，避免大量 `TableRow/TextView` 带来的测量和布局压力。
- 网格线由同一个 View 统一绘制，避免 cell 高度不一致时分隔线对不齐。

列宽会在表格块创建时一次性计算：

```java
computeColumnWidthsDp(tableLines)
```

这可以避免边输出边测量导致表格宽度反复变化。

## 10. 图片和链接交互

图片点击：

- 独占图片卡片点击会调用 `showImagesDialog(clickedUrl)`。
- 普通 Markwon 文本内的图片 span 点击也会调用同一逻辑。
- 弹窗标题会显示当前点击第几张，例如 `图片地址（第 2/5 张）`。

链接点击：

- Markwon 的默认链接行为被 `DialogLinkResolver` 接管。
- 点击链接后弹窗展示链接地址，不跳转浏览器。

输出中交互限制：

- 打字机正在输出时，图片点击、链接点击、长按、双击、文本选择都会被禁用。
- 输出完成后自动恢复。

## 11. 文本选择复制

`SelectableMarkwonTextView` 开启了系统文本选择能力：

```java
setTextIsSelectable(true);
```

并通过 `CopyOnlyActionModeCallback` 将系统菜单限制为只有“复制”。

复制内容来自 TextView 渲染后的文本，因此常见 Markdown 标记会被去掉，例如：

- `**加粗**` 复制为 `加粗`
- `` `code` `` 复制为 `code`
- `[标题](https://example.com)` 复制为 `标题`

表格当前使用 `MarkdownTableView` 绘制，支持链接和图片点击命中；表格内文本选择需要后续实现统一选择层，不能再依赖 Android 原生 TextView 选择能力。

## 12. 性能注意事项

当前实现已经做了以下优化：

- 不在每个打字机 tick 调用 `notifyDataSetChanged()`。
- 流式分片到达时，只更新可见 AI item。
- `MarkdownView` 只在完整已接收内容变化时重建渲染块。
- 普通 Markdown 块记录 `lastRenderedMarkdown`，相同内容不重复提交 Markwon。
- 表格列宽预计算，减少输出过程中的横向尺寸变化。
- 表格使用单 View 绘制，减少长表格的 View 数量。
- 纯文本块使用 `TextView#setText()` 快路径，减少打字机阶段的 Markdown 前缀解析。
- View detach 时移除打字机 Handler 回调。

仍需注意：

- Markdown 特别长且分片非常频繁时，反复重建 block 仍有成本。
- 如果真实业务中单条回复可能达到数万字，建议引入更细粒度的增量解析或分页渲染。
- 如果图片大量来自网络，需要补充图片加载缓存、占位图和失败态。

## 13. 接真实网络流的替换点

可以将 `MockMarkdownStreamer` 替换为真实流式数据源，例如：

```java
public interface MarkdownStreamClient {
    void start(String prompt, Callback callback);
    void cancel();

    interface Callback {
        void onChunk(String chunk);
        void onComplete();
        void onError(Throwable throwable);
    }
}
```

替换时保持以下原则：

- `onChunk` 只追加新分片，不直接判断完整长度。
- `onComplete` 或服务端 done 事件表示连接结束。
- 网络回调如果不在主线程，需要切回主线程再更新 `ListView` 和 `MarkdownView`。
- Activity 或 ViewModel 中保存 `receivedMarkdown`、`typedLength`、`streamComplete`。

## 14. 常见扩展

### 增加 Markdown 插件

在 `MarkdownView#createMarkwon()` 中追加 Markwon 插件。

### 增加代码语言

在 `PrismBundleConfig` 中追加语言：

```java
@PrismBundle(include = {
        "java",
        "kotlin",
        "rust"
})
```

然后重新编译。

### 修改打字机速度

当前速度在 `MarkdownTypingController` 中：

```java
private static final long CHAR_INTERVAL_MS = 20L;
private static final long FRAME_INTERVAL_MS = 48L;
private static final int MAX_CHARS_PER_FRAME = 3;
```

如果要改成每 10ms 或 30ms 一个字符，调整 `CHAR_INTERVAL_MS` 即可；如果要调整 UI 刷新频率，可以修改 `FRAME_INTERVAL_MS` 和 `MAX_CHARS_PER_FRAME`。

### 修改气泡样式

AI 和用户 item 的 XML 与背景 drawable 位于：

- `app/src/main/res/layout/item_chat_ai.xml`
- `app/src/main/res/layout/item_chat_user.xml`
- `app/src/main/res/drawable/bg_ai_bubble.xml`
- `app/src/main/res/drawable/bg_user_bubble.xml`

## 15. 当前边界

- 整体列表仍使用 `ListView`，未替换为 RecyclerView。
- 表格支持独立横滑，但表格内连续文本选择需要后续实现自定义选择层。
- Markdown 表格解析覆盖常见 pipe table，不是完整 CommonMark 表格解析器。
- 图片地址解析覆盖常见 `![alt](url)` 形式，复杂 title、转义括号等极端语法可继续增强。
