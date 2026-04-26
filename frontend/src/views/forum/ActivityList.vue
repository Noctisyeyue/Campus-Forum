<template>
    <div style="display: flex;margin: 20px auto;gap: 20px;max-width: 980px;padding: 0 20px">
        <div style="flex: 1">
            <light-card>
                <div style="display: flex;justify-content: space-between;align-items: center;gap: 10px;flex-wrap: wrap">
                    <div>
                        <div style="font-size: 20px;font-weight: bold">校园活动</div>
                        <div style="font-size: 13px;color: grey;margin-top: 4px">
                            这里展示管理员发布的校园活动，可查看活动时间、地点和详情。
                        </div>
                    </div>
                    <el-button plain @click="router.push('/index')">返回帖子广场</el-button>
                </div>
            </light-card>

            <div style="margin-top: 10px;display: flex;flex-direction: column;gap: 10px" v-infinite-scroll="updateList">
                <light-card v-for="item in topics.list" :key="item.id" class="topic-card"
                            @click="openTopicDetail(item.id)">
                    <div style="display: flex;justify-content: space-between;gap: 12px;flex-wrap: wrap">
                        <div>
                            <div style="display: flex;align-items: center;gap: 8px;flex-wrap: wrap">
                                <topic-tag :type="item.type"/>
                                <span style="font-weight: bold">{{ item.title }}</span>
                            </div>
                            <div style="font-size: 12px;color: grey;margin-top: 6px">
                                发布时间 {{ new Date(item.time).toLocaleString() }}
                            </div>
                        </div>
                        <div class="activity-meta">
                            <div>活动时间：{{ item.activityTime ? new Date(item.activityTime).toLocaleString() : '待补充' }}</div>
                            <div>活动地点：{{ item.location || '待补充' }}</div>
                        </div>
                    </div>
                    <div class="topic-content">{{ item.text }}</div>
                </light-card>

                <light-card v-if="!topics.list.length && topics.end">
                    <el-empty :image-size="90" description="暂时没有校园活动"/>
                </light-card>
            </div>
        </div>

        <div style="width: 280px">
            <div style="position: sticky;top: 20px">
                <light-card>
                    <div style="font-weight: bold">浏览说明</div>
                    <el-divider style="margin: 10px 0"/>
                    <div style="font-size: 14px;color: grey;line-height: 1.8">
                        校园活动支持点赞、收藏和评论，进入详情页可查看主办方与报名截止时间。
                    </div>
                </light-card>
            </div>
        </div>
    </div>
</template>

<script setup>
defineOptions({ name: 'ActivityList' })

import { reactive } from "vue";
import { get } from "@/net";
import router from "@/router";
import LightCard from "@/components/LightCard.vue";
import TopicTag from "@/components/TopicTag.vue";

const topics = reactive({
    list: [],
    page: 0,
    end: false,
    loading: false
})

function updateList() {
    if (topics.end || topics.loading) return
    topics.loading = true
    const page = topics.page
    get(`/api/forum/list-activity?page=${page}`, data => {
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
    router.push(`/index/topic-detail/${id}`)
}

updateList()
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

.activity-meta {
    font-size: 12px;
    color: grey;
    text-align: right;
    line-height: 1.8;
}
</style>
