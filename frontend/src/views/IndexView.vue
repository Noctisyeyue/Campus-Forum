<template>
    <div class="main-content" v-loading="loading" element-loading-text="正在进入，请稍后...">
        <el-container style="height: 100%" v-if="!loading">
            <el-header class="main-content-header">
                <div style="font-size: 20px;font-weight: bold;color: #409EFF">Campus Forum</div>
                <div style="flex: 1;padding: 0 20px;text-align: center">
                    <el-input v-model="searchInput.text" style="width: 100%;max-width: 500px"
                              placeholder="搜索论坛相关内容..."
                              @keyup.enter="doSearch">
                        <template #prefix>
                            <el-icon>
                                <Search/>
                            </el-icon>
                        </template>
                        <template #append>
                            <el-select style="width: 120px" v-model="searchInput.type">
                                <el-option value="1" label="帖子广场"/>
                                <el-option value="2" label="校园活动"/>
                                <el-option value="4" label="教务通知"/>
                            </el-select>
                        </template>
                    </el-input>
                </div>
                <div class="user-info">
                    <ThemeToggle/>
                    <el-popover placement="bottom" :width="350" trigger="click">
                        <template #reference>
                            <el-badge style="margin-right: 15px" is-dot :hidden="!notification.length">
                                <div class="notification">
                                    <el-icon><Bell/></el-icon>
                                    <div style="font-size: 10px">消息</div>
                                </div>
                            </el-badge>
                        </template>
                        <el-empty :image-size="80" description="暂时没有未读消息哦~" v-if="!notification.length"/>
                        <el-scrollbar :max-height="500" v-else>
                            <light-card v-for="item in notification" class="notification-item"
                                        @click="confirmNotification(item.id, item.url)">
                                <div>
                                    <el-tag size="small" :type="item.type">消息</el-tag>&nbsp;
                                    <span style="font-weight: bold">{{item.title}}</span>
                                </div>
                                <el-divider style="margin: 7px 0 3px 0"/>
                                <div class="text-secondary" style="font-size: 13px">
                                    {{item.content}}
                                </div>
                            </light-card>
                        </el-scrollbar>
                        <div style="margin-top: 10px">
                            <el-button size="small" type="info" :icon="Check" @click="deleteAllNotification"
                                       style="width: 100%" plain>全部标记为已读</el-button>
                        </div>
                    </el-popover>
                    <div class="profile">
                        <div>{{ store.user.username }}</div>
                        <div>{{ store.user.email }}</div>
                    </div>
                    <el-dropdown>
                        <el-avatar :src="store.avatarUrl"
                                   :style="!store.avatarUrl ? { background: store.avatarColor(store.user.username) } : {}">
                            {{ store.avatarUrl ? '' : store.avatarText(store.user.username) }}
                        </el-avatar>
                        <template #dropdown>
                            <el-dropdown-item @click="router.push('/index/user-setting')">
                                <el-icon>
                                    <Operation/>
                                </el-icon>
                                个人中心
                            </el-dropdown-item>
                            <el-dropdown-item @click="router.push('/index/messages')">
                                <el-icon>
                                    <Message/>
                                </el-icon>
                                消息列表
                            </el-dropdown-item>
                            <el-dropdown-item v-if="store.isAdmin" @click="router.push('/admin')">
                                <el-icon>
                                    <Monitor/>
                                </el-icon>
                                管理后台
                            </el-dropdown-item>
                            <el-dropdown-item @click="userLogout" divided>
                                <el-icon>
                                    <Back/>
                                </el-icon>
                                退出登录
                            </el-dropdown-item>
                        </template>
                    </el-dropdown>
                </div>
            </el-header>
            <el-container>
                <el-aside width="230px">
                    <el-scrollbar style="height: calc(100vh - 55px)">
                        <el-menu
                                router
                                :default-active="$route.path"
                                :default-openeds="['1', '2', '3']"
                                style="min-height: calc(100vh - 55px)">
                            <el-sub-menu index="1">
                                <template #title>
                                    <el-icon>
                                        <Location/>
                                    </el-icon>
                                    <span><b>校园论坛</b></span>
                                </template>
                                <el-menu-item index="/index">
                                    <template #title>
                                        <el-icon>
                                            <ChatDotSquare/>
                                        </el-icon>
                                        帖子广场
                                    </template>
                                </el-menu-item>
                                <el-menu-item index="/index/activity">
                                    <template #title>
                                        <el-icon>
                                            <Notification/>
                                        </el-icon>
                                        校园活动
                                    </template>
                                </el-menu-item>
                            </el-sub-menu>
                            <el-sub-menu index="2">
                                <template #title>
                                    <el-icon>
                                        <Position/>
                                    </el-icon>
                                    <span><b>探索与发现</b></span>
                                </template>
                                <el-menu-item index="/index/notice-topic">
                                    <template #title>
                                        <el-icon>
                                            <Monitor/>
                                        </el-icon>
                                        教务通知
                                    </template>
                                </el-menu-item>
                            </el-sub-menu>
                            <el-sub-menu index="3">
                                <template #title>
                                    <el-icon>
                                        <Operation/>
                                    </el-icon>
                                    <span><b>个人中心</b></span>
                                </template>
                                <el-menu-item index="/index/user-setting">
                                    <template #title>
                                        <el-icon>
                                            <User/>
                                        </el-icon>
                                        个人信息设置
                                    </template>
                                </el-menu-item>
                                <el-menu-item index="/index/privacy-setting">
                                    <template #title>
                                        <el-icon>
                                            <Lock/>
                                        </el-icon>
                                        账号安全设置
                                    </template>
                                </el-menu-item>
                                <el-menu-item index="/index/my-topics">
                                    <template #title>
                                        <el-icon>
                                            <Document/>
                                        </el-icon>
                                        我的帖子
                                    </template>
                                </el-menu-item>
                                <el-menu-item index="/index/messages">
                                    <template #title>
                                        <el-icon>
                                            <Message/>
                                        </el-icon>
                                        消息列表
                                    </template>
                                </el-menu-item>
                            </el-sub-menu>
                        </el-menu>
                    </el-scrollbar>
                </el-aside>
                <el-main class="main-content-page">
                    <el-scrollbar ref="mainScrollbar"
                                  style="height: calc(100vh - 55px)"
                                  :style="{ visibility: restoringMainScroll ? 'hidden' : 'visible' }"
                                  @scroll="handleMainScroll">
                        <router-view v-slot="{ Component }">
                            <transition name="el-fade-in-linear"
                                        mode="out-in"
                                        @before-enter="handleMainViewBeforeEnter">
                                <component :is="Component" style="height: 100%"/>
                            </transition>
                        </router-view>
                    </el-scrollbar>
                </el-main>
            </el-container>
        </el-container>
    </div>
