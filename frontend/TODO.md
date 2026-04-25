# 前端审查 TODO

## 说明

- 审查依据：`重构文档/重构计划.md`
- 审查范围：`Campus Forum/frontend/src`
- 审查时间：`2026-04-25 10:21`
- 执行者：`CODEX`

## 高优先级

| 优先级 | 位置 | 问题 | 影响 | 建议 |
|---|---|---|---|---|
| 高 | `src/router/index.js`、`src/views/welcome/LoginPage.vue` | 当前前端完全没有 `/admin` 路由，登录成功后固定跳转 `/index`，`store.user.role` 也没有参与任何路由分流 | 管理员账号无法进入后台，与重构计划“共用登录入口，按角色进入前台/后台”不一致 | 补齐 `/admin/**` 路由和后台布局；登录成功后按 `role` 跳转；增加管理员路由守卫 |
| 高 | `src/views/welcome/RegisterPage.vue` | `validateEmail()` 调用 `get()` 时写成了 `get(url, success, undefined, failure)`，第 4 个参数不会生效 | 验证码发送失败时，自定义失败处理不会执行，倒计时可能错误地卡在 `60` 秒 | 改成正确的失败回调签名，失败时恢复倒计时并提示用户 |
| 高 | `src/views/settings/PrivacySetting.vue` | `savePrivacy()` 只有成功回调，没有失败回调，也没有回滚勾选状态 | 请求失败时页面可能一直处于 `saving` 状态，且前端勾选状态与后端真实状态不一致 | 补失败回调；失败时恢复 `saving=false`，并把对应开关回滚到旧值 |
| 高 | `src/views/forum/TopicDetail.vue` | 评论数量只在新增时本地 `++`，删除评论后没有 `--`；分页总数依赖 `topic.data.comments` | 删除最后一条评论后分页总数会失真，可能出现空页或页码不回退 | 评论增删后统一以服务端返回为准刷新详情，或至少同步修正 `comments` 总数并处理越界页 |

## 中优先级

| 优先级 | 位置 | 问题 | 影响 | 建议 |
|---|---|---|---|---|
| 中 | `src/router/index.js` | `beforeEach` 里直接调用 `to.name.startsWith('welcome')`，没有判空 | 访问未匹配路由或异常路由时，守卫可能直接抛错 | 改成先判断 `typeof to.name === 'string'`，并补一个兜底 404 路由 |
| 中 | `src/views/IndexView.vue` | 顶部头像下拉菜单里的“个人设置”“消息列表”没有绑定任何行为 | 用户看到菜单但点击无反应，属于明显的假交互 | 给“个人设置”接入路由跳转；“消息列表”如果暂不做，至少隐藏或标记开发中 |
| 中 | `src/views/IndexView.vue`、`src/views/settings/UserSetting.vue` | 首屏加载状态只在成功分支里关闭，没有失败兜底 | 请求异常时页面可能一直卡在全屏 loading | 为初始化请求补失败回调，确保无论成功失败都能结束 loading，并显示明确提示 |

## 优化项

| 优先级 | 位置 | 问题 | 影响 | 建议 |
|---|---|---|---|---|
| 低 | `npm run build` 构建结果、`src/components/TopicEditor.vue` | 构建时已出现大 chunk 警告，`TopicEditor` 相关产物达到约 `539 kB` | 首次加载和路由切换成本偏高，后续管理端继续加功能后会更明显 | 将富文本编辑器及图片上传相关能力继续懒加载，必要时拆独立 chunk |
| 低 | `src/views/forum/TopicDetail.vue` | 存在未使用的 `axios`、`computed` 导入 | 增加噪音，降低可维护性 | 清理未使用导入，保持文件最小必要依赖 |
| 低 | `src/views/IndexView.vue` | 搜索框和左侧多个菜单仍是静态占位，没有查询或跳转行为 | 演示时容易被误认为“功能失效” | 未接通前先隐藏，或补“开发中”状态，避免假入口暴露给用户 |

## 建议执行顺序

