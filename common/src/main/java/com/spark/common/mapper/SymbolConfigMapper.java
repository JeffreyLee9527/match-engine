package com.spark.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spark.common.model.SymbolConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 交易对配置Mapper
 */
@Mapper
public interface SymbolConfigMapper extends BaseMapper<SymbolConfig> {
    /**
     * 根据交易对名称查询配置
     */
    @Select("SELECT * FROM symbol_config WHERE symbol = #{symbol}")
    SymbolConfig selectBySymbol(@Param("symbol") String symbol);

    /**
     * 根据交易对ID查询配置
     */
    @Select("SELECT * FROM symbol_config WHERE symbol_id = #{symbolId}")
    SymbolConfig selectBySymbolId(@Param("symbolId") Integer symbolId);

    /**
     * 查询所有启用的交易对配置
     */
    @Select("SELECT * FROM symbol_config WHERE enabled = 1 ORDER BY priority DESC, symbol_id ASC")
    List<SymbolConfig> selectEnabledConfigs();
}