</template>

<script setup>
import {get, logout} from '@/net'
import router from "@/router";
import {useStore} from "@/stores/index";
import {nextTick, reactive, ref, watch} from "vue";
import {useRoute} from "vue-router";
import {
    Back,
    Bell,
    ChatDotSquare, Check,
    Document,
    Location, Lock, Message, Monitor,
    Moon,
    Notification, Operation,
    Position,
    School, Search, Sunny,
    User
} from "@element-plus/icons-vue";
import LightCard from "@/components/LightCard.vue";
import ThemeToggle from "@/components/ThemeToggle.vue";
import {ElMessage} from "element-plus";

/** Pinia 全局状态 */
const store = useStore()
/** 当前路由信息 */
const route = useRoute()
/** 页面加载状态，true 时显示全屏 loading */
const loading = ref(true)
/** 主内容区滚动条组件引用 */
const mainScrollbar = ref()
/** 各列表页的滚动位置缓存（path → scrollTop） */
const routeScrollState = new Map()
/** 是否正在恢复可记忆列表页的滚动位置 */
const restoringMainScroll = ref(false)
/** 当前滚动恢复任务序号，用于丢弃过期恢复任务 */
let mainScrollRestoreToken = 0
/** 等待在外层页面进入前应用的滚动重置路径 */
const pendingMainScrollPath = ref('')
/** 需要记忆滚动位置的列表页路由集合 */
const RESTORE_SCROLL_ROUTE_SET = new Set([
    '/index/my-topics',
    '/index/messages'
])

