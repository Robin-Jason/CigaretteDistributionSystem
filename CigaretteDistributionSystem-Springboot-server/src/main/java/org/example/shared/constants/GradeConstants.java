package org.example.shared.constants;

/**
 * 档位相关常量定义。
 * <p>
 * 统一管理项目中与档位相关的常量，包括档位名称、档位数量等。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-14
 */
public final class GradeConstants {

    /**
     * 档位总数（D30到D1共30个档位）
     */
    public static final int GRADE_COUNT = 30;

    /**
     * 档位名称数组（从D30到D1）
     */
    public static final String[] GRADE_NAMES = {
            "D30", "D29", "D28", "D27", "D26", "D25", "D24", "D23", "D22", "D21",
            "D20", "D19", "D18", "D17", "D16", "D15", "D14", "D13", "D12", "D11",
            "D10", "D9", "D8", "D7", "D6", "D5", "D4", "D3", "D2", "D1"
    };

    /**
     * 最高档位索引（D30对应索引0）
     */
    public static final int HIGHEST_GRADE_INDEX = 0;

    /**
     * 最低档位索引（D1对应索引29）
     */
    public static final int LOWEST_GRADE_INDEX = 29;

    private GradeConstants() {
        // 工具类，禁止实例化
    }
}

