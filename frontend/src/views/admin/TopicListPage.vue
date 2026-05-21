<template>
    <div style="padding: 20px">
        <card>
            <span style="font-size: 18px;font-weight: bold">帖子管理</span>
        </card>
        <card style="margin-top: 10px">
            <div style="display: flex;gap: 10px;margin-bottom: 15px;flex-wrap: wrap">
                <el-select v-model="filter.status" placeholder="全部状态" style="width: 130px" clearable>
                    <el-option label="待审核" value="pending_review"/>
                    <el-option label="已发布" value="published"/>
                    <el-option label="已拒绝" value="rejected"/>
                    <el-option label="已下架" value="hidden"/>
                    <el-option label="已删除" value="deleted"/>
                </el-select>
                <el-select v-model="filter.type" placeholder="全部分类" style="width: 130px" clearable>
                    <el-option v-for="t in types" :key="t.id" :label="t.name" :value="t.id"/>
                </el-select>
                <el-input v-model="filter.title" placeholder="标题关键词" style="width: 180px" clearable/>
                <el-input v-model="filter.author" placeholder="作者" style="width: 130px" clearable/>
                <el-button type="primary" @click="loadTopics(0)">搜索</el-button>
            </div>
            <el-table :data="topics" stripe v-loading="loading">
                <el-table-column prop="id" label="ID" width="80"/>
                <el-table-column label="标题" min-width="200">
                    <template #default="{ row }">
                        <el-link type="primary" @click="router.push(`/admin/topic-detail/${row.id}`)">
                            {{ row.title }}
                        </el-link>
                    </template>
                </el-table-column>
                <el-table-column prop="username" label="作者" width="100"/>
                <el-table-column prop="typeName" label="分类" width="100"/>
                <el-table-column label="状态" width="100">
                    <template #default="{ row }">
                        <el-tag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="置顶" width="70">
                    <template #default="{ row }">
                        <el-tag v-if="row.top" type="warning" size="small">是</el-tag>
                        <span v-else style="color: grey">-</span>
                    </template>
                </el-table-column>
                <el-table-column prop="commentCount" label="评论" width="70"/>
                <el-table-column label="发布时间" width="160">
                    <template #default="{ row }">
                        {{ new Date(row.time).toLocaleString() }}
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="260" fixed="right">
                    <template #default="{ row }">
                        <el-link v-if="row.status === 'pending_review'" type="success"
                                 @click="doAction(row.id, 'approve')">&nbsp;通过</el-link>
                        <el-link v-if="row.status === 'pending_review'" type="danger"
                                 @click="openReject(row.id)" style="margin-left: 8px">&nbsp;拒绝</el-link>
                        <el-link v-if="row.status === 'published'" type="warning"
                                 @click="openHide(row.id)" style="margin-left: 8px">&nbsp;下架</el-link>
                        <el-link v-if="row.status === 'hidden'" type="success"
                                 @click="doAction(row.id, 'restore')" style="margin-left: 8px">&nbsp;上架</el-link>
                        <el-link v-if="row.status === 'deleted'" type="success"
                                 @click="doAction(row.id, 'restore')" style="margin-left: 8px">&nbsp;恢复</el-link>
                        <el-link v-if="row.status === 'published' && !row.top" type="warning"
                                 @click="doAction(row.id, 'top')" style="margin-left: 8px">&nbsp;置顶</el-link>
                        <el-link v-if="row.top" type="info"
                                 @click="doAction(row.id, 'untop')" style="margin-left: 8px">&nbsp;取消置顶</el-link>
                        <el-popconfirm title="此操作不可逆，帖子将永久删除，确定继续？" @confirm="doAction(row.id, 'delete')"
                                       style="margin-left: 8px" v-if="row.status !== 'deleted' && row.status !== 'pending_review'">
                            <template #reference>
                                <el-link type="danger">&nbsp;删除</el-link>
                            </template>
                        </el-popconfirm>
                        <el-link type="primary" style="margin-left: 8px"
                                 @click="router.push(`/admin/topic-detail/${row.id}`)">&nbsp;详情</el-link>
                    </template>
                </el-table-column>
            </el-table>
            <div style="display: flex;justify-content: center;margin-top: 15px">
                <el-pagination background layout="prev, pager, next"
                               v-model:current-page="page" @current-change="p => loadTopics(p - 1)"
                               :total="total" :page-size="15" hide-on-single-page/>
            </div>
        </card>
        <el-dialog v-model="reject.show" title="拒绝帖子" width="400px">
            <el-input v-model="reject.reason" type="textarea" :rows="3" placeholder="请输入拒绝理由（可选）"/>
            <template #footer>
                <el-button @click="reject.show = false">取消</el-button>
                <el-button type="danger" @click="confirmReject">确定拒绝</el-button>
            </template>
        </el-dialog>
        <el-dialog v-model="hideDialog.show" title="下架帖子" width="400px">
            <div style="margin-bottom: 10px;color: grey">下架后前台将不再展示此帖子，确定下架？</div>
            <el-input v-model="hideDialog.reason" type="textarea" :rows="3" placeholder="请输入下架原因"/>
            <template #footer>
                <el-button @click="hideDialog.show = false">取消</el-button>
                <el-button type="warning" @click="confirmHide">确定下架</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import {get, post} from "@/net"
