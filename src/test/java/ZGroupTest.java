import com.zyf.util.Z;
import org.junit.Test;

import java.util.*;

public class ZGroupTest {
    public static void main(String[] args) {
        run();
    }

    @Test
    public void test(){
        run();
    }

    private static void run() {
        System.out.println(">>> 运行 ZGroupTest: 链式分组与聚合");

        List<User> myList = Arrays.asList(
                new User("Alice", 20),
                new User("Bob", 20),
                new User("Charlie", null)
        );

        // 1. 简单分组 (Key -> List<T>)
        Map<Integer, List<User>> groups = Z.li(myList).groupBy(User::getAge).toMap();
        assert groups.get(20).size() == 2;
        assert groups.get(null).get(0).getName().equals("Charlie");

        // 2. 链式分组与 ValueStream 转换 (Key -> List<Name>)
        Map<Integer, List<String>> myGroup1 = Z.li(myList)
                .groupBy(User::getAge)
                .valueStream(z -> z.map(User::getName).toList())
                .toMap();
        assert myGroup1.get(20).contains("Alice");

        // 3. 嵌套链式分组 (Age -> (Name -> Count))
        Map<Integer, Map<String, Long>> myGroup2 = Z.li(myList)
                .groupBy(User::getAge)
                .valueStream(z -> z.groupBy(User::getName).valueStream(Z.liStream::count).toMap())
                .toMap();
        assert myGroup2.get(20).get("Alice") == 1L;

        System.out.println("ZGroupTest 通过！\n");
    }
}
