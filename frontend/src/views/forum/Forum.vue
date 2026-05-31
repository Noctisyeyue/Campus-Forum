<template>
    <div>
        <!-- 路由视图：带滑动过渡动画 + keep-alive 缓存指定页面 -->
        <router-view v-slot="{ Component }">
            <transition name="forum-slide" mode="out-in">
                <keep-alive include="TopicList,ActivityList,NoticeTopicList">
                    <component :is="Component"/>
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
/* 路由切换时的滑动淡入淡出动画 */
.forum-slide-enter-active,
.forum-slide-leave-active {
    transition: opacity 0.25s ease, transform 0.25s ease;
}
.forum-slide-enter-from {
    opacity: 0;
    transform: translateX(30px);
}
.forum-slide-leave-to {
    opacity: 0;
    transform: translateX(-30px);
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
