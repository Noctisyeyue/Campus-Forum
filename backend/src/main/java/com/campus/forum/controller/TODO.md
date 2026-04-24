# Controller Layer TODO

| 时间 | 执行者 | 完成内容 | 结果 |
|---|---|---|---|
| 2026-04-24 17:03 | CODEX | 审查 `controller` 目录业务控制器，并生成目录级 TODO 清单 | 已完成 |

## 说明

- 本文件只记录 `controller` 根目录下业务控制器相关问题。
- 当前重点仍然是先保证后端主链路稳定，再逐步修正接口语义和边界问题。

## P1：正确性与接口语义问题

1. 多个“修改数据”的接口仍然使用 `GET`
   - 涉及：
     - `NotificationController` 的 `/delete`、`/delete-all`
     - `ForumController` 的 `/interact`、`/delete-comment`
   - 风险：
     - 不符合 REST 语义
     - 容易被浏览器预取、缓存、中间层误处理
     - 后续联调和接口文档都容易混乱
   - 建议：改为 `POST` / `DELETE`

2. 帖子详情接口在服务返回 `null` 时仍包成成功响应
   - 涉及：
     - `AdminTopicController#getTopic`
     - `ForumController#topic`
   - 风险：
     - 帖子不存在或无权查看时，前端拿到的是 `200 + data=null`
     - 不利于区分“没数据”和“访问失败”
   - 建议：控制器层或服务层统一返回 `404/403`

3. `AdminTopicController` 用 `uid = 0` 作为“管理员查看帖子”的魔法值
   - 现状：`topicService.getTopic(id, 0)`
   - 风险：
     - 业务含义不清晰
     - 后续如果 `getTopic` 严格按用户身份控制，这种写法很脆弱
   - 建议：增加独立管理员查询方法，不要复用普通用户详情接口

4. `ObjectController` 的错误响应不是标准 JSON，且内容类型固定为 `image/jpg`
   - 现状：
     - 错误时写的是 `RestBean.failure(...).toString()`
     - 响应头固定 `Content-Type: image/jpg`
   - 风险：
     - 404 错误体不是标准 JSON
     - PNG/WebP/GIF 等图片响应类型不准确
     - 前端和浏览器行为可能异常
   - 建议：
     - 错误体改用 `asJsonString()`
     - 图片响应类型按对象实际类型设置，错误时改成 `application/json`

## P2：架构和业务边界优化

1. `AdminTypeController` 直接操作 `TopicTypeMapper`
   - 风险：
     - 控制器直接落库，绕开 `service` 层
     - 分类相关业务规则、缓存清理、是否允许删除“正在使用中的分类”都难集中管理
   - 建议：把增删改查下沉到 `TopicService` 或独立 `TopicTypeService`

2. `AdminTypeController` 缺少业务校验
   - 现状：
     - 新增/编辑分类时未校验名称重复、颜色格式
     - 删除分类时未校验是否仍被帖子引用
   - 风险：容易产生脏数据或破坏前台筛选逻辑
   - 建议：增加服务层校验和友好错误返回

3. `ImageController` 只校验文件大小，不校验 MIME/扩展名
   - 风险：理论上可以上传非图片文件
   - 建议：增加内容类型白名单校验，例如 `image/jpeg/png/webp/gif`

## P3：部署与体验优化

1. `AuthorizeController#askVerifyCode` 直接使用 `request.getRemoteAddr()`
   - 风险：部署到反向代理后，验证码限流可能全部打到代理 IP
   - 建议：后续统一接入真实 IP 解析工具

2. 部分管理员操作接口默认“调用即成功”
   - 涉及：
     - `AdminUserController` 的禁用/启用/重置密码
     - `AdminTopicController` 的隐藏/恢复/置顶/取消置顶等
   - 风险：当目标不存在或状态不允许时，前端难以感知失败原因
   - 建议：服务层返回明确结果，控制器统一转成 `404/400/409`

## 当前结论

1. 控制器层整体结构已经能支撑继续开发，不是“完全不能用”。
2. 但接口语义问题比较集中，尤其是“用 `GET` 做删除/互动”和“`null` 仍返回成功”这两类，后面很容易影响前后端联调。
3. 如果下一步要优先稳住接口行为，建议先修：
   - `NotificationController` 和 `ForumController` 的状态变更接口方法
   - `ObjectController` 的错误响应与内容类型
   - 帖子详情接口的 `404/403` 语义
