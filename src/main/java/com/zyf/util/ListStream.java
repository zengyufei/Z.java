package com.zyf.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;

/**
 * 一个功能强大且易用的流式处理工具类，提供类似 Java 8 Stream 的链式操作，
 * 但针对集合处理进行了增强，并采用了更加直观的 API 设计。
 * 支持延迟加载（Lazy Evaluation），大部分中间操作仅在触发终端操作时执行。
 *
 * @param <T> 流中元素的类型
 */
public class ListStream<T> {

    /** 原始数据源，可以是任何可迭代对象 */
    private final Iterable<T> source;

    /**
     * 包内构造函数，建议使用静态工厂方法 {@link #of(Iterable)} 创建实例
     *
     * @param source 数据源
     */
    ListStream(Iterable<T> source) {
        this.source = source;
    }

    /**
     * 创建一个新的 ListStream 实例
     *
     * @param source 实现了 Iterable 接口的数据源，不能为 null
     * @param <T>    元素类型
     * @return 封装了数据源的 ListStream 对象
     */
    public static <T> ListStream<T> of(Iterable<T> source) {
        return new ListStream<>(Objects.requireNonNull(source));
    }

    // ================================ 过滤 (Filtering)  ==================================
    // ====================================================================================
    //  filter { predicate }: 返回一个新的列表，包含所有满足给定条件的元素。

    /**
     * 【别名】根据多个谓词条件过滤元素，所有条件必须同时满足（逻辑与）
     *
     * @param predicates 谓词数组
     * @return 过滤后的 ListStream
     */
    @SafeVarargs
    public final ListStream<T> ands(Predicate<T>... predicates) {
        return filters(predicates);
    }

    /**
     * 过滤满足给定条件的元素
     *
     * @param predicate 过滤谓词
     * @return 过滤后的 ListStream
     */
    public ListStream<T> filter(Predicate<? super T> predicate) {
        return filters(predicate);
    }

    /**
     * 根据多个谓词条件过滤元素，所有条件必须同时满足（逻辑与）
     * 优化：采用循环遍历谓词，避免在高频调用下产生大量 Stream 对象
     *
     * @param predicates 谓词数组
     * @return 过滤后的 ListStream
     */
    @SafeVarargs
    public final ListStream<T> filters(Predicate<? super T>... predicates) {
        Objects.requireNonNull(predicates);
        if (predicates.length == 0) return this;
        return of(createFilteredIterable(elem -> {
            // 手动循环检查每个谓词，一旦不满足则立即返回 false (Short-circuiting)
            for (Predicate<? super T> p : predicates) {
                if (!p.test(elem)) return false;
            }
            return true;
        }));
    }

    //  filterOrs { predicate }: 返回一个新的列表，包含任意满足给定条件的元素。

    // 过滤或的实现
    /**
     * 【别名】根据多个谓词条件过滤元素，满足任意一个条件即可（逻辑或）
     *
     * @param predicates 谓词数组
     * @return 过滤后的 ListStream
     */
    @SafeVarargs
    public final ListStream<T> ors(Predicate<T>... predicates) {
        return filterOrs(predicates);
    }

    /**
     * 根据多个谓词条件过滤元素，满足任意一个条件即可（逻辑或）
     * 优化：采用循环遍历谓词，支持短路逻辑
     *
     * @param predicates 谓词数组
     * @return 过滤后的 ListStream
     */
    @SafeVarargs
    public final ListStream<T> filterOrs(Predicate<T>... predicates) {
        if (predicates == null || predicates.length == 0) return this;
        return of(createFilteredIterable(elem -> {
            // 只要有一个谓词满足，即视为匹配成功
            for (Predicate<T> p : predicates) {
                if (p.test(elem)) return true;
            }
            return false;
        }));
    }

    //  filterNot { predicate }: 返回一个新列表，包含所有不满足给定条件的元素。

    /**
     * 过滤不满足给定条件的元素
     *
     * @param predicate 过滤谓词
     * @return 过滤后的 ListStream
     */
    public ListStream<T> filterNot(Predicate<? super T> predicate) {
        return filterNots(predicate);
    }

    /**
     * 过滤不满足任何给定条件的元素（即所有条件都不满足时才保留）
     *
     * @param predicates 谓词数组
     * @return 过滤后的 ListStream
     */
    @SafeVarargs
    public final ListStream<T> filterNots(Predicate<? super T>... predicates) {
        Objects.requireNonNull(predicates);
        return of(createFilteredIterable(elem -> {
            for (Predicate<? super T> p : predicates) {
                if (p.test(elem)) return false;
            }
            return true;
        }));
    }


    //  filterNull(): 返回一个新列表，其中包含null元素。

    /**
     * 仅保留数据源中为 null 的元素
     *
     * @return 只包含 null 的 ListStream
     */
    public final ListStream<T> filterNull() {
        return of(createFilteredIterable(Objects::isNull));
    }

    /**
     * 根据提取的属性是否为 null 进行过滤
     *
     * @param function 属性提取器
     * @return 过滤后的 ListStream
     */
    public final ListStream<T> filterNull(Function<T, ?> function) {
        return filterNulls(function);
    }

    /**
     * 根据提取的多个属性是否全部为 null 进行过滤
     *
     * @param functions 多个属性提取器
     * @return 过滤后的 ListStream
     */
    @SafeVarargs
    public final ListStream<T> filterNulls(Function<T, ?>... functions) {
        Objects.requireNonNull(functions);
        return of(createFilteredIterable(elem -> {
            for (Function<T, ?> fun : functions) {
                if (fun.apply(elem) != null) return false;
            }
            return true;
        }));
    }

    //  filterNotNull(): 返回一个新列表，其中不包含null元素。

    /**
     * 移除所有为 null 的元素
     *
     * @return 不包含 null 的 ListStream
     */
    public final ListStream<T> filterNotNull() {
        return of(createFilteredIterable(Objects::nonNull));
    }

    /**
     * 根据提取的属性是否不为 null 进行过滤
     *
     * @param function 属性提取器
     * @return 过滤后的 ListStream
     */
    public final ListStream<T> filterNotNull(Function<T, ?> function) {
        return filterNotNulls(function);
    }

    /**
     * 根据提取的多个属性是否全部不为 null 进行过滤
     *
     * @param functions 多个属性提取器
     * @return 过滤后的 ListStream
     */
    @SafeVarargs
    public final ListStream<T> filterNotNulls(Function<T, ?>... functions) {
        Objects.requireNonNull(functions);
        return of(createFilteredIterable(elem -> {
            for (Function<T, ?> fun : functions) {
                if (fun.apply(elem) == null) return false;
            }
            return true;
        }));
    }


    //  filterIndexed { index, value -> predicate }: 类似filter，但谓词同时接收元素的索引。

    /**
     * 带索引的过滤。谓词接收元素的当前索引和元素本身。
     *
     * @param predicates 带索引的过滤谓词
     * @return 过滤后的 ListStream
     */
    @SafeVarargs
    public final ListStream<T> filterIndexeds(BiPredicate<Integer, ? super T>... predicates) {
        Objects.requireNonNull(predicates);
        if (predicates.length == 0) return this;
        return of(createFilteredIterable((index, elem) -> {
            for (BiPredicate<Integer, ? super T> p : predicates) {
                if (!p.test(index, elem)) return false;
            }
            return true;
        }));
    }

    /**
     * 过滤流中属于指定类类型的元素，并将其转换为对应的子类型。
     * 这是一个中间操作，支持延迟计算。
     *
     * @param classOfR 目标类型的 Class 对象
     * @param <R>      目标子类型
     * @return 包含目标类型元素的 ListStream<R>
     */
    public final <R> ListStream<R> filterIsInstance(Class<R> classOfR) {
        Objects.requireNonNull(classOfR, "classOfR cannot be null");

        return ListStream.of(() -> new Iterator<>() {
            final Iterator<T> sourceIterator = source.iterator();
            R nextElement;
            boolean hasNextComputed = false;
            boolean hasNextResult = false;

            @Override
            public boolean hasNext() {
                if (!hasNextComputed) {
                    computeNext();
                }
                return hasNextResult;
            }

            @Override
            public R next() {
                if (!hasNextComputed) {
                    computeNext();
                }
                if (!hasNextResult) {
                    throw new NoSuchElementException();
                }
                hasNextComputed = false;
                return nextElement;
            }

            private void computeNext() {
                while (sourceIterator.hasNext()) {
                    T current = sourceIterator.next();
                    // 类型检查及转换
                    if (classOfR.isInstance(current)) {
                        nextElement = classOfR.cast(current);
                        hasNextResult = true;
                        hasNextComputed = true;
                        return;
                    }
                }
                hasNextResult = false;
                hasNextComputed = true;
            }
        });
    }

    /**
     * 【别名】跳过前 n 个元素
     *
     * @param n 跳过的数量
     * @return 过滤后的 ListStream
     */
    public ListStream<T> drop(int n) {
        return skip(n);
    }

    /**
     * 跳过前 n 个元素
     *
     * @param n 跳过的数量
     * @return 过滤后的 ListStream
     */
    public ListStream<T> skip(int n) {
        if (n < 0) {
            return this;
        }
        return of(createFilteredIterable((index, elem) -> index + 1 > n));
    }

