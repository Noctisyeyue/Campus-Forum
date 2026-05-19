<template>
    <div class="main-content" v-loading="loading" element-loading-text="正在加载管理后台...">
        <el-container style="height: 100%" v-if="!loading">
            <el-header class="main-content-header">
                <div style="font-size: 20px;font-weight: bold;color: #E6A23C">
                    Campus Forum 管理后台
                </div>
                <div style="flex: 1"></div>
                <div class="user-info">
                    <div class="theme-toggle" @click="store.toggleDark()">
                        <el-icon :size="18">
                            <component :is="store.dark ? Sunny : Moon"/>
                        </el-icon>
                    </div>
                    <div class="profile">
                        <div>{{ store.user.username }}</div>
                        <div>管理员</div>
                    </div>
                    <el-dropdown>
                        <el-avatar :src="store.avatarUrl"/>
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
                <el-aside width="230px">
                    <el-scrollbar style="height: calc(100vh - 55px)">
                        <el-menu router :default-active="$route.path"
                                 style="min-height: calc(100vh - 55px)">
                            <el-menu-item index="/admin">
                                <el-icon><DataLine/></el-icon>
                                <span>数据看板</span>
                            </el-menu-item>
                            <el-menu-item index="/admin/topics">
                                <el-icon><Document/></el-icon>
                                <span>帖子管理</span>
                            </el-menu-item>
                            <el-menu-item index="/admin/users">
                                <el-icon><User/></el-icon>
                                <span>用户管理</span>
                            </el-menu-item>
                            <el-menu-item index="/admin/comments">
                                <el-icon><ChatDotSquare/></el-icon>
                                <span>评论管理</span>
                            </el-menu-item>
                            <el-menu-item index="/admin/reports">
                                <el-icon><Warning/></el-icon>
                                <span>举报管理</span>
                            </el-menu-item>
                            <el-menu-item index="/admin/types">
                                <el-icon><CollectionTag/></el-icon>
                                <span>分类管理</span>
                            </el-menu-item>
                            <el-menu-item index="/admin/publish-activity">
                                <el-icon><Calendar/></el-icon>
                                <span>发布校园活动</span>
                            </el-menu-item>
                            <el-menu-item index="/admin/publish-notice-topic">
                                <el-icon><Bell/></el-icon>
                                <span>发布教务通知</span>
                            </el-menu-item>
                            <el-menu-item index="/admin/forum-notice">
                                <el-icon><EditPen/></el-icon>
                                <span>论坛公告设置</span>
                            </el-menu-item>
                        </el-menu>
                    </el-scrollbar>
                </el-aside>
                <el-main class="main-content-page">
                    <el-scrollbar style="height: calc(100vh - 55px)">
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
    DataLine, Document, EditPen, Moon, Sunny, SwitchButton, User, Warning
} from "@element-plus/icons-vue"
import {ElMessage} from "element-plus"

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

.theme-toggle {
    font-size: 18px;
    margin-right: 10px;
    cursor: pointer;
    color: var(--el-text-color-regular);
    transition: color .3s;

    &:hover {
        color: var(--el-color-primary);
    }
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
