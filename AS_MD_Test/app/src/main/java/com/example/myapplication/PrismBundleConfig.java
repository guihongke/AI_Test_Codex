package com.example.myapplication;

import io.noties.prism4j.annotations.PrismBundle;

/**
 * Prism4j 代码高亮语法包配置。
 *
 * <p>Markwon 的 SyntaxHighlightPlugin 会使用注解处理器根据这里声明的语言生成
 * {@code GrammarLocatorDef}。如果 mock 或真实 AI 回复中需要支持更多代码语言，在 include 中追加语言名，
 * 重新编译后即可被代码高亮插件识别。</p>
 */
@PrismBundle(include = {
        "c",
        "clike",
        "cpp",
        "css",
        "dart",
        "go",
        "java",
        "javascript",
        "json",
        "kotlin",
        "markdown",
        "markup",
        "python",
        "sql",
        "swift",
        "yaml"
})
public final class PrismBundleConfig {
    /**
     * 工具配置类不需要实例化。
     */
    private PrismBundleConfig() {
    }
}
