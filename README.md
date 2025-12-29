# Z.java 终极开发指南

`Z.java` 是一个为 Java 8+ 设计的、轻量级且高性能的流式处理工具库。它不仅对齐了 Java 8 Stream 及其后续版本的所有核心能力，还针对 **Spring Boot / MyBatis / JPA** 等实际业务场景提供了大量“降维打击”式的增强算子。

---

## 🚀 核心理念

### 1. 100% Fully Lazy (完全延迟加载)
所有的中间算子（Intermediate Operations）都不会立即触发计算。无论你定义了多长的链条（Map -> Filter -> Sort -> Partition），只有在最终调用 `toList()` 或 `count()` 等终端操作时，数据才会开始流动。

### 2. Zero Overhead (零冗余开销)
- **性能优化**：针对 `Collection` 源优化的 `count()` 为 O(1)，`toList()` 具备容量预分配。
- **内存友好**：支持 `partition` (分块) 延迟拉取，处理海量数据时不会撑爆内存。

### 3. Java 8 深度兼容
无需升级到 Java 21，即可享受 Java 9+ 的 `takeWhile`、`dropWhile`、`fold` (reduce) 等现代算子。

---

## 引入方法

```xml
    <dependencies>
        <dependency>
            <groupId>com.zyf</groupId>
            <artifactId>x-util</artifactId>
            <version>1.1</version>
        </dependency>
    </dependencies>

    <repositories>
        <!-- 曾玉飞 maven 个人仓库 -->
        <repository>
            <id>maven-repo-master</id>
            <url>https://raw.github.com/zengyufei/maven-repo/master/</url>
            <snapshots>
                <enabled>true</enabled>
                <updatePolicy>always</updatePolicy>
            </snapshots>
        </repository>
    </repositories>
```

---

## 🛠 一、 工厂方法 (Creation)

如何快速将数据包装进 `Z` 体系？

| 场景 | 代码示例 | 返回值 | 备注 |
| :--- | :--- | :--- | :--- |
| **集合包装** | `Z.li(list)` / `Z.li(set)` | `Z<T>` | 支持所有 `Iterable` 对象 |
| **变长参数** | `Z.li(1, 2, 3)` | `Z<Integer>` | 快捷静态工厂 |
| **数组转换** | `Z.asList(T... items)` | `List<T>` | 内部封装 `Arrays.asList` |
| **整数范围** | `Z.range(1, 10)` | `Z<Integer>` | 生成 [1, 10) 的延迟序列 |
| **重复序列** | `Z.repeat("A", 5)` | `Z<String>` | 产生 5 个 "A" 的流 |
| **无限/计算流** | `Z.iterate(1, i -> i + 1)` | `Z<Integer>` | 延迟计算下一个值，配合 limit 使用 |

---

## ✨ 二、 转换算子 (Transformation & Mapping)

这些算子负责改变流中元素的形式，且全部支持延迟加载。

| 算子 | 功能描述 | 返回值 | 示例 |
| :--- | :--- | :--- | :--- |
| **`map`** | 1:1 基础转换 | `Z<R>` | `Z.li(1).map(i -> i + "A")` |
| **`mapIndexed`** | 携带索引的转换 | `Z<R>` | `(idx, val) -> idx + val` |
| **`mapNotNull`** | 转换并过滤 null | `Z<R>` | `s -> tryInt(s) // 结果空则剔除` |
| **`flatMap`** | 1:N 扁平化转换 | `Z<R>` | `user -> user.getRoles()` |
| **`apply` (then)** | **业务批量转换** | `Z<R>` | `ids -> dao.findByIds(ids)` |

### 🛠 `apply` 算子的妙用 (ORM / N+1 解决)
在 Spring Boot 开发中，利用 `apply` 可以将 IDs 集合一次性交给外部 Service 批量查询：
```java
List<MchOrder> orders = Z.li(ids)
    .distinct()
    .apply(idList -> orderDao.selectBatch(idList)) // List<ID> -> List<Order>
    .toList();
```

---

## 🔍 三、 过滤与去重 (Filtering & Distinct)

| 算子 | 功能描述 | 返回值 | 备注 |
| :--- | :--- | :--- | :--- |
| **`filter`** | 基础断言过滤 | `Z<T>` | `i -> i > 10` |
| **`filterIndexed`** | 携带索引的过滤 | `Z<T>` | `(idx, val) -> idx % 2 == 0` |
| **`isNull` / `filterNull`**| 字段判空过滤 | `Z<T>` | `User::getName` |
| **`isNotBlank`** | 字段非空字符串过滤 | `Z<T>` | 自动处理 null 和 trim 后的空串 |
| **`discrete`** | 基础去重 | `Z<T>` | 内部基于 `HashSet` |
| **`discrete(Key)`** | 根据特定字段去重 | `Z<T>` | `User::getMobile` |

---

## 📊 四、 排序与截取 (Sorting & Slicing)