/** 搜索栏数据：type 控制搜索范围，text 为搜索关键词 */
const searchInput = reactive({
    type: '1',
    text: ''
})

/** 执行搜索：根据选中的范围跳转到对应列表页，携带搜索关键词 */
function doSearch() {
    const text = searchInput.text.trim()
    if (!text) return
    const typeRouteMap = { '1': '/index', '2': '/index/activity', '4': '/index/notice-topic' }
    router.push({ path: typeRouteMap[searchInput.type], query: { search: text } })
}

/** 未读消息通知列表 */
const notification = ref([])

/** 页面初始化：获取当前登录用户信息 */
get('/api/user/info', (data) => {
    store.user = data
    loading.value = false
})

/** 加载未读消息列表 */
const loadNotification =
        () => get('/api/notification/unread', data => notification.value = data)
loadNotification()

/** 退出登录 */
function userLogout() {
    logout(() => router.push("/"))
}

/**
 * 点击单条通知：标记为已读，刷新未读列表，再跳转到通知指向的页面
 *
 * @param id  通知 ID
 * @param url 通知指向的路由路径
 */
function confirmNotification(id, url) {
    get(`/api/notification/read?id=${id}`, () => {
        loadNotification()
        if (!hasNotificationTarget(url)) {
            ElMessage.info('该消息仅用于通知，没有可跳转的内容')
            return
        }
        router.push(url)
    })
}

/** 将全部未读消息标记为已读 */
function deleteAllNotification() {
    get(`/api/notification/read-all`, loadNotification)
}

/**
 * 判断通知的跳转目标是否有效（排除 null、undefined 等无效路径）
 *
 * @param url 通知指向的路由路径
 * @return true=有效可跳转，false=无效
 */
function hasNotificationTarget(url) {
    return typeof url === 'string'
        && url.trim().length > 0
        && url !== 'null'
        && url !== 'undefined'
        && !url.endsWith('/null')
        && !url.endsWith('/undefined')
}

/**
 * 判断指定路由是否需要记忆滚动位置
 *
 * @param path 路由路径
 * @return true=需要记忆
 */
function shouldRememberScroll(path) {
    return RESTORE_SCROLL_ROUTE_SET.has(path)
}

/** 获取主滚动容器的原生 DOM 元素 */
function currentScrollWrap() {
    return mainScrollbar.value?.wrapRef || null
}

/**
 * 将主滚动容器滚动到指定位置
 *
 * @param top 滚动距离
 * @return 是否成功拿到滚动容器并完成设置
 */
function setMainScrollTop(top) {
    const wrap = currentScrollWrap()
    if (!wrap) return false
    wrap.scrollTop = top
    return true
}

/**
 * 主滚动区域滚动事件处理：记录当前路由的滚动位置
 *
 * @param scrollTop 当前滚动距离
 */
function handleMainScroll({ scrollTop }) {
    if (!shouldRememberScroll(route.path)) return
    routeScrollState.set(route.path, scrollTop)
}

/**
 * 判断当前路由是否为论坛帖子详情页
 *
 * @param path 路由路径
 * @return true 表示论坛详情页
 */
function isForumDetailRoute(path) {
    return typeof path === 'string' && path.startsWith('/index/topic-detail/')
}

/**
 * 判断当前路由是否为论坛列表页
 *
 * @param path 路由路径
 * @return true 表示论坛列表页
 */
function isForumListRoute(path) {
    return ['/index', '/index/', '/index/activity', '/index/notice-topic'].includes(path)
}

