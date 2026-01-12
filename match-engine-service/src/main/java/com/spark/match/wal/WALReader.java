package com.spark.match.wal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * WAL读取器
 */
@Slf4j
@Component
public class WALReader {
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 读取WAL文件（从指定序列号开始）
     *
     * @param basePath   WAL基础路径
     * @param instanceId 实例ID
     * @param fromWalSeq 起始序列号
     * @return WAL记录列表
     */
    public List<WALRecord> readWAL(String basePath, String instanceId, long fromWalSeq) {
        return readWALAfter(basePath, instanceId, fromWalSeq);
    }

    /**
     * 读取WAL文件（从指定序列号开始，包含文件完整性校验）
     * 注意：会对每个WAL文件进行checksum校验，如果文件损坏则跳过
     *
     * @param basePath   WAL基础路径
     * @param instanceId 实例ID
     * @param fromWalSeq 起始序列号
     * @return WAL记录列表
     */
    public List<WALRecord> readWALAfter(String basePath, String instanceId, long fromWalSeq) {
        List<WALRecord> records = new ArrayList<>();
        Path walPath = Paths.get(basePath, instanceId);

        if (!Files.exists(walPath)) {
            log.info("WAL路径不存在: {}", walPath);
            return records;
        }

        try {
            // 获取所有WAL文件，按文件名排序
            List<Path> walFiles = getWalFiles(walPath);
            int corruptedFileCount = 0;

            for (Path walFile : walFiles) {
                // 验证文件完整性
                if (!validateWalFileIntegrity(walFile)) {
                    log.error("WAL文件损坏，跳过: fileName={}", walFile.getFileName());
                    corruptedFileCount++;
                    // 继续处理下一个文件
                    continue;
                }

                List<WALRecord> fileRecords = readWalFile(walFile, fromWalSeq);
                records.addAll(fileRecords);
            }

            if (corruptedFileCount > 0) {
                log.error("检测到{}个损坏的WAL文件，已跳过", corruptedFileCount);
                // 触发告警（这里仅记录日志，实际生产环境可以集成告警系统）
            }

            if (corruptedFileCount == walFiles.size() && !walFiles.isEmpty()) {
                log.error("所有WAL文件都损坏，建议仅从Snapshot恢复");
            }

        } catch (IOException e) {
            log.error("读取WAL失败: basePath={}, instanceId={}", basePath, instanceId, e);
        }

        return records;
    }

