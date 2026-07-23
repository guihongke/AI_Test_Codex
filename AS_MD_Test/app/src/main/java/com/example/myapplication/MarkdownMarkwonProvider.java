package com.example.myapplication;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.View;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.LinkResolver;
import io.noties.markwon.Markwon;
import io.noties.markwon.MarkwonConfiguration;
import io.noties.markwon.core.MarkwonTheme;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.ext.tasklist.TaskListPlugin;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.image.AsyncDrawable;
import io.noties.markwon.image.ImageSizeResolver;
import io.noties.markwon.image.ImagesPlugin;
import io.noties.markwon.image.svg.SvgMediaDecoder;
import io.noties.markwon.linkify.LinkifyPlugin;
import io.noties.markwon.syntax.Prism4jThemeDefault;
import io.noties.markwon.syntax.SyntaxHighlightPlugin;
import io.noties.prism4j.Prism4j;

/**
 * Markdown 渲染器创建与全局配置入口。
 *
 * <p>实际项目中建议将 Markwon 的插件、主题、图片策略和链接策略集中在这一层维护，
 * 避免自定义 View 内部混入过多第三方库初始化细节。</p>
 */
public final class MarkdownMarkwonProvider {
    public static final int TEXT_COLOR = Color.rgb(35, 44, 58);
    public static final int LINK_COLOR = Color.rgb(33, 118, 199);
    private static final float BASE_TEXT_SIZE_DP = 14F;
    private static final float[] HEADING_TEXT_SIZE_DP = new float[]{24F, 18F, 14F, 14F, 9F, 5F};
    private static Markwon sharedMarkwon;

    private MarkdownMarkwonProvider() {
    }

    /**
     * 获取进程内共享的 Markwon 实例。
     *
     * @param context 任意 Context。
     * @return 共享 Markwon。
     */
    public static synchronized Markwon get(Context context) {
        if (sharedMarkwon == null) {
            sharedMarkwon = createMarkwon(context.getApplicationContext());
        }
        return sharedMarkwon;
    }

    /**
     * 创建 Markwon 实例。
     *
     * @param context Application Context。
     * @return 配置完成的 Markwon。
     */
    public static Markwon createMarkwon(Context context) {
        return Markwon.builder(context)
                .usePlugin(new AbstractMarkwonPlugin() {
                    @Override
                    public void configureTheme(MarkwonTheme.Builder builder) {
                        builder.linkColor(LINK_COLOR)
                                .isLinkUnderlined(true)
                                .blockMargin(dp(context, 10))
                                .blockQuoteColor(Color.rgb(73, 134, 202))
                                .listItemColor(Color.rgb(96, 108, 124))
                                .codeTextColor(Color.rgb(168, 68, 73))
                                .codeBlockTextColor(Color.rgb(42, 51, 65))
                                .codeBackgroundColor(Color.rgb(238, 242, 247))
                                .codeBlockBackgroundColor(Color.rgb(241, 244, 248))
                                .codeTypeface(Typeface.MONOSPACE)
                                .codeBlockTypeface(Typeface.MONOSPACE)
                                .thematicBreakColor(Color.rgb(220, 226, 235))
                                .headingBreakHeight(0)
                                .headingTextSizeMultipliers(headingTextSizeMultipliers())
                        ;
                    }

                    @Override
                    public void configureConfiguration(MarkwonConfiguration.Builder builder) {
                        builder.linkResolver(new ViewAwareLinkResolver())
                                .imageSizeResolver(new MaxWidthImageSizeResolver());
                    }
                })
                .usePlugin(ImagesPlugin.create(plugin -> plugin
                        .addMediaDecoder(SvgMediaDecoder.create(context.getResources()))
                        .placeholderProvider(drawable -> new AspectColorDrawable(Color.rgb(238, 242, 247)))
                        .errorHandler((url, throwable) -> new AspectColorDrawable(Color.rgb(244, 229, 229)))))
                .usePlugin(SyntaxHighlightPlugin.create(new Prism4j(new GrammarLocatorDef()), Prism4jThemeDefault.create()))
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TablePlugin.create(context))
                .usePlugin(TaskListPlugin.create(context))
                .usePlugin(HtmlPlugin.create().allowNonClosedTags(true))
                .usePlugin(LinkifyPlugin.create())
                .build();
    }

    private static float[] headingTextSizeMultipliers() {
        float[] multipliers = new float[HEADING_TEXT_SIZE_DP.length];
        for (int i = 0; i < HEADING_TEXT_SIZE_DP.length; i++) {
            multipliers[i] = HEADING_TEXT_SIZE_DP[i] / BASE_TEXT_SIZE_DP;
        }
        return multipliers;
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    /**
     * 链接点击解析器，通过被点击 View 反查 MarkdownView 后分发给组件对外 listener。
     */
    private static final class ViewAwareLinkResolver implements LinkResolver {
        @Override
        public void resolve(View view, String link) {
            MarkdownView markdownView = findMarkdownViewParent(view);
            if (markdownView != null) {
                markdownView.handleLinkClick(view, link);
            }
        }

        private MarkdownView findMarkdownViewParent(View view) {
            View current = view;
            while (current != null) {
                if (current instanceof MarkdownView) {
                    return (MarkdownView) current;
                }
                if (!(current.getParent() instanceof View)) {
                    return null;
                }
                current = (View) current.getParent();
            }
            return null;
        }
    }

    /**
     * 图片尺寸解析器：保留图片原始宽高比例，宽度最大不超过当前文本绘制宽度。
     */
    private static final class MaxWidthImageSizeResolver extends ImageSizeResolver {
        @Override
        public Rect resolveImageSize(AsyncDrawable drawable) {
            Rect sourceBounds = drawable.getResult().getBounds();
            int width = sourceBounds.width();
            int height = sourceBounds.height();
            if (width <= 0) {
                width = Math.max(1, drawable.getResult().getIntrinsicWidth());
            }
            if (height <= 0) {
                height = Math.max(1, drawable.getResult().getIntrinsicHeight());
            }
            int canvasWidth = drawable.getLastKnownCanvasWidth();
            if (canvasWidth > 0 && width > canvasWidth) {
                float ratio = (float) width / (float) canvasWidth;
                width = canvasWidth;
                height = Math.max(1, Math.round(height / ratio));
            }
            return new Rect(0, 0, width, height);
        }
    }

    /**
     * 占位图/错误图 Drawable。自身不强制固定尺寸，让 Markwon 的 ImageSizeResolver 按文本宽度收敛。
     */
    private static final class AspectColorDrawable extends ColorDrawable {
        AspectColorDrawable(int color) {
            super(color);
        }

        @Override
        public int getIntrinsicWidth() {
            return 480;
        }

        @Override
        public int getIntrinsicHeight() {
            return 270;
        }
    }
}
