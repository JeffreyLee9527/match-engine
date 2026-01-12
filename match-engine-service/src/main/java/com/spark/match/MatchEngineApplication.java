package com.spark.match;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 撮合引擎启动类
 */
@SpringBootApplication
@MapperScan("com.spark.common.mapper")
@ComponentScan(basePackages = {"com.spark.match", "com.spark.common"})
public class MatchEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(MatchEngineApplication.class, args);
    }
}