    /**
     * 获取所有WAL文件
     */
    private List<Path> getWalFiles(Path walPath) throws IOException {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> paths = Files.list(walPath)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().startsWith("wal-"))
                    .filter(p -> p.getFileName().toString().endsWith(".log"))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .forEach(files::add);
        }
        return files;
    }

    /**
     * 读取单个WAL文件
     */
    private List<WALRecord> readWalFile(Path walFile, long fromWalSeq) {
        List<WALRecord> records = new ArrayList<>();

        try (FileChannel channel = FileChannel.open(walFile, StandardOpenOption.READ)) {
            long fileSize = channel.size();
            long position = 0;

            while (position < fileSize) {
                // 读取长度前缀
                ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
                int bytesRead = channel.read(lengthBuffer);
                if (bytesRead != 4) {
                    break;
                }
                lengthBuffer.flip();
                int recordLength = lengthBuffer.getInt();

                // 读取记录数据
                ByteBuffer recordBuffer = ByteBuffer.allocate(recordLength);
                bytesRead = channel.read(recordBuffer);
                if (bytesRead != recordLength) {
                    log.info("WAL文件损坏: fileName={}, position={}", walFile.getFileName(), position);
                    break;
                }

                // 解析记录
                String recordJson = new String(recordBuffer.array(), "UTF-8");
                WALRecord record;
                try {
                    record = objectMapper.readValue(recordJson, WALRecord.class);
                } catch (JsonProcessingException e) {
                    log.info("WAL记录解析失败: fileName={}, position={}", walFile.getFileName(), position, e);
                    continue;
                }

                // 校验和验证
                if (!validateChecksum(record)) {
                    log.info("WAL记录校验和失败: walSeq={}, fileName={}", record.getWalSeq(), walFile.getFileName());
                    continue;
                }

                // 只读取大于等于fromWalSeq的记录
                if (record.getWalSeq() >= fromWalSeq) {
                    records.add(record);
                }

                position += 4 + recordLength;
            }
        } catch (Exception e) {
            log.error("读取WAL文件失败: fileName={}", walFile.getFileName(), e);
        }

        return records;
    }

    /**
     * 验证校验和
     * 注意：checksum是基于整个WALRecord序列化后的数据计算的，而不是只基于OrderMessage
     */
    private boolean validateChecksum(WALRecord record) {
        try {
            // 创建临时记录用于计算checksum（checksum字段设为0）
            WALRecord tempRecord = WALRecord.builder()
                    .walSeq(record.getWalSeq())
                    .orderMessage(record.getOrderMessage())
                    .timestamp(record.getTimestamp())
                    .checksum(0L) // 临时设为0
                    .build();
            
            // 序列化整个记录
            String recordJson = objectMapper.writeValueAsString(tempRecord);
            byte[] recordData = recordJson.getBytes("UTF-8");
            long calculatedChecksum = calculateChecksum(recordData);
            return calculatedChecksum == record.getChecksum();
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

    /**
     * 验证WAL文件完整性
     * 通过尝试读取文件中的所有记录并验证checksum来判断文件是否损坏
     *
     * @param walFile WAL文件路径
     * @return true表示文件完整，false表示文件损坏
     */
    private boolean validateWalFileIntegrity(Path walFile) {
        try (FileChannel channel = FileChannel.open(walFile, StandardOpenOption.READ)) {
            long fileSize = channel.size();
            long position = 0;
            int validRecordCount = 0;
            int invalidRecordCount = 0;

            while (position < fileSize) {
                // 读取长度前缀
                ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
                int bytesRead = channel.read(lengthBuffer);
                if (bytesRead != 4) {
                    // 文件末尾，正常结束
                    break;
                }
                lengthBuffer.flip();
                int recordLength = lengthBuffer.getInt();

                if (recordLength <= 0 || recordLength > 10 * 1024 * 1024) {
                    // 记录长度异常，文件可能损坏
                    log.info("WAL文件记录长度异常: fileName={}, position={}, recordLength={}", 
                            walFile.getFileName(), position, recordLength);
                    return false;
                }

                // 读取记录数据
                ByteBuffer recordBuffer = ByteBuffer.allocate(recordLength);
                bytesRead = channel.read(recordBuffer);
                if (bytesRead != recordLength) {
                    // 读取不完整，文件可能损坏
                    log.info("WAL文件读取不完整: fileName={}, position={}, expected={}, actual={}", 
                            walFile.getFileName(), position, recordLength, bytesRead);
                    return false;
                }

                // 尝试解析记录
                String recordJson = new String(recordBuffer.array(), "UTF-8");
                try {
                    WALRecord record = objectMapper.readValue(recordJson, WALRecord.class);
                    // 验证记录级checksum
                    if (validateChecksum(record)) {
                        validRecordCount++;
                    } else {
                        invalidRecordCount++;
                        // 如果无效记录过多，认为文件损坏
                        if (invalidRecordCount > 10) {
                            log.info("WAL文件包含过多无效记录: fileName={}, invalidCount={}", 
                                    walFile.getFileName(), invalidRecordCount);
                            return false;
                        }
                    }
                } catch (JsonProcessingException e) {
                    // 解析失败，记录可能损坏
                    invalidRecordCount++;
                    if (invalidRecordCount > 10) {
                        log.info("WAL文件包含过多无法解析的记录: fileName={}, invalidCount={}", 
                                walFile.getFileName(), invalidRecordCount);
                        return false;
                    }
                }

                position += 4 + recordLength;
            }

            // 如果文件中有有效记录，认为文件基本完整
            return validRecordCount > 0 || (position == 0 && fileSize == 0); // 空文件也算有效
        } catch (Exception e) {
            log.error("验证WAL文件完整性失败: fileName={}", walFile.getFileName(), e);
            return false;
        }
    }
}
