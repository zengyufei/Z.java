import com.zyf.util.Z;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

public class ZChainTest {
    public static void main(String[] args) {
        run();
    }

    @Test
    public void test(){
        run();
    }

    private static void run() {
        System.out.println(">>> 运行 ZChainTest: 超长链式与复杂场景验证");

        List<User> myList = Arrays.asList(
                new User("Banana", 99, 160.0),
                new User("Orange", 98, 177),
                new User("Alice", 25, 165.5),
                new User("Bob", 18, 175.0),
                new User("Charlie", null, 180.2),
                new User("David", 37, 160.0),
                new User("Apple", 111137, 145)
        );

        // 1. 基础 Map/Filter/Match 场景
        final Map<String, User> map = Z.li(myList).toMap(User::getName);
        assert map.containsKey("Alice");

        final Map<String, Integer> map1 = Z.li(myList).isNotNull(User::getAge).toMap(User::getName, User::getAge);
        assert map1.get("Bob") == 18;

        // 2. 分组场景
        Z.li(myList).groupBy(User::getAge, User::getName).toMap();

        Map<Integer, Map<String, Long>> myGroup2 = Z.li(myList)
                .groupingBy(
                        User::getAge,
                        Collectors.groupingBy(User::getName, Collectors.counting())
                )
                .toMap();
        System.out.println("myGroup2 keys: " + myGroup2.keySet());
        assert !myGroup2.isEmpty();


        // 3. 超长链式调用 (模拟用户极限用例)
        // 注意：由于 skip(10) 等操作会清空当前 4 个元素的流，为了让后续逻辑有输出，我们调整参数或关注其空流处理
        Map<Integer, List<String>> chainResult = Z.li(myList)
                .filter(user -> user.getAge() != null && user.getAge() > 10)
                .peekStream(e-> println("filter", e.toList()))
                .isNotNull(User::getAge)
                .peekStream(e-> println("isNotNull",e.toList()))
                .ors(e -> e.getAge() > 20, e -> e.getAge() < 100)
                .peekStream(e-> println("ors",e.toList()))
                .ands(e -> e.getAge() > 24, e -> e.getAge() < 99)
                .peekStream(e-> println("ands",e.toList()))
                .skip(1)
                .peekStream(e-> println("skip",e.toList()))
                .sub(1, 10)
                .peekStream(e-> println("sub",e.toList()))
                .concat(Z.asList(new User("Yama", 50, 167)))
                .peekStream(e-> println("concat",e.toList()))
                .add(new User("Zack", 50, 167))
                .add(new User("Zack", 150, 187))
                .add(new User("Tim", 33, 165))
                .peekStream(e-> println("add",e.toList()))
                .reversed()
                .peekStream(e-> println("reversed",e.toList()))
                .map(User::getName)
                .peekStream(e-> println("map",e.toList()))
                .distinct()
                .peekStream(e-> println("distinct",e.toList()))
                .sort(Comparator.naturalOrder())
                .peekStream(e-> println("sort",e.toList()))
                .map(String::toUpperCase)
                .peekStream(e-> println("toUpperCase",e.toList()))
                .filter(n -> n.startsWith("D") || n.startsWith("T") || n.startsWith("Z"))
                .peekStream(e-> println("filter",e.toList()))
                .limit(2)
                .peekStream(e-> println("limit",e.toList()))
                .map(n -> new User(n, 50, 167))
                .peekStream(e-> println("map",e.toList()))
                .groupBy(User::getAge)
                .peekStream(m -> println("groupBy", m))
                .valueStream(e -> e.map(User::getName).toList())
                .toMap();

        assert !chainResult.isEmpty();
        assert chainResult.get(50).contains("DAVID");
        assert chainResult.get(50).contains("TIM");

        System.out.println("ZChainTest 验证通过！");
    }

    private static void println(String msg, Object e) {
        System.out.println("============" + msg + " begin ============");
        System.out.println(e);
        System.out.println("============" + msg + " end ============");
    }
}
