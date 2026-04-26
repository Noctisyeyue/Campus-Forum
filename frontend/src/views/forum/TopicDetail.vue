<template>
    <div v-if="notFound" style="text-align: center;padding: 80px 20px">
        <el-empty description="帖子不存在或已被下架"/>
        <el-button type="primary" @click="router.push('/index')" style="margin-top: 10px">返回首页</el-button>
    </div>

    <div class="topic-page" v-else-if="topic.data">
        <div class="topic-main" style="position: sticky;top: 0;z-index: 10">
            <card style="display: flex;width: 100%;">
                <el-button :icon="ArrowLeft" type="info" size="small"
                           plain round @click="router.back()">返回列表</el-button>
                <div style="text-align: center;flex: 1">
                    <topic-tag :type="topic.data.type"/>
                    <span style="font-weight: bold;margin-left: 5px">{{ topic.data.title }}</span>
                </div>
            </card>
        </div>

        <div class="topic-main">
            <div class="topic-main-left">
                <el-avatar :src="store.avatarUserUrl(topic.data.user.avatar)" :size="60"/>
                <div>
                    <div style="font-size: 18px;font-weight: bold">
                        {{ topic.data.user.username }}
                        <span style="color: hotpink" v-if="topic.data.user.gender === 1">
                            <el-icon><Female/></el-icon>
                        </span>
                        <span style="color: dodgerblue" v-if="topic.data.user.gender === 0">
                            <el-icon><Male/></el-icon>
                        </span>
                    </div>
                    <div class="desc">{{ topic.data.user.email }}</div>
                </div>
                <el-divider style="margin: 10px 0"/>
                <div style="text-align: left;margin: 0 5px">
                    <div class="desc">微信号: {{ topic.data.user.wx || '已隐藏或未填写' }}</div>
                    <div class="desc">QQ号: {{ topic.data.user.qq || '已隐藏或未填写' }}</div>
                    <div class="desc">手机号: {{ topic.data.user.phone || '已隐藏或未填写' }}</div>
                </div>
                <el-divider style="margin: 10px 0"/>
                <div class="desc" style="margin: 0 5px">{{ topic.data.user.desc }}</div>
            </div>

            <div class="topic-main-right">
                <div v-if="topic.data.activityTime || topic.data.location || topic.data.organizer" class="activity-box">
                    <div class="activity-item">
                        <span>活动时间</span>
                        <span>{{ topic.data.activityTime ? new Date(topic.data.activityTime).toLocaleString() : '待补充' }}</span>
                    </div>
                    <div class="activity-item">
                        <span>活动地点</span>
                        <span>{{ topic.data.location || '待补充' }}</span>
                    </div>
                    <div class="activity-item">
                        <span>主办方</span>
                        <span>{{ topic.data.organizer || '待补充' }}</span>
                    </div>
                    <div class="activity-item" v-if="topic.data.signupDeadline">
                        <span>报名截止</span>
                        <span>{{ new Date(topic.data.signupDeadline).toLocaleString() }}</span>
                    </div>
                </div>

                <div class="topic-content" v-html="convertToHtml(topic.data.content)"></div>
                <el-divider/>
                <div style="font-size: 13px;color: grey;text-align: center">
                    <div>发帖时间: {{ new Date(topic.data.time).toLocaleString() }}</div>
                </div>
                <div style="text-align: right;margin-top: 30px">
                    <el-popconfirm title="确定要删除这篇帖子吗？删除后将进入“已删除”状态。"
                                   confirm-button-text="确定"
                                   cancel-button-text="取消"
                                   @confirm="deleteTopic"
                                   v-if="store.user.id === topic.data.user.id">
                        <template #reference>
                            <el-link :icon="Delete" type="danger" style="margin-right: 20px">&nbsp;删除帖子</el-link>
                        </template>
                    </el-popconfirm>
                    <el-link :icon="EditPen" type="primary" @click="edit = true"
                             style="margin-right: 20px"
                             v-if="store.user.id === topic.data.user.id">&nbsp;编辑帖子</el-link>
                    <el-link :icon="CircleCheck" :type="topic.like ? 'primary' : 'info'"
                             @click="interact('like', '点赞')">
                        &nbsp;{{ topic.like ? '已点赞' : '点个赞吧' }}
                    </el-link>
                    <el-link :icon="Star" :type="topic.collect ? 'warning' : 'info'"
                             @click="interact('collect', '收藏')"
                             style="margin-left: 20px">
                        &nbsp;{{ topic.collect ? '已收藏' : '收藏本帖' }}
                    </el-link>
                </div>
            </div>
        </div>

        <light-card v-if="!topic.commentEnabled" class="comment-disabled">
            教务通知不开放评论。
        </light-card>

        <transition name="el-fade-in-linear" mode="out-in">
            <div v-if="topic.commentEnabled && topic.comments">
                <div class="topic-main" style="margin-top: 10px" v-for="item in topic.comments" :key="item.id">
                    <div class="topic-main-left">
                        <el-avatar :src="store.avatarUserUrl(item.user.avatar)" :size="60"/>
                        <div>
                            <div style="font-size: 18px;font-weight: bold">
                                {{ item.user.username }}
                                <span style="color: hotpink" v-if="item.user.gender === 1">
                                    <el-icon><Female/></el-icon>
                                </span>
                                <span style="color: dodgerblue" v-if="item.user.gender === 0">
                                    <el-icon><Male/></el-icon>
                                </span>
                            </div>
                            <div class="desc">{{ item.user.email }}</div>
                        </div>
                        <el-divider style="margin: 10px 0"/>
                        <div style="text-align: left;margin: 0 5px">
                            <div class="desc">微信号: {{ item.user.wx || '已隐藏或未填写' }}</div>
                            <div class="desc">QQ号: {{ item.user.qq || '已隐藏或未填写' }}</div>
                            <div class="desc">手机号: {{ item.user.phone || '已隐藏或未填写' }}</div>
                        </div>
                    </div>
                    <div class="topic-main-right">
                        <div style="font-size: 13px;color: grey">
                            <div>评论时间: {{ new Date(item.time).toLocaleString() }}</div>
                        </div>
                        <div v-if="item.quote" class="comment-quote">
                            回复: {{ item.quote }}
                        </div>
                        <div class="topic-content" v-html="convertToHtml(item.content)"></div>
                        <div style="text-align: right">
                            <el-link :icon="ChatSquare"
                                     @click="comment.show = true; comment.quote = item"
                                     type="info">&nbsp;回复评论</el-link>
                            <el-popconfirm title="确定要删除这条评论吗？"
                                           confirm-button-text="确定"
                                           cancel-button-text="取消"
                                           @confirm="deleteComment(item.id)"
                                           v-if="item.user.id === store.user.id">
                                <template #reference>
                                    <el-link :icon="Delete" type="danger" style="margin-left: 20px">&nbsp;删除评论</el-link>
                                </template>
                            </el-popconfirm>
                        </div>
                    </div>
                </div>

                <div style="width: fit-content;margin: 20px auto">
                    <el-pagination background layout="prev, pager, next"
                                   v-model:current-page="topic.page"
                                   @current-change="loadComments"
                                   :total="topic.data.comments"
                                   :page-size="10"
                                   hide-on-single-page/>
                </div>
            </div>
        </transition>

        <topic-editor :show="edit"
                      @close="edit = false"
                      v-if="topic.data && store.forum.types"
                      :default-type="topic.data.type"
                      :default-text="topic.data.content"
                      :default-title="topic.data.title"
                      submit-button="更新帖子内容"
                      :submit="updateTopic"/>

        <topic-comment-editor v-if="topic.commentEnabled"
                              :show="comment.show"
                              @close="comment.show = false"
                              :tid="String(tid)"
                              :quote="comment.quote"
                              @comment="onCommentAdd"/>

        <div v-if="topic.commentEnabled" class="add-comment" @click="comment.show = true; comment.quote = null">
            <el-icon><Plus/></el-icon>
        </div>
    </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { QuillDeltaToHtmlConverter } from "quill-delta-to-html";