    /**
     * 从起始位置开始丢弃元素，直到遇到第一个不满足条件的元素为止。从该元素开始保留剩余的所有元素。
     * 这是一个中间操作，支持延迟计算。
     *
     * @param predicate 丢弃条件谓词
     * @return 过滤后的 ListStream
     */
    public final ListStream<T> dropWhile(Predicate<T> predicate) {
        Objects.requireNonNull(predicate, "predicate cannot be null");

        return ListStream.of(() -> new Iterator<>() {
            final Iterator<T> sourceIterator = source.iterator();
            // 状态标志：是否处于丢弃元素的阶段
            boolean dropping = true;
            T nextElement;
            boolean hasNextComputed = false;
            boolean hasNextResult = false;

            @Override
            public boolean hasNext() {
                if (!hasNextComputed) {
                    computeNext();
                }
                return hasNextResult;
            }

            @Override
            public T next() {
                if (!hasNextComputed) {
                    computeNext();
                }
                if (!hasNextResult) {
                    throw new NoSuchElementException();
                }
                hasNextComputed = false;
                return nextElement;
            }

            private void computeNext() {
                while (sourceIterator.hasNext()) {
                    T current = sourceIterator.next();
                    if (dropping) {
                        // 还在丢弃阶段：如果满足条件，则继续丢弃
                        if (predicate.test(current)) {
                            continue;
                        } else {
                            // 遇到第一个不满足条件的元素，切换状态并准备返回
                            dropping = false;
                            nextElement = current;
                            hasNextResult = true;
                            hasNextComputed = true;
                            return;
                        }
                    } else {
                        // 已经过了丢弃阶段：所有后续元素直接返回
                        nextElement = current;
                        hasNextResult = true;
                        hasNextComputed = true;
                        return;
                    }
                }
                hasNextResult = false;
                hasNextComputed = true;
            }
        });
    }

    /**
     * 【别名】限制流中元素的数量，只保留前 n 个元素。
     *
     * @param n 保留的数量
     * @return 过滤后的 ListStream
     */
    public ListStream<T> take(int n) {
        return limit(n);
    }

    /**
     * 限制流中元素的数量，只保留前 n 个元素。
     *
     * @param n 保留的数量
     * @return 过滤后的 ListStream
     */
    public ListStream<T> limit(int n) {
        if (n < 0) {
            return this;
        }
        return of(createFilteredIterable((index, elem) -> index + 1 <= n));
    }

    /**
     * 获取流中连续满足条件的元素。一旦遇到第一个不满足条件的元素，流立即结束。
     * 这是一个中间操作，支持延迟计算。
     *
     * @param predicate 获取条件谓词
     * @return 包含流开头连续满足条件的元素的 ListStream
     */
    public final ListStream<T> takeWhile(Predicate<T> predicate) {
        Objects.requireNonNull(predicate, "predicate cannot be null");

        return ListStream.of(() -> new Iterator<>() {
            final Iterator<T> sourceIterator = source.iterator();
            // 状态标志：是否仍在获取阶段
            boolean taking = true;
            T nextElement;
            boolean hasNextComputed = false;
            boolean hasNextResult = false;

            @Override
            public boolean hasNext() {
                if (!hasNextComputed) {
                    computeNext();
                }
                return hasNextResult;
            }

            @Override
            public T next() {
                if (!hasNextComputed) {
                    computeNext();
                }
                if (!hasNextResult) {
                    throw new NoSuchElementException();
                }
                hasNextComputed = false;
                return nextElement;
            }

            private void computeNext() {
                if (!taking) {
                    hasNextResult = false;
                    hasNextComputed = true;
                    return;
                }

                while (sourceIterator.hasNext()) {
                    T current = sourceIterator.next();
                    if (predicate.test(current)) {
                        // 满足条件：保留并继续
                        nextElement = current;
                        hasNextResult = true;
                        hasNextComputed = true;
                        return;
                    } else {
                        // 遇到第一个不满足条件的元素：标记为结束
                        taking = false;
                        break;
                    }
                }
                hasNextResult = false;
                hasNextComputed = true;
            }
        });
    }

    /**
     * 切片操作。提取指定索引集合中的元素。
     *
     * @param indices 需要保留的索引集合
     * @return 包含指定索引对应元素的 ListStream
     */
    public final ListStream<T> slice(Collection<Integer> indices) {
        Objects.requireNonNull(indices, "indices collection cannot be null");

        return ListStream.of(() -> {
            // 在迭代开始时延迟构建索引集，确保彻底延迟且不影响原始流状态
            final Set<Integer> targetIndices = new HashSet<>(indices);
            
            return new Iterator<>() {
                final Iterator<T> sourceIterator = source.iterator();
                int currentIndex = 0;
                T nextElement;
                boolean hasNextComputed = false;
                boolean hasNextResult = false;

                @Override
                public boolean hasNext() {
                    if (!hasNextComputed) {
                        computeNext();
                    }
                    return hasNextResult;
                }

                @Override
                public T next() {
                    if (!hasNextComputed) {
                        computeNext();
                    }
                    if (!hasNextResult) {
                        throw new NoSuchElementException();
                    }
                    hasNextComputed = false;
                    return nextElement;
                }

                private void computeNext() {
                    while (sourceIterator.hasNext()) {
                        T current = sourceIterator.next();
                        if (targetIndices.contains(currentIndex)) {
                            nextElement = current;
                            hasNextResult = true;
                            hasNextComputed = true;
                            currentIndex++;
                            return;
                        }
                        currentIndex++;
                    }
                    hasNextResult = false;
                    hasNextComputed = true;
                }
            };
        });
    }

    //  slice(range): 返回一个新列表，包含指定范围内的元素。
    public final ListStream<T> slice(int startIndex, int endIndex) {
        if (startIndex < 0) {
            throw new IllegalArgumentException("startIndex cannot be negative: " + startIndex);
        }
        if (endIndex < 0) {
            throw new IllegalArgumentException("endIndex cannot be negative: " + endIndex);
        }
        if (startIndex > endIndex) {
            throw new IllegalArgumentException("startIndex (" + startIndex + ") cannot be greater than endIndex (" + endIndex + ")");
        }

        return ListStream.of(() -> new Iterator<>() {
            final Iterator<T> sourceIterator = source.iterator();
            int currentIndex = 0; // 当前遍历的元素索引
            T nextElement;
            boolean hasNextComputed = false;
            boolean hasNextResult = false;

            @Override
            public boolean hasNext() {
                if (!hasNextComputed) {
                    computeNext();
                }
                return hasNextResult;
            }

            @Override
            public T next() {
                if (!hasNextComputed) {
                    computeNext();
                }
                if (!hasNextResult) {
                    throw new NoSuchElementException();
                }
                hasNextComputed = false;
                return nextElement;
            }

            private void computeNext() {
                // 跳过 startIndex 之前的元素
                while (currentIndex < startIndex && sourceIterator.hasNext()) {
                    sourceIterator.next(); // 消耗元素
                    currentIndex++;
                }

                // 如果当前索引在有效范围内且有下一个元素
                if (currentIndex >= startIndex && currentIndex < endIndex && sourceIterator.hasNext()) {
                    nextElement = sourceIterator.next();
                    currentIndex++;
                    hasNextResult = true;
                    hasNextComputed = true;
                    return;
                }
                // 否则，没有更多元素
                hasNextResult = false;
                hasNextComputed = true;
            }
        });
    }

    //  distinct(): 返回一个新列表，包含所有唯一的元素（基于equals()）。

    public ListStream<T> distinct() {
        // 彻底的延迟初始化：将 Set 的创建移入 Iterable 的 lambda 内部
        return of(() -> {
            Set<Object> seen = new HashSet<>();
            return createFilteredIterable(elem -> {
                if (seen.contains(elem)) {
                    return false;
                }
                seen.add(elem);
                return true;
            }).iterator();
        });
    }

    //  distinctBy { selector }: 返回一个新列表，通过给定选择器函数返回的键来判断唯一性。

    public ListStream<T> distinct(Function<T, ?> keyExtractor) {
        // 彻底的延迟初始化：将 Set 的创建移入 Iterable 的 lambda 内部
        return of(() -> {
            Set<Object> seen = new HashSet<>();
            return createFilteredIterable(elem -> {
                Object key = keyExtractor.apply(elem);
                if (seen.contains(key)) {
                    return false;
                }
                seen.add(key);
                return true;
            }).iterator();
        });
    }

    // ====================================================================================
    // ====================================================================================


