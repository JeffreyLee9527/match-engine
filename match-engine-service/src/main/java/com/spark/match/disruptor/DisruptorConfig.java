package com.spark.match.disruptor;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Disruptor配置
 * 
 * 优化说明：
 * 1. RingBuffer大小：适合单生产单消费场景
 * 2. WaitStrategy：使用BlockingWaitStrategy，单消费场景下CPU占用低
 * 3. 支持配置化：可通过配置文件调整参数
 * 4. 自定义线程工厂：使用非守护线程，便于监控
 */
@Slf4j
@Configuration
public class DisruptorConfig {
    /**
     * RingBuffer大小（必须是2的幂次方）
     * 默认值：4096（适合单生产单消费场景）
     * 
     * 配置建议：
     * - 单生产单消费：4096-8192
     * - 高吞吐场景：8192-16384
     * - 低延迟场景：1024-4096
     * 
     * 内存估算：每个OrderEvent约300-400 bytes
     * - 4096: ~1.5 MB
     * - 8192: ~3 MB
     * - 1M: ~400 MB
     */
    @Value("${disruptor.buffer-size:4096}")
    private int bufferSize;

    /**
     * WaitStrategy类型
     * 可选值：blocking, yielding, busy-spin
     * 默认值：blocking（适合单消费场景，CPU占用低）
     * 
     * 策略说明：
     * - blocking: 阻塞等待，CPU占用低，适合单消费场景
     * - yielding: 让出CPU，延迟略高于blocking，CPU占用中等
     * - busy-spin: 纯自旋，延迟最低但CPU占用100%，适合多消费者高吞吐场景
     */
    @Value("${disruptor.wait-strategy:blocking}")
    private String waitStrategyType;

    @Bean
    public Disruptor<OrderEvent> disruptor(OrderEventHandler handler) {
        // 确保bufferSize是2的幂次方
        int actualBufferSize = roundUpToPowerOfTwo(bufferSize);
        if (actualBufferSize != bufferSize) {
            log.info("Disruptor bufferSize已调整为2的幂次方: {} -> {}", bufferSize, actualBufferSize);
        }

        // 选择WaitStrategy
        WaitStrategy waitStrategy = createWaitStrategy(waitStrategyType);
        
        // 自定义线程工厂（非守护线程，便于监控）
        ThreadFactory threadFactory = new MatchEngineThreadFactory();

        log.info("初始化Disruptor: bufferSize={}, waitStrategy={}, producerType=SINGLE", 
                actualBufferSize, waitStrategyType);

        Disruptor<OrderEvent> disruptor = new Disruptor<>(
                new OrderEventFactory(),
                actualBufferSize,
                threadFactory,
                ProducerType.SINGLE,  // 单生产者（Kafka Consumer单线程）
                waitStrategy
        );

        // 单线程Handler（保证严格顺序）
        disruptor.handleEventsWith(handler);

        disruptor.start();

        log.info("Disruptor启动成功: bufferSize={}, ringBufferSize={}", 
                actualBufferSize, disruptor.getRingBuffer().getBufferSize());

        return disruptor;
    }

    @Bean
    public RingBuffer<OrderEvent> ringBuffer(Disruptor<OrderEvent> disruptor) {
        return disruptor.getRingBuffer();
    }

    /**
     * 创建WaitStrategy
     */
    private WaitStrategy createWaitStrategy(String type) {
        switch (type.toLowerCase()) {
            case "yielding":
                log.info("使用YieldingWaitStrategy（让出CPU策略）");
                return new YieldingWaitStrategy();
            case "busy-spin":
            case "busyspin":
                log.info("使用BusySpinWaitStrategy（纯自旋策略，CPU占用100%，仅适合多消费者高吞吐场景）");
                return new com.lmax.disruptor.BusySpinWaitStrategy();
            case "blocking":
            default:
                log.info("使用BlockingWaitStrategy（阻塞等待策略，适合单消费场景）");
                return new BlockingWaitStrategy();
        }
    }

    /**
     * 向上取整为2的幂次方
     */
    private int roundUpToPowerOfTwo(int size) {
        if (size <= 0) {
            return 1024; // 默认最小值
        }
        if (size > 1024 * 1024) {
            log.info("Disruptor bufferSize过大: {}，已限制为1M", size);
            return 1024 * 1024;
        }
        int power = Integer.highestOneBit(size);
        return (power == size) ? size : power << 1;
    }

    /**
     * 自定义线程工厂
     * 使用非守护线程，便于监控和管理
     */
    private static class MatchEngineThreadFactory implements ThreadFactory {
        private static final AtomicInteger threadNumber = new AtomicInteger(1);
        private static final String THREAD_NAME_PREFIX = "match-engine-disruptor-";

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, THREAD_NAME_PREFIX + threadNumber.getAndIncrement());
            thread.setDaemon(false); // 非守护线程
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        }
    }
}
