<template>
    <div>
        <!-- 路由视图：列表页走 keep-alive 缓存，详情页直接渲染，并保留切换动画 -->
        <router-view v-slot="{ Component, route }">
            <transition name="forum-slide" mode="out-in">
                <keep-alive include="TopicList,ActivityList,NoticeTopicList">
                    <component
                            v-if="Component"
                            :is="Component"
                            :key="resolveRouteKey(route)"/>
                </keep-alive>
            </transition>
        </router-view>
        <!-- 回到顶部按钮（绑定到 IndexView 的滚动容器） -->
        <el-backtop target=".main-content-page .el-scrollbar__wrap" :right="20" :bottom="70"/>
    </div>
</template>

<script setup>
import {get} from "@/net";
import {useStore} from "@/stores/index";

/** Pinia 全局状态 */
const store = useStore()

/**
 * 判断当前论坛子路由是否需要被 keep-alive 缓存
 *
 * @param route 当前匹配到的子路由
 * @return true 表示缓存列表页，false 表示详情页直接重新渲染
 */
function shouldKeepAlive(route) {
    return ['topic-list', 'activity-list', 'notice-topic-list'].includes(route?.name)
}

/**
 * 生成论坛子路由组件的稳定 key
 *
 * @param route 当前匹配到的子路由
 * @return 列表页按路由名缓存，详情页按完整路径重新渲染
 */
function resolveRouteKey(route) {
    return shouldKeepAlive(route) ? route?.name : route?.fullPath
}

/**
 * 加载论坛分类列表，在头部插入"全部"选项后存入全局状态
 * 分类数据供 TopicList 等子页面的分类筛选功能使用
 */
get('/api/forum/types', data => {
    const array = []
    array.push({name: '全部', id: 0, color: 'linear-gradient(45deg, white, red, orange, gold, green, blue)'})
    data.forEach(d => array.push(d))
    store.forum.types = array
})
</script>

<style scoped>
.forum-slide-enter-active,
.forum-slide-leave-active {
    transition: opacity 0.22s ease, transform 0.22s ease;
}

.forum-slide-enter-from {
    opacity: 0;
    transform: translateX(22px);
}

.forum-slide-leave-to {
    opacity: 0;
    transform: translateX(-22px);
}
</style>

<style>
/* 回到顶部按钮增强样式 */
.el-backtop {
    width: 48px !important;
    height: 48px !important;
    background-color: var(--el-color-primary) !important;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15) !important;
    transition: transform 0.25s ease, box-shadow 0.25s ease !important;
}

.el-backtop:hover {
    transform: scale(1.1) !important;
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.25) !important;
    background-color: var(--el-color-primary-light-3) !important;
}

.el-backtop .el-icon {
    color: #fff !important;
    font-size: 20px !important;
}
</style>
