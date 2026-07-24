package com.vivo.chat.weight.refresh.smartrefresh.api;

// Modified from SmartRefreshLayout 2.1.1 for the Vivo-prefixed AndroidX fork.

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.FloatRange;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;

import com.vivo.chat.weight.refresh.smartrefresh.constant.VRefreshState;
import com.vivo.chat.weight.refresh.smartrefresh.listener.VOnLoadMoreListener;
import com.vivo.chat.weight.refresh.smartrefresh.listener.VOnMultiListener;
import com.vivo.chat.weight.refresh.smartrefresh.listener.VOnRefreshListener;
import com.vivo.chat.weight.refresh.smartrefresh.listener.VOnRefreshLoadMoreListener;
import com.vivo.chat.weight.refresh.smartrefresh.listener.VScrollBoundaryDecider;

/**
 * 刷新布局
 * interface of the refresh layout
 * Created by scwang on 2017/5/26.
 */
@SuppressWarnings({"UnusedReturnValue", "SameParameterValue", "unused"})
public interface VRefreshLayout {

    /**
     * Set the Footer's height.
     * 设置 Footer 的高度
     * @param dp Density-independent Pixels 虚拟像素（px需要调用px2dp转换）
     * @return VRefreshLayout
     */
    VRefreshLayout setFooterHeight(float dp);

    /**
     * 设置 Footer 高度
     * @param px 像素
     * @return VRefreshLayout
     */
    VRefreshLayout setFooterHeightPx(int px);

    /**
     * Set the Header's height.
     * 设置 Header 高度
     * @param dp Density-independent Pixels 虚拟像素（px需要调用px2dp转换）
     * @return VRefreshLayout
     */
    VRefreshLayout setHeaderHeight(float dp);

    /**
     * 设置 Header 高度
     * @param px 像素
     * @return VRefreshLayout
     */
    VRefreshLayout setHeaderHeightPx(int px);

    /**
     * Set the Header's start offset（see srlHeaderInsetStart in the RepastPracticeActivity XML in demo-app for the practical application）.
     * 设置 Header 的起始偏移量（使用方法参考 demo-app 中的 RepastPracticeActivity xml 中的 srlHeaderInsetStart）
     * @param dp Density-independent Pixels 虚拟像素（px需要调用px2dp转换）
     * @return VRefreshLayout
     */
    VRefreshLayout setHeaderInsetStart(float dp);

    /**
     * Set the Header's start offset（see srlHeaderInsetStart in the RepastPracticeActivity XML in demo-app for the practical application）.
     * 设置 Header 起始偏移量（使用方法参考 demo-app 中的 RepastPracticeActivity xml 中的 srlHeaderInsetStart）
     * @param px 像素
     * @return VRefreshLayout
     */
    VRefreshLayout setHeaderInsetStartPx(int px);

    /**
     * Set the Footer's start offset.
     * 设置 Footer 起始偏移量（用处和 setHeaderInsetStart 一样）
     * @see VRefreshLayout#setHeaderInsetStart(float)
     * @param dp Density-independent Pixels 虚拟像素（px需要调用px2dp转换）
     * @return VRefreshLayout
     */
    VRefreshLayout setFooterInsetStart(float dp);

    /**
     * Set the Footer's start offset.
     * 设置 Footer 起始偏移量（用处和 setFooterInsetStartPx 一样）
     * @param px 像素
     * @return VRefreshLayout
     */
    VRefreshLayout setFooterInsetStartPx(int px);

    /**
     * Set the damping effect.
     * 显示拖动高度/真实拖动高度 比率（默认0.5，阻尼效果）
     * @param rate ratio = (The drag height of the view)/(The actual drag height of the finger)
     *             比率 = 视图拖动高度 / 手指拖动高度
     * @return VRefreshLayout
     */
    VRefreshLayout setDragRate(@FloatRange(from = 0, to = 1) float rate);

