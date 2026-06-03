<template>
    <el-drawer :model-value="show" @close="emit('close')" @open="init" title="我的帖子收藏列表">
        <div class="collect-list">
            <light-card v-for="(item, index) in list" class="topic-card"
                        @click="router.push(`/index/topic-detail/${item.id}`)">
                <topic-tag :type="item.type"/>
                <div class="title">
                    <b>{{item.title}}</b>
                </div>
                <el-link type="danger" @click.stop="deleteCollect(index, item.id)">删除</el-link>
            </light-card>
        </div>
    </el-drawer>
</template>

<script setup>
import {get} from "@/net";
import {ref} from "vue";
import LightCard from "@/components/LightCard.vue";
import router from "@/router";
import TopicTag from "@/components/TopicTag.vue";
import {ElMessage} from "element-plus";

/** 组件属性定义 */
defineProps({
    /** 是否显示收藏列表抽屉 */
    show: Boolean
})

/** 声明组件事件 */
const emit = defineEmits(['close'])

/** 收藏的帖子列表 */
const list = ref([])

/**
 * 加载收藏列表，抽屉打开时调用
 */
function init() {
    get('/api/forum/collects', data => list.value = data)
}

/**
 * 取消收藏指定帖子，成功后从列表中移除
 *
 * @param index 帖子在列表中的下标
 * @param tid   帖子ID
 */
function deleteCollect(index, tid) {
    get(`/api/forum/interact?tid=${tid}&type=collect&state=false`, () => {
        ElMessage.success('已取消收藏！')
        list.value.splice(index, 1)
    })
}
</script>

<style scoped>
.collect-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.topic-card {
    background-color: rgba(128, 128, 128, 0.2);
    transition: .3s;
    display: flex;
    justify-content: space-between;

    .title {
        margin-left: 5px;
        font-size: 14px;
        flex: 1;
        white-space: nowrap;
        display: block;
        overflow: hidden;
        text-overflow: ellipsis;
    }

    &:hover {
        scale: 1.02;
        cursor: pointer;
    }
}
</style>
