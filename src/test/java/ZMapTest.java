import com.zyf.util.Z;
import org.junit.Test;

import java.util.*;

public class ZMapTest {
    public static void main(String[] args) {
        run();
    }

    @Test
    public void test(){
        run();
    }

    private static void run() {
        System.out.println(">>> 运行 ZMapTest: Map 转换测试");

        List<User> list = Arrays.asList(
                new User("Alice", 20),
                new User("Bob", 18),
                new User("Alice", 25)
        );

        // 1. toMap 多重载
        Map<String, User> map1 = Z.li(list).toMap(User::getName);
        assert map1.get("Alice").getAge() == 25; // 默认覆盖

        Map<String, Integer> map2 = Z.li(list).toMap(User::getName, User::getAge);
        assert map2.get("Alice") == 25;

        Map<String, Integer> map3 = Z.li(list).toMap(
                User::getName,
                User::getAge,
                (oldV, newV) -> oldV // 冲突保留旧值
        );
        assert map3.get("Alice") == 20;

        // 2. toLinkedMap
        Map<String, Integer> linkedMap = Z.li(list).toLinkedMap(User::getName, User::getAge);
        Iterator<String> it = linkedMap.keySet().iterator();
        assert it.next().equals("Alice");
        assert it.next().equals("Bob");

        System.out.println("ZMapTest 通过！\n");
    }
}
