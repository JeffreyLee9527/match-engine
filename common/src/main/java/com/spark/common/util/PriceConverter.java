package com.spark.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 价格转换工具
 * 将BigDecimal转换为Long（最小单位），避免浮点数精度问题
 */
public class PriceConverter {
    private PriceConverter() {
        // 工具类，禁止实例化
    }

    /**
     * 将BigDecimal价格转换为Long（最小单位）
     *
     * @param price     价格
     * @param precision 精度（小数位数）
     * @return Long类型的价格（最小单位）
     */
    public static long toLong(BigDecimal price, int precision) {
        if (price == null) {
            throw new IllegalArgumentException("Price cannot be null");
        }
        if (precision < 0) {
            throw new IllegalArgumentException("Precision cannot be negative");
        }
        BigDecimal multiplier = BigDecimal.TEN.pow(precision);
        return price.multiply(multiplier).longValue();
    }

    /**
     * 将Long（最小单位）转换为BigDecimal价格
     *
     * @param price     价格（最小单位）
     * @param precision 精度（小数位数）
     * @return BigDecimal类型的价格
     */
    public static BigDecimal toBigDecimal(long price, int precision) {
        if (precision < 0) {
            throw new IllegalArgumentException("Precision cannot be negative");
        }
        BigDecimal divisor = BigDecimal.TEN.pow(precision);
        return BigDecimal.valueOf(price).divide(divisor, precision, RoundingMode.DOWN);
    }

    /**
     * 将String价格转换为Long（最小单位）
     *
     * @param price     价格字符串
     * @param precision 精度（小数位数）
     * @return Long类型的价格（最小单位）
     */
    public static long toLong(String price, int precision) {
        if (price == null || price.trim().isEmpty()) {
            throw new IllegalArgumentException("Price cannot be null or empty");
        }
        return toLong(new BigDecimal(price), precision);
    }
}
