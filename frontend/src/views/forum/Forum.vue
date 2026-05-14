<template>
    <div>
        <router-view v-slot="{ Component }">
            <transition name="forum-slide" mode="out-in">
                <keep-alive include="TopicList,ActivityList,NoticeTopicList">
                    <component :is="Component"/>
                </keep-alive>
            </transition>
        </router-view>
        <el-backtop target=".main-content-page .el-scrollbar__wrap" :right="20" :bottom="70"/>
    </div>
</template>

<script setup>
import {get} from "@/net";
import {useStore} from "@/stores/index";

const store = useStore()

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