    /**
     * 设置开始下拉状态的触发比例。
     * Header 可见高度首次达到 HeaderHeight * rate 时进入
     * {@link VRefreshState#PullDownStarted}，同一次下拉只通知一次。
     *
     * @param rate 0 表示 Header 刚显示就触发，默认 1/3
     * @return VRefreshLayout
     */
    VRefreshLayout setHeaderStartTriggerRate(@FloatRange(from = 0, to = 1) float rate);

    /**
     * Set the ratio of the maximum height to drag header.
     * 设置下拉最大高度和Header高度的比率（将会影响可以下拉的最大高度）
     * @param rate ratio = (the maximum height to drag header)/(the height of header)
     *             比率 = 下拉最大高度 / Header的高度
     * @return VRefreshLayout
     */
    VRefreshLayout setHeaderMaxDragRate(@FloatRange(from = 1, to = 10) float rate);

    /**
     * Set the ratio of the maximum height to drag footer.
     * 设置上拉最大高度和Footer高度的比率（将会影响可以上拉的最大高度）
     * @param rate ratio = (the maximum height to drag footer)/(the height of footer)
     *             比率 = 下拉最大高度 / Footer的高度
     * @return VRefreshLayout
     */
    VRefreshLayout setFooterMaxDragRate(@FloatRange(from = 1, to = 10) float rate);

    /**
     * Set the ratio at which the refresh is triggered.
     * 设置 触发刷新距离 与 HeaderHeight 的比率
     * @param rate 触发刷新距离 与 HeaderHeight 的比率
     * @return VRefreshLayout
     */
    VRefreshLayout setHeaderTriggerRate(@FloatRange(from = 0, to = 1.0) float rate);

    /**
     * Set the ratio at which the load more is triggered.
     * 设置 触发加载距离 与 FooterHeight 的比率
     * @param rate 触发加载距离 与 FooterHeight 的比率
     * @return VRefreshLayout
     */
    VRefreshLayout setFooterTriggerRate(@FloatRange(from = 0, to = 1.0) float rate);

    /**
     * Set the rebound interpolator.
     * 设置回弹显示插值器 [放手时回弹动画,结束时收缩动画]
     * @param interpolator 动画插值器
     * @return VRefreshLayout
     */
    VRefreshLayout setReboundInterpolator(@NonNull Interpolator interpolator);

    /**
     * Set the duration of the rebound animation.
     * 设置回弹动画时长 [放手时回弹动画,结束时收缩动画]
     * @param duration 时长
     * @return VRefreshLayout
     */
    VRefreshLayout setReboundDuration(int duration);

    /**
     * Set the footer of VRefreshLayout.
     * 设置指定的 Footer
     * @param footer VRefreshFooter 刷新尾巴
     * @return VRefreshLayout
     */
    VRefreshLayout setRefreshFooter(@NonNull VRefreshFooter footer);

    /**
     * Set the footer of VRefreshLayout.
     * 设置指定的 Footer
     * @param footer VRefreshFooter 刷新尾巴
     * @param width the width in px, can use MATCH_PARENT and WRAP_CONTENT.
     *              宽度 可以使用 MATCH_PARENT, WRAP_CONTENT
     * @param height the height in px, can use MATCH_PARENT and WRAP_CONTENT.
     *               高度 可以使用 MATCH_PARENT, WRAP_CONTENT
     * @return VRefreshLayout
     */
    VRefreshLayout setRefreshFooter(@NonNull VRefreshFooter footer, int width, int height);

    /**
     * Set the header of VRefreshLayout.
     * 设置指定的 Header
     * @param header VRefreshHeader 刷新头
     * @return VRefreshLayout
     */
    VRefreshLayout setRefreshHeader(@NonNull VRefreshHeader header);

    /**
     * Set the header of VRefreshLayout.
     * 设置指定的 Header
     * @param header VRefreshHeader 刷新头
     * @param width the width in px, can use MATCH_PARENT and WRAP_CONTENT.
     *              宽度 可以使用 MATCH_PARENT, WRAP_CONTENT
     * @param height the height in px, can use MATCH_PARENT and WRAP_CONTENT.
     *               高度 可以使用 MATCH_PARENT, WRAP_CONTENT
     * @return VRefreshLayout
     */
    VRefreshLayout setRefreshHeader(@NonNull VRefreshHeader header, int width, int height);

