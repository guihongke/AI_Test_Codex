# VSmartRefreshLayout

本模块是 SmartRefreshLayout 2.1.1 的本地 AndroidX 源码分支。所有公开 Java
类型位于 `com.vivo.chat.weight.refresh.smartrefresh` 根包及其子包，并统一使用
`V` 前缀。

## 依赖

```groovy
dependencies {
    implementation project(':refresh-layout')
}
```

模块要求 `minSdk 23`、`targetSdk 34` 和 Java 11。

详细说明见项目根目录的 [`组件介绍.md`](../组件介绍.md) 和
[`使用文档.md`](../使用文档.md)。前者说明 Layout、状态机、阻尼、回弹及 Header
Adapter 的设计，后者包含完整业务 API、XML 属性和接入示例。

## Demo

运行 `app` 模块后，首页是与 SmartRefreshLayout 示例工程相同思路的 Demo 目录，
点击条目进入独立效果页：

- XML 定义的 `VProgressHeader` / `VProgressFooter`
- `VClassicsHeader` / `VClassicsFooter`
- `VMaterialHeader` / `VClassicsFooter`

每个效果页都可以测试自动刷新、下拉刷新、上拉加载、下一次请求失败、重置和
无更多数据；列表数据由本地 Mock，并通过 XML 定义的 `TextView` 添加
“这是一个header”。入口代码在 `app/src/main/java/com/kgh/refreshLayout/MainActivity.java`，
完整用法在 `RefreshDemoActivity.java`。

## XML Progress Header/Footer

```xml
<com.vivo.chat.weight.refresh.smartrefresh.VRefreshRecyclerLayout
    android:id="@+id/refreshLayout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:srlEnableLoadMore="true">

    <com.vivo.chat.weight.refresh.smartrefresh.header.VProgressHeader
        android:layout_width="match_parent"
        android:layout_height="64dp">

        <ProgressBar
            android:layout_width="32dp"
            android:layout_height="32dp"
            android:layout_centerInParent="true" />
    </com.vivo.chat.weight.refresh.smartrefresh.header.VProgressHeader>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

    <com.vivo.chat.weight.refresh.smartrefresh.footer.VProgressFooter
        android:layout_width="match_parent"
        android:layout_height="64dp">

        <ProgressBar
            android:layout_width="32dp"
            android:layout_height="32dp"
            android:layout_centerInParent="true" />
    </com.vivo.chat.weight.refresh.smartrefresh.footer.VProgressFooter>
</com.vivo.chat.weight.refresh.smartrefresh.VRefreshRecyclerLayout>
```

## Java

```java
VRefreshRecyclerLayout refreshLayout = findViewById(R.id.refreshLayout);
refreshLayout.getRecyclerView().setLayoutManager(new LinearLayoutManager(this));
refreshLayout.setAdapter(dataAdapter);

View header = getLayoutInflater().inflate(
        R.layout.view_list_header,
        refreshLayout.getRecyclerView(),
        false
);
refreshLayout.addHeaderView(header);

refreshLayout.setOnRefreshListener(layout -> loadFirstPage());
refreshLayout.setOnLoadMoreListener(layout -> loadNextPage());

refreshLayout.finishRefresh(true);
refreshLayout.finishLoadMore(0, true, noMoreData);
```

`finishLoadMore(boolean)` 的 boolean 表示本次加载是否成功。需要同时设置“没有更多
数据”时，应使用 `finishLoadMore(int delayed, boolean success, boolean noMoreData)`。

## 内置组件

- `VClassicsHeader` 和 `VClassicsFooter`
- `VMaterialHeader`
- `VProgressHeader` 和 `VProgressFooter`
- 任意 View 可通过 `VRefreshHeaderWrapper` 或 `VRefreshFooterWrapper` 包装

全局默认组件使用 `VSmartRefreshLayout.setDefaultRefreshHeaderCreator(...)` 和
`VSmartRefreshLayout.setDefaultRefreshFooterCreator(...)` 设置。状态监听、自动刷新、
自动加载、阻尼、阈值、回弹时长、无更多数据以及嵌套滚动 API 与 2.1.1 对应能力一致，
类型名称增加 `V` 前缀。
