package com.spark.common.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 交易对ID映射器
 * 提供String（交易对符号）和Integer（交易对ID）之间的映射
 */
public class SymbolIdMapper {
    private static final Map<String, Integer> SYMBOL_TO_ID = new ConcurrentHashMap<>();
    private static final Map<Integer, String> ID_TO_SYMBOL = new ConcurrentHashMap<>();

    private SymbolIdMapper() {
        // 工具类，禁止实例化
    }

    /**
     * 将交易对符号转换为ID
     *
     * @param symbol 交易对符号（如 "BTC/USDT"）
     * @return 交易对ID
     */
    public static Integer symbolToId(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be null or empty");
        }
        return SYMBOL_TO_ID.get(symbol.toUpperCase());
    }

    /**
     * 将交易对ID转换为符号
     *
     * @param symbolId 交易对ID
     * @return 交易对符号（如 "BTC/USDT"）
     */
    public static String idToSymbol(Integer symbolId) {
        if (symbolId == null) {
            throw new IllegalArgumentException("SymbolId cannot be null");
        }
        return ID_TO_SYMBOL.get(symbolId);
    }

    /**
     * 注册交易对映射
     *
     * @param symbol   交易对符号
     * @param symbolId 交易对ID
     */
    public static void register(String symbol, Integer symbolId) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be null or empty");
        }
        if (symbolId == null) {
            throw new IllegalArgumentException("SymbolId cannot be null");
        }
        String upperSymbol = symbol.toUpperCase();
        SYMBOL_TO_ID.put(upperSymbol, symbolId);
        ID_TO_SYMBOL.put(symbolId, upperSymbol);
    }

    /**
     * 移除交易对映射
     *
     * @param symbol 交易对符号
     */
    public static void unregister(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return;
        }
        String upperSymbol = symbol.toUpperCase();
        Integer symbolId = SYMBOL_TO_ID.remove(upperSymbol);
        if (symbolId != null) {
            ID_TO_SYMBOL.remove(symbolId);
        }
    }

    /**
     * 清除所有映射
     */
    public static void clear() {
        SYMBOL_TO_ID.clear();
        ID_TO_SYMBOL.clear();
    }

    /**
     * 检查交易对是否存在
     *
     * @param symbol 交易对符号
     * @return 是否存在
     */
    public static boolean exists(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return false;
        }
        return SYMBOL_TO_ID.containsKey(symbol.toUpperCase());
    }

    /**
     * 检查交易对ID是否存在
     *
     * @param symbolId 交易对ID
     * @return 是否存在
     */
    public static boolean exists(Integer symbolId) {
        if (symbolId == null) {
            return false;
        }
        return ID_TO_SYMBOL.containsKey(symbolId);
    }
}
