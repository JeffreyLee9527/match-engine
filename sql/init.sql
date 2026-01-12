-- Spark Match Engine 数据库初始化脚本
-- 数据库: spark_match_engine
-- 版本: V1.0

-- 删除已存在的数据库（如果存在）
DROP DATABASE IF EXISTS `spark_match_engine`;

-- 创建数据库
CREATE DATABASE `spark_match_engine` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `spark_match_engine`;

-- 交易对配置表（V1.0新增）
CREATE TABLE IF NOT EXISTS `symbol_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键自增ID',
    `symbol_id` INT NOT NULL COMMENT '交易对ID（int类型，内存中使用）',
    `symbol` VARCHAR(32) NOT NULL COMMENT '交易对名称（如BTCUSDT）',
    `base_asset` VARCHAR(16) NOT NULL COMMENT '基础资产（如BTC）',
    `quote_asset` VARCHAR(16) NOT NULL COMMENT '计价资产（如USDT）',
    `tick_size` BIGINT NOT NULL COMMENT '最小价格变动单位（以最小单位存储）',
    `min_quantity` BIGINT NOT NULL COMMENT '最小下单数量（以最小单位存储）',
    `max_quantity` BIGINT NOT NULL COMMENT '最大下单数量（以最小单位存储）',
    `price_precision` INT NOT NULL COMMENT '价格精度（小数点后位数）',
    `quantity_precision` INT NOT NULL COMMENT '数量精度（小数点后位数）',
    `maker_fee` INT NOT NULL COMMENT 'Maker手续费率（基点，1基点=0.01%，如10表示0.1%）',
    `taker_fee` INT NOT NULL COMMENT 'Taker手续费率（基点，1基点=0.01%，如10表示0.1%）',
    `topic` VARCHAR(64) DEFAULT NULL COMMENT 'Kafka Topic名称，如果不配置则使用默认topic',
    `partition` INT DEFAULT NULL COMMENT 'Kafka分片/分区号，如果不配置则使用默认分片0',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0=禁用，1=启用',
    `priority` TINYINT NOT NULL DEFAULT 0 COMMENT '优先级：0=低，1=中，2=高',
    `match_mode` TINYINT NOT NULL DEFAULT 1 COMMENT '撮合模式：0=专用实例，1=共享实例',
    `create_time` BIGINT NOT NULL COMMENT '创建时间（毫秒时间戳）',
    `update_time` BIGINT NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_symbol` (`symbol`),
    UNIQUE KEY `uk_symbol_id` (`symbol_id`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易对配置表';

-- 订单表
CREATE TABLE IF NOT EXISTS `order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键自增ID（用于MySQL内部优化）',
    `order_id` BIGINT NOT NULL COMMENT '订单ID（雪花算法生成，业务主键）',
    `user_id` BIGINT NOT NULL COMMENT '用户ID（雪花算法生成，8字节）',
    `symbol_id` INT NOT NULL COMMENT '交易对ID（关联symbol_config.symbol_id）',
    `order_type` TINYINT NOT NULL COMMENT '订单类型: 0=LIMIT, 1=MARKET',
    `order_side` TINYINT NOT NULL COMMENT '订单方向: 0=BUY, 1=SELL',
    `price` BIGINT NOT NULL COMMENT '价格（以最小单位存储，避免浮点数精度问题）',
    `quantity` BIGINT NOT NULL COMMENT '数量（以最小单位存储）',
    `filled_quantity` BIGINT DEFAULT 0 COMMENT '已成交数量',
    `tif_type` TINYINT NOT NULL COMMENT 'TIF类型: 0=GTC, 1=IOC, 2=FOK',
    `status` TINYINT NOT NULL COMMENT '订单状态: 0=PENDING, 1=PARTIAL_FILLED, 2=FILLED, 3=CANCELLING, 4=CANCELLED, 5=REJECTED, 6=EXPIRED',
    `create_time` BIGINT NOT NULL COMMENT '创建时间（毫秒时间戳）',
    `update_time` BIGINT NOT NULL COMMENT '更新时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_id` (`order_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_symbol_id` (`symbol_id`),
    KEY `idx_status` (`status`),
    KEY `idx_user_symbol_status` (`user_id`, `symbol_id`, `status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 成交表
CREATE TABLE IF NOT EXISTS `trade` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键自增ID（用于MySQL内部优化）',
    `trade_id` BIGINT NOT NULL COMMENT '成交ID（雪花算法生成，业务主键）',
    `symbol_id` INT NOT NULL COMMENT '交易对ID（关联symbol_config.symbol_id）',
    `maker_order_id` BIGINT NOT NULL COMMENT 'Maker订单ID',
    `taker_order_id` BIGINT NOT NULL COMMENT 'Taker订单ID',
    `maker_user_id` BIGINT NOT NULL COMMENT 'Maker用户ID（雪花算法生成）',
    `taker_user_id` BIGINT NOT NULL COMMENT 'Taker用户ID（雪花算法生成）',
    `price` BIGINT NOT NULL COMMENT '成交价格',
    `quantity` BIGINT NOT NULL COMMENT '成交数量',
    `trade_time` BIGINT NOT NULL COMMENT '成交时间（毫秒时间戳）',
    `create_time` BIGINT NOT NULL COMMENT '记录创建时间（毫秒时间戳）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trade_id` (`trade_id`),
    KEY `idx_symbol_id` (`symbol_id`),
    KEY `idx_maker_order_id` (`maker_order_id`),
    KEY `idx_taker_order_id` (`taker_order_id`),
    KEY `idx_trade_time` (`trade_time`),
    KEY `idx_symbol_trade_time` (`symbol_id`, `trade_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成交表';

-- 初始化交易对配置数据
INSERT INTO `symbol_config` (
    `symbol_id`, `symbol`, `base_asset`, `quote_asset`, 
    `tick_size`, `min_quantity`, `max_quantity`, 
    `price_precision`, `quantity_precision`, 
    `maker_fee`, `taker_fee`, 
    `topic`, `partition`,
    `enabled`, `create_time`, `update_time`
) VALUES
(
    1, 'BTCUSDT', 'BTC', 'USDT',
    1000000, 1000000, 100000000000,
    2, 3,
    10, 10,  -- 10 基点 = 0.1%
    NULL, NULL,  -- 使用默认 topic 和 partition
    1, UNIX_TIMESTAMP(NOW()) * 1000, UNIX_TIMESTAMP(NOW()) * 1000
),
(
    2, 'ETHUSDT', 'ETH', 'USDT',
    100000, 100000, 100000000000,
    2, 3,
    10, 10,  -- 10 基点 = 0.1%
    NULL, NULL,  -- 使用默认 topic 和 partition
    1, UNIX_TIMESTAMP(NOW()) * 1000, UNIX_TIMESTAMP(NOW()) * 1000
)
ON DUPLICATE KEY UPDATE `update_time` = UNIX_TIMESTAMP(NOW()) * 1000;
