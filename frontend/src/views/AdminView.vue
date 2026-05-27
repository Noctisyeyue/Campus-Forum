<template>
    <div class="admin-layout" :class="{ 'is-dark': store.dark }" v-loading="loading"
         element-loading-text="正在加载管理后台...">
        <el-container style="height: 100%" v-if="!loading">
            <el-header class="admin-header">
                <div class="admin-header-brand">
                    <el-icon :size="22" class="brand-icon"><Monitor/></el-icon>
                    <span class="brand-text">Campus Forum</span>
                </div>
                <div style="flex: 1"></div>
                <div class="admin-header-actions">
                    <ThemeToggle/>
                    <div class="header-action-btn" @click="router.push('/index')" title="返回前台">
                        <el-icon :size="18"><Back/></el-icon>
                    </div>
                    <div class="header-divider"></div>
                    <div class="header-profile">
                        <span class="header-profile-name">{{ store.user.username }}</span>
                        <el-tag size="small" type="danger" effect="dark">管理员</el-tag>
                    </div>
                    <el-dropdown>
                        <el-avatar :src="store.avatarUrl" :size="34" class="header-avatar"/>
                        <template #dropdown>
                            <el-dropdown-item @click="router.push('/index')">
                                <el-icon><Back/></el-icon>
                                返回前台
                            </el-dropdown-item>
                            <el-dropdown-item @click="userLogout" divided>
                                <el-icon><SwitchButton/></el-icon>
                                退出登录
                            </el-dropdown-item>
                        </template>
                    </el-dropdown>
                </div>
            </el-header>
            <el-container>
                <el-aside width="230px" class="admin-aside">
                    <el-scrollbar style="height: calc(100vh - 56px)">
                        <el-menu router :default-active="$route.path"
                                 class="admin-sidebar">
                            <el-menu-item index="/admin">
                                <el-icon><DataLine/></el-icon>
                                <span>数据看板</span>
                            </el-menu-item>

                            <el-sub-menu index="g-content">
                                <template #title>
                                    <el-icon><Document/></el-icon>
                                    <span>内容管理</span>
                                </template>
                                <el-menu-item index="/admin/topics">
                                    <el-icon><ChatDotSquare/></el-icon>
                                    <span>帖子管理</span>
                                </el-menu-item>
                                <el-menu-item index="/admin/comments">
                                    <el-icon><Comment/></el-icon>
                                    <span>评论管理</span>
                                </el-menu-item>
                                <el-menu-item index="/admin/reports">
                                    <el-icon><Warning/></el-icon>
                                    <span>举报管理</span>
                                </el-menu-item>
                            </el-sub-menu>

                            <el-sub-menu index="g-user">
                                <template #title>
                                    <el-icon><User/></el-icon>
                                    <span>用户与分类</span>
                                </template>
                                <el-menu-item index="/admin/users">
                                    <el-icon><UserFilled/></el-icon>
                                    <span>用户管理</span>
                                </el-menu-item>
                                <el-menu-item index="/admin/types">
                                    <el-icon><CollectionTag/></el-icon>
                                    <span>分类管理</span>
                                </el-menu-item>
                            </el-sub-menu>

                            <el-sub-menu index="g-ops">
                                <template #title>
                                    <el-icon><Bell/></el-icon>
                                    <span>运营发布</span>
                                </template>
                                <el-menu-item index="/admin/publish-activity">
                                    <el-icon><Calendar/></el-icon>
                                    <span>发布校园活动</span>
                                </el-menu-item>
                                <el-menu-item index="/admin/publish-notice-topic">
                                    <el-icon><EditPen/></el-icon>
                                    <span>发布教务通知</span>
                                </el-menu-item>
                                <el-menu-item index="/admin/forum-notice">
                                    <el-icon><Setting/></el-icon>
                                    <span>论坛公告设置</span>
                                </el-menu-item>
                            </el-sub-menu>
                        </el-menu>
                    </el-scrollbar>
                </el-aside>
                <el-main class="admin-main">
                    <el-scrollbar style="height: calc(100vh - 56px)">
                        <router-view v-slot="{ Component }">
                            <transition name="el-fade-in-linear" mode="out-in">
                                <component :is="Component"/>
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
import router from "@/router"
import {useStore} from "@/stores/index"
import {ref} from "vue"
import {
    Back, Bell, Calendar, ChatDotSquare, CollectionTag,
    Comment, DataLine, Document, EditPen, Monitor, Moon,
    Setting, Sunny, SwitchButton, User, UserFilled, Warning
} from "@element-plus/icons-vue"
import {ElMessage} from "element-plus"
import ThemeToggle from "@/components/ThemeToggle.vue"

