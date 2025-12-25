import com.zyf.util.Z;
import org.junit.Test;

import java.util.*;

public class ZMathTest {
    public static void main(String[] args) {
        run();
    }

    @Test
    public void test(){
        run();
    }

    private static void run() {
        System.out.println(">>> 运行 ZMathTest: 数学聚合测试");

        List<User> list = Arrays.asList(
                new User("A", 10, 1.0),
                new User("B", 20, 2.0),
                new User("C", null, 3.0)
        );

        // Int
        assert Z.li(list).filterNotNull(User::getAge).sumInt(User::getAge) == 30;

        // Long
        assert Z.li(1L, 2L, 3L).sumLong() == 6L;

        // Double
        assert Z.li(list).sumDouble(User::getHeight) == 6.0;

        System.out.println("ZMathTest 通过！\n");
    }
}
