package com.spark.match.controller;

import com.spark.common.config.ConfigService;
import com.spark.common.model.Response;
import com.spark.match.orderbook.OrderBookManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 配置管理控制器
 * 提供配置刷新等管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/config")
public class ConfigController {
    @Autowired
    private ConfigService configService;
    
    @Autowired
    private OrderBookManager orderBookManager;

    /**
     * 刷新配置缓存
     * 从数据库重新加载交易对配置到缓存
     * 
     * @return 刷新结果
     */
    @PostMapping("/refresh")
    public Response<ConfigRefreshResult> refreshConfig() {
        try {
            log.info("收到配置刷新请求");
            
            // 刷新配置缓存
            configService.refreshConfigCache();
            
            // 刷新订单簿管理器的交易对配置
            orderBookManager.refreshTradingPairs();
            
            int configCount = configService.getEnabledTradingPairs().size();
            
            log.info("配置刷新成功: count={}", configCount);
            
            ConfigRefreshResult result = new ConfigRefreshResult();
            result.setConfigCount(configCount);
            result.setMessage("配置刷新成功");
            
            return Response.success("配置刷新成功", result);
        } catch (Exception e) {
            log.error("配置刷新失败", e);
            return Response.error(500, "配置刷新失败: " + e.getMessage());
        }
    }

    /**
     * 配置刷新结果
     */
    @lombok.Data
    public static class ConfigRefreshResult {
        /**
         * 配置数量
         */
        private int configCount;
        
        /**
         * 消息
         */
        private String message;
    }
}
