<template>
    <div class="admin-page" v-loading="loading">
        <div class="admin-page-header">
            <div class="admin-page-title">帖子详情</div>
            <el-button :icon="ArrowLeft" size="small" plain round @click="router.back()">返回列表</el-button>
        </div>
        <div class="admin-form-card" v-if="topic">
            <div style="display: flex;gap: 10px;margin-bottom: 15px;flex-wrap: wrap">
                <el-button v-if="topic.status === 'pending_review'" type="success"
                           @click="doAction('approve')">审核通过</el-button>
                <el-button v-if="topic.status === 'pending_review'" type="danger"
                           @click="rejectVisible = true">拒绝</el-button>
                <el-button v-if="topic.status === 'published'" type="warning"
                           @click="hideVisible = true">下架帖子</el-button>
                <el-button v-if="topic.status === 'hidden'" type="success"
                           @click="doAction('restore')">上架帖子</el-button>
                <el-button v-if="topic.status === 'deleted'" type="success"
                           @click="doAction('restore')">恢复帖子</el-button>
                <el-button v-if="topic.status === 'published' && !topic.top" type="warning"
                           @click="doAction('top')">置顶</el-button>
                <el-button v-if="topic.top" type="info"
                           @click="doAction('untop')">取消置顶</el-button>
                <el-popconfirm title="此操作不可逆，帖子将永久删除，确定继续？" @confirm="doAction('delete')"
                               v-if="topic.status !== 'deleted' && topic.status !== 'pending_review'">
                    <template #reference>
                        <el-button type="danger">删除帖子</el-button>
                    </template>
                </el-popconfirm>
            </div>
            <div style="margin-bottom: 10px;display: flex;gap: 10px;align-items: center">
                <el-tag :type="statusTag(topic.status)" size="small">{{ statusText(topic.status) }}</el-tag>
                <el-tag v-if="topic.top" type="warning" size="small">置顶</el-tag>
                <topic-tag v-if="topic.type" :type="findType(topic.type)"/>
                <span style="color: grey;font-size: 13px">{{ new Date(topic.time).toLocaleString() }}</span>
            </div>
            <h2>{{ topic.title }}</h2>
            <el-divider/>
            <div style="display: flex;gap: 15px;margin-bottom: 15px">
                <el-avatar :src="store.avatarUserUrl(topic.user.avatar)" :size="40"/>
                <div>
                    <div style="font-weight: bold">{{ topic.user.username }}</div>
                    <div style="font-size: 12px;color: grey">{{ topic.user.email }}</div>
                </div>
            </div>
            <el-alert v-if="topic.status === 'hidden' && topic.hideReason"
                      :title="`下架原因：${topic.hideReason}`"
                      type="warning" :closable="false" style="margin-bottom: 15px"/>
            <el-alert v-if="topic.status === 'rejected' && topic.reviewReason"
                      :title="`拒绝原因：${topic.reviewReason}`"
                      type="error" :closable="false" style="margin-bottom: 15px"/>
            <div class="topic-content" v-html="convertToHtml(topic.content)"></div>
        </div>
        <div class="admin-form-card" style="margin-top: 16px" v-if="topic">
            <span style="font-weight: bold">评论 ({{ topic.comments }})</span>
            <el-divider/>
            <div v-for="item in comments" style="margin-bottom: 15px">
                <div style="display: flex;justify-content: space-between;align-items: center">
                    <div>
                        <span style="font-weight: bold">{{ item.username }}</span>
                        <span style="color: grey;font-size: 12px;margin-left: 10px">
                            {{ new Date(item.time).toLocaleString() }}
                        </span>
                    </div>
                    <el-popconfirm title="确定删除该评论吗？" @confirm="deleteComment(item.id)"
                                   v-if="item.status === 'normal'">
                        <template #reference>
                            <el-link type="danger" size="small">&nbsp;删除</el-link>
                        </template>
                    </el-popconfirm>
                </div>
                <div style="font-size: 14px;margin-top: 5px;opacity: 0.8">{{ item.content }}</div>
                <el-divider style="margin: 10px 0"/>
            </div>
        </div>
        <el-dialog v-model="rejectVisible" title="拒绝帖子" width="420px">
            <div class="dialog-hint">
                <el-icon :size="18" color="#E6A23C"><Warning/></el-icon>
                <span>拒绝后帖子将不会展示，作者可以看到拒绝原因</span>
            </div>
            <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请输入拒绝理由（可选）"/>
            <template #footer>
                <el-button @click="rejectVisible = false">取消</el-button>
                <el-button type="danger" @click="confirmReject">确定拒绝</el-button>
            </template>
        </el-dialog>
        <el-dialog v-model="hideVisible" title="下架帖子" width="420px">
            <div class="dialog-hint">
                <el-icon :size="18" color="#E6A23C"><Warning/></el-icon>
                <span>下架后前台将不再展示此帖子</span>
            </div>
            <el-input v-model="hideReason" type="textarea" :rows="3" placeholder="请输入下架原因"/>
            <template #footer>
                <el-button @click="hideVisible = false">取消</el-button>
                <el-button type="warning" @click="confirmHide">确定下架</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import {get, post} from "@/net"
