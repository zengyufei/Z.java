package com.zyf.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

// 内部类，封装流操作
public class MapStream<K, V> {
    /** Map 数据源提供者 */
    private final Supplier<Map<K, V>> mapSupplier;
    /** 缓存的计算结果 */
    private volatile Map<K, V> cachedMap;
    /** 延迟执行的操作链 */
    private final List<Consumer<Map<K, V>>> operations = new ArrayList<>();

    public MapStream(Map<K, V> map) {
        this.mapSupplier = null;
        this.cachedMap = map;
    }

    public MapStream(Supplier<Map<K, V>> mapSupplier) {
        this.mapSupplier = mapSupplier;
    }

    /**
     * 确保数据已计算且操作链已执行。
     * 采用双重检查锁定确保线程安全且仅计算一次。
     */
    private void ensureComputed() {
        if (cachedMap == null && mapSupplier != null) {
            synchronized (this) {
                if (cachedMap == null) {
                    // 1. 获取基础数据
                    cachedMap = mapSupplier.get();
                    // 2. 执行积累的操作链
                    for (Consumer<Map<K, V>> operation : operations) {
                        operation.accept(cachedMap);
                    }
                    // 3. 清空操作链，释放内存
                    operations.clear();
                }
            }
        } else if (cachedMap != null && !operations.isEmpty()) {
            // 如果 cachedMap 已存在（可能是构造函数传入的），但仍有待处理的操作
            synchronized (this) {
                for (Consumer<Map<K, V>> operation : operations) {
                    operation.accept(cachedMap);
                }
                operations.clear();
            }
        }
    }

    /**
     * 中间操作：检查键是否存在并消费。
     * 此操作将推迟到终结操作时执行。
     */
    public MapStream<K, V> hasKey(K key, Consumer<V> consumer) {
        operations.add(m -> {
            if (m.containsKey(key)) {
                consumer.accept(m.get(key));
            }
        });
        return this;
    }

    /**
     * 中间操作：检查键是否存在并消费，支持默认值。
     * 此操作将推迟到终结操作时执行。
     */
    public MapStream<K, V> hasKey(K key, V defaultValue, Consumer<V> consumer) {
        operations.add(m -> {
            if (m.containsKey(key)) {
                consumer.accept(m.getOrDefault(key, defaultValue));
            } else {
                consumer.accept(defaultValue);
            }
        });
        return this;
    }

    /**
     * 中间操作：向 Map 中添加元素。
     * 此操作将推迟到终结操作时执行。
     */
    public MapStream<K, V> put(K k, V v) {
        operations.add(m -> m.put(k, v));
        return this;
    }

    /**
     * 终结操作：获取最终的 Map 结果。
     * 触发所有延迟计算和操作。
     */
    public Map<K, V> toMap() {
        ensureComputed();
        return cachedMap;
    }
}