import { get, post } from "@/net";
import router from "@/router";
import { useStore } from "@/stores/index";
import Card from "@/components/Card.vue";
import LightCard from "@/components/LightCard.vue";
import TopicTag from "@/components/TopicTag.vue";
import TopicEditor from "@/components/TopicEditor.vue";
import TopicCommentEditor from "@/components/TopicCommentEditor.vue";
import { ElMessage } from "element-plus";
import { ArrowLeft, ChatSquare, CircleCheck, Delete, EditPen, Female, Male, Plus, Star } from "@element-plus/icons-vue";

const route = useRoute()
const store = useStore()

const tid = computed(() => Number(route.params.tid))
const notFound = ref(false)
const edit = ref(false)

const topic = reactive({
    data: null,
    like: false,
    collect: false,
    comments: null,
    page: 1,
    commentEnabled: true
})

const comment = reactive({
    show: false,
    quote: null
})

function resetTopicState() {
    topic.data = null
    topic.like = false
    topic.collect = false
    topic.comments = null
    topic.page = 1
    topic.commentEnabled = true
}

function init(id) {
    if (!id) {
        resetTopicState()
        notFound.value = true
        return
    }
    resetTopicState()
    notFound.value = false
    get(`/api/forum/topic?tid=${id}`, data => {
        if (!data) {
            resetTopicState()
            notFound.value = true
            return
        }
        topic.data = data
        topic.like = !!data.interact?.like
        topic.collect = !!data.interact?.collect
        topic.commentEnabled = data.allowComment !== false
        if (topic.commentEnabled) {
            loadComments(1)
        } else {
            topic.comments = []
        }
    }, () => {
        resetTopicState()
        notFound.value = true
    })
}

