# Controller Exception TODO

| 时间 | 执行者 | 完成内容 | 结果 |
|---|---|---|---|
| 2026-04-24 17:03 | CODEX | 审查 `controller/exception` 目录异常处理器，并生成目录级 TODO 清单 | 已完成 |

## P1：异常处理覆盖不足

1. `ValidationController` 只处理 `ValidationException`
   - 现状：没有看到对以下常见 Spring MVC 校验异常的显式处理：
     - `MethodArgumentNotValidException`
     - `BindException`
     - `ConstraintViolationException`
   - 风险：
     - 很多 `@Valid @RequestBody`、`@RequestParam` 校验失败不会走这里
     - 最终可能落到默认错误处理，返回格式不统一
   - 建议：补齐上述异常处理，统一返回 `400 + 明确提示`

## P2：健壮性优化

1. `ErrorPageController` 默认读取 `errorAttributes.get("message").toString()`
   - 风险：当 `message` 为空时可能出现空指针
   - 建议：先判空，再提供默认文案

2. `ValidationController` 当前返回信息过于笼统
   - 现状：统一返回“请求参数有误”
   - 风险：开发联调阶段排错效率偏低
   - 建议：开发环境可考虑输出首条校验消息，生产环境保留统一文案

## 当前结论

1. 异常控制器已经有基础框架，但对 Spring 常见参数校验异常的覆盖还不够。
2. 这部分短期不会阻止项目启动，但会直接影响接口报错是否稳定、统一、可调试。
