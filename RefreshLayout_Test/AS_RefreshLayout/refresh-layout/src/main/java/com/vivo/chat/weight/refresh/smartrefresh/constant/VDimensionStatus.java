package com.vivo.chat.weight.refresh.smartrefresh.constant;

// Modified from SmartRefreshLayout 2.1.1 for the Vivo-prefixed AndroidX fork.

/**
 * 尺寸值的定义状态，用于在值覆盖的时候决定优先级
 * 越往下优先级越高
 */
@SuppressWarnings("WeakerAccess")
public class VDimensionStatus {

    public static final VDimensionStatus DefaultUnNotify = new VDimensionStatus(0,false);//默认值，但是还没通知确认
    public static final VDimensionStatus Default = new VDimensionStatus(1,true);//默认值
    public static final VDimensionStatus XmlWrapUnNotify = new VDimensionStatus(2,false);//Xml计算，但是还没通知确认
    public static final VDimensionStatus XmlWrap = new VDimensionStatus(3,true);//Xml计算
    public static final VDimensionStatus XmlExactUnNotify = new VDimensionStatus(4,false);//Xml 的view 指定，但是还没通知确认
    public static final VDimensionStatus XmlExact = new VDimensionStatus(5,true);//Xml 的view 指定
    public static final VDimensionStatus XmlLayoutUnNotify = new VDimensionStatus(6,false);//Xml 的layout 中指定，但是还没通知确认
    public static final VDimensionStatus XmlLayout = new VDimensionStatus(7,true);//Xml 的layout 中指定
    public static final VDimensionStatus CodeExactUnNotify = new VDimensionStatus(8,false);//代码指定，但是还没通知确认
    public static final VDimensionStatus CodeExact = new VDimensionStatus(9,true);//代码指定
    public static final VDimensionStatus DeadLockUnNotify = new VDimensionStatus(10,false);//锁死，但是还没通知确认
    public static final VDimensionStatus DeadLock = new VDimensionStatus(10,true);//锁死

    public final int ordinal;
    public final boolean notified;

    public static final VDimensionStatus[] values = new VDimensionStatus[]{
            DefaultUnNotify,
            Default,
            XmlWrapUnNotify,
            XmlWrap,
            XmlExactUnNotify,
            XmlExact,
            XmlLayoutUnNotify,
            XmlLayout,
            CodeExactUnNotify,
            CodeExact,
            DeadLockUnNotify,
            DeadLock
    };

    private VDimensionStatus(int ordinal,boolean notified) {
        this.ordinal = ordinal;
        this.notified = notified;
    }

    /**
     * 转换为未通知状态
     * @return 未通知状态
     */
    public VDimensionStatus unNotify() {
        if (notified) {
            VDimensionStatus prev = values[ordinal - 1];
            if (!prev.notified) {
                return prev;
            }
            return DefaultUnNotify;
        }
        return this;
    }

    /**
     * 转换为通知状态
     * @return 通知状态
     */
    public VDimensionStatus notified() {
        if (!notified) {
            return values[ordinal + 1];
        }
        return this;
    }

    /**
     * 是否可以被新的状态替换
     * @param status 新转台
     * @return 小于等于
     */
    public boolean canReplaceWith(VDimensionStatus status) {
        return ordinal < status.ordinal || ((!notified || CodeExact == this) && ordinal == status.ordinal);
    }

//    /**
//     * 是否没有达到新的状态
//     * @param status 新转台
//     * @return 大于等于 gte
//     */
//    public boolean gteStatusWith(VDimensionStatus status) {
//        return ordinal() >= status.ordinal();
//    }
}