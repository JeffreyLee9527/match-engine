package com.spark.match.snapshot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spark.match.orderbook.OrderBook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Snapshot写入器
 * 异步创建Snapshot，不阻塞撮合线程
 */
@Slf4j
@Component
public class SnapshotWriter {
    private final AtomicLong snapshotSeq = new AtomicLong(0);
    @Value("${snapshot.base-path:./data/snapshot}")
    private String basePath;
    @Value("${snapshot.instance-id:${wal.instance-id:default}}")
    private String instanceId;
    @Autowired
    private ObjectMapper objectMapper;

    @jakarta.annotation.PostConstruct
    public void init() {
        initialize();
    }

    private void initialize() {
        try {
            Path snapshotPath = Paths.get(basePath, instanceId);
            Files.createDirectories(snapshotPath);
        } catch (IOException e) {
            log.error("初始化Snapshot失败", e);
            throw new RuntimeException("初始化Snapshot失败", e);
        }
    }

    /**
     * 创建Snapshot（异步）
     * 关键：使用深拷贝避免Snapshot创建时OrderBook被修改
     *
     * @param orderBook         订单簿
     * @param lastAppliedWalSeq 最后应用的WAL序列号
     */
    @Async
    public void createSnapshot(OrderBook orderBook, long lastAppliedWalSeq) {
        long startTime = System.currentTimeMillis();
        long stepStartTime;
        try {
            long seq = snapshotSeq.incrementAndGet();
            String fileName = String.format("snapshot-%s-%d-%d.dat", instanceId, orderBook.getSymbolId(), System.currentTimeMillis());
            Path filePath = Paths.get(basePath, instanceId, fileName);

            // 【关键】深拷贝OrderBook，避免Snapshot创建时OrderBook被修改
            stepStartTime = System.currentTimeMillis();
            OrderBook orderBookCopy = orderBook.deepCopy();
            long deepCopyDuration = System.currentTimeMillis() - stepStartTime;
            log.debug("[Snapshot耗时] 深拷贝OrderBook耗时: {}ms, symbolId={}", deepCopyDuration, orderBook.getSymbolId());

            // 序列化OrderBook（使用深拷贝）
            SnapshotData snapshotData = SnapshotData.builder()
                    .snapshotVersion(1)
                    .symbolId(orderBookCopy.getSymbolId())
                    .lastAppliedWalSeq(lastAppliedWalSeq)
                    .orderBook(orderBookCopy)
                    .timestamp(System.currentTimeMillis())
                    .checksum(0L) // 临时值，将在序列化后计算
                    .build();

            // 先序列化计算checksum
            stepStartTime = System.currentTimeMillis();
            String snapshotJson = objectMapper.writeValueAsString(snapshotData);
            byte[] snapshotBytes = snapshotJson.getBytes("UTF-8");
            long checksum = calculateChecksum(snapshotBytes);
            long serializeDuration = System.currentTimeMillis() - stepStartTime;
            log.debug("[Snapshot耗时] 序列化耗时: {}ms, symbolId={}", serializeDuration, orderBook.getSymbolId());
            
            // 重新设置checksum并序列化
            snapshotData.setChecksum(checksum);
            snapshotJson = objectMapper.writeValueAsString(snapshotData);
            snapshotBytes = snapshotJson.getBytes("UTF-8");
            
            // 写入文件
            stepStartTime = System.currentTimeMillis();
            Files.write(filePath, snapshotBytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            long writeDuration = System.currentTimeMillis() - stepStartTime;
            log.debug("[Snapshot耗时] 文件写入耗时: {}ms, symbolId={}", writeDuration, orderBook.getSymbolId());

            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("[Snapshot耗时] Snapshot创建成功: fileName={}, symbolId={}, lastAppliedWalSeq={}, 总耗时={}ms, 深拷贝耗时={}ms, 序列化耗时={}ms, 写入耗时={}ms", 
                    fileName, orderBook.getSymbolId(), lastAppliedWalSeq, totalDuration, deepCopyDuration, serializeDuration, writeDuration);

            // 清理旧的Snapshot文件（保留最近12个）
            stepStartTime = System.currentTimeMillis();
            cleanupOldSnapshots(orderBook.getSymbolId());
            log.debug("[Snapshot耗时] 清理旧文件耗时: {}ms, symbolId={}", 
                    System.currentTimeMillis() - stepStartTime, orderBook.getSymbolId());
        } catch (JsonProcessingException e) {
            long totalDuration = System.currentTimeMillis() - startTime;
            log.error("[Snapshot耗时] Snapshot序列化失败: 总耗时={}ms, symbolId={}", totalDuration, orderBook.getSymbolId(), e);
        } catch (Exception e) {
            long totalDuration = System.currentTimeMillis() - startTime;
            log.error("[Snapshot耗时] 创建Snapshot失败: 总耗时={}ms, symbolId={}", totalDuration, orderBook.getSymbolId(), e);
        }
    }

    /**
     * 清理旧的Snapshot文件
     */
    private void cleanupOldSnapshots(Integer symbolId) {
        try {
            Path snapshotPath = Paths.get(basePath, instanceId);
            if (!Files.exists(snapshotPath)) {
                return;
            }

            Files.list(snapshotPath)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().contains("snapshot-" + instanceId + "-" + symbolId))
                    .sorted((p1, p2) -> {
                        try {
                            return Long.compare(Files.getLastModifiedTime(p2).toMillis(), Files.getLastModifiedTime(p1).toMillis());
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .skip(12) // 保留最近12个（1小时，每5分钟一个）
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            log.info("删除旧Snapshot: {}", path.getFileName());
                        } catch (IOException e) {
                            log.info("删除旧Snapshot失败: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.error("清理旧Snapshot失败: symbolId={}", symbolId, e);
        }
    }

    /**
     * 计算校验和
     */
    private long calculateChecksum(byte[] data) {
        long checksum = 0;
        for (byte b : data) {
            checksum = (checksum * 31) + b;
        }
        return checksum;
    }

    /**
     * Snapshot数据模型
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SnapshotData {
        /**
         * 快照版本号
         */
        private Integer snapshotVersion;
        /**
         * 交易对ID
         */
        private Integer symbolId;
        /**
         * 最后应用的WAL序列号
         */
        private Long lastAppliedWalSeq;
        /**
         * 订单簿状态
         */
        private OrderBook orderBook;
        /**
         * 时间戳
         */
        private Long timestamp;
        /**
         * 文件完整性校验和
         */
        private Long checksum;
    }
}