| 算子 | 功能描述 | 返回值 | 备注 |
| :--- | :--- | :--- | :--- |
| **`sort`** | 字段排序 | `Z<T>` | `sort(User::getAge, Sort.Desc, Sort.NullLast)` |
| **`reversed`** | 延迟翻转流 | `Z<T>` | 只有在迭代时才执行翻转动作 |
| **`shuffled`** | 随机打乱流 | `Z<T>` | 随机化处理 |
| **`limit` / `skip`** | 截取与跳过 | `Z<T>` | 对标原生 Stream |
| **`takeWhile`** | Java 9 风格截取 | `Z<T>` | 满足条件时取样，否则截断 |
| **`partition` / `split`** | **延迟分块** | `Z<List<T>>` | `partition(500)` 将流化为 500 大小的批次 |

---

## 🗂 五、 分组体系 (Grouping)

Z.java 的分组是**完全延迟加载**的，只有在你调用 `toMap()` 或再次开始流式处理时才会执行。

### 1. 基础分组 (`groupBy`)
```java
// 返回 ZMap 类型，封装了延迟计算逻辑
Z.ZMap<Integer, User> group = Z.li(users).groupBy(User::getAge);

// 转换为内存 Map
Map<Integer, List<User>> map = group.toMap();
```

### 2. 分组后转换 (`valueStream`)
```java
// 统计每个年龄的人数
Map<Integer, Long> ageCount = Z.li(users)
    .groupBy(User::getAge)
    .valueStream(list -> list.count())
    .toMap();
```

---

## 🏁 六、 终端操作 (Terminal Operations)

调用这些算子会**立即触发**流的遍历计算。

| 算子 | 返回值类型 | 说明 |
| :--- | :--- | :--- |
| **`toList`** | `List<T>` | 自动预分配容量，性能卓越 |
| **`toSet`** | `Set<T>` | 自动去重收集 |
| **`count`** | `long` | 若源是 Collection，复杂度 O(1) |
| **`findFirst`** | `Optional<T>` | 获取首个元素 |
| **`getFirst`** | `T` / `null` | 快捷获取首个，支持默认值回退 |
| **`getEnd`** | `T` / `null` | 快捷获取末尾，List 源复杂度 O(1) |
| **`get(index)`** | `T` | 提取指定索引的元素 |
| **`joining`** | `String` | 快捷字符串拼接 |
| **`sumInt`** | `int` | 聚合求和 |
| **`fold` / `reduce`**| `R` | 灵活累加器 (Kotlin fold 语义) |
| **`frequency`** | `Map<T, Long>` | 元素频率统计 |
| **`summarizing`** | `Stats` | 包含 count/sum/min/max/avg 的汇总 |

---

## 🔍 七、 调试算子 (`peekStream`)

不同于 `peek` 只能看单个元素，`peekStream` 可以让你观察当前链条中的“所有”剩余元素快照：
```java
Z.li(users)
  .filter(...)
  .peekStream(s -> System.out.println("当前合格数: " + s.count()))
  .map(...)
  .toList();
```

---

## 🏗 八、 多流协作 (Multi-stream)

| 算子 | 功能描述 | 返回值 | 备注 |
| :--- | :--- | :--- | :--- |
| **`concat`** | 合并两个流 | `Z<T>` | 顺序拼接 |
| **`zip`** | 拉链合并 | `Z<R>` | `(left, right) -> left + right` |
| **`intersect`** | 计算交集 | `Z<T>` | 保持左流顺序 |
| **`union`** | 计算并集 | `Z<T>` | 自动去重 |
| **`minus`** | 计算差集 | `Z<T>` | 左流有而右流无的元素 |

---

## 🏆 业务最佳实践 (Best Practices)

### 场景 A：Spring Boot 分页记录关联加载
这是 Web 开发中最频繁的场景：
```java
public PageResult<ShopResult> page(PageParam p) {
    PageResult<ShopResult> page = dao.selectPage(p);
    
    // 1. 批量加载所属分类信息
    Map<Long, Category> categoryMap = Z.li(page.getRecords())
        .map(ShopResult::getCategoryId)
        .discrete() 
        .apply(ids -> categoryDao.selectBatchIds(ids))
        .toMap(Category::getId);
    
    // 2. 字段回填
    Z.li(page.getRecords()).forEach(r -> {
        r.setCategoryName(categoryMap.getOrDefault(r.getCategoryId(), Category.EMPTY).getName());
    });
    
    return page;
}
```

### 场景 B：大批量数据分批处理 (批量 SQL)
避免一次性向数据库发送数万条数据：
```java
Z.li(massiveList)
  .partition(1000) // 每 1000 个分一组
  .forEach(batch -> {
      myMapper.batchInsert(batch);
      log.info("已完成一轮批量写入");
  });
```

### 场景 C：复杂索引逻辑处理
```java
// 仅对偶数行的元素进行特殊转换
Z.li(list)
  .mapIndexed((idx, val) -> idx % 2 == 0 ? val.toUpper() : val)
  .toList();
```

---

**© 2025 Z.java Project - 让 Java 流处理重回简单与自由。**