    /**
     * Set the content of VRefreshLayout（Suitable for non-XML pages, not suitable for replacing empty layouts）。
     * 设置指定的 Content（适用于非XML页面，不适合用替换空布局）
     * @param content View 内容视图
     * @return VRefreshLayout
     */
    VRefreshLayout setRefreshContent(@NonNull View content);

    /**
     * Set the content of VRefreshLayout（Suitable for non-XML pages, not suitable for replacing empty layouts）.
     * 设置指定的 Content（适用于非XML页面，不适合用替换空布局）
     * @param content View 内容视图
     * @param width the width in px, can use MATCH_PARENT and WRAP_CONTENT.
     *              宽度 可以使用 MATCH_PARENT, WRAP_CONTENT
     * @param height the height in px, can use MATCH_PARENT and WRAP_CONTENT.
     *               高度 可以使用 MATCH_PARENT, WRAP_CONTENT
     * @return VRefreshLayout
     */
    VRefreshLayout setRefreshContent(@NonNull View content, int width, int height);

    /**
     * Whether to enable pull-down refresh (enabled by default).
     * 是否启用下拉刷新（默认启用）
     * @param enabled 是否启用
     * @return VRefreshLayout
     */
    VRefreshLayout setEnableRefresh(boolean enabled);

    /**
     * Set whether to enable pull-up loading more (enabled by default).
     * 设置是否启用上拉加载更多（默认启用）
     * @param enabled 是否启用
     * @return VRefreshLayout
     */
    VRefreshLayout setEnableLoadMore(boolean enabled);

    /**
     * Sets whether to listen for the list to trigger a load event when scrolling to the bottom (default true).
     * 设置是否监听列表在滚动到底部时触发加载事件（默认true）
     * @param enabled 是否启用
     * @return VRefreshLayout
     */
    VRefreshLayout setEnableAutoLoadMore(boolean enabled);

    /**
     * Set whether to pull down the content while pulling down the header.
     * 设置是否启在下拉 Header 的同时下拉内容
     * @param enabled 是否启用
     * @return VRefreshLayout
     */
    VRefreshLayout setEnableHeaderTranslationContent(boolean enabled);

    /**
     * Set whether to pull up the content while pulling up the header.
     * 设置是否启在上拉 Footer 的同时上拉内容
     * @param enabled 是否启用
     * @return VRefreshLayout
     */
    VRefreshLayout setEnableFooterTranslationContent(boolean enabled);

    /**
     * Set whether to enable cross-border rebound function.
     * 设置是否启用越界回弹
     * @param enabled 是否启用
     * @return VRefreshLayout
     */
    VRefreshLayout setEnableOverScrollBounce(boolean enabled);

    /**
     * Set whether to enable the pure scroll mode.
     * 设置是否开启纯滚动模式
     * @param enabled 是否启用
     * @return VRefreshLayout
     */
    VRefreshLayout setEnablePureScrollMode(boolean enabled);

    /**
     * Set whether to scroll the content to display new data after loading more complete.
     * 设置是否在加载更多完成之后滚动内容显示新数据
     * @param enabled 是否启用
     * @return VRefreshLayout
     */
    VRefreshLayout setEnableScrollContentWhenLoaded(boolean enabled);

    /**
     * Set whether to scroll the content to display new data after the refresh is complete.
     * 是否在刷新完成之后滚动内容显示新数据
     * @param enabled 是否启用
     * @return VRefreshLayout
     */
    VRefreshLayout setEnableScrollContentWhenRefreshed(boolean enabled);

    /**
     * Set whether to pull up and load more when the content is not full of one page.
     * 设置在内容不满一页的时候，是否可以上拉加载更多
     * @param enabled 是否启用
     * @return VRefreshLayout
     */
    VRefreshLayout setEnableLoadMoreWhenContentNotFull(boolean enabled);

    /**
     * Set whether to enable cross-border drag (imitation iphone effect).
     * 设置是否启用越界拖动（仿苹果效果）
     * @param enabled 是否启用
     * @return VRefreshLayout
     */
    VRefreshLayout setEnableOverScrollDrag(boolean enabled);

