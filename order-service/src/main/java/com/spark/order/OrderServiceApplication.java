package com.spark.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 订单服务启动类
 */
@SpringBootApplication
@MapperScan({"com.spark.order.mapper", "com.spark.common.mapper"})
@ComponentScan(basePackages = {"com.spark.order", "com.spark.common"})
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
