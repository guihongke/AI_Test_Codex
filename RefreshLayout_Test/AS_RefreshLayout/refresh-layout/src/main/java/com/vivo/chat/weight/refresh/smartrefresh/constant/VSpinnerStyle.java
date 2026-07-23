package com.vivo.chat.weight.refresh.smartrefresh.constant;

// Modified from SmartRefreshLayout 2.1.1 for the Vivo-prefixed AndroidX fork.

/**
 * 顶部和底部的组件在拖动时候的变换方式
 * Created by scwang on 2017/5/26.
 */
@SuppressWarnings("DeprecatedIsStillUsed")
public class VSpinnerStyle {

    public static final VSpinnerStyle Translate = new VSpinnerStyle(0, true, false);
    /**
     * Scale 下拉过程中会动态 【测量】（header）和 【布局】（layout）降低app 性能，
     * 官方自带的 Header 都已经从【Scale】转向【FixedBehind】来提高性能
     * 自定义可以参考官方的 【飞机】【贝塞尔】【快递】等 Header
     * @deprecated use {@link VSpinnerStyle#FixedBehind}
     */
    @Deprecated
    public static final VSpinnerStyle Scale = new VSpinnerStyle(1, true, true);
    public static final VSpinnerStyle FixedBehind = new VSpinnerStyle(2, false, false);
    public static final VSpinnerStyle FixedFront = new VSpinnerStyle(3, true, false);
    public static final VSpinnerStyle MatchLayout = new VSpinnerStyle(4, true, false);

    public static final VSpinnerStyle[] values = new VSpinnerStyle[] {
            Translate, //平行移动        特点: HeaderView高度不会改变，
            Scale, //拉伸形变            特点：在下拉和上弹（HeaderView高度改变）时候，会自动触发OnDraw事件
            FixedBehind, //固定在背后    特点：HeaderView高度不会改变，
            FixedFront, //固定在前面     特点：HeaderView高度不会改变，
            MatchLayout//填满布局        特点：HeaderView高度不会改变，尺寸充满 VRefreshLayout
    };

    public final int ordinal;
    public final boolean front;
    public final boolean scale;

    protected VSpinnerStyle(int ordinal, boolean front, boolean scale) {
        this.ordinal = ordinal;
        this.front = front;
        this.scale = scale;
    }
}
