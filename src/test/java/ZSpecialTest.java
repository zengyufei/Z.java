import com.zyf.util.Z;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ZSpecialTest {
    public static void main(String[] args) {
        run();
    }

    @Test
    public void test(){
        run();
    }

    private static void run() {
        System.out.println(">>> 运行 ZSpecialTest: 特殊流操作与延迟验证");

        // 1. flatMap
        List<List<Integer>> nested = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4));
        List<Integer> flat = Z.li(nested).flatMap(e -> e).toList();
        assert flat.equals(Arrays.asList(1, 2, 3, 4));

        // 2. concat & joining
        String joined = Z.li("A", "B").concat(Z.li("C")).joining(",");
        assert joined.equals("A,B,C");

        // 3. peekStream (针对子流的副作用)
        List<User> users = Arrays.asList(new User("Alice", 20), new User("Bob", 15));
        Z.li(users).peekStream(stream -> stream
                .filter(u -> u.getAge() > 18)
                .forEach(u -> u.setName(u.getName() + "_Adult"))
        ).toList();
        assert users.get(0).getName().equals("Alice_Adult");
        assert users.get(1).getName().equals("Bob");

        // 4. Reduce
        List<Integer> reduced = Z.li(1, 2, 3).reduce(ArrayList::new, ArrayList::add);
        assert reduced.size() == 3;

        // 5. Lazy Evaluation Proof
        AtomicInteger counter = new AtomicInteger(0);
        Z<Integer> stream = Z.li(1, 2, 3).peek(i -> counter.incrementAndGet());
        assert counter.get() == 0;

        Z<Integer> reversedStream = stream.reversed();
        assert counter.get() == 0 : "reversed() 不应触发执行";
        System.out.println("测试 reversed() 延迟加载 (counter == 0): 通过");

        // 6. GroupBy Lazy Evaluation
        counter.set(0);
        Z<Integer> stream2 = Z.li(1, 2, 3).peek(i -> counter.incrementAndGet());
        Z.ZMap<Integer, Integer> groupLazy = stream2.groupBy(i -> i % 2);
        assert counter.get() == 0 : "groupBy() 不应触发执行";
        System.out.println("测试 groupBy() 延迟加载 (counter == 0): 通过");

        groupLazy.toMap();
        assert counter.get() == 3 : "group.toMap() 应触发执行";
        System.out.println("测试 group.toMap() 延迟触发 (counter == 3): 通过");

        System.out.println("ZSpecialTest 通过！\n");
    }
}