/**
 * 对当前主内容路由应用滚动位置
 *
 * @param path 路由路径
 * @param delays 延迟校准时间点
 */
function applyMainScroll(path, delays = [0]) {
    const token = ++mainScrollRestoreToken
    const top = shouldRememberScroll(path) && routeScrollState.has(path)
        ? routeScrollState.get(path)
        : 0
    delays.forEach(delay => {
        window.setTimeout(() => {
            if (token !== mainScrollRestoreToken) return
            setMainScrollTop(top)
        }, delay)
    })
}

/**
 * 恢复可记忆列表页的滚动位置，并在恢复完成前短暂隐藏内容，避免先露出顶部再跳回原处
 *
 * @param path 路由路径
 */
function restoreRememberedMainScroll(path) {
    const top = routeScrollState.get(path)
    if (typeof top !== 'number') {
        restoringMainScroll.value = false
        return
    }
    const token = ++mainScrollRestoreToken
    const delays = [0, 40, 100, 180, 280, 380]
    restoringMainScroll.value = true
    delays.forEach(delay => {
        window.setTimeout(() => {
            if (token !== mainScrollRestoreToken) return
            setMainScrollTop(top)
            if (delay === delays[delays.length - 1]) {
                restoringMainScroll.value = false
            }
        }, delay)
    })
    window.setTimeout(() => {
        if (token === mainScrollRestoreToken) {
            restoringMainScroll.value = false
        }
    }, delays[delays.length - 1] + 80)
}

/**
 * 外层新页面进入前应用延迟的滚动重置，避免旧的论坛列表页在离场时先闪到顶部
 */
function handleMainViewBeforeEnter() {
    if (!pendingMainScrollPath.value) return
    applyMainScroll(pendingMainScrollPath.value, [0, 16])
    pendingMainScrollPath.value = ''
}

/**
 * 监听主路由路径变化：
 * 1. 切回可记忆列表页时恢复历史位置
 * 2. 进入论坛详情页时保持当前滚动，交给详情页自身在挂载后置顶
 * 3. 进入其他普通页面时回到顶部
 */
watch(() => route.path, async (path, oldPath) => {
    await nextTick()
    pendingMainScrollPath.value = ''
    if (shouldRememberScroll(path) && routeScrollState.has(path)) {
        restoreRememberedMainScroll(path)
        return
    }
    if (isForumDetailRoute(path)) {
        restoringMainScroll.value = false
        return
    }
    restoringMainScroll.value = false
    if (isForumListRoute(oldPath)) {
        pendingMainScrollPath.value = path
        return
    }
    applyMainScroll(path, [0, 16])
}, { immediate: true })
</script>

<style lang="less" scoped>
.notification-item {
    transition: .3s;
    &:hover {
        cursor: pointer;
        opacity: 0.7;
    }
}

.notification {
    font-size: 22px;
    line-height: 14px;
    text-align: center;
    transition: color .3s;

    &:hover {
        color: grey;
        cursor: pointer;
    }
}

.main-content-page {
    padding: 0;
    background-color: #f7f8fa;
}

.dark .main-content-page {
    background-color: #212225;
}

.main-content {
    height: 100vh;
    width: 100vw;
}

.main-content-header {
    border-bottom: solid 1px var(--el-border-color);
    height: 55px;
    display: flex;
    align-items: center;
    box-sizing: border-box;
    background-color: var(--el-bg-color);

    .user-info {
        display: flex;
        justify-content: flex-end;
        align-items: center;

        .el-avatar:hover {
            cursor: pointer;
        }

        .profile {
            text-align: right;
            margin-right: 20px;

            :first-child {
                font-size: 18px;
                font-weight: bold;
                line-height: 20px;
            }

            :last-child {
                font-size: 10px;
                color: grey;
            }
        }
    }
}

:global(.dark) .profile :last-child {
    color: #a0a3a8;
}
</style>
