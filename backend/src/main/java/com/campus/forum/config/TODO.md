# Config Layer TODO

| 时间 | 执行者 | 完成内容 | 结果 |
|---|---|---|---|
| 2026-04-24 16:18 | CODEX | 根据 `config` 层审查结果生成目录级 TODO 清单 | 已完成 |

## 说明

- 本文件只记录 `config` 目录相关问题。
- 目标是先支持项目跑通，再按优先级回补配置层问题。

## P0：启动前确认

1. `SecurityConfiguration` 依赖类必须全部存在并可注入
   - 涉及：`JwtAuthenticationFilter`、`RequestLogFilter`、`AccountService`
   - 风险：缺任意一个都会导致编译失败或启动失败
   - 结论：继续推进前必须确认这些类已迁移完成

## P1：联调前修复

1. `/api/auth/logout` 的接口语义需要统一
   - 现状：`SecurityConfiguration` 中使用 Spring Security 的 `logoutUrl("/api/auth/logout")`
   - 风险：默认行为通常按 `POST` 处理，但文档层目前容易让人理解成 `GET`
   - 建议：统一为 `POST /api/auth/logout`，前后端和文档保持一致

2. Swagger 中登录/登出接口描述和真实行为不完全一致
   - 现状：`/api/auth/login` 被手工描述为 `QueryParameter`
   - 风险：和 Spring Security `formLogin` 的真实交互方式可能不一致，影响联调
   - 建议：后续改为更贴近真实请求方式的文档描述

3. Swagger 全局安全声明可能影响登录接口展示
   - 现状：全局声明了 `Authorization` 安全要求
   - 风险：登录接口在文档中也可能被误认为需要先带 token
   - 建议：给登录、登出接口单独移除安全要求

## P2：项目跑通后尽快优化

1. `RestTemplate` 没有超时配置
   - 现状：直接 `new RestTemplate()`
   - 风险：天气等外部接口异常时可能阻塞请求链路
   - 建议：补充连接超时、读取超时，并结合业务层兜底

2. MyBatis-Plus 分页插件未显式指定数据库类型
   - 现状：`new PaginationInnerInterceptor()`
   - 风险：当前 MySQL 下大概率可用，但不够明确
   - 建议：改为 `DbType.MYSQL`

## 下一步建议

1. 先确保 `SecurityConfiguration` 依赖类和登录链路能正常编译运行。
2. 开始前后端联调前，优先统一登出方法和 Swagger 文档。
3. 外部接口接入前，补 `RestTemplate` 超时配置。
