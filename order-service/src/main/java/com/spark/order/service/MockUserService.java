package com.spark.order.service;

/**
 * Mock用户服务（V1.0）
 */
public interface MockUserService {
    /**
     * 获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息（Mock）
     */
    UserInfo getUserInfo(Long userId);

    /**
     * 检查用户状态
     *
     * @param userId 用户ID
     * @return 是否正常
     */
    boolean checkUserStatus(Long userId);

    /**
     * 用户信息（Mock）
     */
    class UserInfo {
        private Long userId;
        private String status;

        public UserInfo(Long userId, String status) {
            this.userId = userId;
            this.status = status;
        }

        public Long getUserId() {
            return userId;
        }

        public String getStatus() {
            return status;
        }
    }
}
