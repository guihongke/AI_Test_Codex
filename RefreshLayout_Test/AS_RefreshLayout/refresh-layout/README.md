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
