<template>
    <div>
        <!-- 帖子不存在或已下架的空状态 -->
        <div v-if="notFound" style="text-align: center;padding: 80px 20px">
            <el-empty description="帖子不存在或已被下架"/>
            <el-button type="primary" @click="router.push('/index')" style="margin-top: 10px">返回首页</el-button>
        </div>

        <!-- 帖子详情主内容 -->
        <div class="topic-page" v-else-if="topic.data">
        <!-- 顶部粘性导航栏：返回按钮 + 分类标签 + 帖子标题 -->
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

        <!-- 帖子内容区：左侧作者信息 + 右侧帖子正文 -->
        <div class="topic-main">
            <!-- 左侧：作者头像、昵称、性别、联系方式、个人简介 -->
            <div class="topic-main-left">
                <el-avatar :src="store.avatarUserUrl(topic.data.user.avatar)" :size="60"
                           :style="!topic.data.user.avatar ? { background: store.avatarColor(topic.data.user.username), fontSize: '24px' } : {}">
                    {{ topic.data.user.avatar ? '' : (topic.data.user.username?.[0] || '') }}
                </el-avatar>
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

            <!-- 右侧：活动信息、帖子正文、发帖时间、操作按钮 -->
            <div class="topic-main-right">
                <!-- 活动信息卡片（仅活动类型帖子显示） -->
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

                <!-- 帖子正文（Quill Delta 转 HTML 渲染） -->
                <div class="topic-content" v-html="convertToHtml(topic.data.content)"></div>
                <el-divider/>
                <div class="text-secondary" style="font-size: 13px;text-align: center">
                    <div>发帖时间: {{ new Date(topic.data.time).toLocaleString() }} &nbsp;&nbsp; 浏览量: {{ topic.data.viewCount }}</div>
                </div>
                <!-- 操作按钮：删除、编辑（仅作者可见）、点赞、收藏、举报（非作者可见） -->
                <div style="text-align: right;margin-top: 30px">
                    <el-popconfirm title="确定要删除这篇帖子吗？删除后将进入'已删除'状态。"
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
                    <el-link :icon="Warning" type="danger"
                             @click="openReport('topic', topic.data.id)"
                             style="margin-left: 20px"
                             v-if="store.user.id !== topic.data.user.id">&nbsp;举报</el-link>
                </div>
            </div>
        </div>

        <!-- 教务通知不开放评论的提示 -->
        <light-card v-if="!topic.commentEnabled" class="comment-disabled">
            教务通知不开放评论。
        </light-card>

        <!-- 评论列表（带淡入淡出过渡动画） -->
        <transition name="el-fade-in-linear" mode="out-in">
            <div v-if="topic.commentEnabled && topic.comments">
                <div class="topic-main" style="margin-top: 10px" v-for="item in topic.comments" :key="item.id">
                    <!-- 评论左侧：评论者头像、昵称、性别、联系方式 -->
                    <div class="topic-main-left">
                        <el-avatar :src="store.avatarUserUrl(item.user.avatar)" :size="60"
                                   :style="!item.user.avatar ? { background: store.avatarColor(item.user.username), fontSize: '24px' } : {}">
                            {{ item.user.avatar ? '' : (item.user.username?.[0] || '') }}
                        </el-avatar>
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
                    <!-- 评论右侧：评论时间、回复引用、评论正文、操作按钮 -->
                    <div class="topic-main-right">
                        <div class="text-secondary" style="font-size: 13px">
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
                            <el-link :icon="Warning" type="danger"
                                     @click="openReport('comment', item.id)"
                                     style="margin-left: 20px"
                                     v-if="item.user.id !== store.user.id">&nbsp;举报</el-link>
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

                <!-- 评论分页（超过一页时显示） -->
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

        <!-- 帖子编辑器弹窗（仅作者点击编辑后显示） -->
        <topic-editor :show="edit"
                      @close="edit = false"
                      v-if="topic.data && store.forum.types"
                      :default-type="topic.data.type"
                      :default-text="topic.data.content"
                      :default-title="topic.data.title"
                      submit-button="更新帖子内容"
                      :submit="updateTopic"/>

        <!-- 评论编辑器弹窗 -->
        <topic-comment-editor v-if="topic.commentEnabled"
                              :show="comment.show"
                              @close="comment.show = false"
                              :tid="String(tid)"
                              :quote="comment.quote"
                              @comment="onCommentAdd"/>

        <!-- 举报对话框 -->
        <el-dialog v-model="report.show" title="举报" width="420px" @close="report.reason = ''; report.detail = ''">
            <div style="margin-bottom: 15px">
                <div style="margin-bottom: 8px;font-weight: bold">举报原因</div>
                <el-radio-group v-model="report.reason">
                    <el-radio value="垃圾广告">垃圾广告</el-radio>
                    <el-radio value="虚假信息">虚假信息</el-radio>
                    <el-radio value="违规内容">违规内容</el-radio>
                    <el-radio value="人身攻击">人身攻击</el-radio>
                    <el-radio value="色情低俗">色情低俗</el-radio>
                    <el-radio value="其他">其他</el-radio>
                </el-radio-group>
            </div>
            <div>
                <div style="margin-bottom: 8px;font-weight: bold">补充说明（可选）</div>
                <el-input type="textarea" v-model="report.detail" :rows="3" placeholder="请描述具体情况" maxlength="500"/>
            </div>
            <template #footer>
                <el-button @click="report.show = false">取消</el-button>
                <el-button type="danger" @click="submitReport" :disabled="!report.reason">提交举报</el-button>
            </template>
        </el-dialog>

            <!-- 右下角悬浮写评论按钮 -->
            <div v-if="topic.commentEnabled" class="add-comment" @click="comment.show = true; comment.quote = null">
                <el-icon><Plus/></el-icon>
            </div>
        </div>
    </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from "vue";
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
import { ArrowLeft, ChatSquare, CircleCheck, Delete, EditPen, Female, Male, Plus, Star, Warning } from "@element-plus/icons-vue";

