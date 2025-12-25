import com.zyf.util.Z;
import org.junit.Test;

import java.util.*;

public class ZSortTest {
    public static void main(String[] args) {
        run();
    }

    @Test
    public void test(){
        run();
    }

    private static void run() {
        System.out.println(">>> 运行 ZSortTest: 排序体系测试");

        List<User> list = Arrays.asList(
                new User("Alice", 20, 165.5),
                new User("Bob", 18, 175.0),
                new User("Charlie", 20, 160.0),
                new User("David", null, 180.2)
        );

        // 1. 基础升降序
        List<Integer> ascAges = Z.li(list).filterNotNull(User::getAge).sortAsc(User::getAge).map(User::getAge).toList();
        assert ascAges.get(0) == 18;

        List<Integer> descAges = Z.li(list).filterNotNull(User::getAge).sortDesc(User::getAge).map(User::getAge).toList();
        assert descAges.get(0) == 20;

        // 2. 带 NullFirst/NullLast 的排序
        List<User> nullFirst = Z.li(list).sort(User::getAge, Z.Sort.Asc, Z.Sort.NullFirst).toList();
        assert nullFirst.get(0).getName().equals("David");

        List<User> nullLast = Z.li(list).sort(User::getAge, Z.Sort.Asc, Z.Sort.NullLast).toList();
        assert nullLast.get(3).getName().equals("David");

        // 3. 多级排序 (Age Desc, Height Asc)
        List<User> multiSort = Z.li(list).sort(
                ctx -> ctx.createComparator(User::getAge, Z.Sort.Desc, Z.Sort.NullLast),
                ctx -> ctx.createComparator(User::getHeight, Z.Sort.Asc)
        ).toList();
        // Index 0 & 1 should be Age 20. Charlie(160) should be before Alice(165.5)
        assert multiSort.get(0).getName().equals("Charlie");
        assert multiSort.get(1).getName().equals("Alice");

        System.out.println("ZSortTest 通过！\n");
    }
}
