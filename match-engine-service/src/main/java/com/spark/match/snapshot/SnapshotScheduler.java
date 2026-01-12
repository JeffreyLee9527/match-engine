package com.spark.match.snapshot;

import com.spark.match.orderbook.OrderBook;
import com.spark.match.orderbook.OrderBookManager;
import com.spark.match.wal.WALWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Snapshot调度器
 * 定期创建Snapshot（每5分钟）
 */
@Slf4j
@Component
public class SnapshotScheduler {
    @Autowired
    private OrderBookManager orderBookManager;
    @Autowired
    private SnapshotWriter snapshotWriter;

    /**
     * 定期创建Snapshot（每5分钟）
     * 关键：使用每个订单簿的lastAppliedWalSeq，而不是全局的currentWalSeq
     */
    @Scheduled(fixedRate = 300000) // 5分钟 = 300000毫秒
    public void createSnapshots() {
        long startTime = System.currentTimeMillis();
        try {
            Map<Integer, OrderBook> orderBooks = orderBookManager.getAllOrderBooks();

            log.info("[Snapshot耗时] 开始创建Snapshot: orderBookCount={}", orderBooks.size());

            for (OrderBook orderBook : orderBooks.values()) {
                // 使用订单簿的lastAppliedWalSeq，而不是全局的currentWalSeq
                // 因为不同订单簿可能应用到了不同的WAL序列号
                long lastAppliedWalSeq = orderBook.getLastAppliedWalSeq();
                snapshotWriter.createSnapshot(orderBook, lastAppliedWalSeq);
            }

            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("[Snapshot耗时] Snapshot批量创建完成: orderBookCount={}, 总耗时={}ms", orderBooks.size(), totalDuration);
        } catch (Exception e) {
            long totalDuration = System.currentTimeMillis() - startTime;
            log.error("[Snapshot耗时] 创建Snapshot失败: 总耗时={}ms", totalDuration, e);
        }
    }
}
