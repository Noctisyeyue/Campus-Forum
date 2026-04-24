# Filter Layer TODO

| 时间 | 执行者 | 完成内容 | 结果 |
|---|---|---|---|
| 2026-04-24 16:18 | CODEX | 根据 `filter` 层审查结果重生成目录级 TODO 清单 | 已完成 |

## 说明

- 本文件只记录 `filter` 目录相关问题。
- 当前策略是先保证项目能跑，再按优先级回补过滤器问题。

## P1：前后端联调前必须修复

1. `CorsFilter` 没有直接放行 `OPTIONS` 预检请求
   - 现状：预检请求会继续进入后续安全链
   - 风险：浏览器跨域联调时，可能再次出现 `OPTIONS -> 401`
   - 建议：
     - 在 `CorsFilter` 中对 `OPTIONS` 直接返回 `200/204`
     - 或在安全配置中显式放行 `OPTIONS /**`

2. `RequestLogFilter` 设置 `MDC` 后没有清理
   - 现状：有 `MDC.put("reqId", ...)`，但没有 `remove/clear`
   - 风险：线程复用时日志串号，`RestBean` 请求 ID 可能污染
   - 建议：改成 `try/finally`，并在 `finally` 中清理 `MDC`

## P2：项目跑通后尽快优化

1. `FlowLimitingFilter` 使用 `synchronized (address.intern())`
   - 现状：依赖字符串常量池加锁
   - 风险：长期运行有内存和并发设计风险
   - 建议：后续移除本地锁，改为完全依赖 Redis 原子能力

2. 限流与日志都直接读取 `request.getRemoteAddr()`
   - 现状：本地开发可用
   - 风险：部署到 Nginx、网关后，客户端 IP 可能失真
   - 建议：后续抽取统一真实 IP 工具，优先读取代理头

## P3：可观测性优化

1. `RequestLogFilter` 目前不记录 JSON 请求体摘要
   - 现状：只记录 `request.getParameterMap()`
   - 风险：JSON 接口排查信息不足
   - 建议：后续按需补请求体摘要日志，并注意敏感字段脱敏

2. `RequestLogFilter` 对非 `200` 响应的记录过于粗略
   - 现状：主要记录“状态码错误”
   - 风险：接口异常时定位效率偏低
   - 建议：后续保留错误响应摘要，但避免输出敏感信息

## 当前结论

1. 这些问题可以先不修，先推进项目启动和主链路打通。
2. 但一旦开始浏览器跨域联调，`CorsFilter` 和 `RequestLogFilter` 的两个 P1 问题要优先处理。