    // ================================ 映射 (Mapping)  ==================================
    // ====================================================================================
    /**
     * 元素映射。将流中的每个元素通过映射函数转换为另一种形式。
     *
     * @param mapper 映射函数
     * @param <R>    映射后的元素类型
     * @return 映射后的 ListStream
     */
    public <R> ListStream<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper);
        return of(() -> new Iterator<>() {
            final Iterator<T> iterator = source.iterator();

            public boolean hasNext() {
                return iterator.hasNext();
            }

            public R next() {
                return mapper.apply(iterator.next());
            }
        });
    }

    /**
     * 带索引的映射。映射函数同时接收元素的当前索引和元素本身。
     *
     * @param mapper 带索引的映射函数
     * @param <R>    映射后的元素类型
     * @return 映射后的 ListStream
     */
    public <R> ListStream<R> mapIndexed(BiFunction<Integer, ? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper);
        return of(() -> new Iterator<>() {
            final Iterator<T> iterator = source.iterator();
            int index = 0;

            public boolean hasNext() {
                return iterator.hasNext();
            }

            public R next() {
                final R r = mapper.apply(index++, iterator.next());
                return r;
            }
        });
    }

    //  mapNotNull { transform }: 类似map，但会过滤掉转换后为null的元素。

    public <R> ListStream<R> mapNotNull(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper);
        return of(() -> new Iterator<>() {
            final Iterator<T> iterator = source.iterator();
            R nextElement;
            boolean hasNextComputed = false;
            boolean hasNextResult = false;

            @Override
            public boolean hasNext() {
                if (!hasNextComputed) {
                    computeNext(Objects::nonNull);
                }
                return hasNextResult;
            }

            @Override
            public R next() {
                if (!hasNextComputed) {
                    computeNext(Objects::nonNull);
                }
                if (!hasNextResult) {
                    throw new NoSuchElementException();
                }
                hasNextComputed = false;
                return nextElement;
            }

            private void computeNext(Predicate<R> condition) {
                while (iterator.hasNext()) {
                    final T next = iterator.next();
                    R elem = mapper.apply(next);
                    if (condition.test(elem)) {
                        nextElement = elem;
                        hasNextResult = true;
                        hasNextComputed = true;
                        return;
                    }
                }
                hasNextResult = false;
                hasNextComputed = true;
            }
        });
    }

    //  flatMap { transform }: 将每个元素映射为一个Iterable，然后将所有结果展平到一个列表中。

    public <R> ListStream<R> flatMap(Function<? super T, ? extends Iterable<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");

        return of(() -> new Iterator<>() {
            final Iterator<T> iterator = source.iterator();
            private Iterator<? extends R> currentIterator = Collections.emptyIterator();
            private boolean hasNextComputed;
            private boolean hasNextResult;

            @Override
            public boolean hasNext() {
                if (!hasNextComputed) {
                    computeNext();
                }
                return hasNextResult;
            }

            @Override
            public R next() {
                if (!hasNextComputed) {
                    computeNext();
                }
                if (!hasNextResult) {
                    throw new NoSuchElementException();
                }
                hasNextComputed = false;
                return currentIterator.next();
            }

            private void computeNext() {
                while (!currentIterator.hasNext() && iterator.hasNext()) {
                    T nextElement = iterator.next();
                    if (nextElement != null) {
                        Iterable<? extends R> nextIterable = mapper.apply(nextElement);
                        if (nextIterable != null) {
                            currentIterator = nextIterable.iterator();
                        }
                    }
                }
                hasNextResult = currentIterator.hasNext();
                hasNextComputed = true;
            }
        });
    }

    //  zip(other): 将两个列表的元素按位置配对，生成一个Pair的列表。

    public <U> ListPair<T, U, Pair<T, U>> zip(Iterable<U> other) {
        Objects.requireNonNull(other, "list cannot be null");
        return ListPair.of(() -> new Iterator<>() {
            final Iterator<T> it1 = source.iterator();
            final Iterator<U> it2 = other.iterator();

            @Override
            public boolean hasNext() {
                return it1.hasNext() && it2.hasNext();
            }

            @Override
            public Pair<T, U> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return Pair.of(it1.next(), it2.next());
            }
        });
    }

    //  unzip(): 将一个Pair列表解构为两个列表（第一个元素一个列表，第二个元素一个列表）。
    public final <A, B> Pair<List<A>, List<B>> unzip() {
        final List<A> firstElements = new ArrayList<>();
        final List<B> secondElements = new ArrayList<>();

        // 遍历原始 ListStream，并解构每个 Pair
        for (final T element : source) {
            try {
                @SuppressWarnings("unchecked") // 运行时类型擦除，此处需要抑制警告
                Pair<A, B> pair = (Pair<A, B>) element;
                firstElements.add(pair.first);
                secondElements.add(pair.second);
            } catch (ClassCastException e) {
                throw new IllegalStateException("ListStream elements must be of type Pair to call unzip(). " +
                        "Encountered non-Pair element: " + element, e);
            }
        }

        // 直接返回包含两个 List 的 Pair
        return Pair.of(firstElements, secondElements);
    }

    // ====================================================================================
    // ====================================================================================


    // ================================ 聚合 (Aggregating / Reducing)  ==================================
    // ====================================================================================
    //  reduce { acc, value -> operation }: 从第一个元素开始，将元素累计起来，每次使用上一次的累计值和当前元素进行操作。


    /**
     * 基础聚合操作。将流中的元素通过映射后，使用累加器进行聚合。
     *
     * @param supplier 结果提供者（初始值）
     * @param func     元素转换函数
     * @param function 聚合累加函数
     * @param <R>      最终结果类型
     * @param <E>      中间转换类型
     * @return 聚合后的结果
     */
    public <S, E, R> R reduce(Supplier<R> supplier, Function<T, E> func, BiFunction<R, E, R> function) {
        R r = supplier.get();
        for (T t : source) {
            E e = func.apply(t);
            r = function.apply(r, e);
        }
        return r;
    }

    public <S, E, R> R reduce(Supplier<R> supplier, Function<T, E> func, BiConsumer<R, E> consumer) {
        R r = supplier.get();
        for (T t : source) {
            E e = func.apply(t);
            consumer.accept(r, e);
        }
        return r;
    }

    public <R> R reduce(Supplier<R> func, BiFunction<R, T, R> function) {
        R r = func.get();
        for (T t : source) {
            r = function.apply(r, t);
        }
        return r;
    }

    public <R> R reduce(Supplier<R> func, BiConsumer<R, T> consumer) {
        R r = func.get();
        for (T t : source) {
            consumer.accept(r, t);
        }
        return r;
    }

    public <E, R> List<R> reduceList(Function<T, E> func, BiConsumer<List<R>, E> consumer) {
        List<R> rs = new ArrayList<>();
        for (T t : source) {
            E e = func.apply(t);
            consumer.accept(rs, e);
        }
        return rs;
    }

    public <R> List<R> reduceList(Function<T, R> func) {
        List<R> rs = new ArrayList<>();
        for (T t : source) {
            R e = func.apply(t);
            rs.add(e);
        }
        return rs;
    }


    public <E, R> Set<R> reduceSet(Function<T, E> func, BiConsumer<Set<R>, E> consumer) {
        Set<R> rs = new HashSet<>();
        for (T t : source) {
            E e = func.apply(t);
            consumer.accept(rs, e);
        }
        return rs;
    }

    public <R> Set<R> reduceSet(Function<T, R> func) {
        Set<R> rs = new HashSet<>();
        for (T t : source) {
            R e = func.apply(t);
            rs.add(e);
        }
        return rs;
    }

    //  reduceIndexed { index, acc, value -> operation }: 类似reduce，但操作函数同时接收元素的索引。


    public <E, R> R reduceIndexed(Supplier<R> supplier, Function<T, E> func, BBiFunction<Integer, R, E, R> function) {
        R r = supplier.get();
        int index = 0;
        for (T t : source) {
            E e = func.apply(t);
            r = function.apply(index++, r, e);
        }
        return r;
    }

    public <S, E, R> R reduceIndexed(Supplier<R> supplier, Function<T, E> func, BBiConsumer<Integer, R, E> consumer) {
        R r = supplier.get();
        int index = 0;
        for (T t : source) {
            E e = func.apply(t);
            consumer.accept(index++, r, e);
        }
        return r;
    }

    public <R> R reduceIndexed(Supplier<R> func, BBiFunction<Integer, R, T, R> function) {
        R r = func.get();
        int index = 0;
        for (T t : source) {
            r = function.apply(index++, r, t);
        }
        return r;
    }

    public <R> R reduceIndexed(Supplier<R> func, BBiConsumer<Integer, R, T> consumer) {
        R r = func.get();
        int index = 0;
        for (T t : source) {
            consumer.accept(index++, r, t);
        }
        return r;
    }

    public <E, R> List<R> reduceIndexedList(Function<T, E> func, BBiConsumer<Integer, List<R>, E> consumer) {
        List<R> rs = new ArrayList<>();
        int index = 0;
        for (T t : source) {
            E e = func.apply(t);
            consumer.accept(index++, rs, e);
        }
        return rs;
    }

    public <E, R> Set<R> reduceIndexedSet(Function<T, E> func, BBiConsumer<Integer, Set<R>, E> consumer) {
        Set<R> rs = new HashSet<>();
        int index = 0;
        for (T t : source) {
            E e = func.apply(t);
            consumer.accept(index++, rs, e);
        }
        return rs;
    }

    //  fold(initial) { acc, value -> operation }: 类似reduce，但可以提供一个初始值。
    public final <R> R fold(R initial, BiFunction<R, T, R> operation) {
        Objects.requireNonNull(operation, "operation cannot be null");

        R accumulator = initial; // 初始化累加器

        for (final T element : source) {
            accumulator = operation.apply(accumulator, element); // 应用操作，更新累加器
        }
        return accumulator; // 返回最终的累加值
    }
    //  foldIndexed(initial) { index, acc, value -> operation }: 类似fold，但操作函数同时接收元素的索引。

    public final <R> R foldIndexed(R initial, BBiFunction<Integer, R, T, R> operation) {
        Objects.requireNonNull(operation, "operation cannot be null");

        R accumulator = initial; // 初始化累加器
        Iterator<T> iterator = source.iterator();
        int index = 0; // 初始化索引

        while (iterator.hasNext()) {
            T element = iterator.next();
            accumulator = operation.apply(index, accumulator, element); // 应用操作，更新累加器
            index++; // 索引递增
        }
        return accumulator; // 返回最终的累加值
    }
    //  sum(): 计算数字集合中所有元素的和。


    public double sumDouble(Function<T, Number> mapper) {
        return sumBigDecimal(mapper).doubleValue();
    }

    public int sumInt(Function<T, Number> mapper) {
        return sumBigDecimal(mapper).intValue();
    }

    public long sumLong(Function<T, Number> mapper) {
        return sumBigDecimal(mapper).longValue();
    }

    /**
     * 对流中元素提取的数字进行求和，返回 BigDecimal 以保证精度。
     *
     * @param mapper 数字提取函数
     * @return BigDecimal 类型的总和
     */
    public BigDecimal sumBigDecimal(Function<T, Number> mapper) {
        BigDecimal sum = BigDecimal.ZERO;
        for (T t : source) {
            Number r = mapper.apply(t);
            if (r != null) {
                // 优化：使用 valueOf 避免 new BigDecimal(String.valueOf()) 的开销
                sum = sum.add(BigDecimal.valueOf(r.doubleValue()));
            }
        }
        return sum;
    }

    public Double sumDouble() {
        return sumBigDecimal().doubleValue();
    }

    public Integer sumInt() {
        return sumBigDecimal().intValue();
    }

    public Long sumLong() {
        return sumBigDecimal().longValue();
    }

    public BigDecimal sumBigDecimal() {
        BigDecimal sum = BigDecimal.ZERO;
        for (T t : source) {
            if (t instanceof Number n) {
                sum = sum.add(BigDecimal.valueOf(n.doubleValue()));
            } else if (t != null) {
                throw new IllegalArgumentException("不是数字,不能计算: " + t.getClass().getName());
            }
        }
        return sum;
    }

    //  average(): 计算数字集合中所有元素的平均值。
    public final Double averageDouble(Function<T, Number> mapper) {
        return averageBigDecimal(mapper).doubleValue();
    }

    public final Integer averageInt(Function<T, Number> mapper) {
        return averageBigDecimal(mapper).intValue();
    }

    public final Long averageLong(Function<T, Number> mapper) {
        return averageBigDecimal(mapper).longValue();
    }

    /**
     * 计算流中提取数字的平均值。
     *
     * @param mapper 数字提取函数
     * @return 平均值，保留 2 位小数点，四舍五入
     */
    public final BigDecimal averageBigDecimal(Function<T, Number> mapper) {
        BigDecimal total = BigDecimal.ZERO;
        long count = 0;

        for (final T element : source) {
            final Number number = mapper.apply(element);
            if (number == null) {
                continue;
            }
            total = total.add(BigDecimal.valueOf(number.doubleValue()));
            count++;
        }

        if (count == 0) {
            return total;
        } else {
            // 设置默认精度和舍入模式
            return total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        }
    }

    //  average(): 计算数字集合中所有元素的平均值。
    public final Double averageDouble() {
        return averageBigDecimal().doubleValue();
    }

    public final Integer averageInt() {
        return averageBigDecimal().intValue();
    }

    public final Long averageLong() {
        return averageBigDecimal().longValue();
    }

    public final BigDecimal averageBigDecimal() {
        BigDecimal total = BigDecimal.ZERO;
        long count = 0;

        for (final T element : source) {
            if (element instanceof Number n) {
                total = total.add(BigDecimal.valueOf(n.doubleValue()));
                count++;
            } else if (element != null) {
                throw new IllegalStateException("Element is not a Number type when calculating average: " + element.getClass().getName() + " -> " + element);
            }
        }

        if (count == 0) {
            return total;
        } else {
            return total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        }
    }
    //  count(): 返回集合中的元素数量。


    // 获取大小的方法
    public long size() {
        return count();
    }

    /**
     * 返回流中的元素总数。
     * 优化：如果数据源本身支持 size()，则直接调用以提高效率。
     *
     * @return 元素数量
     */
    public long count() {
        // 如果是Collection类型，直接返回size，避免 O(N) 遍历
        if (source instanceof Collection) {
            return ((Collection<?>) source).size();
        }
        // 对于普通 Iterable，必须遍历计数
        long count = 0;
        for (T ignored : source) {
            count++;
        }
        return count;
    }

    //  count { predicate }: 返回满足给定条件的元素数量。

    @SafeVarargs
    public final long count(Predicate<T>... predicates) {
        long count = 0;
        for (T e : source) {
            for (Predicate<T> p : predicates) {
                if (p.test(e)) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    //  maxOrNull(): 返回集合中的最大元素，如果为空则返回null。

    /**
     * 返回流中的最大元素。要求元素实现 Comparable 接口。
     *
     * @return 最大元素，如果流为空则返回 null
     * @throws IllegalStateException 如果元素未实现 Comparable
     */
    public final T maxOrNull() {
        Iterator<T> iterator = source.iterator();
        if (!iterator.hasNext()) {
            return null;
        }

        T maxElement = iterator.next();

        while (iterator.hasNext()) {
            T current = iterator.next();
            // 运行时类型检查，确保可比较
            if (!(current instanceof Comparable)) {
                throw new IllegalStateException("Elements must be Comparable to use maxOrNull()");
            }
            @SuppressWarnings("unchecked")
            Comparable<T> comparableCurrent = (Comparable<T>) current;

            if (comparableCurrent.compareTo(maxElement) > 0) {
                maxElement = current;
            }
        }
        return maxElement;
    }


    //  maxByOrNull { selector }: 返回通过选择器函数得到最大值的元素，如果为空则返回null。

    public final <R extends Comparable<R>> T maxByOrNull(Function<T, R> selector) {
        Objects.requireNonNull(selector, "selector cannot be null");
        Iterator<T> iterator = source.iterator();
        if (!iterator.hasNext()) {
            return null;
        }

        T maxElement = iterator.next();
        R maxValue = selector.apply(maxElement);

        while (iterator.hasNext()) {
            T current = iterator.next();
            R currentValue = selector.apply(current);
            if (currentValue.compareTo(maxValue) > 0) {
                maxValue = currentValue;
                maxElement = current;
            }
        }
        return maxElement;
    }

    //  maxOfOrNull { selector }: 返回通过选择器函数得到的最大值，如果为空则返回null。

    public final <R extends Comparable<R>> R maxOfOrNull(Function<T, R> selector) {
        Objects.requireNonNull(selector, "selector cannot be null");
        Iterator<T> iterator = source.iterator();
        if (!iterator.hasNext()) {
            return null;
        }

        R maxValue = selector.apply(iterator.next()); // 获取第一个元素的 selector 结果作为初始最大值

        while (iterator.hasNext()) {
            T current = iterator.next();
            R currentValue = selector.apply(current);
            if (currentValue.compareTo(maxValue) > 0) {
                maxValue = currentValue;
            }
        }
        return maxValue;
    }

    //  minOrNull(): 返回集合中的最小元素，如果为空则返回null。

    public final T minOrNull() {
        Iterator<T> iterator = source.iterator();
        if (!iterator.hasNext()) {
            return null; // 集合为空，返回 null
        }

        T minElement = iterator.next(); // 假定第一个元素是最小值

        while (iterator.hasNext()) {
            T current = iterator.next();
            if (!(current instanceof Comparable)) {
                throw new IllegalStateException("Elements must be Comparable to use minOrNull()");
            }
            @SuppressWarnings("unchecked")
            Comparable<T> comparableCurrent = (Comparable<T>) current;

            if (comparableCurrent.compareTo(minElement) < 0) { // 比较方向相反
                minElement = current; // 找到更小的元素，更新最小值
            }
        }
        return minElement;
    }

    //  minByOrNull { selector }: 返回通过选择器函数得到最小值的元素，如果为空则返回null。
    public final <R extends Comparable<R>> T minByOrNull(Function<T, R> selector) {
        Objects.requireNonNull(selector, "selector cannot be null");
        Iterator<T> iterator = source.iterator();
        if (!iterator.hasNext()) {
            return null;
        }

        T minElement = iterator.next();
        R minValue = selector.apply(minElement);

        while (iterator.hasNext()) {
            T current = iterator.next();
            R currentValue = selector.apply(current);
            if (currentValue.compareTo(minValue) < 0) { // 比较方向相反
                minValue = currentValue;
                minElement = current;
            }
        }
        return minElement;
    }

    //  minOfOrNull { selector }: 返回通过选择器函数得到的最小值，如果为空则返回null。

    public final <R extends Comparable<R>> R minOfOrNull(Function<T, R> selector) {
        Objects.requireNonNull(selector, "selector cannot be null");
        Iterator<T> iterator = source.iterator();
        if (!iterator.hasNext()) {
            return null;
        }

        R minValue = selector.apply(iterator.next()); // 获取第一个元素的 selector 结果作为初始最小值

        while (iterator.hasNext()) {
            T current = iterator.next();
            R currentValue = selector.apply(current);
            if (currentValue.compareTo(minValue) < 0) { // 比较方向相反
                minValue = currentValue;
            }
        }
        return minValue;
    }

    //  sumOf { selector }: 对通过选择器函数得到的数字值求和。

    public final double sumOf(Function<T, ? extends Number> selector) {
        Objects.requireNonNull(selector, "selector cannot be null");
        double total = 0.0;
        Iterator<T> iterator = source.iterator();

        while (iterator.hasNext()) {
            T element = iterator.next();
            Number value = selector.apply(element);
            if (value != null) {
                total += value.doubleValue(); // 将选择器返回的 Number 值转换为 double 累加
            }
            // 如果 selector 返回 null，则跳过该值
        }
        return total;
    }

    //  associate { transform }: 将集合中的每个元素映射为一个Pair，然后将这些Pair放入一个Map中。

    public final <K, V> Map<K, V> associate(Function<T, Pair<K, V>> transform) {
        Objects.requireNonNull(transform, "transform function cannot be null");
        Map<K, V> resultMap = new HashMap<>();
        Iterator<T> iterator = source.iterator();

        while (iterator.hasNext()) {
            T element = iterator.next();
            Pair<K, V> pair = transform.apply(element);
            if (pair != null) {
                resultMap.put(pair.first, pair.second);
            }
        }
        return resultMap;
    }

    //  associateBy { keySelector }: 根据键选择器函数创建一个Map，键是选择器返回的值，值是原始元素。

    public final <K> Map<K, T> associateBy(Function<T, K> keySelector) {
        Objects.requireNonNull(keySelector, "keySelector cannot be null");
        Map<K, T> resultMap = new HashMap<>();
        Iterator<T> iterator = source.iterator();

        while (iterator.hasNext()) {
            T element = iterator.next();
            K key = keySelector.apply(element);
            resultMap.put(key, element); // 键是选择器结果，值是原始元素
        }
        return resultMap;
    }

    //  associateWith { valueTransform }: 根据值转换函数创建一个Map，键是原始元素，值是转换函数返回的值。

    public final <V> Map<T, V> associateWith(Function<T, V> valueTransform) {
        Objects.requireNonNull(valueTransform, "valueTransform cannot be null");
        Map<T, V> resultMap = new HashMap<>();
        Iterator<T> iterator = source.iterator();

        while (iterator.hasNext()) {
            T element = iterator.next();
            V value = valueTransform.apply(element);
            resultMap.put(element, value); // 键是原始元素，值是转换函数结果
        }
        return resultMap;
    }

    //  groupBy { keySelector }: 根据键选择器函数对元素进行分组，返回一个Map，其中键是选择器返回的值，值是一个包含所有具有该键的元素的列表。


    /**
     * 将元素转换为Map，使用keyMapper生成key
     * 如果有重复的key，将值收集到List中
     */
    public <K> MapListStream<K, T> groupBy(Function<T, K> keyMapper) {
        return groupBy(keyMapper, Function.identity());
    }

    /**
     * 将元素转换为Map，使用keyMapper生成key，valueMapper生成value
     * 如果有重复的key，将值收集到List中
     */
    public <K, V> MapListStream<K, V> groupBy(
            Function<T, K> keyMapper,
            Function<T, V> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper cannot be null");
        Objects.requireNonNull(valueMapper, "valueMapper cannot be null");

        // 改造为延迟执行：直接返回 MapListStream，由其内部处理遍历逻辑
        return new MapListStream<>(source, keyMapper, valueMapper);
    }

    //  groupingBy { keySelector }: 返回一个Grouping对象，用于更复杂的聚合操作，如eachCount()、fold()等。

    public <S, A, V> MapStream<S, V> groupingBy(
            Function<T, S> keyMapper,
            Collector<T, A, V> collector
    ) {
        // 彻底的延迟执行：将分组聚合逻辑封装在 MapStream 的 Supplier 中
        return new MapStream<>(() -> {
            Supplier<A> supplier = collector.supplier();
            BiConsumer<A, T> accumulator = collector.accumulator();
            Function<A, V> finisher = collector.finisher();

            Map<S, A> accumulatorMap = new HashMap<>();
            for (T element : source) {
                S key = keyMapper.apply(element);
                A acc = accumulatorMap.computeIfAbsent(key, k -> supplier.get());
                accumulator.accept(acc, element);
            }

            Map<S, V> finalResult = new HashMap<>(accumulatorMap.size());
            for (Map.Entry<S, A> entry : accumulatorMap.entrySet()) {
                finalResult.put(entry.getKey(), finisher.apply(entry.getValue()));
            }
            return finalResult;
        });
    }


    // ====================================================================================
    // ====================================================================================


    // ================================ 检查 (Checking)  ==================================
    // ====================================================================================
    //  any(): 检查集合是否包含任何元素。

    public final boolean any() {
        // 这是最直接的方式，判断迭代器是否有下一个元素
        return source.iterator().hasNext();
    }

    //  any { predicate }: 检查集合中是否有任何元素满足给定条件。

    @SafeVarargs
    public final boolean any(Predicate<T>... predicates) {
        return anyMatch(predicates);
    }

    /**
     * 检查流中是否至少有一个元素满足给定的谓词条件之一。
     *
     * @param predicates 谓词数组
     * @return 只要有一个元素满足任意一个谓词，就返回 true
     */
    @SafeVarargs
    public final boolean anyMatch(Predicate<T>... predicates) {
        if (predicates == null || predicates.length == 0) return any();
        for (T elem : source) {
            // 优化：采用循环匹配，支持短路返回
            for (Predicate<T> p : predicates) {
                if (p.test(elem)) return true;
            }
        }
        return false;
    }

    //  all { predicate }: 检查集合中是否所有元素都满足给定条件。

    @SafeVarargs
    public final boolean all(Predicate<T>... predicates) {
        return allMatch(predicates);
    }

    @SafeVarargs
    public final boolean allMatch(Predicate<T>... predicates) {
        if (predicates == null || predicates.length == 0) return true;
        for (T elem : source) {
            for (Predicate<T> p : predicates) {
                if (!p.test(elem)) return false;
            }
        }
        return true;
    }

    //  none(): 检查集合是否不包含任何元素。

    public final boolean none() {
        // 最直接的方式就是检查迭代器是否还有下一个元素
        return !source.iterator().hasNext();
    }

    //  none { predicate }: 检查集合中是否没有元素满足给定条件。

    @SafeVarargs
    public final boolean none(Predicate<T>... predicates) {
        return noneMatch(predicates);
    }

    @SafeVarargs
    public final boolean noneMatch(Predicate<T>... predicates) {
        if (predicates == null || predicates.length == 0) return none();
        for (T elem : source) {
            for (Predicate<T> p : predicates) {
                if (p.test(elem)) return false;
            }
        }
        return true;
    }

    //  contains(element): 检查集合是否包含指定元素。
    public final boolean contains(T element) {
        Iterator<T> iterator = source.iterator();
        while (iterator.hasNext()) {
            T current = iterator.next();
            if (Objects.equals(current, element)) { // 使用 Objects.equals 安全比较
                return true; // 找到匹配元素，返回 true
            }
        }
        return false; // 遍历完所有元素，没有找到，返回 false
    }

    //  containsAll(elements): 检查集合是否包含指定集合中的所有元素。

    public final boolean containsAll(Collection<T> elements) {
        Objects.requireNonNull(elements, "elements collection cannot be null");

        if (elements.isEmpty()) {
            return true; // 如果要检查的集合是空的，则认为当前集合包含所有其元素
        }

        for (T elementToFind : elements) {
            // 对 elements 中的每个元素，检查它是否在当前的 ListStream 中
            if (!this.contains(elementToFind)) { // 内部会调用上面实现的 contains(element)
                return false; // 只要有一个元素不存在，就返回 false
            }
        }
        return true; // 所有元素都存在，返回 true
    }

    //  isEmpty(): 检查集合是否为空。

    public boolean isEmpty() {
        if (source == null) {
            return true;
        }

        // 如果是Collection类型，直接使用isEmpty()方法
        if (source instanceof Collection) {
            return ((Collection<?>) source).isEmpty();
        }

        // 如果是普通Iterable，检查是否有第一个元素
        return !source.iterator().hasNext();
    }

    private boolean isNotEmpty() {
        return !isEmpty();
    }

    //  single(): 返回集合中唯一的元素，如果集合为空或包含多个元素则抛出异常。
    public final T single() {
        Iterator<T> iterator = source.iterator();

        // 检查是否为空
        if (!iterator.hasNext()) {
            throw new NoSuchElementException("Collection is empty.");
        }

        // 获取第一个元素
        T singleElement = iterator.next();

        // 检查是否还有其他元素
        if (iterator.hasNext()) {
            throw new IllegalStateException("Collection contains more than one element.");
        }

        return singleElement;
    }

    //  singleOrNull(): 返回集合中唯一的元素，如果集合为空或包含多个元素则返回null。
    public final T singleOrNull() {
        Iterator<T> iterator = source.iterator();

        // 检查是否为空
        if (!iterator.hasNext()) {
            return null; // 集合为空，返回 null
        }

        // 获取第一个元素
        T singleElement = iterator.next();

        // 检查是否还有其他元素
        if (iterator.hasNext()) {
            return null; // 集合包含多个元素，返回 null
        }

        return singleElement; // 正好有一个元素，返回该元素
    }
    //  first(): 返回集合中的第一个元素，如果为空则抛出异常。

    /**
     * 获取流中的第一个元素。
     *
     * @return 集合中的第一个元素
     * @throws IllegalCallerException 如果集合为空
     */
    public T first() {
        Iterator<T> iterator = source.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        } else {
            throw new IllegalCallerException("无法找到参数");
        }
    }

    //  first { predicate }: 返回第一个满足条件的元素，如果不存在则抛出异常。

    @SafeVarargs
    public final T first(Predicate<T>... predicates) {
        Objects.requireNonNull(predicates);
        for (final T t : source) {
            boolean matches = true;
            for (Predicate<T> p : predicates) {
                if (!p.test(t)) {
                    matches = false;
                    break;
                }
            }
            if (matches) return t;
        }
        throw new IllegalCallerException("无法找到参数");
    }


    //  firstOrNull(): 返回集合中的第一个元素，如果为空则返回null。

    public T firstOrNull() {
        Iterator<T> iterator = source.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        } else {
            return null;
        }
    }

    //  firstOrNull { predicate }: 返回第一个满足条件的元素，如果不存在则返回null。

    public T firstOrNull(Predicate<T> predicate) {
        for (final T t : source) {
            if (predicate.test(t)) {
                return t;
            }
        }
        return null;
    }

    //  last(): 返回集合中的最后一个元素，如果为空则抛出异常。

    public T last() {
        Iterator<T> iterator = source.iterator();

        T next = null;
        while (iterator.hasNext()) {
            next = iterator.next();
        }

        if (next == null) {
            throw new IllegalCallerException("无法找到参数");
        }

        return next;
    }

    //  last { predicate }: 返回最后一个满足条件的元素，如果不存在则抛出异常。

    @SafeVarargs
    public final T last(Predicate<T>... predicates) {
        Objects.requireNonNull(predicates);
        T next = null;
        for (final T temp : source) {
            boolean matches = true;
            for (Predicate<T> p : predicates) {
                if (!p.test(temp)) {
                    matches = false;
                    break;
                }
            }
            if (matches) next = temp;
        }

        if (next == null) {
            throw new IllegalCallerException("无法找到参数");
        }

        return next;
    }

    //  lastOrNull(): 返回集合中的最后一个元素，如果为空则返回null。

    public T lastOrNull() {
        Iterator<T> iterator = source.iterator();

        T next = null;
        while (iterator.hasNext()) {
            next = iterator.next();
        }

        return next;
    }

    //  lastOrNull { predicate }: 返回最后一个满足条件的元素，如果不存在则返回null。

    @SafeVarargs
    public final T lastOrNull(Predicate<T>... predicates) {
        Objects.requireNonNull(predicates);
        T next = null;
        for (final T temp : source) {
            boolean matches = true;
            for (Predicate<T> p : predicates) {
                if (!p.test(temp)) {
                    matches = false;
                    break;
                }
            }
            if (matches) next = temp;
        }

        return next;
    }

    //  indexOf(element): 返回指定元素的第一个索引，如果不存在则返回-1。

    /**
     * 返回指定元素在流中的第一个索引位置。
     *
     * @param element 要查找的元素
     * @return 元素的索引，如果未找到则返回 -1
     */
    public final int indexOf(T element) {
        Iterator<T> iterator = source.iterator();
        int index = 0;

        while (iterator.hasNext()) {
            T current = iterator.next();
            // 使用 Objects.equals 安全比较
            if (Objects.equals(current, element)) {
                return index;
            }
            index++;
        }

        return -1;
    }

    // ====================================================================================
    // ====================================================================================


    // ================================ 排序 (Ordering)  ==================================
    // ====================================================================================
    //  sorted(): 返回一个新列表，按元素的自然顺序升序排序。
    /**
     * 对流中元素进行自然升序排序。
     * 这是一个及早执行的操作，会触发所有之前的延迟计算。
     *
     * @return 排序后的 ListStream
     */
    public final ListStream<T> sorted() {
        return of(() -> {
            List<T> tempList = toList();
            Collections.sort((List<Comparable>) tempList);
            return tempList.iterator();
        });
    }

    //  sortedDescending(): 返回一个新列表，按元素的自然顺序降序排序。

    public <U extends Comparable<? super U>> ListStream<T> sortDesc(
            Function<? super T, ? extends U> keyExtractor) {
        return sort(keyExtractor, Sort.Desc, Sort.NullLast);
    }

    //  sortedBy { selector }: 返回一个新列表，通过选择器函数返回的键进行升序排序。
    //  sortedByDescending { selector }: 返回一个新列表，通过选择器函数返回的键进行降序排序。


    //  sortedWith(comparator): 返回一个新列表，使用提供的比较器进行排序。

    @SuppressWarnings("unused")
    public ListStream<T> sort(Comparator<T> comparator) {
        return of(() -> {
            List<T> sortedList = toList();
            sortedList.sort(comparator);
            return sortedList.iterator();
        });
    }

    public <U extends Comparable<? super U>> ListStream<T> sort(
            Function<? super T, ? extends U> keyExtractor,
            Sort order) {
        return sort(keyExtractor, order, Sort.NullLast);
    }

    /**
     * 根据提取的比较键对元素进行排序
     *
     * @param keyExtractor 键提取函数
     * @param order        排序顺序（升序/降序）
     * @param nullPosition 空值位置（前/后）
     * @return 排序后的ListStream
     */
    public <U extends Comparable<? super U>> ListStream<T> sort(
            Function<? super T, ? extends U> keyExtractor,
            Sort order,
            Sort nullPosition) {
        Objects.requireNonNull(keyExtractor, "keyExtractor cannot be null");
        Objects.requireNonNull(order, "order cannot be null");
        Objects.requireNonNull(nullPosition, "nullPosition cannot be null");

        return of(() -> {
            List<T> sortedList = toList();
            if (sortedList.isEmpty()) {
                return Collections.emptyIterator();
            }
            SortStream<T> sortStream = new SortStream<>();
            Comparator<T> comparator = sortStream.createComparator(keyExtractor, order, nullPosition);
            sortedList.sort(comparator);
            return sortedList.iterator();
        });
    }

    /**
     * 根据提取的比较键对元素进行排序
     *
     * @return 排序后的ListStream
     */
    @SafeVarargs
    public final <U extends Comparable<? super U>> ListStream<T> sort(Function<SortStream<T>, Comparator<T>>... streamOperation) {

        return of(() -> {
            List<T> sortedList = toList();
            if (sortedList.isEmpty()) {
                return Collections.emptyIterator();
            }

            Comparator<T> comparator = null;
            for (Function<SortStream<T>, Comparator<T>> comparatorFunction : streamOperation) {
                if (comparator == null) {
                    comparator = comparatorFunction.apply(new SortStream<>());
                } else {
                    comparator = comparator.thenComparing(comparatorFunction.apply(new SortStream<>()));
                }
            }
            sortedList.sort(comparator);
            return sortedList.iterator();
        });
    }


    /**
     * 简化版排序方法 - 升序，空值在最后
     */
    public <U extends Comparable<? super U>> ListStream<T> sortAsc(
            Function<? super T, ? extends U> keyExtractor) {
        return sort(keyExtractor, Sort.Asc, Sort.NullLast);
    }

    //  shuffled(): 返回一个随机排列的新列表。

    public final ListStream<T> shuffled() {
        return of(() -> {
            List<T> tempList = toList();
            Collections.shuffle(tempList);
            return tempList.iterator();
        });
    }

    //  reversed(): 返回一个元素顺序颠倒的新列表。

    public ListStream<T> reversed() {
        return of(() -> {
            List<T> list = toList();
            Collections.reverse(list);
            return list.iterator();
        });
    }

    // ====================================================================================
    // ====================================================================================


    // ================================ 转换 (Transforming)  ==================================
    // ====================================================================================
    //  toSet(): 将集合转换为一个Set。

    public Set<T> toSet() {
        // 如果是Collection类型，直接返回size
        if (source instanceof Set<T>) {
            return (Set<T>) source;
        }
        Set<T> result = new HashSet<>();
        source.forEach(result::add);
        return result;
    }

    //  toList(): 将集合转换为一个List。

    /**
     * 将流中的元素收集到一个列表中。
     * 优化：如果数据源本身已经是 List，则直接返回。
     *
     * @return 包含流中所有元素的 List
     */
    public List<T> toList() {
        if (source instanceof List<T>) {
            return (List<T>) source;
        }
        List<T> result = new ArrayList<>();
        source.forEach(result::add);
        return result;
    }

    //  toMutableList(): 将集合转换为一个可变的MutableList。
    //  toMutableSet(): 将集合转换为一个可变的MutableSet。
    //  toCollection(destination): 将集合中的所有元素添加到给定的可变集合中。
    //  joinToString(separator = ", ", prefix = "", postfix = "", limit = -1, truncated = "...", transform = null): 将集合中的元素连接成一个字符串。

    public String joining(CharSequence symbol) {
        return joinToString(symbol);
    }

    public String joinToString(CharSequence symbol) {
        StringJoiner sb = new StringJoiner(symbol);
        for (T t : source) {
            if (t instanceof CharSequence) {
                sb.add((CharSequence) t);
            } else {
                if (t != null) {
                    sb.add(t.toString());
                }
            }
        }
        return sb.toString();
    }


    // ====================================================================================
    // ====================================================================================


    // ================================ 元素操作 (Element Operations)  ==================================
    // ====================================================================================
    //  elementAt(index): 返回指定索引处的元素。

    /**
     * 返回指定索引处的元素。
     * 优化：针对 List 和 Collection 类型提供了快速访问路径。
     *
     * @param index 索引
     * @return 该索引处的元素
     * @throws IndexOutOfBoundsException 如果索引越界
     */
    public T elementAt(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
        if (source instanceof final Collection<T> ts) {
            final int size = ts.size();
            if (index >= size) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
            }
            if (ts instanceof List<T> list) {
                // 针对 List 直接通过 get 访问
                return list.get(index);
            }
            // 使用 stream.skip().findFirst() 进行局部遍历优化
            return ts.stream().skip(index).findFirst()
                    .orElseThrow(() -> new IndexOutOfBoundsException("Index: " + index + ", Size: " + size));
        }
        // 普通 Iterable 只能顺序查找
        int i = 0;
        for (final T t : source) {
            if (i == index) {
                return t;
            }
            i++;
        }
        if (index > i) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + (i + 1));
        } else {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + i);
        }
    }

    //  elementAtOrNull(index): 返回指定索引处的元素，如果索引越界则返回null。

    public T elementAtOrNull(int index) {
        if (index < 0) {
            return null;
        }
        if (source instanceof final Collection<T> ts) {
            if (index >= ts.size()) {
                return null;
            }
            if (ts instanceof List<T> list) {
                return list.get(index);
            }
            return ts.stream().skip(index).findFirst().orElse(null);
        }
        int i = 0;
        for (final T t : source) {
            if (i == index) {
                return t;
            }
            i++;
        }
        return null;
    }

    //  elementAtOrElse(index, defaultValue): 返回指定索引处的元素，如果索引越界则返回defaultValue。
    public T elementAtOrElse(int index, T defaultValue) {
        if (index < 0) {
            return defaultValue;
        }
        if (source instanceof final Collection<T> ts) {
            if (index >= ts.size()) {
                return defaultValue;
            }
            if (ts instanceof List<T> list) {
                return list.get(index);
            }
            return ts.stream().skip(index).findFirst().orElse(defaultValue);
        }
        int i = 0;
        for (final T t : source) {
            if (i == index) {
                return t;
            }
            i++;
        }
        return defaultValue;
    }

    //  plus(element) / plus(elements): 返回一个包含原集合元素和新元素的集合。

    public final ListStream<T> plus(T element) {
        return ListStream.of(() -> new Iterator<>() {
            final Iterator<T> sourceIterator = source.iterator(); // 原始集合的迭代器
            boolean elementAdded = false; // 标记新元素是否已经返回
            T nextElement;
            boolean hasNextComputed = false;
            boolean hasNextResult = false;

            @Override
            public boolean hasNext() {
                if (!hasNextComputed) {
                    computeNext();
                }
                return hasNextResult;
            }

            @Override
            public T next() {
                if (!hasNextComputed) {
                    computeNext();
                }
                if (!hasNextResult) {
                    throw new NoSuchElementException();
                }
                hasNextComputed = false;
                return nextElement;
            }

            private void computeNext() {
                // 首先遍历原始集合的元素
                if (sourceIterator.hasNext()) {
                    nextElement = sourceIterator.next();
                    hasNextResult = true;
                    hasNextComputed = true;
                    return;
                }

                // 如果原始集合遍历完毕，且新元素还没添加
                if (!elementAdded) {
                    nextElement = element; // 返回要添加的单个元素
                    elementAdded = true;    // 标记为已添加
                    hasNextResult = true;
                    hasNextComputed = true;
                    return;
                }

                // 所有元素都已返回
                hasNextResult = false;
                hasNextComputed = true;
            }
        });
    }

    @SafeVarargs
    public final ListStream<T> plus(Iterable<T>... others) {
        Objects.requireNonNull(others, "list cannot be null");
        return ListStream.of(() -> new Iterator<>() {
            final int endIndex = others.length - 1;
            final Iterator<T> iterator = source.iterator();
            int index = 0;
            Iterator<T> otherIterator = null;
            T nextElement;
            boolean hasNextComputed = false;
            boolean hasNextResult = false;

            @Override
            public boolean hasNext() {
                if (!hasNextComputed) {
                    computeNext();
                }
                return hasNextResult;
            }

            @Override
            public T next() {
                if (!hasNextComputed) {
                    computeNext();
                }
                if (!hasNextResult) {
                    throw new NoSuchElementException();
                }
                hasNextComputed = false;
                return nextElement;
            }

            private void computeNext() {
                while (iterator.hasNext()) {
                    nextElement = iterator.next();
                    hasNextResult = true;
                    hasNextComputed = true;
                    return;
                }

                if (otherIterator == null) {
                    otherIterator = others[index].iterator();
                }

                while (index <= endIndex) {
                    while (otherIterator.hasNext()) {
                        nextElement = otherIterator.next();
                        hasNextResult = true;
                        hasNextComputed = true;
                        return;
                    }
                    index += 1;
                    if (index <= endIndex) {
                        otherIterator = others[index].iterator();
                    }
                }

                hasNextResult = false;
                hasNextComputed = true;
            }
        });
    }


    //  minus(element) / minus(elements): 返回一个移除指定元素的集合。

    public final ListStream<T> minus(T element) {
        return ListStream.of(() -> new Iterator<>() {
            final Iterator<T> iterator = source.iterator();
            T nextElement;
            boolean hasNextComputed = false;
            boolean hasNextResult = false;

            @Override
            public boolean hasNext() {
                if (!hasNextComputed) {
                    computeNextSingle();
                }
                return hasNextResult;
            }

            @Override
            public T next() {
                if (!hasNextComputed) {
                    computeNextSingle();
                }
                if (!hasNextResult) {
                    throw new NoSuchElementException();
                }
                hasNextComputed = false;
                return nextElement;
            }

            private void computeNextSingle() {
                while (iterator.hasNext()) {
                    T current = iterator.next();
                    if (!Objects.equals(current, element)) { // 检查是否是要移除的元素
                        nextElement = current;
                        hasNextResult = true;
                        hasNextComputed = true;
                        return;
                    }
                }
                hasNextResult = false;
                hasNextComputed = true;
            }
        });
    }

    @SafeVarargs
    public final ListStream<T> minus(Iterable<T>... elementsToRemove) {
        return of(() -> {
            // 在迭代开始时构建移除集，彻底延迟且支持流重用
            Set<T> removalSet = new HashSet<>();
            if (elementsToRemove != null) {
                for (Iterable<T> iterable : elementsToRemove) {
                    for (T item : iterable) {
                        removalSet.add(item);
                    }
                }
            }

            return new Iterator<>() {
                final Iterator<T> iterator = source.iterator();
                T nextElement;
                boolean hasNextComputed = false;
                boolean hasNextResult = false;

                @Override
                public boolean hasNext() {
                    if (!hasNextComputed) {
                        computeNextMultiple();
                    }
                    return hasNextResult;
                }

                @Override
                public T next() {
                    if (!hasNextComputed) {
                        computeNextMultiple();
                    }
                    if (!hasNextResult) {
                        throw new NoSuchElementException();
                    }
                    hasNextComputed = false;
                    return nextElement;
                }

                private void computeNextMultiple() {
                    while (iterator.hasNext()) {
                        T current = iterator.next();
                        if (!removalSet.contains(current)) {
                            nextElement = current;
                            hasNextResult = true;
                            hasNextComputed = true;
                            return;
                        }
                    }
                    hasNextResult = false;
                    hasNextComputed = true;
                }
            };
        });
    }

    // ====================================================================================
    // ====================================================================================

    // ================================ 其他 (Miscellaneous)  ==================================
    // ====================================================================================
    //  chunked(size): 将集合分解成固定大小的块。

    public void chunked(int size, Consumer<List<T>> consumer) {
        split(size, consumer);
    }

    public List<List<T>> chunkedToList(int size) {
        return splitToList(size);
    }

    public void split(int size, Consumer<List<T>> consumer) {
        if (size <= 0) throw new IllegalArgumentException("size must be > 0");
        List<T> temp = new ArrayList<>(size);
        for (T t : source) {
            temp.add(t);
            if (temp.size() == size) {
                consumer.accept(temp);
                temp = new ArrayList<>(size);
            }
        }
        if (!temp.isEmpty()) {
            consumer.accept(temp);
        }
    }

    public List<List<T>> splitToList(int size) {
        List<List<T>> parts = new ArrayList<>();
        split(size, parts::add);
        return parts;
    }


    //  windowed(size, step = 1, partialWindows = false): 创建滑动窗口。
    /**
     * 滑动窗口操作。根据指定的窗口大小和步长创建子列表组成的流。
     *
     * @param size           窗口大小
     * @param step           步长
     * @param partialWindows 是否允许返回不完整的末尾窗口
     * @return 子列表组成的 ListStream
     */
    public final ListStream<List<T>> windowed(int size, int step, boolean partialWindows) {
        if (size <= 0) {
            throw new IllegalArgumentException("Window size must be greater than 0: " + size);
        }
        if (step <= 0) {
            throw new IllegalArgumentException("Step must be greater than 0: " + step);
        }

        return ListStream.of(() -> new Iterator<>() {
            final Iterator<T> sourceIterator = source.iterator();
            // 使用 Deque 管理窗口内的可见元素
            final Deque<T> window = new LinkedList<>();
            int currentWindowIndex = 0;
            boolean hasNextComputed = false;
            boolean hasNextResult = false;
            List<T> nextWindow;

            @Override
            public boolean hasNext() {
                if (!hasNextComputed) {
                    computeNext();
                }
                return hasNextResult;
            }

            @Override
            public List<T> next() {
                if (!hasNextComputed) {
                    computeNext();
                }
                if (!hasNextResult) {
                    throw new NoSuchElementException();
                }
                hasNextComputed = false;
                return nextWindow;
            }

            private void computeNext() {
                while (sourceIterator.hasNext() || (partialWindows && !window.isEmpty())) {
                    // 补充窗口中缺失的元素
                    while (sourceIterator.hasNext() && window.size() < size) {
                        final T next = sourceIterator.next();
                        window.addLast(next);
                    }

                    // 检查是否满足返回窗口的条件
                    if (window.size() == size) {
                        nextWindow = new ArrayList<>(window);
                        hasNextResult = true;
                        hasNextComputed = true;

                        // 根据 step 滑动窗口：移除窗口开头的元素
                        for (int i = 0; i < step; i++) {
                            if (!window.isEmpty()) {
                                window.removeFirst();
                            } else {
                                break;
                            }
                        }
                        currentWindowIndex += step;
                        return;
                    }
                    // 处理末尾不完整的窗口
                    else if (partialWindows && !window.isEmpty()) {
                        nextWindow = new ArrayList<>(window);
                        hasNextResult = true;
                        hasNextComputed = true;
                        window.clear();
                        return;
                    }
                    else {
                        break;
                    }
                }
                hasNextResult = false;
                hasNextComputed = true;
            }
        });
    }

    // 提供带默认 step = 1 的重载方法
    public final ListStream<List<T>> windowed(int size) {
        return windowed(size, 1, false);
    }

    // 提供带默认 step = 1 和 partialWindows = false 的重载方法
    public final ListStream<List<T>> windowed(int size, int step) {
        return windowed(size, step, false);
    }
    /**
     * 对流中的每个元素执行给定的操作，执行后返回流本身。这通常用于调试。
     * 这是一个中间操作，支持延迟执行。
     *
     * @param action 执行的操作
     * @return 原始的 ListStream
     */
    public final ListStream<T> onEach(Consumer<? super T> action) {
        return peek(action);
    }

    /**
     * 分区操作。根据谓词条件将流分为两个列表。
     *
     * @param predicates 分区谓词
     * @return 包含两个列表的 Tuple2，第一个列表包含满足条件的元素，第二个包含不满足条件的元素
     */
    public Tuple2<List<T>, List<T>> partitionTuple2(Predicate<T>... predicates) {
        Tuple2<List<T>, List<T>> parts = new Tuple2<>();
        parts.t1 = new ArrayList<>();
        parts.t2 = new ArrayList<>();
        for (T t : source) {
            boolean matches = true;
            for (Predicate<T> p : predicates) {
                if (!p.test(t)) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                parts.t1.add(t);
            } else {
                parts.t2.add(t);
            }
        }

        return parts;
    }

    public List<List<T>> partition(Predicate<T>... predicates) {
        List<List<T>> parts = new ArrayList<>();
        parts.add(new ArrayList<>());
        parts.add(new ArrayList<>());
        for (T t : source) {
            boolean matches = true;
            for (Predicate<T> p : predicates) {
                if (!p.test(t)) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                parts.get(0).add(t);
            } else {
                parts.get(1).add(t);
            }
        }

        return parts;
    }

    @SafeVarargs
    public final List<List<T>> partitionEveryOne(Predicate<T>... predicates) {
        final int length = predicates.length;
        List<List<T>> parts = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            parts.add(new ArrayList<>());
        }
        for (T t : source) {
            for (int i = 0; i < predicates.length; i++) {
                final Predicate<T> predicate = predicates[i];
                if (predicate.test(t)) {
                    parts.get(i).add(t);
                }
            }
        }

        return parts;
    }

    // ====================================================================================
    // ====================================================================================

    /**
     * 将元素转换为Map，使用keyMapper生成key
     * 如果有重复的key，保留最后一个值
     */
    public <K> Map<K, T> toMap(Function<T, K> keyMapper) {
        return toMap(keyMapper, Function.identity());
    }

    /**
     * 将流元素转换为 Map。如果存在重复键，默认保留较晚出现的元素（Overwrite）。
     *
     * @param keyMapper   键提取器
     * @param valueMapper 值提取器
     * @param <K>         键类型
     * @param <V>         值类型
     * @return 结果 Map
     */
    public <K, V> Map<K, V> toMap(Function<T, K> keyMapper, Function<T, V> valueMapper) {
        return toMap(keyMapper, valueMapper, (v1, v2) -> v2);
    }

    /**
     * 将元素转换为Map，使用keyMapper生成key，valueMapper生成value
     * 如果有重复的key，使用mergeFunction合并值
     */
    public <K, V> Map<K, V> toMap(
            Function<T, K> keyMapper,
            Function<T, V> valueMapper,
            BinaryOperator<V> mergeFunction) {
        Objects.requireNonNull(keyMapper, "keyMapper cannot be null");
        Objects.requireNonNull(valueMapper, "valueMapper cannot be null");
        Objects.requireNonNull(mergeFunction, "mergeFunction cannot be null");

        Map<K, V> result = new HashMap<>();
        for (T element : source) {
            if (element != null) {
                K key = keyMapper.apply(element);
                if (key != null) {
                    V value = valueMapper.apply(element);
                    result.merge(key, value, mergeFunction);
                }
            }
        }
        return result;
    }


    public ListStream<T> add(T t) {
        return plus(t);
    }

    public ListStream<T> add(int index, T t) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
        return of(() -> new Iterator<T>() {
            private final Iterator<T> it = source.iterator();
            private int curr = 0;
            private boolean added = false;
            private T nextElem;
            private boolean hasNextComputed = false;
            private boolean hasNextResult = false;

            @Override
            public boolean hasNext() {
                if (!hasNextComputed) {
                    if (!added && curr == index) {
                        nextElem = t;
                        hasNextResult = true;
                        hasNextComputed = true;
                    } else if (it.hasNext()) {
                        nextElem = it.next();
                        curr++;
                        hasNextResult = true;
                        hasNextComputed = true;
                    } else {
                        hasNextResult = false;
                        hasNextComputed = true;
                    }
                }
                return hasNextResult;
            }

            @Override
            public T next() {
                if (!hasNext()) throw new NoSuchElementException();
                hasNextComputed = false;
                if (!added && nextElem == t && curr == index) {
                    added = true;
                }
                return nextElem;
            }
        });
    }

    public ListStream<T> addAll(List<T> ts) {
        return plus(ts);
    }

    public ListStream<T> concat(List<T> ts) {
        return plus(ts);
    }

    @SafeVarargs
    public final ListStream<T> add(T... ts) {
        return plus(X.asList(ts));
    }


    /**
     * 返回一个新的 ListStream，包含原始列表的子列表，从索引 0 开始，长度为 subLen。
     *
     * @param subLen 子列表的长度
     * @return 新的 ListStream 包含指定长度的子列表
     * @throws IllegalArgumentException 如果 subLen 为负数
     */
    public ListStream<T> sub(int subLen) {
        if (subLen < 0) {
            throw new IllegalArgumentException("subLen must be non-negative");
        }
        return sub(0, subLen);
    }

    /**
     * 截取子流。
     *
     * @param subBegin 起始索引（包含）
     * @param subEnd   结束索引（不包含）
     * @return 截取后的子流
     */
    public ListStream<T> sub(int subBegin, int subEnd) {
        if (subBegin < 0) {
            throw new IllegalArgumentException("subBegin must be non-negative");
        }
        if (subEnd < subBegin) {
            throw new IllegalArgumentException("subEnd must not be less than subBegin");
        }

        return of(() -> new Iterator<T>() {
            private final Iterator<T> iterator = source.iterator();
            private int currentIndex = 0;

            @Override
            public boolean hasNext() {
                // 先消费（丢弃）起点的元素
                while (currentIndex < subBegin && iterator.hasNext()) {
                    iterator.next();
                    currentIndex++;
                }
                return currentIndex < subEnd && iterator.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                T next = iterator.next();
                currentIndex++;
                return next;
            }
        });
    }


    @SafeVarargs
    public final ListStream<T> isNull(Function<T, ?>... getters) {
        return filterBlank(getters);
    }

    @SafeVarargs
    public final ListStream<T> isNotNull(Function<T, ?>... getters) {
        return filterNotBlank(getters);
    }

    @SafeVarargs
    public final ListStream<T> isBlank(Function<T, ?>... getters) {
        return filterBlank(getters);
    }

    @SafeVarargs
    public final ListStream<T> isNotBlank(Function<T, ?>... getters) {
        return filterNotBlank(getters);
    }

    @SafeVarargs
    public final ListStream<T> filterNotBlank(Function<T, ?>... functions) {
        return of(createFilteredIterable(elem -> isNotBlankElement(elem, functions)));
    }

    @SafeVarargs
    public final ListStream<T> filterBlank(Function<T, ?>... functions) {
        return of(createFilteredIterable(elem -> isBlankElement(elem, functions)));
    }

    private boolean isBlankElement(T elem, Function<T, ?>[] functions) {
        if (functions == null || functions.length == 0) {
            if (elem == null) return true;
            if (elem instanceof CharSequence str) return str.isEmpty() || "".contentEquals(str);
            return false;
        }

        for (Function<T, ?> fun : functions) {
            Object value = fun.apply(elem);
            boolean isBlank = value == null || (value instanceof CharSequence str && (str.isEmpty() || "".contentEquals(str)));
            if (!isBlank) return false;
        }
        return true;
    }


    private boolean isNotBlankElement(T elem, Function<T, ?>[] functions) {
        if (functions == null || functions.length == 0) {
            if (elem == null) return false;
            if (elem instanceof CharSequence str) return !str.isEmpty() && !"".contentEquals(str);
            return true;
        }

        for (Function<T, ?> fun : functions) {
            Object value = fun.apply(elem);
            boolean isNotBlank = value != null && (!(value instanceof CharSequence str) || (!str.isEmpty() && !"".contentEquals(str)));
            if (!isNotBlank) return false;
        }
        return true;
    }

    private Iterable<T> createFilteredIterable(BiPredicate<Integer, T> filterCondition) {
        return () -> new AbstractStreamIterator<T, T>(source.iterator()) {
            private int index = 0;

            @Override
            protected boolean computeNext() {
                while (sourceIterator.hasNext()) {
                    T elem = sourceIterator.next();
                    int i = index++;
                    if (filterCondition.test(i, elem)) {
                        nextElement = elem;
                        return true;
                    }
                }
                return false;
            }
        };
    }

    private Iterable<T> createFilteredIterable(Predicate<T> filterCondition) {
        return () -> new AbstractStreamIterator<T, T>(source.iterator()) {
            @Override
            protected boolean computeNext() {
                while (sourceIterator.hasNext()) {
                    T elem = sourceIterator.next();
                    if (filterCondition.test(elem)) {
                        nextElement = elem;
                        return true;
                    }
                }
                return false;
            }
        };
    }

    /**
     * 将元素转换为LinkedHashMap，保持插入顺序
     */
    public <K> Map<K, T> toLinkedMap(Function<T, K> keyMapper) {
        return toLinkedMap(keyMapper, Function.identity());
    }

    /**
     * 将元素转换为LinkedHashMap，保持插入顺序
     */
    public <K, V> Map<K, V> toLinkedMap(
            Function<T, K> keyMapper,
            Function<T, V> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper cannot be null");
        Objects.requireNonNull(valueMapper, "valueMapper cannot be null");

        if (isEmpty()) {
            return new LinkedHashMap<>();
        }

        Map<K, V> result = new LinkedHashMap<>();
        for (T element : source) {
            if (element != null) {
                K key = keyMapper.apply(element);
                if (key != null) {
                    V value = valueMapper.apply(element);
                    result.put(key, value);
                }
            }
        }
        return result;
    }


    public final ListStream<T> peek(Consumer<T> consumer) {
        return of(() -> new Iterator<>() {
            final Iterator<T> iterator = source.iterator();

            public boolean hasNext() {
                return iterator.hasNext();
            }

            public T next() {
                final T item = iterator.next();
                consumer.accept(item);
                return item;
            }
        });
    }

    public ListStream<T> peekStream(Consumer<ListStream<T>> streamOperation) {
        streamOperation.accept(of(source));
        return this;
    }


    public void forEach(Consumer<? super T> action) {
        Objects.requireNonNull(action);
        source.forEach(action);
    }


    /**
     * 抽象流迭代器基类。
     * 封装了 `hasNext` 和 `next` 的通用状态逻辑，子类只需实现 `computeNext()` 方法。
     * 有效防止了在多次调用 `hasNext` 时产生的副作用或重复计算。
     */
    private abstract static class AbstractStreamIterator<T, R> implements Iterator<R> {
        /** 源数据迭代器 */
        protected final Iterator<T> sourceIterator;
        /** 缓存的下一个元素 */
        protected R nextElement;
        /** 是否已计算过下一个元素 */
        private boolean hasNextComputed = false;
        /** 计算出的 hasNext 结果 */
        private boolean hasNextResult = false;

        protected AbstractStreamIterator(Iterator<T> sourceIterator) {
            this.sourceIterator = sourceIterator;
        }

        @Override
        public boolean hasNext() {
            if (!hasNextComputed) {
                hasNextResult = computeNext();
                hasNextComputed = true;
            }
            return hasNextResult;
        }

        @Override
        public R next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            // 消耗掉当前缓存的结果
            hasNextComputed = false;
            return nextElement;
        }

        /**
         * 计算下一个元素的逻辑。由子类实现。
         *
         * @return 如果有下一个元素则返回 true，并将元素存入 nextElement；否则返回 false。
         */
        protected abstract boolean computeNext();
    }

}