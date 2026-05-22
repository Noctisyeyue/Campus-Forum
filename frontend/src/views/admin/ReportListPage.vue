<template>
    <div class="admin-page">
        <div class="admin-page-header">
            <div>
                <div class="admin-page-title">举报管理</div>
                <div class="admin-page-desc text-secondary">处理用户提交的帖子和评论举报，维护社区秩序</div>
            </div>
            <div class="admin-page-stats" v-if="total > 0">
                <span class="stat-badge">共 {{ total }} 条</span>
            </div>
        </div>
        <div class="admin-page-filter">
            <div class="filter-group">
                <el-select v-model="filter.status" placeholder="全部状态" style="width: 130px" clearable>
                    <el-option value="pending" label="待处理"/>
                    <el-option value="resolved" label="已处理"/>
                    <el-option value="dismissed" label="已驳回"/>
                </el-select>
                <el-select v-model="filter.targetType" placeholder="全部类型" style="width: 130px" clearable>
                    <el-option value="topic" label="帖子"/>
                    <el-option value="comment" label="评论"/>
                </el-select>
            </div>
            <el-button type="primary" @click="loadReports(0)" :icon="Search">搜索</el-button>
        </div>
        <div class="admin-page-body">
            <el-table :data="reports" stripe v-loading="loading" class="admin-table">
                <el-table-column prop="id" label="ID" width="65"/>
                <el-table-column prop="reporterName" label="举报人" width="100"/>
                <el-table-column label="类型" width="80">
                    <template #default="{ row }">
                        <el-tag size="small" :type="row.targetType === 'topic' ? '' : 'warning'" effect="light">
                            {{ row.targetType === 'topic' ? '帖子' : '评论' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="目标" min-width="180">
                    <template #default="{ row }">
                        <el-link type="primary" v-if="row.topicId"
                                 @click="router.push(`/admin/topic-detail/${row.topicId}`)">
                            {{ row.targetSummary || '无' }}
                        </el-link>
                        <span v-else>{{ row.targetSummary || '无' }}</span>
                    </template>
                </el-table-column>
                <el-table-column prop="reason" label="原因" width="100"/>
                <el-table-column label="说明" min-width="120">
                    <template #default="{ row }">
                        <el-tooltip :content="row.detail" placement="top" :disabled="!row.detail || row.detail.length <= 15">
                            {{ row.detail ? (row.detail.length > 15 ? row.detail.substring(0, 15) + '...' : row.detail) : '-' }}
                        </el-tooltip>
                    </template>
                </el-table-column>
                <el-table-column label="时间" width="160">
                    <template #default="{ row }">
                        {{ new Date(row.time).toLocaleString() }}
                    </template>
                </el-table-column>
                <el-table-column label="状态" width="90">
                    <template #default="{ row }">
                        <el-tag :type="statusType(row.status)" size="small" effect="light">
                            {{ statusLabel(row.status) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="150">
                    <template #default="{ row }">
                        <div class="action-cell">
                            <template v-if="row.status === 'pending'">
                                <el-button type="success" size="small" plain round
                                           @click="confirmResolve(row.id)">采纳</el-button>
                                <el-button type="warning" size="small" plain round
                                           @click="openDismiss(row)">驳回</el-button>
                            </template>
                            <span v-else class="status-done" :class="row.status">
                                {{ row.status === 'resolved' ? '已处理' : (row.adminNote || '已驳回') }}
                            </span>
                        </div>
                    </template>
                </el-table-column>
            </el-table>
            <div class="admin-pagination">
                <el-pagination background layout="prev, pager, next"
                               v-model:current-page="page" @current-change="p => loadReports(p - 1)"
                               :total="total" :page-size="15" hide-on-single-page/>
            </div>
        </div>

        <el-dialog v-model="dismiss.show" title="驳回举报" width="420px" class="admin-dialog">
            <div class="dialog-hint">
                <el-icon :size="18" color="#909399"><Warning/></el-icon>
                <span>驳回后该举报将被关闭，请说明驳回原因</span>
            </div>
            <el-input type="textarea" v-model="dismiss.note" :rows="3" placeholder="请填写驳回原因（必填）"/>
            <template #footer>
                <el-button @click="dismiss.show = false">取消</el-button>
                <el-button type="primary" @click="confirmDismiss" :disabled="!dismiss.note.trim()">确定驳回</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import {get, post} from "@/net"
import {reactive, ref, watch} from "vue"
import {ElMessage, ElMessageBox} from "element-plus"
import {Search, Warning} from "@element-plus/icons-vue"
import router from "@/router"
import {useRoute} from "vue-router"

const reports = ref([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const filter = reactive({ status: '', targetType: '' })
const route = useRoute()

function loadReports(p) {
    loading.value = true
    page.value = p + 1
    let url = `/api/admin/reports?page=${p}`
    if (filter.status) url += `&status=${filter.status}`
    if (filter.targetType) url += `&targetType=${filter.targetType}`
    get(url, data => {
        reports.value = data
        total.value = (p + 1) * 15
        loading.value = false
    })
}

function confirmResolve(id) {
    ElMessageBox.confirm('采纳举报后将下架/删除被举报的内容，确定继续？', '采纳举报', {
        confirmButtonText: '确定采纳',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => resolveReport(id, 'delete', null)).catch(() => {})
}

function resolveReport(id, action, note) {
    let url = `/api/admin/reports/${id}/resolve?action=${action}`
    if (note) url += `&note=${encodeURIComponent(note)}`
    post(url, null, () => {
        ElMessage.success(action === 'delete' ? '已处理' : '已驳回')
        loadReports(page.value - 1)
    })
}

const dismiss = reactive({ show: false, id: null, note: '' })

function openDismiss(row) {
    dismiss.id = row.id
    dismiss.note = ''
    dismiss.show = true
}

function confirmDismiss() {
    resolveReport(dismiss.id, 'dismiss', dismiss.note)
    dismiss.show = false
}

function statusType(status) {
    if (status === 'pending') return 'warning'
    if (status === 'resolved') return 'success'
    return 'info'
}

function statusLabel(status) {
    if (status === 'pending') return '待处理'
    if (status === 'resolved') return '已处理'
    return '已驳回'
}

function applyRouteQuery() {
    filter.status = typeof route.query.status === 'string' ? route.query.status : ''
    filter.targetType = typeof route.query.targetType === 'string' ? route.query.targetType : ''
    loadReports(0)
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

.action-cell {
    display: flex;
    flex-wrap: wrap;
    gap: 2px;
}

.status-done {
    font-size: 12px;

    &.resolved { color: #67c23a; }
    &.dismissed { color: #909399; }
}

.admin-pagination {
    display: flex;
    justify-content: center;
    margin-top: 16px;
}

.dialog-hint {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 14px;
    background: var(--el-color-info-light-9);
    border-radius: 6px;
    margin-bottom: 14px;
    font-size: 13px;
    color: var(--el-text-color-regular);
}
</style>
