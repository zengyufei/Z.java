import com.zyf.util.Z;
import org.junit.Test;

import java.util.*;

public class ZBaseTest {
    public static void main(String[] args) {
        run();
    }

    @Test
    public void test(){
        run();
    }

    private static void run() {
        System.out.println(">>> 运行 ZBaseTest: 工厂、截取与匹配");

        // 1. 工厂方法
        List<Integer> list1 = Z.asList(1, 2, 3);
        assert list1.size() == 3;

        List<Integer> list2 = Z.li(1, 2, 3, 4).toList();
        assert list2.size() == 4;

        List<User> users = Z.li(
                new User("Alice", 20),
                new User("Bob", 18),
                new User("Charlie", 25)
        ).toList();

        // 2. Match
        assert Z.li(users).anyMatch(e -> e.getAge() == 18);
        assert Z.li(users).noneMatch(e -> e.getAge() > 100);
        assert !Z.li(users).noneMatch(e -> e.getAge() == 18);
        assert Z.li(users).allMatch(e -> e.getAge() > 10);
        assert !Z.li(users).allMatch(e -> e.getAge() > 20);

        // 3. FindFirst & GetFirst & GetEnd
        assert Z.li(users).findFirst().get().getName().equals("Alice");
        assert Z.li(users).getFirst().getName().equals("Alice");
        assert "None".equals(Z.li().getFirst("None"));
        assert "LazyGone".equals(Z.li().getFirst(() -> "LazyGone"));

        assert Z.li(users).getEnd().getName().equals("Charlie");
        assert "Tail".equals(Z.li().getEnd("Tail"));
        assert "LazyTail".equals(Z.li().getEnd(() -> "LazyTail"));

        // 4. Limit/Skip/Sub
        List<User> subList = Z.li(users).sub(1, 3).toList(); // Index 1 to 2
        assert subList.size() == 2 && subList.get(0).getName().equals("Bob");

        List<User> limitList = Z.li(users).limit(1).toList();
        assert limitList.size() == 1 && limitList.get(0).getName().equals("Alice");

        System.out.println("ZBaseTest 通过！\n");
    }
}
