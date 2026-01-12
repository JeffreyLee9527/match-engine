package com.spark.match.recovery;

import com.spark.common.config.ConfigService;
import com.spark.common.model.SymbolConfig;
import com.spark.common.enums.MessageType;
import com.spark.common.model.OrderMessage;
import com.spark.common.util.SymbolIdMapper;
import com.spark.match.matcher.Matcher;
import com.spark.match.matcher.MatcherFactory;
import com.spark.match.orderbook.Order;
import com.spark.match.orderbook.OrderBook;
import com.spark.match.orderbook.OrderBookManager;
import com.spark.match.snapshot.SnapshotReader;
import com.spark.match.snapshot.SnapshotWriter;
import com.spark.match.wal.WALReader;
import com.spark.match.wal.WALRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订单簿恢复服务
 * 启动时恢复订单簿状态：
 * 1. 加载最新Snapshot
 * 2. 重放WAL日志
 * 3. 校验WAL文件完整性
 */
@Slf4j
@Service
public class OrderBookRecoveryService implements CommandLineRunner {
    @Autowired
    private OrderBookManager orderBookManager;
    @Autowired
    private SnapshotReader snapshotReader;
    @Autowired
    private WALReader walReader;
    @Autowired
    private MatcherFactory matcherFactory;
    @Autowired
    private ConfigService configService;

    @Value("${wal.base-path:./data/wal}")
    private String walBasePath;

    @Value("${wal.instance-id:default}")
    private String instanceId;

    @Override
    public void run(String... args) {
        log.info("开始恢复订单簿状态...");
        recoverOrderBooks();
        log.info("订单簿状态恢复完成");
        
        // 注意：Kafka Consumer会在恢复完成后自动启动
        // 根据架构设计，Kafka Consumer应该从WAL之后的新事件开始消费
        // 由于Kafka offset和WAL seq的映射关系，这里假设Kafka Consumer会从最新offset开始消费
        // 实际生产环境中，应该根据WAL的lastAppliedWalSeq找到对应的Kafka offset并seek
        log.info("Kafka Consumer将在恢复完成后开始消费新消息");
    }

    /**
     * 恢复所有订单簿
     * 从数据库获取所有启用的交易对配置，并逐一恢复
     */
    private void recoverOrderBooks() {
        try {
            // 从数据库获取所有启用的交易对
            List<String> enabledPairs = configService.getEnabledTradingPairs();
            log.info("开始恢复订单簿: 交易对数量={}", enabledPairs.size());

            for (String symbol : enabledPairs) {
                SymbolConfig config = configService.getTradingPairConfig(symbol);
                if (config != null && config.getSymbolId() != null) {
                    // 注册交易对映射
                    SymbolIdMapper.register(symbol, config.getSymbolId());

                    // 恢复订单簿
                    recoverOrderBook(config.getSymbolId());
                } else {
                    log.info("交易对配置无效，跳过恢复: symbol={}", symbol);
                }
            }

            log.info("订单簿恢复完成: 已恢复交易对数量={}", enabledPairs.size());
        } catch (Exception e) {
            log.error("恢复订单簿失败", e);
            throw new RuntimeException("恢复订单簿失败", e);
        }
    }

    /**
     * 恢复单个订单簿
     *
     * @param symbolId 交易对ID
     */
    public void recoverOrderBook(Integer symbolId) {
        try {
            // 1. 检查是否存在Snapshot
            SnapshotWriter.SnapshotData snapshotData = snapshotReader.readLatestSnapshot(symbolId);

            OrderBook orderBook;
            long fromWalSeq = 0;

            if (snapshotData != null) {
                // 从Snapshot恢复：使用Snapshot中的OrderBook
                orderBook = snapshotData.getOrderBook();
                fromWalSeq = snapshotData.getLastAppliedWalSeq() + 1; // 从下一个序列号开始
                // 注册Snapshot中的OrderBook到管理器
                orderBookManager.registerOrderBook(symbolId, orderBook);
                log.info("从Snapshot恢复订单簿: symbolId={}, lastAppliedWalSeq={}", symbolId, snapshotData.getLastAppliedWalSeq());
            } else {
                // 没有Snapshot，创建新订单簿
                orderBook = orderBookManager.createOrderBook(symbolId);
                log.info("未找到Snapshot，创建新订单簿: symbolId={}", symbolId);
            }

            // 2. 重放WAL日志
            List<WALRecord> walRecords = walReader.readWAL(walBasePath, instanceId, fromWalSeq);
            log.info("读取WAL记录: symbolId={}, count={}, fromWalSeq={}", symbolId, walRecords.size(), fromWalSeq);

            // 3. 重放WAL记录
            for (WALRecord record : walRecords) {
                // 只处理该交易对的WAL记录
                if (record.getOrderMessage().getSymbolId().equals(symbolId)) {
                    replayWALRecord(orderBook, record);
                }
            }

            log.info("订单簿恢复完成: symbolId={}, walRecordCount={}, lastAppliedWalSeq={}", 
                    symbolId, walRecords.size(), orderBook.getLastAppliedWalSeq());
        } catch (Exception e) {
            log.error("恢复订单簿失败: symbolId={}", symbolId, e);
        }
    }

    /**
     * 重放WAL记录
     * 根据OrderMessage的类型执行相应的操作（创建订单或取消订单）
     *
     * @param orderBook 订单簿
     * @param record    WAL记录
     */
    private void replayWALRecord(OrderBook orderBook, WALRecord record) {
        try {
            OrderMessage orderMessage = record.getOrderMessage();
            long walSeq = record.getWalSeq();

            if (orderMessage.getMessageType() == MessageType.ORDER_CANCEL) {
                // 取消订单
                Order order = orderBook.getOrder(orderMessage.getOrderId());
                if (order != null) {
                    orderBook.removeOrder(orderMessage.getOrderId());
                    log.info("WAL重放-订单取消: walSeq={}, orderId={}", walSeq, orderMessage.getOrderId());
                } else {
                    log.info("WAL重放-订单不存在: walSeq={}, orderId={}", walSeq, orderMessage.getOrderId());
                }
            } else if (orderMessage.getMessageType() == MessageType.ORDER_CREATE) {
                // 创建订单并撮合
                Order order = convertToOrder(orderMessage);
                Matcher matcher = matcherFactory.getMatcher(order.getOrderType());
                // 重放撮合（不发送通知，因为这是恢复过程）
                matcher.match(orderBook, order);
                log.info("WAL重放-订单撮合: walSeq={}, orderId={}", walSeq, orderMessage.getOrderId());
            }

            // 更新订单簿的最后应用的WAL序列号
            orderBook.setLastAppliedWalSeq(walSeq);
        } catch (Exception e) {
            log.error("WAL重放失败: walSeq={}, orderId={}", record.getWalSeq(), 
                    record.getOrderMessage().getOrderId(), e);
            // 继续处理下一条记录，不中断恢复流程
        }
    }

    /**
     * 将OrderMessage转换为Order对象
     */
    private Order convertToOrder(OrderMessage orderMessage) {
        return Order.builder()
                .orderId(orderMessage.getOrderId())
                .userId(orderMessage.getUserId())
                .symbolId(orderMessage.getSymbolId())
                .orderType(orderMessage.getOrderType())
                .orderSide(orderMessage.getOrderSide())
                .price(orderMessage.getPrice())
                .quantity(orderMessage.getQuantity())
                .filledQuantity(0L) // 恢复时从0开始
                .tifType(orderMessage.getTifType())
                .createTime(orderMessage.getTimestamp())
                .build();
    }
}
