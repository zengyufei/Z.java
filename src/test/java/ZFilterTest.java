import com.zyf.util.Z;
import org.junit.Test;

import java.util.*;

public class ZFilterTest {
    public static void main(String[] args) {
        run();
    }

    @Test
    public void test(){
        run();
    }

    private static void run() {
        System.out.println(">>> 运行 ZFilterTest: 增强过滤与复合逻辑");

        List<User> list = Arrays.asList(
                new User("Alice", 20),
                new User("  ", 18),
                new User(null, 25),
                new User("David", null)
        );

        // 1. isNull/isNotNull/filterNotNull
        assert Z.li(list).isNotNull(User::getAge).count() == 3;
        assert Z.li(list).filterNotNull(User::getName).count() == 3;
        assert Z.li(list).isNull(User::getAge).count() == 1;

        // 2. isBlank/isNotBlank/filterNotBlank
        assert Z.li(list).isNotBlank(User::getName).count() == 2; // Alice and David
        assert Z.li(list).filterBlank(User::getName).count() == 2; // "  " and null

        // 3. Logic: filters (AND) & filterOrs (OR)
        long countOr = Z.li(list).filterOrs(
                e -> "Alice".equals(e.getName()),
                e -> "David".equals(e.getName())
        ).count();
        assert countOr == 2;

        long countAnd = Z.li(list).filters(
                e -> e.getName() != null,
                e -> e.getAge() != null,
                e -> e.getAge() > 19
        ).count();
        assert countAnd == 1; // Only Alice

        System.out.println("ZFilterTest 通过！\n");
    }
}
