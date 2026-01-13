#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
订单服务并发测试脚本
支持并发测试下单和撤单接口

使用方法:
    python concurrent_test.py --url http://localhost:8081 --concurrency 10 --total 100 --test-type both

参数说明:
    --url: 服务地址 (默认: http://localhost:8081)
    --concurrency: 并发数 (默认: 10)
    --total: 总调用次数 (默认: 100)
    --test-type: 测试类型 (place/cancel/both, 默认: both)
    --symbol: 交易对 (默认: BTCUSDT)
    --user-id: 用户ID (默认: 1000000000000000001)
"""

import asyncio
import aiohttp
import argparse
import time
import json
import random
from collections import defaultdict
from typing import List, Dict, Tuple
from dataclasses import dataclass
from datetime import datetime


@dataclass
class TestResult:
    """测试结果"""
    success: bool
    status_code: int
    response_time: float
    error_message: str = ""
    order_id: str = None


class OrderLoadTester:
    """订单服务负载测试器"""
    
    # 交易对配置（tickSize用于价格对齐）
    # BTCUSDT: tickSize = 1000000 (0.01 USDT)
    # ETHUSDT: tickSize = 100000 (0.001 USDT)
    SYMBOL_TICKSIZE = {
        "BTCUSDT": 1000000,
        "ETHUSDT": 100000
    }
    
    def __init__(self, base_url: str, user_id: int, symbol: str = "BTCUSDT"):
        self.base_url = base_url.rstrip('/')
        self.user_id = user_id
        self.symbol = symbol
        self.created_orders: List[str] = []
        self.lock = asyncio.Lock()
        # 获取该交易对的tickSize，默认使用BTCUSDT的配置
        self.tick_size = self.SYMBOL_TICKSIZE.get(symbol, 1000000)
    
    def align_price(self, price: int) -> int:
        """将价格对齐到tickSize的整数倍"""
        return (price // self.tick_size) * self.tick_size
        
    async def place_order(self, session: aiohttp.ClientSession, 
                         order_side: str = None, price: int = None) -> TestResult:
        """下单接口测试"""
        url = f"{self.base_url}/api/orders"
        headers = {
            "Content-Type": "application/json",
            "userId": str(self.user_id)
        }
        
        # 随机生成订单方向
        side = order_side or random.choice(["BUY", "SELL"])
        # 随机生成价格（BTCUSDT，假设价格在50000-60000之间，以最小单位存储）
        # 1 USDT = 100000000，所以50000 USDT = 5000000000000
        # 需要确保价格是tickSize的整数倍
        if price is None:
            # 生成随机价格，然后对齐到tickSize
            raw_price = random.randint(5000000000000, 6000000000000)
            price = self.align_price(raw_price)
        
        payload = {
            "symbol": self.symbol,
            "orderType": "LIMIT",
            "orderSide": side,
            "price": price,
            "quantity": 100000000,  # 0.001 BTC (1 BTC = 100000000)
            "tifType": "GTC"
        }
        
        start_time = time.time()
        try:
            async with session.post(url, json=payload, headers=headers, timeout=aiohttp.ClientTimeout(total=10)) as response:
                response_time = time.time() - start_time
                response_text = await response.text()
                
                if response.status == 200:
                    try:
                        result = await response.json()
                        order_id = result.get("data", {}).get("orderId")
                        if order_id:
                            async with self.lock:
                                self.created_orders.append(str(order_id))
                        return TestResult(
                            success=True,
                            status_code=response.status,
                            response_time=response_time,
                            order_id=str(order_id) if order_id else None
                        )
                    except:
                        return TestResult(
                            success=False,
                            status_code=response.status,
                            response_time=response_time,
                            error_message=f"解析响应失败: {response_text[:200]}"
                        )
                else:
                    return TestResult(
                        success=False,
                        status_code=response.status,
                        response_time=response_time,
                        error_message=f"HTTP {response.status}: {response_text[:200]}"
                    )
        except asyncio.TimeoutError:
            return TestResult(
                success=False,
                status_code=0,
                response_time=time.time() - start_time,
                error_message="请求超时"
            )
        except Exception as e:
            return TestResult(
                success=False,
                status_code=0,
                response_time=time.time() - start_time,
                error_message=f"异常: {str(e)}"
            )
    
    async def cancel_order(self, session: aiohttp.ClientSession, order_id: str = None) -> TestResult:
        """撤单接口测试"""
        # 如果没有提供order_id，尝试从已创建的订单中获取
        if not order_id:
            async with self.lock:
                if self.created_orders:
                    order_id = self.created_orders.pop(0)
                else:
                    return TestResult(
                        success=False,
                        status_code=0,
                        response_time=0,
                        error_message="没有可取消的订单"
                    )
        
        url = f"{self.base_url}/api/orders/{order_id}"
        headers = {
            "userId": str(self.user_id)
        }
        
        start_time = time.time()
        try:
            async with session.delete(url, headers=headers, timeout=aiohttp.ClientTimeout(total=10)) as response:
                response_time = time.time() - start_time
                response_text = await response.text()
                
                if response.status == 200:
                    return TestResult(
                        success=True,
                        status_code=response.status,
                        response_time=response_time
                    )
                else:
                    return TestResult(
                        success=False,
                        status_code=response.status,
                        response_time=response_time,
                        error_message=f"HTTP {response.status}: {response_text[:200]}"
                    )
        except asyncio.TimeoutError:
            return TestResult(
                success=False,
                status_code=0,
                response_time=time.time() - start_time,
                error_message="请求超时"
            )
        except Exception as e:
            return TestResult(
                success=False,
                status_code=0,
                response_time=time.time() - start_time,
                error_message=f"异常: {str(e)}"
            )
    
    async def run_test(self, concurrency: int, total: int, test_type: str):
        """运行并发测试"""
        print(f"\n{'='*60}")
        print(f"开始并发测试")
        print(f"{'='*60}")
        print(f"服务地址: {self.base_url}")
        print(f"并发数: {concurrency}")
        print(f"总请求数: {total}")
        print(f"测试类型: {test_type}")
        print(f"用户ID: {self.user_id}")
        print(f"交易对: {self.symbol}")
        print(f"{'='*60}\n")
        
        # 创建信号量控制并发数
        semaphore = asyncio.Semaphore(concurrency)
        
        async def place_order_with_limit():
            async with semaphore:
                return await self.place_order(session)
        
        async def cancel_order_with_limit():
            async with semaphore:
                return await self.cancel_order(session)
        
        # 创建连接池
        connector = aiohttp.TCPConnector(limit=concurrency * 2)
        async with aiohttp.ClientSession(connector=connector) as session:
            start_time = time.time()
            results: List[TestResult] = []
            
            if test_type == "place":
                # 只测试下单
                tasks = [place_order_with_limit() for _ in range(total)]
                results = await asyncio.gather(*tasks)
                
            elif test_type == "cancel":
                # 先创建一些订单用于撤单测试
                print("正在创建测试订单...")
                place_tasks = [place_order_with_limit() for _ in range(min(total, 100))]
                place_results = await asyncio.gather(*place_tasks)
                created_count = sum(1 for r in place_results if r.success)
                print(f"成功创建 {created_count} 个订单用于撤单测试\n")
                
                # 撤单测试
                cancel_tasks = [cancel_order_with_limit() for _ in range(min(total, created_count))]
                results = await asyncio.gather(*cancel_tasks)
                
            elif test_type == "both":
                # 混合测试：先创建足够的订单
                print("正在创建测试订单...")
                place_count = total + (total // 2)  # 创建更多订单以确保有足够的订单可以撤单
                place_tasks = [place_order_with_limit() for _ in range(place_count)]
                place_results = await asyncio.gather(*place_tasks)
                created_count = sum(1 for r in place_results if r.success)
                print(f"成功创建 {created_count} 个订单\n")
                
                # 混合执行：并发执行下单和撤单
                async def mixed_task():
                    async with semaphore:
                        if random.random() < 0.5 and self.created_orders:
                            return await self.cancel_order(session)
                        else:
                            return await self.place_order(session)
                
                tasks = [mixed_task() for _ in range(total)]
                results = await asyncio.gather(*tasks)
            
            elapsed_time = time.time() - start_time
            
            # 统计结果
            self.print_statistics(results, elapsed_time, test_type)
    
    def print_statistics(self, results: List[TestResult], elapsed_time: float, test_type: str):
        """打印统计信息"""
        total = len(results)
        success_count = sum(1 for r in results if r.success)
        fail_count = total - success_count
        
        response_times = [r.response_time for r in results if r.success]
        
        if response_times:
            avg_time = sum(response_times) / len(response_times)
            min_time = min(response_times)
            max_time = max(response_times)
            sorted_times = sorted(response_times)
            p50 = sorted_times[len(sorted_times) // 2]
            p95 = sorted_times[int(len(sorted_times) * 0.95)]
            p99 = sorted_times[int(len(sorted_times) * 0.99)]
        else:
            avg_time = min_time = max_time = p50 = p95 = p99 = 0
        
        qps = total / elapsed_time if elapsed_time > 0 else 0
        
        # 错误统计
        error_stats = defaultdict(int)
        for r in results:
            if not r.success:
                error_stats[r.error_message or f"HTTP {r.status_code}"] += 1
        
        print(f"\n{'='*60}")
        print(f"测试结果统计")
        print(f"{'='*60}")
        print(f"总请求数: {total}")
        print(f"成功数: {success_count} ({success_count/total*100:.2f}%)")
        print(f"失败数: {fail_count} ({fail_count/total*100:.2f}%)")
        print(f"总耗时: {elapsed_time:.2f} 秒")
        print(f"QPS: {qps:.2f}")
        print(f"\n响应时间统计 (秒):")
        print(f"  平均: {avg_time*1000:.2f} ms")
        print(f"  最小: {min_time*1000:.2f} ms")
        print(f"  最大: {max_time*1000:.2f} ms")
        print(f"  P50:  {p50*1000:.2f} ms")
        print(f"  P95:  {p95*1000:.2f} ms")
        print(f"  P99:  {p99*1000:.2f} ms")
        
        if error_stats:
            print(f"\n错误统计:")
            for error, count in sorted(error_stats.items(), key=lambda x: x[1], reverse=True):
                print(f"  {error}: {count}")
        
        print(f"{'='*60}\n")


async def main():
    parser = argparse.ArgumentParser(description='订单服务并发测试脚本')
    parser.add_argument('--url', type=str, default='http://localhost:8081',
                       help='服务地址 (默认: http://localhost:8081)')
    parser.add_argument('--concurrency', type=int, default=10,
                       help='并发数 (默认: 10)')
    parser.add_argument('--total', type=int, default=100,
                       help='总调用次数 (默认: 100)')
    parser.add_argument('--test-type', type=str, default='both',
                       choices=['place', 'cancel', 'both'],
                       help='测试类型: place(下单), cancel(撤单), both(混合) (默认: both)')
    parser.add_argument('--symbol', type=str, default='BTCUSDT',
                       help='交易对 (默认: BTCUSDT)')
    parser.add_argument('--user-id', type=int, default=1000000000000000001,
                       help='用户ID (默认: 1000000000000000001)')
    
    args = parser.parse_args()
    
    tester = OrderLoadTester(args.url, args.user_id, args.symbol)
    
    # 运行测试
    await tester.run_test(args.concurrency, args.total, args.test_type)


if __name__ == '__main__':
    asyncio.run(main())
