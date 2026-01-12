package com.spark.order.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.spark.common.enums.OrderSide;
import com.spark.common.enums.OrderStatus;
import com.spark.common.enums.OrderType;
import com.spark.common.enums.TIFType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis Plus 配置
 */
@Configuration
public class MyBatisPlusConfig implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    @Lazy
    private SqlSessionFactory sqlSessionFactory;

    /**
     * MyBatis-Plus拦截器配置
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInnerInterceptor.setMaxLimit(1000L);
        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        return interceptor;
    }

    /**
     * 注册枚举类型处理器
     * 在应用上下文完全刷新后注册，避免循环依赖
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 避免重复注册（父上下文也会触发此事件）
        if (event.getApplicationContext().getParent() == null) {
            TypeHandlerRegistry registry = sqlSessionFactory.getConfiguration().getTypeHandlerRegistry();
            
            // 注册订单类型TypeHandler
            registry.register(OrderType.class, OrderTypeHandler.class);
            
            // 注册订单方向TypeHandler
            registry.register(OrderSide.class, OrderSideHandler.class);
            
            // 注册TIF类型TypeHandler
            registry.register(TIFType.class, TIFTypeHandler.class);
            
            // 注册订单状态TypeHandler
            registry.register(OrderStatus.class, OrderStatusHandler.class);
        }
    }

    /**
     * 订单类型TypeHandler
     */
    @MappedTypes(OrderType.class)
    @MappedJdbcTypes(JdbcType.TINYINT)
    public static class OrderTypeHandler extends BaseTypeHandler<OrderType> {
        @Override
        public void setNonNullParameter(PreparedStatement ps, int i, OrderType parameter, JdbcType jdbcType) throws SQLException {
            ps.setInt(i, parameter.getCode());
        }

        @Override
        public OrderType getNullableResult(ResultSet rs, String columnName) throws SQLException {
            int code = rs.getInt(columnName);
            return rs.wasNull() ? null : OrderType.fromCode(code);
        }

        @Override
        public OrderType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            int code = rs.getInt(columnIndex);
            return rs.wasNull() ? null : OrderType.fromCode(code);
        }

        @Override
        public OrderType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
            int code = cs.getInt(columnIndex);
            return cs.wasNull() ? null : OrderType.fromCode(code);
        }
    }

    /**
     * 订单方向TypeHandler
     */
    @MappedTypes(OrderSide.class)
    @MappedJdbcTypes(JdbcType.TINYINT)
    public static class OrderSideHandler extends BaseTypeHandler<OrderSide> {
        @Override
        public void setNonNullParameter(PreparedStatement ps, int i, OrderSide parameter, JdbcType jdbcType) throws SQLException {
            ps.setInt(i, parameter.getCode());
        }

        @Override
        public OrderSide getNullableResult(ResultSet rs, String columnName) throws SQLException {
            int code = rs.getInt(columnName);
            return rs.wasNull() ? null : OrderSide.fromCode(code);
        }

        @Override
        public OrderSide getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            int code = rs.getInt(columnIndex);
            return rs.wasNull() ? null : OrderSide.fromCode(code);
        }

        @Override
        public OrderSide getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
            int code = cs.getInt(columnIndex);
            return cs.wasNull() ? null : OrderSide.fromCode(code);
        }
    }

    /**
     * TIF类型TypeHandler
     */
    @MappedTypes(TIFType.class)
    @MappedJdbcTypes(JdbcType.TINYINT)
    public static class TIFTypeHandler extends BaseTypeHandler<TIFType> {
        @Override
        public void setNonNullParameter(PreparedStatement ps, int i, TIFType parameter, JdbcType jdbcType) throws SQLException {
            ps.setInt(i, parameter.getCode());
        }

        @Override
        public TIFType getNullableResult(ResultSet rs, String columnName) throws SQLException {
            int code = rs.getInt(columnName);
            return rs.wasNull() ? null : TIFType.fromCode(code);
        }

        @Override
        public TIFType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            int code = rs.getInt(columnIndex);
            return rs.wasNull() ? null : TIFType.fromCode(code);
        }

        @Override
        public TIFType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
            int code = cs.getInt(columnIndex);
            return cs.wasNull() ? null : TIFType.fromCode(code);
        }
    }

    /**
     * 订单状态TypeHandler
     */
    @MappedTypes(OrderStatus.class)
    @MappedJdbcTypes(JdbcType.TINYINT)
    public static class OrderStatusHandler extends BaseTypeHandler<OrderStatus> {
        @Override
        public void setNonNullParameter(PreparedStatement ps, int i, OrderStatus parameter, JdbcType jdbcType) throws SQLException {
            ps.setInt(i, parameter.getCode());
        }

        @Override
        public OrderStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
            int code = rs.getInt(columnName);
            return rs.wasNull() ? null : OrderStatus.fromCode(code);
        }

        @Override
        public OrderStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            int code = rs.getInt(columnIndex);
            return rs.wasNull() ? null : OrderStatus.fromCode(code);
        }

        @Override
        public OrderStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
            int code = cs.getInt(columnIndex);
            return cs.wasNull() ? null : OrderStatus.fromCode(code);
        }
    }
}