/** 路由实例 */
const route = useRoute()
/** Pinia 全局状态 */
const store = useStore()

/** 当前帖子 ID（从路由参数 /index/topic-detail/:tid 获取） */
const tid = computed(() => Number(route.params.tid))
/** 帖子是否不存在或已下架 */
const notFound = ref(false)
/** 是否显示帖子编辑器 */
const edit = ref(false)

/** 帖子详情响应式状态 */
const topic = reactive({
    /** 帖子详情数据（包含标题、内容、作者、互动信息等） */
    data: null,
    /** 当前用户是否已点赞 */
    like: false,
    /** 当前用户是否已收藏 */
    collect: false,
    /** 评论列表 */
    comments: null,
    /** 当前评论页码 */
    page: 1,
    /** 是否允许评论（教务通知类帖子禁用评论） */
    commentEnabled: true
})

/** 评论编辑器状态 */
const comment = reactive({
    /** 是否显示评论编辑器 */
    show: false,
    /** 回复目标评论（null 为顶级评论） */
    quote: null
})

/** 举报对话框状态 */
const report = reactive({
    /** 是否显示举报对话框 */
    show: false,
    /** 举报目标类型：topic 或 comment */
    targetType: '',
    /** 举报目标 ID */
    targetId: null,
    /** 举报原因 */
    reason: '',
    /** 补充说明 */
    detail: ''
})

/**
 * 将主内容区滚动到顶部，避免进入详情页时沿用列表页的滚动位置
 */
function scrollMainContentToTop() {
    const wrap = document.querySelector('.main-content-page .el-scrollbar__wrap')
    if (wrap) {
        wrap.scrollTop = 0
    }
}

/**
 * 打开举报对话框，设置目标信息并重置表单
 *
 * @param type 举报目标类型（'topic' 或 'comment'）
 * @param id   举报目标 ID
 */
function openReport(type, id) {
    report.targetType = type
    report.targetId = id
    report.reason = ''
    report.detail = ''
    report.show = true
}

/**
 * 提交举报请求
 */
function submitReport() {
    post('/api/forum/report', {
        targetType: report.targetType,
        targetId: report.targetId,
        reason: report.reason,
        detail: report.detail
    }, () => {
        report.show = false
        ElMessage.success('举报已提交，感谢您的反馈')
    })
}

/**
 * 重置帖子详情状态（用于切换帖子或加载失败时清空旧数据）
 */
