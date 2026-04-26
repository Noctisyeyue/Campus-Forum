<template>
    <div class="main-content" v-loading="loading" element-loading-text="正在进入，请稍后...">
        <el-container style="height: 100%" v-if="!loading">
            <el-header class="main-content-header">
                <div style="font-size: 20px;font-weight: bold;color: #409EFF">Campus Forum</div>
                <div style="flex: 1;padding: 0 20px;text-align: center">
                    <el-input v-model="searchInput.text" style="width: 100%;max-width: 500px"
                              placeholder="搜索论坛相关内容...">
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
                                <div style="font-size: 13px;color: grey">
                                    {{item.content}}
                                </div>
                            </light-card>
                        </el-scrollbar>
                        <div style="margin-top: 10px">
                            <el-button size="small" type="info" :icon="Check" @click="deleteAllNotification"
                                       style="width: 100%" plain>清除全部未读消息</el-button>
                        </div>
                    </el-popover>
                    <div class="profile">
                        <div>{{ store.user.username }}</div>
                        <div>{{ store.user.email }}</div>
                    </div>
                    <el-dropdown>
                        <el-avatar :src="store.avatarUrl"/>
                        <template #dropdown>
                            <el-dropdown-item>
                                <el-icon>
                                    <Operation/>
                                </el-icon>
                                个人设置
                            </el-dropdown-item>
                            <el-dropdown-item>
                                <el-icon>
                                    <Message/>
                                </el-icon>
                                消息列表
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
                                <el-menu-item index="/index/my-topics">
                                    <template #title>
                                        <el-icon>
                                            <Document/>
                                        </el-icon>
                                        我的帖子
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
                                    <span><b>个人设置</b></span>
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
                            </el-sub-menu>
                        </el-menu>
                    </el-scrollbar>
                </el-aside>
                <el-main class="main-content-page">
                    <el-scrollbar ref="mainScrollbar"
                                  style="height: calc(100vh - 55px)"
                                  @scroll="handleMainScroll">
                        <router-view v-slot="{ Component }">
                            <transition name="el-fade-in-linear" mode="out-in">
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
    Notification, Operation,
    Position,
    School, Search,
    User
} from "@element-plus/icons-vue";
import LightCard from "@/components/LightCard.vue";
import {ElMessage} from "element-plus";

const store = useStore()
const route = useRoute()
const loading = ref(true)
const mainScrollbar = ref()
const routeScrollState = new Map()
const RESTORE_SCROLL_ROUTE_SET = new Set([
    '/index',
    '/index/activity',
    '/index/notice-topic',
    '/index/my-topics'
])

const searchInput = reactive({
    type: '1',
    text: ''
})
const notification = ref([])

get('/api/user/info', (data) => {
    store.user = data
    loading.value = false
    if (data.role === 'admin') router.replace('/admin')
})
const loadNotification =
        () => get('/api/notification/list', data => notification.value = data)
loadNotification()

function userLogout() {
    logout(() => router.push("/"))
}

function confirmNotification(id, url) {
    get(`/api/notification/delete?id=${id}`, () => {
        loadNotification()
        if (!hasNotificationTarget(url)) {
            ElMessage.info('该消息仅用于通知，没有可跳转的内容')
            return
        }
        router.push(url)
    })
}

function deleteAllNotification() {
    get(`/api/notification/delete-all`, loadNotification)
}

function hasNotificationTarget(url) {
    return typeof url === 'string'
        && url.trim().length > 0
        && url !== 'null'
        && url !== 'undefined'
        && !url.endsWith('/null')
        && !url.endsWith('/undefined')
}

function shouldRememberScroll(path) {
    return RESTORE_SCROLL_ROUTE_SET.has(path)
}

function currentScrollWrap() {
    return mainScrollbar.value?.wrapRef || null
}

function handleMainScroll({ scrollTop }) {
    if (!shouldRememberScroll(route.path)) return
    routeScrollState.set(route.path, scrollTop)
}

function restoreMainScroll(path) {
    if (!shouldRememberScroll(path)) return
    const top = routeScrollState.get(path)
    if (typeof top !== 'number') return
    ;[0, 30, 80, 160, 320, 600].forEach(delay => {
        window.setTimeout(() => {
            const wrap = currentScrollWrap()
            if (wrap) {
                wrap.scrollTop = top
            }
        }, delay)
    })
}

watch(() => route.path, async path => {
    await nextTick()
    restoreMainScroll(path)
})
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
</style>