const store = useStore()
const loading = ref(true)

get('/api/user/info', (data) => {
    store.user = data
    loading.value = false
    if (data.role !== 'admin') {
        ElMessage.warning('无权限访问管理后台')
        router.replace('/index')
    }
})

function userLogout() {
    logout(() => router.push("/"))
}
</script>

<style lang="less" scoped>
/* ==================== 全局布局 ==================== */
.admin-layout {
    height: 100vh;
    width: 100vw;

    --admin-header-bg: #ffffff;
    --admin-header-border: #e5e7eb;
    --admin-header-text: #1f2937;
    --admin-header-text-secondary: #6b7280;
    --admin-brand-color: #d97706;
    --admin-aside-bg: #ffffff;
    --admin-aside-border: #e5e7eb;
    --admin-main-bg: #f5f7fa;
    --admin-action-hover: #f3f4f6;
    --admin-divider: #e5e7eb;
    --admin-avatar-ring: #e5e7eb;
}

.admin-layout.is-dark {
    --admin-header-bg: #1d1e22;
    --admin-header-border: #333639;
    --admin-header-text: #e5eaf3;
    --admin-header-text-secondary: #8d929a;
    --admin-brand-color: #f59e0b;
    --admin-aside-bg: #1d1e22;
    --admin-aside-border: #333639;
    --admin-main-bg: #141518;
    --admin-action-hover: #26272b;
    --admin-divider: #333639;
    --admin-avatar-ring: #44464a;
}

/* ==================== 顶部导航栏 ==================== */
.admin-header {
    height: 56px;
    display: flex;
    align-items: center;
    padding: 0 20px;
    border-bottom: 1px solid var(--admin-header-border);
    background: var(--admin-header-bg);
    box-sizing: border-box;
    z-index: 10;
}

.admin-header-brand {
    display: flex;
    align-items: center;
    gap: 10px;
    user-select: none;

    .brand-icon {
        color: var(--admin-brand-color);
    }

    .brand-text {
        font-size: 17px;
        font-weight: 700;
        color: var(--admin-header-text);
        letter-spacing: 0.02em;
    }
}

.admin-header-actions {
    display: flex;
    align-items: center;
    gap: 6px;
}

.header-action-btn {
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    cursor: pointer;
    color: var(--admin-header-text-secondary);
    transition: background-color .2s, color .2s;

    &:hover {
        background: var(--admin-action-hover);
        color: var(--admin-header-text);
    }
}

.header-divider {
    width: 1px;
    height: 24px;
    background: var(--admin-divider);
    margin: 0 8px;
}

.header-profile {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-right: 12px;

    .header-profile-name {
        font-size: 14px;
        font-weight: 600;
        color: var(--admin-header-text);
    }
}

.header-avatar {
    border: 2px solid var(--admin-avatar-ring);
    cursor: pointer;
    transition: border-color .2s;

    &:hover {
        border-color: var(--admin-brand-color);
    }
}

/* ==================== 侧边栏 ==================== */
.admin-aside {
    border-right: 1px solid var(--admin-aside-border);
    background: var(--admin-aside-bg);
}

.admin-sidebar {
    min-height: calc(100vh - 56px);
    border-right: none;
    padding-top: 8px;

    :deep(.el-sub-menu__title) {
        height: 44px;
        line-height: 44px;
        font-weight: 600;
        font-size: 13px;
        color: var(--admin-header-text-secondary);
    }

    :deep(.el-menu-item) {
        height: 40px;
        line-height: 40px;
        font-size: 13px;
        border-radius: 6px;
        margin: 2px 8px;
        padding-left: 44px !important;
    }

    :deep(.el-menu-item.is-active) {
        font-weight: 600;
    }
}

/* ==================== 主内容区 ==================== */
.admin-main {
    padding: 0;
    background: var(--admin-main-bg);
}
</style>
