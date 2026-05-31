/** 论坛列表页滚动位置缓存（path -> scrollTop） */
const forumScrollState = new Map()

/** 当前论坛滚动恢复任务序号，用于丢弃过期恢复任务 */
let forumScrollRestoreToken = 0

/**
 * 获取用户端主内容区滚动容器
 *
 * @return {HTMLElement|null} 主滚动容器 DOM
 */
function forumScrollWrap() {
    return document.querySelector('.main-content-page .el-scrollbar__wrap')
}

/**
 * 保存指定论坛列表页的滚动位置
 *
 * @param {string} path 论坛列表页路由路径
 * @return {void}
 */
export function saveForumScroll(path) {
    const wrap = forumScrollWrap()
    if (!wrap || !path) return
    forumScrollState.set(path, wrap.scrollTop)
}

/**
 * 恢复指定论坛列表页的滚动位置
 *
 * @param {string} path 论坛列表页路由路径
 * @param {number[]} delays 延迟校准时间点
 * @return {void}
 */
export function restoreForumScroll(path, delays = [0, 40, 100, 180]) {
    if (!path || !forumScrollState.has(path)) return
    const top = forumScrollState.get(path)
    const token = ++forumScrollRestoreToken
    delays.forEach(delay => {
        window.setTimeout(() => {
            if (token !== forumScrollRestoreToken) return
            const wrap = forumScrollWrap()
            if (wrap) {
                wrap.scrollTop = top
            }
        }, delay)
    })
}