watch(() => route.params.tid, value => init(Number(value)), { immediate: true })

function convertToHtml(content) {
    if (!content) return ''
    try {
        const ops = JSON.parse(content).ops || []
        return new QuillDeltaToHtmlConverter(ops, { inlineStyles: true }).convert()
    } catch (e) {
        console.error(e)
        return ''
    }
}

function interact(type, message) {
    get(`/api/forum/interact?tid=${tid.value}&type=${type}&state=${!topic[type]}`, () => {
        topic[type] = !topic[type]
        ElMessage.success(topic[type] ? `${message}成功！` : `已取消${message}！`)
    })
}

function updateTopic(editor, success) {
    post('/api/forum/update-topic', {
        id: tid.value,
        type: editor.type.id,
        title: editor.title,
        content: editor.text
    }, () => {
        success()
        edit.value = false
        ElMessage.success('帖子已更新，等待管理员审核！')
        router.push('/index/my-topics')
    })
}

function loadComments(page) {
    if (!topic.commentEnabled) {
        topic.comments = []
        return
    }
    topic.comments = null
    topic.page = page
    get(`/api/forum/comments?tid=${tid.value}&page=${page - 1}`, data => topic.comments = data || [])
}

function onCommentAdd() {
    comment.show = false
    topic.data.comments += 1
    loadComments(Math.floor((topic.data.comments - 1) / 10) + 1)
}

function deleteComment(id) {
    get(`/api/forum/delete-comment?id=${id}`, () => {
        ElMessage.success('删除评论成功！')
        topic.data.comments = Math.max((topic.data.comments || 1) - 1, 0)
        loadComments(topic.page)
    })
}

function deleteTopic() {
    post(`/api/forum/delete-topic?tid=${tid.value}`, null, () => {
        ElMessage.success('帖子已删除')
        router.push('/index/my-topics')
    })
}
</script>

<style scoped lang="less">
.comment-quote {
    font-size: 13px;
    color: grey;
    background-color: rgba(94, 94, 94, 0.1);
    padding: 10px;
    margin-top: 10px;
    border-radius: 5px;
}

.comment-disabled {
    width: 800px;
    margin: 0 auto;
    color: grey;
}

.activity-box {
    background: #f8fafc;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 12px 14px;
    margin-bottom: 14px;
}

.activity-item {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    font-size: 13px;
    color: #606266;
    line-height: 1.9;
}

.add-comment {
    position: fixed;
    bottom: 20px;
    right: 20px;
    width: 40px;
    height: 40px;
    border-radius: 50%;
    font-size: 18px;
    color: var(--el-color-primary);
    text-align: center;
    line-height: 45px;
    background: var(--el-bg-color-overlay);
    box-shadow: var(--el-box-shadow-lighter);

    &:hover {
        background: var(--el-border-color-extra-light);
        cursor: pointer;
    }
}

.topic-page {
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding: 10px 0;
}

.topic-main {
    display: flex;
    border-radius: 7px;
    margin: 0 auto;
    background-color: var(--el-bg-color);
    width: 800px;

    .topic-main-left {
        width: 200px;
        padding: 10px;
        text-align: center;
        border-right: solid 1px var(--el-border-color);

        .desc {
            font-size: 12px;
            color: grey;
        }
    }

    .topic-main-right {
        width: 600px;
        padding: 10px 20px;
        display: flex;
        flex-direction: column;

        .topic-content {
            font-size: 14px;
            line-height: 22px;
            opacity: 0.8;
            flex: 1;
        }
    }
}
</style>
