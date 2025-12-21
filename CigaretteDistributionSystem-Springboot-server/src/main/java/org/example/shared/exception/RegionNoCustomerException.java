package org.example.shared.exception;

/**
 * 区域无客户异常
 * <p>
 * 当某个区域在 customer_filter 表中没有对应的客户数据时抛出此异常。
 * 该异常表示该区域的30个档位客户数全为0，无法进行分配。
 * </p>
 *
 * @author Robin
 * @version 1.0
 * @since 2025-12-15
 */
public class RegionNoCustomerException extends RuntimeException {

    private final String regionName;
    private final Integer year;
    private final Integer month;
    private final Integer weekSeq;

    public RegionNoCustomerException(String regionName, Integer year, Integer month, Integer weekSeq) {
        super(String.format("区域 '%s' 在时间分区 %d-%d-%d 中没有客户数据（30个档位全为0）", 
                regionName, year, month, weekSeq));
        this.regionName = regionName;
        this.year = year;
        this.month = month;
        this.weekSeq = weekSeq;
    }

    public String getRegionName() {
        return regionName;
    }

    public Integer getYear() {
        return year;
    }

    public Integer getMonth() {
        return month;
    }

    public Integer getWeekSeq() {
        return weekSeq;
    }
}