    /**
     * Set whether or not Footer follows the content after there is no more data.
     * 设置是否在没有更多数据之后 Footer 跟随内容
     * @param enabled 是否启用
     * @return VRefreshLayout
     */
    VRefreshLayout setEnableFooterFollowWhenNoMoreData(boolean enabled);

    /**
     * Set whether to clip header when the Header is in the FixedBehind state.
     * 设置是否在当 Header 处于 FixedBehind 状态的时候剪裁遮挡 Header
     * @param enabled 是否启用
     * @return VRefreshLayout
     */
    VRefreshLayout setEnableClipHeaderWhenFixedBehind(boolean enabled);

    /**
     * Set whether to clip footer when the Footer is in the FixedBehind state.
     * 设置是否在当 Footer 处于 FixedBehind 状态的时候剪裁遮挡 Footer
     * @param enabled 是否启用
     * @return VRefreshLayout
     */
    VRefreshLayout setEnableClipFooterWhenFixedBehind(boolean enabled);

    /**
     * Setting whether nesting scrolling is enabled (default off + smart on).
     * 设置是会否启用嵌套滚动功能（默认关闭+智能开启）
     * @param enabled 是否启用
     * @return VRefreshLayout
     */
    VRefreshLayout setEnableNestedScroll(boolean enabled);
    /**
     * 设置固定在 Header 下方的视图Id，可以在 Footer 上下滚动的时候保持不跟谁滚动
     * @param id 固定在头部的视图Id
     * @return VRefreshLayout
     */
    VRefreshLayout setFixedHeaderViewId(@IdRes int id);
    /**
     * 设置固定在 Footer 上方的视图Id，可以在 Header 上下滚动的时候保持不跟谁滚动
     * @param id 固定在底部的视图Id
     * @return VRefreshLayout
     */
    VRefreshLayout setFixedFooterViewId(@IdRes int id);
    /**
     * 设置在 Header 上下滚动时，需要跟随滚动的视图Id，默认整个内容视图
     * @param id 固定在头部的视图Id
     * @return VRefreshLayout
     */
    VRefreshLayout setHeaderTranslationViewId(@IdRes int id);
    /**
     * 设置在 Footer 上下滚动时，需要跟随滚动的视图Id，默认整个内容视图
     * @param id 固定在头部的视图Id
     * @return VRefreshLayout
     */
    VRefreshLayout setFooterTranslationViewId(@IdRes int id);
    /**
     * Set whether to enable the action content view when refreshing.
     * 设置是否开启在刷新时候禁止操作内容视图
     * @param disable 是否禁止
     * @return VRefreshLayout
     */
    VRefreshLayout setDisableContentWhenRefresh(boolean disable);

    /**
     * Set whether to enable the action content view when loading.
     * 设置是否开启在加载时候禁止操作内容视图
     * @param disable 是否禁止
     * @return VRefreshLayout
     */
    VRefreshLayout setDisableContentWhenLoading(boolean disable);

    /**
     * Set refresh listener separately.
     * 单独设置刷新监听器
     * @param listener VOnRefreshListener 刷新监听器
     * @return VRefreshLayout
     */
    VRefreshLayout setOnRefreshListener(VOnRefreshListener listener);

    /**
     * Set load more listener separately.
     * 单独设置加载监听器
     * @param listener VOnLoadMoreListener 加载监听器
     * @return VRefreshLayout
     */
    VRefreshLayout setOnLoadMoreListener(VOnLoadMoreListener listener);

    /**
     * Set refresh and load listeners at the same time.
     * 同时设置刷新和加载监听器
     * @param listener VOnRefreshLoadMoreListener 刷新加载监听器
     * @return VRefreshLayout
     */
    VRefreshLayout setOnRefreshLoadMoreListener(VOnRefreshLoadMoreListener listener);

