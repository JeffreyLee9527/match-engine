package com.spark.order.service.impl;

import com.spark.order.service.MockUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Mock用户服务实现（V1.0）
 */
@Slf4j
@Service
public class MockUserServiceImpl implements MockUserService {
    @Override
    public UserInfo getUserInfo(Long userId) {
        // V1.0 Mock实现：默认返回正常状态
        return new UserInfo(userId, "ACTIVE");
    }

    @Override
    public boolean checkUserStatus(Long userId) {
        // V1.0 Mock实现：默认返回true
        return true;
    }
}
