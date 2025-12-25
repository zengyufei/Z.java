import com.zyf.util.Z;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;

public class ZLazyAuditTest {
    public static void main(String[] args) {
        run();
    }

    @Test
    public void test(){
        run();
    }

    private static void run() {
        System.out.println(">>> 开始 Z.java 100% 延迟加载深度审计");
        AtomicInteger counter = new AtomicInteger(0);

        // 准备一个带计数器的流
        Z<Integer> base = Z.range(1, 101).peek(i -> counter.incrementAndGet());

        // 1. 测试生成器与取样
        Z<Integer> takeStream = base.takeWhile(i -> i <= 10);
        assert counter.get() == 0 : "takeWhile 不应触发执行";

        long count = takeStream.count();
        // takeWhile 满足 10 个，会多拉取第 11 个来判断条件失败，所以是 11
        assert counter.get() == 11 : "终端操作触发后应执行。Count: " + counter.get();
        System.out.println("审计 takeWhile 延迟加载: 通过");

        // 2. 测试 Partition (分块)
        counter.set(0);
        Z<List<Integer>> parts = Z.range(1, 11).peek(i -> counter.incrementAndGet()).partition(3);
        assert counter.get() == 0 : "partition 不应触发执行";

        Iterator<List<Integer>> it = parts.iterator();
        assert counter.get() == 0 : "获取 iterator 不应触发执行";

        it.next(); // 拉取第一块 [1, 2, 3]
        assert counter.get() == 3 : "拉取第一块应触碰 3 个元素";

        it.next(); // 拉取第二块 [4, 5, 6]
        assert counter.get() == 6 : "拉取第二块应触碰累计 6 个元素";
        System.out.println("审计 partition 物理延迟拉取: 通过");

        // 3. 测试 Zip (拉链)
        counter.set(0);
        Z<Integer> left = Z.range(1, 11).peek(i -> counter.incrementAndGet());
        Z<String> right = Z.repeat("X", 5);
        Z<String> zipped = left.zip(right, (l, r) -> l + r);
        assert counter.get() == 0 : "zip 不应触发执行";

        zipped.toList();
        assert counter.get() == 5 : "zip 应只触碰左流匹配的 5 个元素";
        System.out.println("审计 zip 协同延迟拉取: 通过");

        // 4. 测试 mapNotNull
        counter.set(0);
        Z<Integer> nn = Z.range(1, 11).peek(i -> counter.incrementAndGet())
                         .mapNotNull(i -> i % 2 == 0 ? i : null);
        assert counter.get() == 0 : "mapNotNull 不应触发执行";
        nn.count();
        assert counter.get() == 10 : "mapNotNull 终端触发全量执行";
        System.out.println("审计 mapNotNull 延迟加载: 通过");

        System.out.println(">>> 延迟加载专项审计全部通过！Z.java 完全合规。");
    }
}