    /**
     * Set up a multi-function listener.
     * Recommended {@link com.vivo.chat.weight.refresh.smartrefresh.simple.VSimpleMultiListener}
     * 设置多功能监听器
     * 建议使用 {@link com.vivo.chat.weight.refresh.smartrefresh.simple.VSimpleMultiListener}
     * @param listener OnMultiPurposeListener 多功能监听器
     * @return VRefreshLayout
     */
    VRefreshLayout setOnMultiListener(VOnMultiListener listener);

    /**
     * Set the scroll boundary Decider, Can customize when you can refresh.
     * Recommended {@link com.vivo.chat.weight.refresh.smartrefresh.simple.VSimpleBoundaryDecider}
     * 设置滚动边界判断器
     * 建议使用 {@link com.vivo.chat.weight.refresh.smartrefresh.simple.VSimpleBoundaryDecider}
     * @param boundary VScrollBoundaryDecider 判断器
     * @return VRefreshLayout
     */
    VRefreshLayout setScrollBoundaryDecider(VScrollBoundaryDecider boundary);

    /**
     * Set theme color int (primaryColor and accentColor).
     * 设置主题颜色
     * @param primaryColors ColorInt 主题颜色
     * @return VRefreshLayout
     */
    VRefreshLayout setPrimaryColors(@ColorInt int... primaryColors);

    /**
     * Set theme color id (primaryColor and accentColor).
     * 设置主题颜色
     * @param primaryColorId ColorRes 主题颜色ID
     * @return VRefreshLayout
     */
    VRefreshLayout setPrimaryColorsId(@ColorRes int... primaryColorId);

    /**
     * finish refresh.
     * 完成刷新
     * @return VRefreshLayout
     */
    VRefreshLayout finishRefresh();

    /**
     * finish refresh.
     * 完成刷新
     * @param delayed 开始延时
     * @return VRefreshLayout
     */
    VRefreshLayout finishRefresh(int delayed);

    /**
     * finish refresh.
     * 完成加载
     * @param success 数据是否成功刷新 （会影响到上次更新时间的改变）
     * @return VRefreshLayout
     */
    VRefreshLayout finishRefresh(boolean success);

    /**
     * finish refresh.
     * 完成刷新
     * @param delayed 开始延时
     * @param success 数据是否成功刷新 （会影响到上次更新时间的改变）
     * @param noMoreData 是否有更多数据
     * @return VRefreshLayout
     */
    VRefreshLayout finishRefresh(int delayed, boolean success, Boolean noMoreData);

    /**
     * finish load more with no more data.
     * 完成刷新并标记没有更多数据
     * @return VRefreshLayout
     */
    VRefreshLayout finishRefreshWithNoMoreData();

    /**
     * finish load more.
     * 完成加载
     * @return VRefreshLayout
     */
    VRefreshLayout finishLoadMore();

    /**
     * finish load more.
     * 完成加载
     * @param delayed 开始延时
     * @return VRefreshLayout
     */
    VRefreshLayout finishLoadMore(int delayed);

    /**
     * finish load more.
     * 完成加载
     * @param success 数据是否成功
     * @return VRefreshLayout
     */
    VRefreshLayout finishLoadMore(boolean success);

    /**
     * finish load more.
     * 完成加载
     * @param delayed 开始延时
     * @param success 数据是否成功
     * @param noMoreData 是否有更多数据
     * @return VRefreshLayout
     */
    VRefreshLayout finishLoadMore(int delayed, boolean success, boolean noMoreData);

    /**
     * finish load more with no more data.
     * 完成加载并标记没有更多数据
     * @return VRefreshLayout
     */
    VRefreshLayout finishLoadMoreWithNoMoreData();

    /**
     * Close the Header or Footer, can't replace finishRefresh and finishLoadMore.
     * 关闭 Header 或者 Footer
     * 注意：
     * 1.closeHeaderOrFooter 任何时候任何状态都能关闭  header 和 footer
     * 2.finishRefresh 和 finishLoadMore 只能在 刷新 或者 加载 的时候关闭
     * @return VRefreshLayout
     */
    VRefreshLayout closeHeaderOrFooter();

