package com.spark.match.wal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spark.common.model.OrderMessage;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * WAL写入器
 * 关键：必须在Consumer线程中同步写入，同步fsync
 */
@Slf4j
@Component
public class WALWriter {
    // 当前WAL序列号
    private final AtomicLong walSeq = new AtomicLong(0);
    @Value("${wal.base-path:./data/wal}")
    private String basePath;
    @Value("${wal.instance-id:default}")
    private String instanceId;
    @Value("${wal.max-file-size:104857600}")
    private long maxFileSize;
    @Value("${wal.max-file-age:3600000}")
    private long maxFileAge; // 1小时 = 3600000毫秒
    @Autowired
    private ObjectMapper objectMapper;
    // 当前WAL文件
    private volatile FileChannel currentChannel;
    // 当前WAL文件名和大小
    private volatile String currentFileName;
    private volatile long currentFileSize;
    // 当前WAL文件创建时间
    private volatile long currentFileCreateTime;

    @PostConstruct
    public void init() {
        initialize();
    }

    private void initialize() {
        try {
            Path walPath = Paths.get(basePath, instanceId);
            Files.createDirectories(walPath);
            rollWALFile();
            currentFileCreateTime = System.currentTimeMillis();
        } catch (IOException e) {
            log.error("初始化WAL失败", e);
            throw new RuntimeException("初始化WAL失败", e);
        }
    }

    /**
     * 追加写WAL（同步fsync）
     *
     * @param message 订单消息
     * @return WAL序列号
     */
    public synchronized long append(OrderMessage message) {
        long startTime = System.currentTimeMillis();
        long stepStartTime;
        try {
            // 检查文件大小或时间，决定是否滚动
            long currentTime = System.currentTimeMillis();
            boolean shouldRotate = currentFileSize >= maxFileSize || (currentFileCreateTime > 0 && (currentTime - currentFileCreateTime) >= maxFileAge);

            if (shouldRotate) {
                stepStartTime = System.currentTimeMillis();
                rollWALFile();
                log.debug("[WAL耗时] 文件滚动耗时: {}ms", System.currentTimeMillis() - stepStartTime);
            }

            // 创建WAL记录
            long seq = walSeq.incrementAndGet();
            // 临时值，将在序列化后计算
            WALRecord record = WALRecord.builder()
                    .walSeq(seq)
                    .orderMessage(message)
                    .timestamp(System.currentTimeMillis())
                    .checksum(0L)
                    .build();

            // 序列化记录
            stepStartTime = System.currentTimeMillis();
            String recordJson = objectMapper.writeValueAsString(record);
            byte[] recordData = recordJson.getBytes("UTF-8");
            long serializeDuration = System.currentTimeMillis() - stepStartTime;

            // 基于recordData计算checksum
            long checksum = calculateChecksum(recordData);
            // 重新设置checksum（需要重新序列化）
            record.setChecksum(checksum);
            recordJson = objectMapper.writeValueAsString(record);
            recordData = recordJson.getBytes("UTF-8");

            // 写入长度前缀（4字节）
            ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
            lengthBuffer.putInt(recordData.length);
            lengthBuffer.flip();

            // 追加写文件（同步）
            stepStartTime = System.currentTimeMillis();
            currentChannel.write(lengthBuffer);
            currentChannel.write(ByteBuffer.wrap(recordData));
            currentChannel.force(true); // 同步数据和元数据，保证数据落盘
            long fsyncDuration = System.currentTimeMillis() - stepStartTime;

            currentFileSize += 4 + recordData.length;

            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("[WAL耗时] WAL写入完成: walSeq={}, fileName={}, 总耗时={}ms, 序列化耗时={}ms, fsync耗时={}ms, orderId={}", 
                    seq, currentFileName, totalDuration, serializeDuration, fsyncDuration, message.getOrderId());
            return seq;
        } catch (JsonProcessingException e) {
            log.error("WAL消息序列化失败: message={}", message, e);
            throw new RuntimeException("WAL消息序列化失败", e);
        } catch (Exception e) {
            log.error("WAL写入失败: message={}", message, e);
            throw new RuntimeException("WAL写入失败", e);
        }
    }

    /**
     * 滚动WAL文件
     */
    private void rollWALFile() {
        FileChannel oldChannel = currentChannel;
        String oldFileName = currentFileName;
        try {
            // 创建新文件
            String fileName = String.format("wal-%s-%d.log", instanceId, System.currentTimeMillis());
            Path filePath = Paths.get(basePath, instanceId, fileName);
            FileChannel newChannel = FileChannel.open(filePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);

            // 原子性更新
            currentChannel = newChannel;
            currentFileName = fileName;
            currentFileSize = Files.size(filePath);
            currentFileCreateTime = System.currentTimeMillis();

            // 关闭旧文件
            if (oldChannel != null && oldChannel.isOpen()) {
                oldChannel.force(true); // 同步数据和元数据
                oldChannel.close();
            }

            log.info("WAL文件滚动: fileName={}, size={}", fileName, currentFileSize);

            // 清理旧WAL文件
            cleanupOldWALFiles();
        } catch (IOException e) {
            // 恢复状态
            currentChannel = oldChannel;
            currentFileName = oldFileName;
            log.error("WAL文件滚动失败", e);
            throw new RuntimeException("WAL文件滚动失败", e);
        }
    }

    /**
     * 计算校验和
     */
    private long calculateChecksum(byte[] data) {
        long checksum = 0;
        for (byte b : data) {
            checksum = (checksum * 31 + b) & 0xFFFFFFFFFFFFFFFFL;
        }
        return checksum;
    }

    /**
     * 获取当前WAL序列号
     */
    public long getCurrentWalSeq() {
        return walSeq.get();
    }

    /**
     * 清理旧WAL文件
     * 保留最近10个WAL文件，删除更旧的文件
     */
    private void cleanupOldWALFiles() {
        try {
            Path walPath = Paths.get(basePath, instanceId);
            if (!Files.exists(walPath)) {
                return;
            }

            List<Path> walFiles = Files.list(walPath).filter(Files::isRegularFile).filter(p -> p.getFileName().toString().startsWith("wal-")).filter(p -> p.getFileName().toString().endsWith(".log")).sorted((p1, p2) -> {
                try {
                    return Long.compare(Files.getLastModifiedTime(p2).toMillis(), Files.getLastModifiedTime(p1).toMillis());
                } catch (IOException e) {
                    return 0;
                }
            }).toList();

            // 保留最近10个文件，删除更旧的文件
            if (walFiles.size() > 10) {
                for (int i = 10; i < walFiles.size(); i++) {
                    try {
                        Files.delete(walFiles.get(i));
                        log.info("删除旧WAL文件: {}", walFiles.get(i).getFileName());
                    } catch (IOException e) {
                        log.info("删除旧WAL文件失败: {}", walFiles.get(i).getFileName(), e);
                    }
                }
            }
        } catch (IOException e) {
            log.error("清理旧WAL文件失败", e);
        }
    }

    /**
     * 关闭WAL写入器
     */
    public void close() {
        try {
            if (currentChannel != null && currentChannel.isOpen()) {
                currentChannel.force(true); // 同步数据和元数据
                currentChannel.close();
            }
        } catch (IOException e) {
            log.error("关闭WAL写入器失败", e);
        }
    }
}