import {reactive, ref, watch} from "vue"
import {ElMessage} from "element-plus"
import Card from "@/components/Card.vue"
import router from "@/router"
import {useRoute} from "vue-router"

const topics = ref([])
const types = ref([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const route = useRoute()

const filter = reactive({ status: '', type: '', title: '', author: '' })
const reject = reactive({ show: false, id: null, reason: '' })
const hideDialog = reactive({ show: false, id: null, reason: '' })

// 加载分类列表用于筛选
get('/api/admin/types', data => types.value = data)

function statusTag(status) {
    const map = { pending_review: 'warning', published: 'success', rejected: 'danger', hidden: 'info', deleted: 'info' }
    return map[status] || 'info'
}

function statusText(status) {
    const map = { pending_review: '待审核', published: '已发布', rejected: '已拒绝', hidden: '已下架', deleted: '已删除' }
    return map[status] || status
}

function loadTopics(p) {
    loading.value = true
    page.value = p + 1
    let url = `/api/admin/topics?page=${p}`
    if (filter.status) url += `&status=${filter.status}`
    if (filter.type) url += `&type=${filter.type}`
    if (filter.title) url += `&title=${encodeURIComponent(filter.title)}`
    if (filter.author) url += `&author=${encodeURIComponent(filter.author)}`
    get(url, data => {
        topics.value = data
        total.value = (p + 1) * 15
        loading.value = false
    })
}

function doAction(id, action) {
    post(`/api/admin/topics/${id}/${action}`, null, () => {
        ElMessage.success('操作成功')
        loadTopics(page.value - 1)
    })
}

function openReject(id) {
    reject.id = id
    reject.reason = ''
    reject.show = true
}

function confirmReject() {
    let url = `/api/admin/topics/${reject.id}/reject`
    if (reject.reason) url += `?reason=${encodeURIComponent(reject.reason)}`
    post(url, null, () => {
        ElMessage.success('已拒绝')
        reject.show = false
        loadTopics(page.value - 1)
    })
}

function openHide(id) {
    hideDialog.id = id
    hideDialog.reason = ''
    hideDialog.show = true
}

function confirmHide() {
    if (!hideDialog.reason) {
        ElMessage.warning('请填写下架原因')
        return
    }
    post(`/api/admin/topics/${hideDialog.id}/hide?reason=${encodeURIComponent(hideDialog.reason)}`, null, () => {
        ElMessage.success('已下架')
        hideDialog.show = false
        loadTopics(page.value - 1)
    })
}

function applyRouteQuery() {
    filter.status = typeof route.query.status === 'string' ? route.query.status : ''
    filter.type = route.query.type ? Number(route.query.type) : ''
    filter.title = typeof route.query.title === 'string' ? route.query.title : ''
    filter.author = typeof route.query.author === 'string' ? route.query.author : ''
    loadTopics(0)
}

watch(() => route.query, applyRouteQuery, { immediate: true })
</script>