    /**
     * Restore the original state after finishLoadMoreWithNoMoreData.
     * 设置没有更多数据的状态
     * @param noMoreData 是否有更多数据
     * @return VRefreshLayout
     * 尽量使用下面三个方法代替，他们可以让状态切换与动画结束合拍
     *      use {@link VRefreshLayout#resetNoMoreData()}
     *      use {@link VRefreshLayout#finishRefreshWithNoMoreData()}
     *      use {@link VRefreshLayout#finishLoadMoreWithNoMoreData()}
     */
    VRefreshLayout setNoMoreData(boolean noMoreData);

    /**
     * Restore the original state after finishLoadMoreWithNoMoreData.
     * 恢复没有更多数据的原始状态
     * @return VRefreshLayout
     */
    VRefreshLayout resetNoMoreData();

    /**
     * Get header of VRefreshLayout
     * 获取当前 Header
     * @return VRefreshLayout
     */
    @Nullable
    VRefreshHeader getRefreshHeader();

    /**
     * Get footer of VRefreshLayout
     * 获取当前 Footer
     * @return VRefreshLayout
     */
    @Nullable
    VRefreshFooter getRefreshFooter();

    /**
     * Get the current state of VRefreshLayout
     * 获取当前状态
     * @return VRefreshLayout
     */
    @NonNull
    VRefreshState getState();

    /**
     * Get the ViewGroup of VRefreshLayout
     * 获取实体布局视图
     * @return ViewGroup
     */
    @NonNull
    ViewGroup getLayout();

    /**
     * Display refresh animation and trigger refresh event.
     * 显示刷新动画并且触发刷新事件
     * @return true or false, Status non-compliance will fail.
     *         是否成功（状态不符合会失败）
     */
    boolean autoRefresh();

    /**
     * Display refresh animation and trigger refresh event, Delayed start.
     * 显示刷新动画并且触发刷新事件，延时启动
     * @param delayed 开始延时
     * @return true or false, Status non-compliance will fail.
     *         是否成功（状态不符合会失败）
     */
    boolean autoRefresh(int delayed);

    /**
     * Display refresh animation without triggering events.
     * 显示刷新动画，不触发事件
     * @return true or false, Status non-compliance will fail.
     *         是否成功（状态不符合会失败）
     */
    boolean autoRefreshAnimationOnly();

    /**
     * Display refresh animation, Multifunction.
     * 显示刷新动画并且触发刷新事件
     * @param delayed 开始延时
     * @param duration 拖拽动画持续时间
     * @param dragRate 拉拽的高度比率
     * @param animationOnly animation only 只有动画
     * @return true or false, Status non-compliance will fail.
     *         是否成功（状态不符合会失败）
     */
    boolean autoRefresh(int delayed, int duration, float dragRate, boolean animationOnly);

    /**
     * Display load more animation and trigger load more event.
     * 显示加载动画并且触发刷新事件
     * @return true or false, Status non-compliance will fail.
     *         是否成功（状态不符合会失败）
     */
    boolean autoLoadMore();

    /**
     * Display load more animation and trigger load more event, Delayed start.
     * 显示加载动画并且触发刷新事件, 延时启动
     * @param delayed 开始延时
     * @return true or false, Status non-compliance will fail.
     *         是否成功（状态不符合会失败）
     */
    boolean autoLoadMore(int delayed);

    /**
     * Display load more animation without triggering events.
     * 显示加载动画，不触发事件
     * @return true or false, Status non-compliance will fail.
     *         是否成功（状态不符合会失败）
     */
    boolean autoLoadMoreAnimationOnly();

    /**
     * Display load more animation and trigger load more event, Delayed start.
     * 显示加载动画, 多功能选项
     * @param delayed 开始延时
     * @param duration 拖拽动画持续时间
     * @param dragRate 拉拽的高度比率
     * @param animationOnly 是否只是显示动画，不回调
     * @return true or false, Status non-compliance will fail.
     *         是否成功（状态不符合会失败）
     */
    boolean autoLoadMore(int delayed, int duration, float dragRate, boolean animationOnly);

    /**
     * 是否正在刷新
     * @return VRefreshLayout
     */
    boolean isRefreshing();

    /**
     * 是否正在加载
     * @return VRefreshLayout
     */
    boolean isLoading();

}