import {useRoute} from "vue-router"
import router from "@/router"
import {useStore} from "@/stores/index"
import {ref} from "vue"
import {ElMessage} from "element-plus"
import {ArrowLeft, Warning} from "@element-plus/icons-vue"
import {QuillDeltaToHtmlConverter} from "quill-delta-to-html"
import Card from "@/components/Card.vue"
import TopicTag from "@/components/TopicTag.vue"

const route = useRoute()
const store = useStore()
const topic = ref(null)
const comments = ref([])
const loading = ref(true)
const tid = route.params.id
const rejectVisible = ref(false)
const rejectReason = ref('')
const hideVisible = ref(false)
const hideReason = ref('')
function statusTag(status) {
    const map = { pending_review: 'warning', published: 'success', rejected: 'danger', hidden: 'info', deleted: 'info' }
    return map[status] || 'info'
}

function statusText(status) {
    const map = { pending_review: '待审核', published: '已发布', rejected: '已拒绝', hidden: '已下架', deleted: '已删除' }
    return map[status] || status
}

function convertToHtml(content) {
    const ops = JSON.parse(content).ops
    const converter = new QuillDeltaToHtmlConverter(ops, { inlineStyles: true })
    return converter.convert()
}

function findType(typeId) {
    const t = store.forum.types.find(t => t.id === typeId)
    return t || { id: typeId, name: '未知', color: '#999', desc: '' }
}

function loadTopic() {
    loading.value = true
    get(`/api/admin/topics/${tid}`, data => {
        topic.value = data
        loading.value = false
        loadComments()
    })
}
loadTopic()

function loadComments() {
    get(`/api/admin/comments?page=0&tid=${tid}`, data => comments.value = data)
}

function doAction(action) {
    post(`/api/admin/topics/${tid}/${action}`, null, () => {
        ElMessage.success('操作成功')
        if (action === 'delete') {
            router.push('/admin/topics')
        } else {
            loadTopic()
        }
    })
}

function confirmReject() {
    let url = `/api/admin/topics/${tid}/reject`
    if (rejectReason.value) url += `?reason=${encodeURIComponent(rejectReason.value)}`
    post(url, null, () => {
        ElMessage.success('已拒绝')
        rejectVisible.value = false
        loadTopic()
    })
}

function confirmHide() {
    if (!hideReason.value) {
        ElMessage.warning('请填写下架原因')
        return
    }
    post(`/api/admin/topics/${tid}/hide?reason=${encodeURIComponent(hideReason.value)}`, null, () => {
        ElMessage.success('已下架')
        hideVisible.value = false
        loadTopic()
    })
}

function deleteComment(id) {
    post(`/api/admin/comments/${id}/delete`, null, () => {
        ElMessage.success('删除成功')
        loadComments()
    })
}
</script>

<style lang="less" scoped>
.admin-page {
    padding: 20px 24px;
}

.admin-page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
}

.admin-page-title {
    font-size: 20px;
    font-weight: 700;
    color: var(--el-text-color-primary);
}

.admin-form-card {
    background: var(--el-bg-color);
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    padding: 20px;
}

.topic-content {
    font-size: 14px;
    line-height: 22px;
    opacity: 0.85;
}

.dialog-hint {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 14px;
    background: var(--el-color-warning-light-9);
    border-radius: 6px;
    margin-bottom: 14px;
    font-size: 13px;
    color: var(--el-text-color-regular);
}
</style>
