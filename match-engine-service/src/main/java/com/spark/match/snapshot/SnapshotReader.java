package com.spark.match.snapshot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spark.match.orderbook.OrderBook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Snapshot读取器
 */
@Slf4j
@Component
public class SnapshotReader {
    @Value("${snapshot.base-path:./data/snapshot}")
    private String basePath;
    @Value("${snapshot.instance-id:${wal.instance-id:default}}")
    private String instanceId;
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 读取最新的Snapshot
     *
     * @param symbolId 交易对ID
     * @return Snapshot数据（包含OrderBook和lastAppliedWalSeq）
     */
    public SnapshotWriter.SnapshotData readLatestSnapshot(Integer symbolId) {
        try {
            Path snapshotPath = Paths.get(basePath, instanceId);
            if (!Files.exists(snapshotPath)) {
                log.info("Snapshot路径不存在: {}", snapshotPath);
                return null;
            }

            // 获取最新的Snapshot文件
            List<Path> snapshotFiles = Files.list(snapshotPath)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().contains("snapshot-" + instanceId + "-" + symbolId))
                    .sorted(Comparator.comparing((Path p) -> {
                        try {
                            return Files.getLastModifiedTime(p).toMillis();
                        } catch (IOException e) {
                            return 0L;
                        }
                    }).reversed())
                    .toList();

            if (snapshotFiles.isEmpty()) {
                log.info("未找到Snapshot文件: symbolId={}", symbolId);
                return null;
            }

            Path latestSnapshot = snapshotFiles.get(0);
            String snapshotJson = new String(Files.readAllBytes(latestSnapshot), "UTF-8");
            SnapshotWriter.SnapshotData snapshotData;
            try {
                snapshotData = objectMapper.readValue(snapshotJson, SnapshotWriter.SnapshotData.class);
            } catch (JsonProcessingException e) {
                log.error("Snapshot反序列化失败: symbolId={}, fileName={}", symbolId, latestSnapshot.getFileName(), e);
                return null;
            }

            // 验证checksum
            if (!validateChecksum(snapshotData, snapshotJson)) {
                log.error("Snapshot校验和验证失败: symbolId={}, fileName={}", symbolId, latestSnapshot.getFileName());
                return null;
            }

            log.info("读取Snapshot成功: fileName={}, symbolId={}, lastAppliedWalSeq={}",
                    latestSnapshot.getFileName(), symbolId, snapshotData.getLastAppliedWalSeq());

            return snapshotData;
        } catch (Exception e) {
            log.error("读取Snapshot失败: symbolId={}", symbolId, e);
            return null;
        }
    }

    /**
     * 验证校验和
     */
    private boolean validateChecksum(SnapshotWriter.SnapshotData snapshotData, String snapshotJson) {
        try {
            // 创建临时数据用于计算checksum（checksum字段设为0）
            SnapshotWriter.SnapshotData tempData = SnapshotWriter.SnapshotData.builder()
                    .snapshotVersion(snapshotData.getSnapshotVersion())
                    .symbolId(snapshotData.getSymbolId())
                    .lastAppliedWalSeq(snapshotData.getLastAppliedWalSeq())
                    .orderBook(snapshotData.getOrderBook())
                    .timestamp(snapshotData.getTimestamp())
                    .checksum(0L) // 临时设为0
                    .build();

            // 序列化整个数据
            String tempJson = objectMapper.writeValueAsString(tempData);
            byte[] tempBytes = tempJson.getBytes("UTF-8");
            long calculatedChecksum = calculateChecksum(tempBytes);
            
            return calculatedChecksum == snapshotData.getChecksum();
        } catch (JsonProcessingException e) {
            log.error("校验和验证失败: 序列化错误", e);
            return false;
        } catch (Exception e) {
            log.error("校验和验证失败", e);
            return false;
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
}
