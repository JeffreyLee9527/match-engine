package com.spark.match.wal;

import com.spark.common.model.OrderMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * WAL记录模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WALRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * WAL序列号
     */
    private Long walSeq;

    /**
     * 订单消息
     */
    private OrderMessage orderMessage;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 校验和（用于文件完整性校验）
     */
    private Long checksum;
}
