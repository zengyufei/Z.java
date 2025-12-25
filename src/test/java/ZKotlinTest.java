import com.zyf.util.Z;
import org.junit.Test;

import java.util.*;

public class ZKotlinTest {
    public static void main(String[] args) {
        run();
    }

    @Test
    public void test(){
        run();
    }

    private static void run() {
        System.out.println(">>> 运行 ZKotlinTest: Kotlin 风格算子集成验证");

        // 1. Indexed Operations
        List<String> indexedMap = Z.li("a", "b", "c")
            .mapIndexed((i, s) -> i + ":" + s)
            .toList();
        assert indexedMap.get(0).equals("0:a");
        assert indexedMap.get(2).equals("2:c");

        List<String> indexedFilter = Z.li("a", "b", "c", "d")
            .filterIndexed((i, s) -> i % 2 == 0)
            .toList();
        assert indexedFilter.size() == 2;
        assert indexedFilter.get(1).equals("c");

        StringBuilder sb = new StringBuilder();
        Z.li("X", "Y").forEachIndexed((i, s) -> sb.append(i).append(s));
        assert sb.toString().equals("0X1Y");
        System.out.println("Indexed Operations 验证通过");

        // 2. mapNotNull
        List<Integer> nonNulls = Z.li("1", "A", "2", "B")
            .mapNotNull(s -> {
                try { return Integer.parseInt(s); }
                catch (NumberFormatException e) { return null; }
            })
            .toList();
        assert nonNulls.size() == 2;
        assert nonNulls.get(1) == 2;
        System.out.println("mapNotNull 验证通过");

        // 3. get(index) & shuffled
        List<Integer> nums = Z.range(0, 10).toList();
        assert Z.li(nums).get(5) == 5;
        assert Z.li(nums).get(10) == null;
        assert Z.li(nums).get(-1) == null;

        Z<Integer> shuffled = Z.li(nums).shuffled();
        List<Integer> shuffledList = shuffled.toList();
        assert shuffledList.size() == 10;
        // 随机性检查（极低概率完全相等）
        boolean different = false;
        for (int i = 0; i < nums.size(); i++) {
            if (!nums.get(i).equals(shuffledList.get(i))) {
                different = true;
                break;
            }
        }
        assert different : "shuffled() 应当打乱顺序";
        System.out.println("get(index) & shuffled 验证通过");

        // 4. fold (initial, BiFunction)
        // 计算长度总和 (String -> Integer)
        int totalLength = Z.li("Hello", "World", "Hi")
            .fold(0, (acc, s) -> acc + s.length());
        assert totalLength == 12;

        // 拼接字符串 (Integer -> String)
        String joined = Z.range(1, 4)
            .fold("Start", (acc, i) -> acc + "-" + i);
        assert joined.equals("Start-1-2-3");
        System.out.println("fold (indexed-style) 验证通过");

        System.out.println("ZKotlinTest 全部通过！");
    }
}
