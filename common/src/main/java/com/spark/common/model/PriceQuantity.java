package com.spark.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 价格数量对
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceQuantity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 价格（最小单位）
     */
    private Long price;

    /**
     * 数量（最小单位）
     */
    private Long quantity;
}
