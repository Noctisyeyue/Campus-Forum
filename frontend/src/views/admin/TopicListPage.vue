<template>
    <div class="admin-page">
        <div class="admin-page-header">
            <div>
                <div class="admin-page-title">帖子管理</div>
                <div class="admin-page-desc text-secondary">管理全站帖子的审核、发布、下架和置顶操作</div>
            </div>
            <div class="admin-page-stats" v-if="total > 0">
                <span class="stat-badge">共 {{ total }} 条</span>
            </div>
        </div>
        <div class="admin-page-filter">
            <div class="filter-group">
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
                <el-input v-model="filter.title" placeholder="标题关键词" style="width: 180px" clearable
                          :prefix-icon="Search"/>
                <el-input v-model="filter.author" placeholder="作者" style="width: 130px" clearable/>
            </div>
            <el-button type="primary" @click="loadTopics(0)" :icon="Search">搜索</el-button>
        </div>
        <div class="admin-page-body">
            <el-table :data="topics" stripe v-loading="loading" class="admin-table">
                <el-table-column prop="id" label="ID" width="70"/>
                <el-table-column label="标题" min-width="200">
                    <template #default="{ row }">
                        <el-link type="primary" @click="router.push(`/admin/topic-detail/${row.id}`)">
                            {{ row.title }}
                        </el-link>
                    </template>
                </el-table-column>
                <el-table-column prop="username" label="作者" width="100"/>
                <el-table-column prop="typeName" label="分类" width="100"/>
                <el-table-column label="状态" width="90">
                    <template #default="{ row }">
                        <el-tag :type="statusTag(row.status)" size="small" effect="light">
                            {{ statusText(row.status) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="置顶" width="65">
                    <template #default="{ row }">
                        <el-tag v-if="row.top" type="warning" size="small" effect="light">是</el-tag>
                        <span v-else class="text-muted">-</span>
                    </template>
                </el-table-column>
                <el-table-column prop="commentCount" label="评论" width="65"/>
                <el-table-column label="发布时间" width="160">
                    <template #default="{ row }">
                        {{ new Date(row.time).toLocaleString() }}
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="140" fixed="right">
                    <template #default="{ row }">
                        <div class="action-cell">
                            <template v-if="row.status === 'pending_review'">
                                <el-button type="success" size="small" plain round
                                           @click="doAction(row.id, 'approve')">通过</el-button>
                                <el-button type="danger" size="small" plain round
                                           @click="openReject(row.id)">拒绝</el-button>
                            </template>
                            <template v-else-if="row.status === 'published'">
                                <el-dropdown trigger="click">
                                    <el-button type="primary" size="small" plain round>
                                        管理<el-icon class="el-icon--right"><ArrowDown/></el-icon>
                                    </el-button>
                                    <template #dropdown>
                                        <el-dropdown-item v-if="!row.top" @click="doAction(row.id, 'top')">
                                            置顶
                                        </el-dropdown-item>
                                        <el-dropdown-item v-if="row.top" @click="doAction(row.id, 'untop')">
                                            取消置顶
                                        </el-dropdown-item>
                                        <el-dropdown-item @click="openHide(row.id)">
                                            <span style="color: #E6A23C">下架</span>
                                        </el-dropdown-item>
                                        <el-dropdown-item divided @click="confirmDelete(row.id)">
                                            <span style="color: #F56C6C">删除</span>
                                        </el-dropdown-item>
                                    </template>
                                </el-dropdown>
                            </template>
                            <template v-else-if="row.status === 'hidden'">
                                <el-button type="success" size="small" plain round
                                           @click="doAction(row.id, 'restore')">上架</el-button>
                            </template>
                            <template v-else-if="row.status === 'deleted'">
                                <el-button type="success" size="small" plain round
                                           @click="doAction(row.id, 'restore')">恢复</el-button>
                                <el-button type="danger" size="small" plain round
                                           @click="confirmDelete(row.id)">删除</el-button>
                            </template>
                            <template v-else>
                                <span class="text-muted">-</span>
                            </template>
                        </div>
                    </template>
                </el-table-column>
            </el-table>
            <div class="admin-pagination">
                <el-select v-model="pageSize" style="width: 110px; margin-right: 12px" @change="loadTopics(0)">
                    <el-option :value="15" label="15 条/页"/>
                    <el-option :value="30" label="30 条/页"/>
                    <el-option :value="50" label="50 条/页"/>
                    <el-option :value="100" label="100 条/页"/>
                </el-select>
                <el-pagination background layout="prev, pager, next"
                               v-model:current-page="page" @current-change="p => loadTopics(p - 1)"
                               :total="total" :page-size="pageSize"/>
            </div>
        </div>

        <el-dialog v-model="reject.show" title="拒绝帖子" width="420px" class="admin-dialog">
            <div class="dialog-hint">
                <el-icon :size="18" color="#E6A23C"><Warning/></el-icon>
                <span>拒绝后帖子将不会展示，作者可以看到拒绝原因</span>
            </div>
            <el-input v-model="reject.reason" type="textarea" :rows="3" placeholder="请输入拒绝理由"/>
            <template #footer>
                <el-button @click="reject.show = false">取消</el-button>
                <el-button type="danger" @click="confirmReject">确定拒绝</el-button>
            </template>
        </el-dialog>
        <el-dialog v-model="hideDialog.show" title="下架帖子" width="420px" class="admin-dialog">
            <div class="dialog-hint">
                <el-icon :size="18" color="#E6A23C"><Warning/></el-icon>
                <span>下架后前台将不再展示此帖子</span>
            </div>
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
import {ElMessage, ElMessageBox} from "element-plus"
import {ArrowDown, Search, Warning} from "@element-plus/icons-vue"
import router from "@/router"
import {useRoute} from "vue-router"

const topics = ref([])
const types = ref([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const pageSize = ref(15)
const route = useRoute()

const filter = reactive({ status: '', type: '', title: '', author: '' })
const reject = reactive({ show: false, id: null, reason: '' })
const hideDialog = reactive({ show: false, id: null, reason: '' })

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
    let url = `/api/admin/topics?page=${p}&pageSize=${pageSize.value}`
    if (filter.status) url += `&status=${filter.status}`
    if (filter.type) url += `&type=${filter.type}`
    if (filter.title) url += `&title=${encodeURIComponent(filter.title)}`
    if (filter.author) url += `&author=${encodeURIComponent(filter.author)}`
    get(url, data => {
        topics.value = data.list
        total.value = data.total
        loading.value = false
    })
}

function doAction(id, action) {
    post(`/api/admin/topics/${id}/${action}`, null, () => {
        ElMessage.success('操作成功')
        loadTopics(page.value - 1)
    })
}

function confirmDelete(id) {
    ElMessageBox.confirm('此操作不可逆，帖子将永久删除，确定继续？', '删除帖子', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => doAction(id, 'delete')).catch(() => {})
}

function openReject(id) {
    reject.id = id
    reject.reason = ''
    reject.show = true
}

function confirmReject() {
    if (!reject.reason.trim()) {
        ElMessage.warning('请填写拒绝理由')
        return
    }
    let url = `/api/admin/topics/${reject.id}/reject?reason=${encodeURIComponent(reject.reason.trim())}`
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

.admin-page-desc {
    margin-top: 4px;
    font-size: 13px;
}

.admin-page-stats {
    display: flex;
    gap: 12px;
}

.stat-badge {
    display: inline-flex;
    align-items: center;
    padding: 4px 14px;
    border-radius: 20px;
    font-size: 13px;
    font-weight: 500;
    background: var(--el-color-primary-light-9);
    color: var(--el-color-primary);
}

.admin-page-filter {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 14px 16px;
    background: var(--el-bg-color);
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    margin-bottom: 16px;
}

.filter-group {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
}

.admin-page-body {
    background: var(--el-bg-color);
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    padding: 16px;
}

.admin-table {
    width: 100%;
}

.action-cell {
    display: flex;
    flex-wrap: wrap;
    gap: 2px;
}

.admin-pagination {
    display: flex;
    justify-content: center;
    margin-top: 16px;
}

.text-muted {
    color: var(--el-text-color-placeholder);
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
