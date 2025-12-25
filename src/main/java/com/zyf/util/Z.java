package com.zyf.util;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class Z<T> implements Iterable<T> {

    private final Iterable<T> source;
    private Object intermediateResult;

    private Z(Iterable<T> source) {
        this.source = source;
    }

    // --- 静态工厂 ---
    public static <T> Z<T> li(Iterable<T> list) {
        return new Z<>(list);
    }

    @SafeVarargs
    public static <T> Z<T> li(T... items) {
        return new Z<>(Arrays.asList(items));
    }

    @SafeVarargs
    public static <T> List<T> asList(T... items) {
        return Arrays.asList(items);
    }

    public static Z<Integer> range(int start, int end) {
        return new Z<>(() -> new Iterator<Integer>() {
            private int current = start;
            @Override public boolean hasNext() { return current < end; }
            @Override public Integer next() { return current++; }
        });
    }

    public static <T> Z<T> iterate(T seed, UnaryOperator<T> f) {
        return new Z<>(() -> new Iterator<T>() {
            private T current = null;
            private boolean first = true;
            @Override public boolean hasNext() { return true; }
            @Override public T next() {
                if (first) { current = seed; first = false; }
                else { current = f.apply(current); }
                return current;
            }
        });
    }

    public static <T> Z<T> repeat(T element, int count) {
        return range(0, count).map(i -> element);
    }

    // --- 中间操作 (延迟执行) ---
    public <R> Z<R> map(Function<? super T, ? extends R> mapper) {
        return new Z<>(() -> new Iterator<R>() {
            private final Iterator<T> it = source.iterator();
            @Override public boolean hasNext() { return it.hasNext(); }
            @Override public R next() { return mapper.apply(it.next()); }
        });
    }

    public <R> Z<R> mapIndexed(BiFunction<Integer, ? super T, ? extends R> mapper) {
        return new Z<>(() -> new Iterator<R>() {
            private final Iterator<T> it = source.iterator();
            private int index = 0;
            @Override public boolean hasNext() { return it.hasNext(); }
            @Override public R next() { return mapper.apply(index++, it.next()); }
        });
    }

    @SuppressWarnings("unchecked")
    public <R> Z<R> mapNotNull(Function<? super T, ? extends R> mapper) {
        return (Z<R>) map(mapper).filter(Objects::nonNull);
    }

    public <R> Z<R> apply(Function<List<T>, ? extends Iterable<R>> func) {
        return Z.li(func.apply(this.toList()));
    }

    public <R> Z<R> then(Function<List<T>, ? extends Iterable<R>> func) {
        return apply(func);
    }

    public <R> Z<R> flatMap(Function<? super T, ? extends Iterable<? extends R>> mapper) {
        return new Z<>(() -> new Iterator<R>() {
            private final Iterator<T> it = source.iterator();
            private Iterator<? extends R> currentSubIt = Collections.emptyIterator();

            @Override
            public boolean hasNext() {
                while (!currentSubIt.hasNext() && it.hasNext()) {
                    currentSubIt = mapper.apply(it.next()).iterator();
                }
                return currentSubIt.hasNext();
            }

            @Override
            public R next() {
                if (!hasNext()) throw new NoSuchElementException();
                return currentSubIt.next();
            }
        });
    }

    public Z<T> filter(Predicate<? super T> predicate) {
        return new Z<>(() -> new Iterator<T>() {
            private final Iterator<T> it = source.iterator();
            private T nextElem;
            private boolean hasNextSet = false;

            @Override
            public boolean hasNext() {
                while (!hasNextSet && it.hasNext()) {
                    T t = it.next();
                    if (predicate.test(t)) {
                        nextElem = t;
                        hasNextSet = true;
                    }
                }
                return hasNextSet;
            }

            @Override
            public T next() {
                if (!hasNextSet && !hasNext()) throw new NoSuchElementException();
                hasNextSet = false;
                T res = nextElem;
                nextElem = null;
                return res;
            }
        });
    }

    public Z<T> filterIndexed(BiPredicate<Integer, ? super T> predicate) {
        return new Z<>(() -> new Iterator<T>() {
            private final Iterator<T> it = source.iterator();
            private int index = 0;
            private T nextElem;
            private boolean hasNextSet = false;

            @Override
            public boolean hasNext() {
                while (!hasNextSet && it.hasNext()) {
                    T t = it.next();
                    if (predicate.test(index++, t)) {
                        nextElem = t;
                        hasNextSet = true;
                    }
                }
                return hasNextSet;
            }

            @Override
            public T next() {
                if (!hasNextSet && !hasNext()) throw new NoSuchElementException();
                hasNextSet = false;
                T res = nextElem;
                nextElem = null;
                return res;
            }
        });
    }

    public Z<T> distinct() {
        return distinct(Function.identity());
    }

    public Z<T> distinct(Function<? super T, ?> keyExtractor) {
        return new Z<>(() -> new Iterator<T>() {
            private final Iterator<T> it = source.iterator();
            private final Set<Object> seen = new HashSet<>();
            private T nextElem;
            private boolean hasNextSet = false;

            @Override
            public boolean hasNext() {
                while (!hasNextSet && it.hasNext()) {
                    T t = it.next();
                    if (seen.add(keyExtractor.apply(t))) {
                        nextElem = t;
                        hasNextSet = true;
                    }
                }
                return hasNextSet;
            }

            @Override
            public T next() {
                if (!hasNextSet && !hasNext()) throw new NoSuchElementException();
                hasNextSet = false;
                T res = nextElem;
                nextElem = null;
                return res;
            }
        });
    }

    public Z<T> limit(long n) {
        return new Z<>(() -> new Iterator<T>() {
            private final Iterator<T> it = source.iterator();
            private long count = 0;
            @Override public boolean hasNext() { return count < n && it.hasNext(); }
            @Override public T next() {
                if (!hasNext()) throw new NoSuchElementException();
                count++;
                return it.next();
            }
        });
    }

    public Z<T> skip(long n) {
        return new Z<>(() -> new Iterator<T>() {
            private final Iterator<T> it = source.iterator();
            private boolean skipped = false;
            @Override public boolean hasNext() {
                if (!skipped) {
                    for (int i = 0; i < n && it.hasNext(); i++) it.next();
                    skipped = true;
                }
                return it.hasNext();
            }
            @Override public T next() { return it.next(); }
        });
    }

    public Z<T> sub(int start) { return skip(start); }
    public Z<T> sub(int start, int end) { return skip(start).limit(end - start); }

    public Z<List<T>> partition(int size) {
        return new Z<>(() -> new Iterator<List<T>>() {
            private final Iterator<T> it = source.iterator();
            @Override public boolean hasNext() { return it.hasNext(); }
            @Override public List<T> next() {
                List<T> chunk = new ArrayList<>(size);
                for (int i = 0; i < size && it.hasNext(); i++) chunk.add(it.next());
                return chunk;
            }
        });
    }

    public Z<List<T>> split(int size) { return partition(size); }

    public Z<T> peek(Consumer<? super T> consumer) {
        return new Z<>(() -> new Iterator<T>() {
            private final Iterator<T> it = source.iterator();
            @Override public boolean hasNext() { return it.hasNext(); }
            @Override public T next() {
                T t = it.next();
                consumer.accept(t);
                return t;
            }
        });
    }

    public Z<T> peekStream(Consumer<Z<T>> streamConsumer) {
        return new Z<>(() -> {
            List<T> list = new ArrayList<>();
            for (T t : source) list.add(t);
            streamConsumer.accept(Z.li(list));
            return list.iterator();
        });
    }

    public Z<T> takeWhile(Predicate<? super T> p) {
        return new Z<>(() -> new Iterator<T>() {
            private final Iterator<T> it = source.iterator();
            private T nextElem;
            private boolean hasNextSet = false;
            private boolean finished = false;
            @Override public boolean hasNext() {
                if (finished) return false;
                if (hasNextSet) return true;
                if (it.hasNext()) {
                    T t = it.next();
                    if (p.test(t)) {
                        nextElem = t;
                        hasNextSet = true;
                        return true;
                    }
                }
                finished = true;
                return false;
            }
            @Override public T next() {
                if (!hasNext()) throw new NoSuchElementException();
                hasNextSet = false;
                return nextElem;
            }
        });
    }

    public Z<T> dropWhile(Predicate<? super T> p) {
        return new Z<>(() -> new Iterator<T>() {
            private final Iterator<T> it = source.iterator();
            private boolean dropped = false;
            private T nextElem;
            private boolean hasNextSet = false;
            @Override public boolean hasNext() {
                if (hasNextSet) return true;
                if (!dropped) {
                    while (it.hasNext()) {
                        T t = it.next();
                        if (!p.test(t)) {
                            nextElem = t;
                            hasNextSet = true;
                            dropped = true;
                            return true;
                        }
                    }
                    dropped = true;
                    return false;
                }
                return it.hasNext();
            }
            @Override public T next() {
                if (hasNextSet) { hasNextSet = false; return nextElem; }
                return it.next();
            }
        });
    }

    public Z<T> concat(Iterable<T> other) {
        return new Z<>(() -> new Iterator<T>() {
            private final Iterator<T> it1 = source.iterator();
            private final Iterator<T> it2 = other.iterator();
            @Override public boolean hasNext() { return it1.hasNext() || it2.hasNext(); }
            @Override public T next() { return it1.hasNext() ? it1.next() : it2.next(); }
        });
    }

    public Z<T> add(T item) {
        return concat(Collections.singletonList(item));
    }

    public <U, R> Z<R> zip(Iterable<U> other, BiFunction<? super T, ? super U, ? extends R> joiner) {
        return new Z<>(() -> new Iterator<R>() {
            private final Iterator<T> it1 = source.iterator();
            private final Iterator<U> it2 = other.iterator();
            @Override public boolean hasNext() { return it1.hasNext() && it2.hasNext(); }
            @Override public R next() { return joiner.apply(it1.next(), it2.next()); }
        });
    }

    public Z<T> intersect(Iterable<T> other) {
        return new Z<>(() -> {
            Set<T> set = new HashSet<>();
            for (T t : other) set.add(t);
            return Z.li(this).filter(set::contains).iterator();
        });
    }

    public Z<T> union(Iterable<T> other) {
        return concat(other).distinct();
    }

    public Z<T> minus(Iterable<T> other) {
        return new Z<>(() -> {
            Set<T> set = new HashSet<>();
            for (T t : other) set.add(t);
            return Z.li(this).filter(t -> !set.contains(t)).iterator();
        });
    }

    public <V> Z<T> filter(Function<? super T, V> extractor, Predicate<V> valuePredicate) {
        return filter(e -> valuePredicate.test(extractor.apply(e)));
    }

    // --- 快捷过滤 ---
    public Z<T> isNull(Function<? super T, ?> extractor) { return filter(extractor, Objects::isNull); }
    public Z<T> filterNull(Function<? super T, ?> extractor) { return isNull(extractor); }
    public Z<T> isNotNull(Function<? super T, ?> extractor) { return filter(extractor, Objects::nonNull); }
    public Z<T> filterNotNull(Function<? super T, ?> extractor) { return isNotNull(extractor); }
    
    private boolean isBlankObj(Object obj) {
        if (obj == null) return true;
        if (obj instanceof String) return ((String) obj).trim().isEmpty();
        return false;
    }

    public Z<T> isBlank(Function<? super T, ?> extractor) { return filter(extractor, this::isBlankObj); }
    public Z<T> filterBlank(Function<? super T, ?> extractor) { return isBlank(extractor); }
    public Z<T> isNotBlank(Function<? super T, ?> extractor) { return filter(extractor, e -> !isBlankObj(e)); }
    public Z<T> filterNotBlank(Function<? super T, ?> extractor) { return isNotBlank(extractor); }

    @SafeVarargs
    public final Z<T> filters(Predicate<? super T>... predicates) {
        return filter(t -> {
            for (Predicate<? super T> p : predicates) if (!p.test(t)) return false;
            return true;
        });
    }

    @SafeVarargs
    public final Z<T> ands(Predicate<? super T>... predicates) {
        return filters(predicates);
    }

    @SafeVarargs
    public final Z<T> filterOrs(Predicate<? super T>... predicates) {
        return filter(t -> {
            for (Predicate<? super T> p : predicates) if (p.test(t)) return true;
            return false;
        });
    }

    @SafeVarargs
    public final Z<T> ors(Predicate<? super T>... predicates) {
        return filterOrs(predicates);
    }

    // --- 排序 (延迟执行，但在迭代开始时会进行阻塞式收集) ---
    public enum Sort { Asc, Desc, NullFirst, NullLast }

    public Z<T> sort(Comparator<? super T> comparator) {
        return new Z<>(() -> {
            List<T> list = new ArrayList<>();
            for (T item : source) list.add(item);
            list.sort(comparator);
            return list.iterator();
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> Comparator<T> buildBaseComparator(Function<? super T, ? extends Comparable> extractor, Sort... options) {
        boolean desc = false;
        boolean nullFirst = false;
        for (Sort s : options) {
            if (s == Sort.Desc) desc = true;
            else if (s == Sort.NullFirst) nullFirst = true;
        }
        final boolean isDesc = desc;
        final boolean isNullFirst = nullFirst;

        return (a, b) -> {
            Comparable v1 = extractor.apply(a);
            Comparable v2 = extractor.apply(b);
            if (v1 == v2) return 0;
            if (v1 == null) return isNullFirst ? -1 : 1;
            if (v2 == null) return isNullFirst ? 1 : -1;
            int res = v1.compareTo(v2);
            return isDesc ? -res : res;
        };
    }

    public Z<T> sort(Function<? super T, ? extends Comparable> extractor, Sort... options) {
        return sort(buildBaseComparator(extractor, options));
    }

    public Z<T> sortAsc(Function<? super T, ? extends Comparable> extractor) { return sort(extractor, Sort.Asc); }
    public Z<T> sortDesc(Function<? super T, ? extends Comparable> extractor) { return sort(extractor, Sort.Desc); }

    public Z<T> reversed() {
        return new Z<>(() -> {
            List<T> list = new ArrayList<>();
            for (T item : source) list.add(item);
            Collections.reverse(list);
            return list.iterator();
        });
    }

    public Z<T> shuffled() {
        return new Z<>(() -> {
            List<T> list = new ArrayList<>();
            for (T item : source) list.add(item);
            Collections.shuffle(list);
            return list.iterator();
        });
    }

    public interface ComparatorContext<T> {
        default Comparator<T> createComparator(Function<? super T, ? extends Comparable> extractor, Sort... options) {
            return buildBaseComparator(extractor, options);
        }
    }

    @SafeVarargs
    public final Z<T> sort(Function<ComparatorContext<T>, Comparator<T>>... builders) {
        ComparatorContext<T> ctx = new ComparatorContext<T>() {};
        Comparator<T> combined = null;
        for (Function<ComparatorContext<T>, Comparator<T>> b : builders) {
            combined = (combined == null) ? b.apply(ctx) : combined.thenComparing(b.apply(ctx));
        }
        return sort(combined);
    }

    // --- 终端操作 ---
    public List<T> toList() {
        List<T> result;
        if (source instanceof Collection) {
            result = new ArrayList<>(((Collection<T>) source).size());
        } else {
            result = new ArrayList<>();
        }
        for (T item : this) result.add(item);
        return result;
    }

    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> toMap() {
        if (intermediateResult instanceof Supplier) {
            return (Map<K, V>) ((Supplier<?>) intermediateResult).get();
        }
        if (intermediateResult instanceof Map) return (Map<K, V>) intermediateResult;
        return Collections.emptyMap();
    }

    public Set<T> toSet() {
        Set<T> set = new LinkedHashSet<>();
        for (T t : this) set.add(t);
        return set;
    }

    public Set<T> toTreeSet(Comparator<? super T> comparator) {
        Set<T> set = new TreeSet<>(comparator);
        for (T t : this) set.add(t);
        return set;
    }

    public <K> Map<K, T> toMap(Function<? super T, ? extends K> keyMapper) { return toMap(keyMapper, Function.identity(), (u, v) -> v); }
    public <K, V> Map<K, V> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) { return toMap(keyMapper, valueMapper, (u, v) -> v); }
    public <K, V> Map<K, V> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper, BinaryOperator<V> merge) {
        Map<K, V> map = new HashMap<>();
        for (T t : this) map.merge(keyMapper.apply(t), valueMapper.apply(t), merge);
        return map;
    }

    public <K, V> Map<K, V> toLinkedMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        Map<K, V> map = new LinkedHashMap<>();
        for (T t : this) map.put(keyMapper.apply(t), valueMapper.apply(t));
        return map;
    }

    public Map<Boolean, List<T>> partitionBy(Predicate<? super T> p) {
        Map<Boolean, List<T>> res = new HashMap<>();
        res.put(true, new ArrayList<>());
        res.put(false, new ArrayList<>());
        for (T t : this) res.get(p.test(t)).add(t);
        return res;
    }

    public Map<Boolean, List<T>> splitBy(Predicate<? super T> p) { return partitionBy(p); }

    public long count() {
        if (source instanceof Collection) return ((Collection<?>) source).size();
        long count = 0;
        for (T ignored : this) count++;
        return count;
    }

    public Optional<T> findFirst() {
        Iterator<T> it = iterator();
        return it.hasNext() ? Optional.of(it.next()) : Optional.empty();
    }

    public T getFirst() { return findFirst().orElse(null); }
    public T getFirst(T defaultValue) { return findFirst().orElse(defaultValue); }
    public T getFirst(Supplier<? extends T> supplier) { return findFirst().orElseGet(supplier); }

    public T get(int index) {
        if (source instanceof List && index >= 0) {
            List<T> l = (List<T>) source;
            return l.size() > index ? l.get(index) : null;
        }
        int i = 0;
        for (T t : this) if (i++ == index) return t;
        return null;
    }

    public T getEnd() {
        if (source instanceof List) {
            List<T> l = (List<T>) source;
            return l.isEmpty() ? null : l.get(l.size() - 1);
        }
        T last = null;
        for (T t : this) last = t;
        return last;
    }
    public T getEnd(T defaultValue) {
        T end = getEnd();
        return end != null ? end : defaultValue;
    }
    public T getEnd(Supplier<? extends T> supplier) {
        T end = getEnd();
        return end != null ? end : supplier.get();
    }

    public boolean anyMatch(Predicate<? super T> p) { for (T t : this) if (p.test(t)) return true; return false; }
    public boolean noneMatch(Predicate<? super T> p) { return !anyMatch(p); }
    public boolean allMatch(Predicate<? super T> p) { for (T t : this) if (!p.test(t)) return false; return true; }

    public boolean contains(T value) {
        for (T t : this) if (Objects.equals(t, value)) return true;
        return false;
    }

    public Map<T, Long> frequency() {
        Map<T, Long> map = new HashMap<>();
        for (T t : this) map.merge(t, 1L, Long::sum);
        return map;
    }

    public Optional<T> min(Comparator<? super T> comparator) {
        T min = null;
        for (T t : this) {
            if (min == null || comparator.compare(t, min) < 0) min = t;
        }
        return Optional.ofNullable(min);
    }

    public <V extends Comparable<V>> Optional<T> minBy(Function<? super T, V> extractor) {
        return min((a, b) -> extractor.apply(a).compareTo(extractor.apply(b)));
    }

    public Optional<T> max(Comparator<? super T> comparator) {
        T max = null;
        for (T t : this) {
            if (max == null || comparator.compare(t, max) > 0) max = t;
        }
        return Optional.ofNullable(max);
    }

    public <V extends Comparable<V>> Optional<T> maxBy(Function<? super T, V> extractor) {
        return max((a, b) -> extractor.apply(a).compareTo(extractor.apply(b)));
    }

    public double average() {
        long count = 0;
        double sum = 0;
        for (T t : this) {
            if (t instanceof Number) {
                sum += ((Number) t).doubleValue();
                count++;
            }
        }
        return count == 0 ? 0 : sum / count;
    }

    public DoubleSummaryStatistics summarizing() {
        DoubleSummaryStatistics stats = new DoubleSummaryStatistics();
        for (T t : this) {
            if (t instanceof Number) stats.accept(((Number) t).doubleValue());
        }
        return stats;
    }

    public void forEach(Consumer<? super T> action) { for (T t : this) action.accept(t); }

    public void forEachIndexed(BiConsumer<Integer, ? super T> action) {
        int index = 0;
        for (T t : this) action.accept(index++, t);
    }

    public <R> R reduce(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator) {
        R result = supplier.get();
        for (T t : this) accumulator.accept(result, t);
        return result;
    }

    public <R> R fold(R initial, BiFunction<R, ? super T, R> accumulator) {
        R result = initial;
        for (T t : this) result = accumulator.apply(result, t);
        return result;
    }

    public <V, R> R reduce(Supplier<R> supplier, Function<? super T, V> mapper, BiConsumer<R, V> accumulator) {
        R result = supplier.get();
        for (T t : this) accumulator.accept(result, mapper.apply(t));
        return result;
    }

    public String joining(String delimiter) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (T t : this) {
            if (!first) sb.append(delimiter);
            sb.append(t);
            first = false;
        }
        return sb.toString();
    }

    // --- 数学聚合 ---
    public int sumInt() { return sumInt(e -> (e instanceof Number) ? ((Number) e).intValue() : 0); }
    public int sumInt(ToIntFunction<? super T> mapper) {
        int sum = 0;
        for (T t : this) sum += mapper.applyAsInt(t);
        return sum;
    }
    public long sumLong() { return sumLong(e -> (e instanceof Number) ? ((Number) e).longValue() : 0L); }
    public long sumLong(ToLongFunction<? super T> mapper) {
        long sum = 0;
        for (T t : this) sum += mapper.applyAsLong(t);
        return sum;
    }
    public double sumDouble() { return sumDouble(e -> (e instanceof Number) ? ((Number) e).doubleValue() : 0.0); }
    public double sumDouble(ToDoubleFunction<? super T> mapper) {
        double sum = 0;
        for (T t : this) sum += mapper.applyAsDouble(t);
        return sum;
    }

    // --- 分组增强 (全部延迟加载) ---
    public <K> ZMap<K, T> groupBy(Function<? super T, ? extends K> classifier) {
        return new ZMap<>(() -> {
            Map<K, List<T>> map = new LinkedHashMap<>();
            for (T t : this) map.computeIfAbsent(classifier.apply(t), k -> new ArrayList<>()).add(t);
            return map;
        });
    }

    private <R> Z<T> asLazyMap(Supplier<R> supplier) {
        Z<T> res = new Z<>(source);
        res.intermediateResult = supplier;
        return res;
    }

    public <K, V> Z<T> groupBy(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return asLazyMap((Supplier<Map<K, List<V>>>) () -> {
            Map<K, List<V>> map = new LinkedHashMap<>();
            for (T t : source) {
                map.computeIfAbsent(keyMapper.apply(t), k -> new ArrayList<>()).add(valueMapper.apply(t));
            }
            return map;
        });
    }

    public <K, A, D> Z<T> groupingBy(Function<? super T, ? extends K> classifier, Collector<? super T, A, D> downstream) {
        return asLazyMap((Supplier<Map<K, D>>) () -> {
            Map<K, A> container = new HashMap<>();
            for (T t : source) {
                K key = classifier.apply(t);
                A acc = container.computeIfAbsent(key, k -> downstream.supplier().get());
                downstream.accumulator().accept(acc, t);
            }
            Map<K, D> result = new HashMap<>();
            container.forEach((k, a) -> result.put(k, downstream.finisher().apply(a)));
            return result;
        });
    }

    public static class ZMap<K, V> {
        private final Supplier<Map<K, List<V>>> mapSupplier;
        ZMap(Supplier<Map<K, List<V>>> mapSupplier) { this.mapSupplier = mapSupplier; }

        public <R> ZMapTransformed<K, R> valueStream(Function<Z<V>, R> mapper) {
            return new ZMapTransformed<>(() -> {
                Map<K, List<V>> map = mapSupplier.get();
                Map<K, R> res = new LinkedHashMap<>();
                map.forEach((k, v) -> res.put(k, mapper.apply(Z.li(v))));
                return res;
            });
        }

        public ZMap<K, V> peekStream(Consumer<Map<K, List<V>>> consumer) {
            return new ZMap<>(() -> {
                Map<K, List<V>> map = mapSupplier.get();
                consumer.accept(map);
                return map;
            });
        }

        public Map<K, List<V>> toMap() { return mapSupplier.get(); }
    }

    public static class ZMapTransformed<K, R> {
        private final Supplier<Map<K, R>> finalMapSupplier;
        ZMapTransformed(Supplier<Map<K, R>> finalMapSupplier) { this.finalMapSupplier = finalMapSupplier; }

        public ZMapTransformed<K, R> peekStream(Consumer<Map<K, R>> consumer) {
            return new ZMapTransformed<>(() -> {
                Map<K, R> map = finalMapSupplier.get();
                consumer.accept(map);
                return map;
            });
        }

        public Map<K, R> toMap() { return finalMapSupplier.get(); }
    }

    public static class liStream {
        public static long count(Z<?> z) { return z.count(); }
    }

    @Override
    public Iterator<T> iterator() { return source.iterator(); }
}
