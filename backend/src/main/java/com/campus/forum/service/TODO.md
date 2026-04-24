# Service Layer TODO

| 时间 | 执行者 | 完成内容 | 结果 |
|---|---|---|---|
| 2026-04-24 16:35 | CODEX | 审查 `service` 层接口与实现，并生成目录级 TODO 清单 | 已完成 |

## 说明

- 本文件只记录 `service` 目录相关问题。
- 当前目标仍然是先跑通主链路，因此区分“当前阻塞”和“可后置优化”。

## P1：主链路和联调前优先修复

1. `WeatherServiceImpl` 的天气降级仍不完整
   - 位置：`impl/WeatherServiceImpl.java`
   - 现状：
     - `rest.getForObject(...)` 抛出的 `404/超时/HTTP` 异常没有被统一捕获
     - 只在 GZIP 解压阶段做了 `IOException` 兜底
     - `geo.getJSONArray("location").getJSONObject(0)` 默认假设一定有结果
   - 风险：外部天气接口异常时，业务仍可能直接 `500`
   - 建议：把 `getForObject`、空结果判断、JSON 解析统一包进降级逻辑

2. `TopicServiceImpl#getTopic` 缺少帖子状态可见性控制
   - 位置：`impl/TopicServiceImpl.java`
   - 现状：按 `id` 直接查帖子详情，没有看到对 `published / pending_review / rejected / hidden / deleted` 的访问控制分支
   - 风险：和重构计划不一致，普通用户可能读到不该公开的帖子
   - 建议：补充规则：
     - 普通用户只能看 `published`
     - 作者本人可看自己的待审核/被拒绝帖子
     - 管理员可看全部

3. `TopicServiceImpl#updateTopic` 没校验更新结果却始终返回成功
   - 位置：`impl/TopicServiceImpl.java`
   - 现状：执行 `baseMapper.update(...)` 后直接 `return null`
   - 风险：当帖子不存在、不是本人帖子、或更新失败时，前端仍会收到“成功”
   - 建议：检查受影响行数，失败时返回明确错误信息

4. `TopicServiceImpl#createComment` 先插评论，再查目标帖子
   - 位置：`impl/TopicServiceImpl.java`
   - 现状：`commentMapper.insert(comment)` 在前，`baseMapper.selectById(vo.getTid())` 在后
   - 风险：
     - 可能给不存在帖子写评论
     - 可能给 `hidden / deleted / pending_review` 帖子写评论
     - 后续通知逻辑还可能触发空指针或脏数据
   - 建议：先校验目标帖子与引用评论是否存在且允许评论，再执行插入

5. `TopicServiceImpl#listTopicByPage` 空列表时返回 `null`
   - 位置：`impl/TopicServiceImpl.java`
   - 现状：`if (topics.isEmpty()) return null;`
   - 风险：前端需要额外判空，容易出现 `null` 导致的渲染分支问题
   - 建议：统一返回空列表 `[]`

## P2：项目跑通后尽快修复

1. `TopicServiceImpl#fillUserDetailsByPrivacy` 对隐私记录缺少空保护
   - 位置：`impl/TopicServiceImpl.java`
   - 现状：`accountPrivacyMapper.selectById(uid)` 后直接调用 `hiddenFields()`
   - 风险：如果数据不完整或迁移过程中少了隐私记录，会直接空指针
   - 建议：为空时补默认 `AccountPrivacy(uid)` 或兜底空隐藏字段数组

2. `ImageServiceImpl` 把 MinIO bucket 名写死为 `study`
   - 位置：`impl/ImageServiceImpl.java`
   - 风险：新项目重构后环境切换不方便，和配置化目标不一致
   - 建议：提到 `application-*.yml`

3. `ImageServiceImpl#fetchImageFromMinio` 没显式关闭 `GetObjectResponse`
   - 位置：`impl/ImageServiceImpl.java`
   - 风险：长期运行可能出现连接/流资源泄漏
   - 建议：改成 `try-with-resources`

4. `AccountPrivacyServiceImpl#savePrivacy` 对非法字段类型没有显式拒绝
   - 位置：`impl/AccountPrivacyServiceImpl.java`
   - 现状：`switch` 没有 `default`
   - 风险：传入非法 `type` 时也会 `saveOrUpdate`，行为静默且不易排查
   - 建议：增加白名单校验，非法参数直接返回失败

## P3：结构和性能优化项

1. `AccountDetailsServiceImpl#saveAccountDetails` 使用方法级 `synchronized`
   - 位置：`impl/AccountDetailsServiceImpl.java`
   - 风险：所有用户更新资料会串行化，扩大锁粒度
   - 建议：后续改成更小粒度的唯一性校验或数据库约束兜底

2. `AccountServiceImpl#registerEmailVerifyCode` 仍在使用 `address.intern()` 加锁
   - 位置：`impl/AccountServiceImpl.java`
   - 风险：和过滤器层类似，存在字符串常量池和本地锁设计问题
   - 建议：后续直接依赖 Redis 限流，不再加 JVM 本地锁

3. `NotificationServiceImpl#findUserNotification` 没有明确排序
   - 位置：`impl/NotificationServiceImpl.java`
   - 风险：通知展示顺序不稳定，影响前端体验
   - 建议：按时间或主键倒序返回

## 当前结论

1. `service` 层已经基本具备继续推进的基础，不是“完全不能用”的状态。
2. 但 `WeatherServiceImpl` 和 `TopicServiceImpl` 里有几处会直接影响当前阶段正确性的逻辑，需要尽快处理。
3. 如果下一步要优先让项目稳定跑起来，建议先修：
   - 天气接口统一降级
   - 帖子详情状态权限控制
   - 帖子编辑结果校验
   - 评论创建前置校验
