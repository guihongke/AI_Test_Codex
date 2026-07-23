# Markwon 完整语法演示

## 标题演示

# 一级标题 Heading H1
## 二级标题 Heading H2
### 三级标题 Heading H3
#### 四级标题 Heading H4
##### 五级标题 Heading H5
###### 六级标题 Heading H6

---

## 文本样式

这是**加粗文本**，这是*斜体文本*，这是***加粗斜体***，这是~~删除线文本~~。

这是`行内代码`示例，可以在文本中高亮显示代码片段。

这是一段普通正文，展示了 Markdown 的基本文本渲染能力。Markwon 是一个功能强大的 Android Markdown 渲染库，支持丰富的扩展插件。

---

## 引用块

> 这是一段引用文本。Markwon 支持多级引用。
>
> > 这是二级嵌套引用，可以多层嵌套使用。
> >
> > > 这是三级嵌套引用，展示了深层次的引用嵌套效果。
>
> 回到一级引用。Markdown 的引用语法非常直观易用。

---

## 无序列表

- 第一项：Android 开发
- 第二项：**加粗的列表项**
- 第三项：包含`行内代码`的列表项
  - 嵌套子项 A：Kotlin
  - 嵌套子项 B：Java
  - 嵌套子项 C：Flutter
- 第四项：包含链接的列表项

---

## 有序列表

1. 第一步：创建 Android 项目
2. 第二步：添加 Markwon 依赖
3. 第三步：配置 Markwon 实例
   1. 子步骤 3.1：设置图片加载器
   2. 子步骤 3.2：启用表格扩展
   3. 子步骤 3.3：启用语法高亮
4. 第四步：渲染 Markdown 内容
5. 第五步：自定义样式

---

## 任务列表

- [x] 已完成：添加 Markwon 核心依赖
- [x] 已完成：配置 Glide 图片加载
- [x] 已完成：启用表格扩展
- [ ] 未完成：添加自定义 CSS 样式
- [ ] 未完成：实现暗黑模式
- [ ] 未完成：性能优化

---

## 代码块

### Java 代码示例

```java
public class MarkwonDemo {
    public static void main(String[] args) {
        // 创建 Markwon 实例
        final Markwon markwon = Markwon.builder(context)
            .usePlugin(GlideImagesPlugin.create(context))
            .usePlugin(TablePlugin.create(context))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TaskListPlugin.create(context))
            .build();

        // 渲染 Markdown
        String markdown = "# Hello Markwon";
        markwon.setMarkdown(textView, markdown);
    }
}
```

### Python 代码示例

```python
import asyncio

class MarkdownRenderer:
    """Markdown 渲染器示例"""

    def __init__(self, content: str):
        self.content = content
        self.parsed = []

    async def render(self) -> str:
        """异步渲染 Markdown"""
        await asyncio.sleep(0.1)
        return self.content.upper()

# 使用示例
renderer = MarkdownRenderer("# Hello World")
result = asyncio.run(renderer.render())
print(result)
```

### SQL 查询示例

```sql
SELECT
    u.id,
    u.username,
    COUNT(o.id) AS order_count,
    SUM(o.amount) AS total_amount
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
WHERE u.status = 'active'
  AND o.created_at >= '2024-01-01'
GROUP BY u.id, u.username
HAVING COUNT(o.id) > 5
ORDER BY total_amount DESC
LIMIT 10;
```

---

## 表格演示

### 基本表格

| 序号 | 姓名 | 年龄 | 城市 |
|------|------|------|------|
| 1 | 张三 | 28 | 北京 |
| 2 | 李四 | 32 | 上海 |
| 3 | 王五 | 25 | 广州 |
| 4 | 赵六 | 30 | 深圳 |
| 5 | 孙七 | 27 | 杭州 |

### 超宽表格（可横向滚动）

| 序号 | 项目名称 | 技术栈 | 版本号 | 负责人 | 开始日期 | 结束日期 | 状态 | 优先级 | 备注 |
|------|----------|--------|--------|--------|----------|----------|------|--------|------|
| 1 | 用户中心重构 | Spring Boot + Vue | v3.2.1 | 张三 | 2024-01-15 | 2024-03-20 | 已完成 | 高 | 已上线运行稳定 |
| 2 | 消息推送服务 | Go + Redis + Kafka | v1.5.0 | 李四 | 2024-02-01 | 2024-04-15 | 进行中 | 高 | 日均推送量 100 万 |
| 3 | 数据分析平台 | Python + Spark + Flink | v2.0.0 | 王五 | 2024-03-01 | 2024-06-30 | 规划中 | 中 | 需要大数据集群支持 |
| 4 | 移动端 App | Flutter + Dart | v1.2.0 | 赵六 | 2024-01-20 | 2024-05-10 | 测试中 | 高 | iOS 和 Android 双端 |
| 5 | 微服务网关 | Java + Spring Cloud | v4.1.0 | 孙七 | 2024-04-01 | 2024-07-15 | 规划中 | 中 | 需要兼容旧版 API |

### 对齐方式

| 左对齐 | 居中对齐 | 右对齐 |
|:-------|:-------:|-------:|
| 内容 A | 内容 B | 100 |
| 长文本内容测试 | 居中显示 | 9999 |
| Left | Center | Right |

---

## 链接

这是一个[Markwon 项目主页](https://github.com/noties/Markwon)的链接。

自动识别链接：https://www.android.com

电子邮件链接：contact@example.com

---

## 图片

![Android Logo](https://developer.android.com/static/images/logos/android.svg)

---

## 水平分割线

上面的内容...

---

中间的内容...

* * *

下面的内容...

---

## 内联 HTML

Markwon 支持部分 HTML 标签：

<u>这段文字带下划线</u>

<span style="color: red;">红色文字（如果支持内联样式）</span>

<sub>下标文本</sub> 普通文本 <sup>上标文本</sup>

<kbd>Ctrl</kbd> + <kbd>C</kbd> 复制快捷键

---

## 混合复杂内容

> **注意事项**：在使用 Markwon 时，请注意以下要点：
>
> 1. 确保添加了 `INTERNET` 权限以加载网络图片
> 2. 表格插件需要单独引入 `ext-tables` 扩展
> 3. 代码高亮需要 `prism4j` 语言包
>
> ```java
> // 提示：在 Application 中初始化 Markwon 可提升性能
> Markwon.markwon(context); // 单例模式
> ```

> 最终总结：Markwon 是目前 Android 平台上功能最全面的 Markdown 渲染库之一，支持丰富的语法扩展和自定义功能，非常适合用于文档展示、博客阅读器等场景。

---

**全文结束** 🎉
