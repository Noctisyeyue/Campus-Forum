<template>
    <div style="display: flex;margin: 20px auto;gap: 20px;max-width: 980px;padding: 0 20px">
        <div style="flex: 1">
            <light-card v-if="topics.search" style="display: flex;align-items: center;justify-content: space-between">
                <span style="font-size: 14px;color: #606266">
                    搜索: <b>{{ topics.search }}</b>
                </span>
                <el-link type="info" :underline="false" @click="clearSearch">清除搜索</el-link>
            </light-card>
            <light-card>
                <div style="display: flex;justify-content: space-between;align-items: center;gap: 10px;flex-wrap: wrap">
                    <div>
                        <div style="font-size: 20px;font-weight: bold">教务通知</div>
                        <div class="text-secondary" style="font-size: 13px;margin-top: 4px">
                            教务通知由管理员直接发布，支持点赞和收藏，但不开放评论。
                        </div>
                    </div>
                    <el-button plain @click="router.push('/index')">返回帖子广场</el-button>
                </div>
            </light-card>

            <div style="margin-top: 10px;display: flex;flex-direction: column;gap: 10px" v-infinite-scroll="updateList">
                <light-card v-for="item in topics.list" :key="item.id" class="topic-card"
                            @click="openTopicDetail(item.id)">
                    <div style="display: flex;justify-content: space-between;gap: 12px;align-items: flex-start;flex-wrap: wrap">
                        <div style="flex: 1;min-width: 0">
                            <div style="display: flex;align-items: center;gap: 8px;flex-wrap: wrap">
                                <topic-tag :type="item.type"/>
                                <span style="font-weight: bold">{{ item.title }}</span>
                            </div>
                            <div class="topic-content">{{ item.text }}</div>
                        </div>
                        <div class="text-secondary" style="font-size: 12px">
                            {{ new Date(item.time).toLocaleString() }}
                        </div>
                    </div>
                    <div style="display: flex;gap: 20px;font-size: 13px;margin-top: 12px;opacity: 0.8">
                        <div>
                            <el-icon style="vertical-align: middle"><CircleCheck/></el-icon> {{item.like}}点赞
                        </div>
                        <div>
                            <el-icon style="vertical-align: middle"><Star/></el-icon> {{item.collect}}收藏
                        </div>
                    </div>
                </light-card>
            </div>

            <light-card v-if="!topics.list.length && topics.end" style="margin-top: 10px">
                <el-empty :image-size="90" :description="topics.search ? '没有找到相关通知' : '暂时没有教务通知'"/>
            </light-card>
        </div>

        <div style="width: 280px">
            <div style="position: sticky;top: 20px">
                <light-card>
                    <div style="font-weight: bold">通知规则</div>
                    <el-divider style="margin: 10px 0"/>
                    <div class="text-secondary" style="font-size: 14px;line-height: 1.8">
                        教务通知详情页不显示评论区，避免把正式通知混入讨论流。
                    </div>
                </light-card>
            </div>
        </div>
    </div>
</template>

<script setup>
defineOptions({ name: 'NoticeTopicList' })

import { onActivated, onDeactivated, reactive, watch } from "vue";
import { useRoute } from "vue-router";
import { CircleCheck, Star } from "@element-plus/icons-vue";
import { get } from "@/net";
import router from "@/router";
import LightCard from "@/components/LightCard.vue";
import TopicTag from "@/components/TopicTag.vue";
import {restoreForumScroll, saveForumScroll} from "@/utils/forumScroll";

const route = useRoute()

const topics = reactive({
    list: [],
    page: 0,
    end: false,
    loading: false,
    search: ''
})

watch(() => route.query.search, val => {
    topics.search = val || ''
    resetList()
}, { immediate: true })

onActivated(() => restoreForumScroll('/index/notice-topic'))

onDeactivated(() => saveForumScroll('/index/notice-topic'))

function updateList() {
    if (topics.end || topics.loading) return
    topics.loading = true
    const page = topics.page
    let url = `/api/forum/list-notice-topic?page=${page}`
    if (topics.search) url += `&title=${encodeURIComponent(topics.search)}`
    get(url, data => {
        if (data) {
            const existIds = new Set(topics.list.map(item => item.id))
            data.forEach(item => {
                if (!existIds.has(item.id)) {
                    topics.list.push(item)
                }
            })
            topics.page = page + 1
        }
        if (!data || data.length < 10) {
            topics.end = true
        }
        topics.loading = false
    }, () => {
        topics.loading = false
    })
}

function openTopicDetail(id) {
    saveForumScroll('/index/notice-topic')
    router.push(`/index/topic-detail/${id}`)
}

function resetList() {
    topics.page = 0
    topics.end = false
    topics.list = []
    updateList()
}

function clearSearch() {
    topics.search = ''
    router.replace({ path: '/index/notice-topic' })
}
</script>

<style scoped lang="less">
.topic-card {
    padding: 16px;
    transition: .3s;

    &:hover {
        cursor: pointer;
        transform: translateY(-2px);
    }
}

.topic-content {
    font-size: 13px;
    color: grey;
    margin-top: 10px;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 3;
    overflow: hidden;
    text-overflow: ellipsis;
}

:global(.dark) .topic-content {
    color: #a0a3a8;
}
</style>