1. 先补管理员登录分流、`/admin` 路由和守卫，这是和重构计划偏差最大的缺口。
2. 再修注册验证码失败回调、隐私设置失败兜底、评论分页总数同步，这三项会直接影响实际使用。
3. 最后处理假交互入口、loading 兜底和大 chunk 优化，提升演示完整度和可维护性。

## 审查结论

当前用户端主体已经能跑通，但还停留在“可用原型”状态。最明显的问题不是样式层面，而是管理员入口缺失、若干失败分支没兜住、以及部分 UI 已展示却没有实际行为。

## 管理端补充审查

### 高优先级

| 优先级 | 位置 | 问题 | 影响 | 建议 |
|---|---|---|---|---|
| 高 | `src/views/admin/DashboardPage.vue` | 数据看板所有总量都直接用分页接口第一页的 `data.length` 统计 | 当帖子、用户、评论数量超过单页上限后，看板数据会长期失真，无法用于演示或管理决策 | 后端补统计接口，或前端显式循环拉取全部页；不要再把第一页条数当总数 |
| 高 | `src/views/admin/UserDetailPage.vue`、`backend/.../AdminUserController.java`、`backend/.../AccountVO.java` | 用户详情页调用的是 `/api/admin/users/{id}`，但后端返回 `AccountVO` 不含 `status`；页面却依赖 `user.status` 显示状态并决定启用/禁用动作 | 详情页状态显示为空，且 `toggleStatus()` 会默认走 `enable` 分支，导致禁用按钮逻辑错误 | 要么后端详情接口改为返回含 `status` 的管理员 VO，要么前端改用已有列表数据结构并兼容空值 |
| 高 | `src/views/admin/TopicListPage.vue`、`UserListPage.vue`、`CommentListPage.vue` | 三个列表页都用 `total = (p + 1) * pageSize` 伪造分页总数 | 分页器会显示并允许跳转到不存在的页，筛选结果较少时尤其明显 | 后端返回真实总数；如果暂时做不到，就改成“上一页/下一页”或基于 `data.length < pageSize` 控制是否还有下一页 |

### 中优先级

| 优先级 | 位置 | 问题 | 影响 | 建议 |
|---|---|---|---|---|
| 中 | `src/views/admin/TopicDetailPage.vue`、`src/components/TopicTag.vue`、`src/views/AdminView.vue` | 管理端没有像前台 `Forum.vue` 那样加载分类到 `store.forum.types`，而详情页又把对象传给只接收数字 `type` 的 `TopicTag` | 管理端帖子详情页的分类标签大概率无法正确显示颜色和名称 | 管理端单独加载分类缓存；`TopicTag` 只传分类 ID，或在详情页内直接渲染本地分类标签 |
| 中 | `src/views/AdminView.vue`、`src/router/index.js` | 路由守卫只校验“是否登录”，不校验“是否管理员”；`AdminView` 内部再异步拉用户信息判断角色 | 非管理员已登录用户仍能先进入 `/admin` 路由，再靠页面内请求回跳；权限体验和边界都不够稳 | 在路由守卫里补管理员角色判断，至少在进入 `/admin/**` 前完成阻断 |
| 中 | `src/views/AdminView.vue`、`TopicListPage.vue`、`UserDetailPage.vue`、`TypeManagePage.vue` | 多个管理页面初始化请求只有成功分支，没有失败兜底；`axios.put/delete` 也没有统一 catch | 网络失败或 403/500 时可能一直 loading、或直接出现未处理 Promise 错误 | 统一补失败回调/`catch`，并复用 `net/index.js` 的错误处理逻辑 |

### 优化项

| 优先级 | 位置 | 问题 | 影响 | 建议 |
|---|---|---|---|---|
| 低 | `src/views/admin/DashboardPage.vue` | `shallowRef`、`loaded`、`checkDone()` 均未使用 | 增加噪音，降低代码可读性 | 清理未使用的导入和死代码 |
| 低 | `src/views/admin/TypeManagePage.vue` | 分类增删改直接混用 `axios` 和 `post/get` 两套请求风格 | 请求处理分散，后续统一鉴权和报错行为会更难维护 | 补统一的 `put`/`del` 请求封装，或至少集中抽成一个模块 |
