package com.example.myapplication;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Markdown 渲染模型的轻量 LRU 缓存。
 *
 * <p>该类只负责缓存策略，不关心 Markdown 如何解析，也不持有 View。这样 {@link MarkdownView}
 * 可以把“内容解析”和“缓存淘汰”解耦，后续如果要替换成业务侧内存缓存或磁盘缓存，只需要调整这一层。</p>
 *
 * @param <K> 缓存 key 类型。
 * @param <V> 缓存 value 类型。
 */
final class MarkdownRenderCache<K, V> {
    private final int maxSize;
    private final LinkedHashMap<K, V> cache;

    /**
     * 创建一个按访问顺序淘汰的 LRU 缓存。
     *
     * @param maxSize 最大缓存条数。
     */
    MarkdownRenderCache(int maxSize) {
        this.maxSize = maxSize;
        this.cache = new LinkedHashMap<K, V>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > MarkdownRenderCache.this.maxSize;
            }
        };
    }

    /**
     * 获取缓存内容。
     *
     * @param key 缓存 key。
     * @return 命中时返回 value，否则返回 null。
     */
    synchronized V get(K key) {
        return cache.get(key);
    }

    /**
     * 写入缓存内容。
     *
     * @param key 缓存 key。
     * @param value 缓存 value。
     */
    synchronized void put(K key, V value) {
        cache.put(key, value);
    }
}
