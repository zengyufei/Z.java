import com.zyf.util.Z;
import org.junit.Test;

import java.util.*;
import java.util.stream.*;

public class ZUltimateTest {
    public static void main(String[] args) {
        run();
    }

    @Test
    public void test(){
        run();
    }

    private static void run() {
        System.out.println(">>> 运行 ZUltimateTest: 全能算子深度验证");

        // 1. Generators
        assert Z.range(1, 5).toList().size() == 4;
        assert Z.range(1, 5).getEnd() == 4;
        assert Z.iterate(1, i -> i * 2).limit(4).getEnd() == 8;
        assert Z.repeat("A", 3).joining("").equals("AAA");
        System.out.println("Generators 验证通过");

        // 2. Aggregation
        List<Integer> nums = Z.range(1, 11).toList(); // 1..10
        assert Z.li(nums).min(Comparator.naturalOrder()).get() == 1;
        assert Z.li(nums).max(Comparator.naturalOrder()).get() == 10;
        assert Z.li(nums).average() == 5.5;
        DoubleSummaryStatistics stats = Z.li(nums).summarizing();
        assert stats.getCount() == 10;
        assert stats.getSum() == 55.0;
        assert stats.getMax() == 10.0;

        List<User> users = Arrays.asList(
            new User("A", 10, 160),
            new User("B", 20, 170),
            new User("C", 15, 165)
        );
        assert Z.li(users).minBy(User::getAge).get().getName().equals("A");
        assert Z.li(users).maxBy(User::getAge).get().getName().equals("B");
        System.out.println("Aggregation 验证通过");

        // 3. Slicing & Control (takeWhile, dropWhile, partition)
        List<Integer> seq = Z.li(1, 2, 3, 10, 11, 4, 5).toList();
        assert Z.li(seq).takeWhile(i -> i < 10).toList().size() == 3;
        assert Z.li(seq).dropWhile(i -> i < 10).findFirst().get() == 10;

        // Partition (split)
        Z<List<Integer>> parts = Z.range(0, 10).partition(3); // [0,1,2], [3,4,5], [6,7,8], [9]
        List<List<Integer>> partList = parts.toList();
        assert partList.size() == 4;
        assert partList.get(0).size() == 3;
        assert partList.get(3).size() == 1;
        assert Z.range(0, 10).split(3).count() == 4; // Alias check

        Map<Boolean, List<Integer>> splitResult = Z.range(1, 11).splitBy(i -> i % 2 == 0);
        assert splitResult.get(true).size() == 5;
        System.out.println("Slicing & Control 验证通过");

        // 4. Multi-stream (zip, intersect, union, minus)
        List<String> names = Arrays.asList("Alice", "Bob");
        List<Integer> ages = Arrays.asList(20, 30, 40);
        List<String> zipped = Z.li(names).zip(ages, (n, a) -> n + ":" + a).toList();
        assert zipped.size() == 2;
        assert zipped.get(1).equals("Bob:30");

        List<Integer> list1 = Arrays.asList(1, 2, 3, 4);
        List<Integer> list2 = Arrays.asList(3, 4, 5, 6);
        assert Z.li(list1).intersect(list2).count() == 2;
        assert Z.li(list1).union(list2).count() == 6;
        assert Z.li(list1).minus(list2).count() == 2; // [1, 2]
        System.out.println("Multi-stream 验证通过");

        // 5. Shortcuts & Collectors
        assert Z.li(1, 2, 2, 3).toSet().size() == 3;
        assert Z.li(3, 1, 2).toTreeSet(Comparator.naturalOrder()).iterator().next() == 1;
        assert Z.li(1, 2, 3).contains(2);
        assert !Z.li(1, 2, 3).contains(5);

        Map<String, Long> freq = Z.li("A", "B", "A", "C", "B", "A").frequency();
        assert freq.get("A") == 3;
        assert freq.get("C") == 1;
        System.out.println("Shortcuts & Collectors 验证通过");

        System.out.println("ZUltimateTest 全部通过！");
    }
}
