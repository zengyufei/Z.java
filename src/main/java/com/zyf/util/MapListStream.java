package com.zyf.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

// 内部类，封装流操作
public final class MapListStream<K, V> {
    private final Iterable<?> source;
    private final Function<?, K> keyMapper;
    private final Function<?, V> valueMapper;
    
    private volatile Map<K, List<V>> cachedMap;

    /**
     * 已弃用的构造函数，为了兼容性保留，但内部会将其视为已计算的结果
     */
    @Deprecated
    public MapListStream(Map<K, List<V>> map) {
        this.source = null;
        this.keyMapper = null;
        this.valueMapper = null;
        this.cachedMap = map;
    }

    /**
     * 惰性构造函数
     *
     * @param source      原始数据源
     * @param keyMapper   键提取器
     * @param valueMapper 值提取器
     */
    @SuppressWarnings("unchecked")
    public <T> MapListStream(Iterable<T> source, Function<T, K> keyMapper, Function<T, V> valueMapper) {
        this.source = source;
        this.keyMapper = (Function<?, K>) keyMapper;
        this.valueMapper = (Function<?, V>) valueMapper;
    }

    /**
     * 确保计算已完成。采用双重检查锁定确保线程安全且仅计算一次。
     */
    @SuppressWarnings("unchecked")
    private void ensureComputed() {
        if (cachedMap == null) {
            synchronized (this) {
                if (cachedMap == null) {
                    Map<K, List<V>> result = new LinkedHashMap<>();
                    if (source != null) {
                        for (Object element : source) {
                            if (element != null) {
                                K key = ((Function<Object, K>) keyMapper).apply(element);
                                V value = ((Function<Object, V>) valueMapper).apply(element);
                                result.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
                            }
                        }
                    }
                    cachedMap = result;
                }
            }
        }
    }

    /**
     * 获取最终生成的 Map。调用该方法会触发流的遍历（如果尚未计算）。
     *
     * @return 包含分组结果的 Map
     */
    public Map<K, List<V>> toMap() {
        ensureComputed();
        return cachedMap;
    }

    /**
     * 获取指定键对应的值列表。
     *
     * @param key 键
     * @return 值列表，如果不存在则返回空列表
     */
    public List<V> getValues(K key) {
        ensureComputed();
        return cachedMap.getOrDefault(key, new ArrayList<>());
    }

//        public <R> MapStream<K, R> apply(Function<ListStream<V>, R> func) {
//            final Map<K, R> newMap = new LinkedHashMap<>();
//            for (Map.Entry<K, List<V>> entry : map.entrySet()) {
//                final K key = entry.getKey();
//                final List<V> values = entry.getValue();
//                R apply = func.apply(new ListStream<>(values));
//                newMap.put(key, apply);
//            }
//            return new MapStream<>(newMap);
//        }

//        public <R> MapStream<K, List<R>> valueStream(Function<ListStream<V>, List<R>> func) {
//            final Map<K, List<R>> newMap = new LinkedHashMap<>();
//            for (Map.Entry<K, List<V>> entry : map.entrySet()) {
//                final K key = entry.getKey();
//                final List<V> values = entry.getValue();
//                List<R> rList = new ArrayList<>(func.apply(new ListStream<>(values)));
//                newMap.put(key, rList);
//            }
//            return new MapStream<>(newMap);
//        }

//        public <R> MapStream<K, Map<K, List<V>>> valueStreamGroupByMap(Function<ListStream<V>, Map<K, List<V>>> func) {
//            final Map<K, Map<K, List<V>>> newMap = new LinkedHashMap<>();
//            for (Map.Entry<K, List<V>> entry : map.entrySet()) {
//                final K key = entry.getKey();
//                final List<V> values = entry.getValue();
//                newMap.put(key, func.apply(new ListStream<>(values)));
//            }
//            return new MapStream<>(newMap);
//        }

    /**
     * 对每个分组的值列表进一步应用流操作，返回一个新的 MapStream。
     * 这是一个彻底延迟的操作，只有在终结操作触发时才会执行转换。
     *
     * @param func 对每个分组的 ListStream 执行的转换函数
     * @param <R>  转换后的结果类型
     * @return 包含转换结果的 MapStream
     */
    public <R> MapStream<K, R> valueStream(Function<ListStream<V>, R> func) {
        return new MapStream<>(() -> {
            ensureComputed(); // 确保原始分组已计算
            final Map<K, R> newMap = new LinkedHashMap<>();
            for (Map.Entry<K, List<V>> entry : cachedMap.entrySet()) {
                final K key = entry.getKey();
                final List<V> values = entry.getValue();
                newMap.put(key, func.apply(new ListStream<>(values)));
            }
            return newMap;
        });
    }
}
