import com.zyf.util.Z;
import org.junit.Test;

import java.util.*;


public class ZMyBatisTest {

    public static void main(String[] args) {
        run();
    }

    @Test
    public void test(){
        run();
    }

    // 模拟业务实体
    static class MchAppResult {
        String appId;
        String config;
        public MchAppResult(String appId) { this.appId = appId; }
        public String getAppId() { return appId; }
        public void setConfig(String config) { this.config = config; }
    }

    static class CashierMchConfig {
        String appId;
        String detail;
        public CashierMchConfig(String appId, String detail) { this.appId = appId; this.detail = detail; }
        public String getAppId() { return appId; }
        public String getDetail() { return detail; }
    }

    private static void run() {
        System.out.println(">>> 运行 ZMyBatisTest: 模拟批量关联查询场景");

        // 1. 模拟分页查询出来的原始记录
        List<MchAppResult> records = Arrays.asList(
            new MchAppResult("app_01"),
            new MchAppResult("app_02"),
            new MchAppResult("app_03")
        );

        // 2. 模拟 Service 层根据 IDs 批量查询的方法
        // cashierMchConfigManager.findAllByFields(..., appIds)
        java.util.function.Function<List<String>, List<CashierMchConfig>> mockDaoFindAll = (ids) -> {
            System.out.println("DAO: 正在执行批量查询 SQL -> SELECT * FROM config WHERE app_id IN " + ids);
            return Arrays.asList(
                new CashierMchConfig("app_01", "Config_01"),
                new CashierMchConfig("app_03", "Config_03")
            );
        };

        // 3. 使用 Z.java 优雅处理
        Map<String, CashierMchConfig> configMap = Z.li(records)
                .map(MchAppResult::getAppId)
                .distinct()
                .apply(mockDaoFindAll::apply) // 此处即为新算子 apply
                .toMap(CashierMchConfig::getAppId);

        // 4. 回填数据 (使用 Z 遍历)
        Z.li(records).forEach(record -> {
            CashierMchConfig cfg = configMap.get(record.getAppId());
            if (cfg != null) record.setConfig(cfg.getDetail());
        });

        // 5. 验证结果
        assert records.get(0).config.equals("Config_01");
        assert records.get(1).config == null;
        assert records.get(2).config.equals("Config_03");

        System.out.println("ZMyBatisTest 业务场景验证通过！");
    }
}
