package com.spark.common.config;

/**
 * 路由配置（topic和partition）
 */
public class RoutingConfig {
    private final String topic;
    private final int partition;

    public RoutingConfig(String topic, int partition) {
        this.topic = topic;
        this.partition = partition;
    }

    public String getTopic() {
        return topic;
    }

    public int getPartition() {
        return partition;
    }
}
