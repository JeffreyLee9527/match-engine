package com.spark.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 数量转换工具
 * 将BigDecimal转换为Long（最小单位），避免浮点数精度问题
 */
public class QuantityConverter {
    private QuantityConverter() {
        // 工具类，禁止实例化
    }

    /**
     * 将BigDecimal数量转换为Long（最小单位）
     *
     * @param quantity  数量
     * @param precision 精度（小数位数）
     * @return Long类型的数量（最小单位）
     */
    public static long toLong(BigDecimal quantity, int precision) {
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity cannot be null");
        }
        if (precision < 0) {
            throw new IllegalArgumentException("Precision cannot be negative");
        }
        BigDecimal multiplier = BigDecimal.TEN.pow(precision);
        return quantity.multiply(multiplier).longValue();
    }

    /**
     * 将Long（最小单位）转换为BigDecimal数量
     *
     * @param quantity  数量（最小单位）
     * @param precision 精度（小数位数）
     * @return BigDecimal类型的数量
     */
    public static BigDecimal toBigDecimal(long quantity, int precision) {
        if (precision < 0) {
            throw new IllegalArgumentException("Precision cannot be negative");
        }
        BigDecimal divisor = BigDecimal.TEN.pow(precision);
        return BigDecimal.valueOf(quantity).divide(divisor, precision, RoundingMode.DOWN);
    }

    /**
     * 将String数量转换为Long（最小单位）
     *
     * @param quantity  数量字符串
     * @param precision 精度（小数位数）
     * @return Long类型的数量（最小单位）
     */
    public static long toLong(String quantity, int precision) {
        if (quantity == null || quantity.trim().isEmpty()) {
            throw new IllegalArgumentException("Quantity cannot be null or empty");
        }
        return toLong(new BigDecimal(quantity), precision);
    }
}