function resetTopicState() {
    topic.data = null
    topic.like = false
    topic.collect = false
    topic.comments = null
    topic.page = 1
    topic.commentEnabled = true
}

/**
 * 初始化帖子详情：请求后端获取帖子数据、互动状态和评论列表
 *
 * @param id 帖子 ID，无效时直接显示 notFound
 */
function init(id) {
    if (!id) {
        resetTopicState()  // 清空所有状态
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
        topic.data = data                                        // 存帖子数据
        topic.like = !!data.interact?.like                       // 存点赞状态
        topic.collect = !!data.interact?.collect                 // 存收藏状态
        topic.commentEnabled = data.allowComment !== false
        if (topic.commentEnabled) {
            loadComments(1)    // 加载第1页评论
        } else {
            topic.comments = []
        }
    }, () => {
        resetTopicState()
        notFound.value = true
    })
}

/** 监听路由参数变化，切换帖子时重新加载详情（immediate: true 确保首次进入也加载） */
watch(() => route.params.tid, value => init(Number(value)), { immediate: true })

/** 详情页挂载后将主滚动容器置顶 */
onMounted(() => nextTick(scrollMainContentToTop))

/**
 * 将 Quill Delta JSON 字符串转换为 HTML 用于渲染富文本内容
 *
 * @param content Quill Delta 格式的 JSON 字符串
 * @return 转换后的 HTML 字符串，解析失败返回空字符串
 */
function convertToHtml(content) {
    if (!content) return ''            // 没内容就返回空
    try {
        const ops = JSON.parse(content).ops || []               // 从 JSON 字符串中取出 ops 数组
        return new QuillDeltaToHtmlConverter(ops, { inlineStyles: true }).convert()
    } catch (e) {
        console.error(e)
        return ''
    }
}

/**
 * 切换互动状态（点赞/收藏），请求后端后更新本地状态
 *
 * @param type    互动类型（'like' 或 'collect'）
 * @param message 操作名称（用于提示消息）
 */
function interact(type, message) {
    get(`/api/forum/interact?tid=${tid.value}&type=${type}&state=${!topic[type]}`, () => {
        topic[type] = !topic[type]
        ElMessage.success(topic[type] ? `${message}成功！` : `已取消${message}！`)
    })
}

/**
 * 提交帖子更新，成功后跳转到"我的帖子"页面（更新后需重新审核）
 *
 * @param editor 编辑器数据（包含 type、title、text）
 * @param success 编辑器关闭回调
 */
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

/**
 * 加载指定页码的评论列表
 *
 * @param page 页码（从 1 开始，请求时转为 0 开始）
 */
function loadComments(page) {
    if (!topic.commentEnabled) {
        topic.comments = []
        return
    }
    topic.comments = null
    topic.page = page
    get(`/api/forum/comments?tid=${tid.value}&page=${page - 1}`, data => topic.comments = data || [])
}

/**
 * 评论添加成功回调：关闭编辑器、更新评论总数、跳转到新评论所在页
 */
function onCommentAdd() {
    comment.show = false
    topic.data.comments += 1
    loadComments(Math.floor((topic.data.comments - 1) / 10) + 1)
}

/**
 * 删除评论，成功后更新评论总数并刷新当前页
 *
 * @param id 评论 ID
 */
function deleteComment(id) {
    get(`/api/forum/delete-comment?id=${id}`, () => {
        ElMessage.success('删除评论成功！')
        topic.data.comments = Math.max((topic.data.comments || 1) - 1, 0)
        loadComments(topic.page)
    })
}

/**
 * 删除帖子（软删除），成功后跳转到"我的帖子"页面
 */
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
    background: var(--el-fill-color-light);
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    padding: 12px 14px;
    margin-bottom: 14px;
}

.activity-item {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    font-size: 13px;
    color: var(--el-text-color-regular);
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

            :deep(img) {
                max-width: 100%;
                height: auto;
            }
        }
    }
}

:global(.dark) .comment-quote {
    color: #909399;
    background-color: rgba(255, 255, 255, 0.05);
}

:global(.dark) .comment-disabled {
    color: #909399;
}

:global(.dark) .topic-main .topic-main-left .desc {
    color: #909399;
}
</style>
