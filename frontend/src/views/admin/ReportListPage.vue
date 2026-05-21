<template>
    <div style="padding: 20px">
        <card>
            <span style="font-size: 18px;font-weight: bold">举报管理</span>
        </card>
        <card style="margin-top: 10px">
            <div style="display: flex;gap: 10px;margin-bottom: 15px">
                <el-select v-model="filter.status" placeholder="全部状态" style="width: 130px" clearable>
                    <el-option value="pending" label="待处理"/>
                    <el-option value="resolved" label="已处理"/>
                    <el-option value="dismissed" label="已驳回"/>
                </el-select>
                <el-select v-model="filter.targetType" placeholder="全部类型" style="width: 130px" clearable>
                    <el-option value="topic" label="帖子"/>
                    <el-option value="comment" label="评论"/>
                </el-select>
                <el-button type="primary" @click="loadReports(0)">搜索</el-button>
            </div>
            <el-table :data="reports" stripe v-loading="loading">
                <el-table-column prop="id" label="ID" width="70"/>
                <el-table-column prop="reporterName" label="举报人" width="100"/>
                <el-table-column label="类型" width="80">
                    <template #default="{ row }">
                        <el-tag size="small" :type="row.targetType === 'topic' ? 'primary' : 'warning'">
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
                        <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="160">
                    <template #default="{ row }">
                        <template v-if="row.status === 'pending'">
                            <el-popconfirm title="确定处理该举报（下架/删除内容）？"
                                           @confirm="resolveReport(row.id, 'delete', null)">
                                <template #reference>
                                    <el-link type="danger">&nbsp;处理</el-link>
                                </template>
                            </el-popconfirm>
                            <el-link type="info" style="margin-left: 10px"
                                     @click="openDismiss(row)">&nbsp;驳回</el-link>
                        </template>
                        <span v-else style="font-size: 13px" :style="{ color: row.status === 'resolved' ? '#f56c6c' : '#909399' }">
                            {{ row.status === 'resolved' ? '已处理（内容已下架/删除）' : (row.adminNote || '已驳回') }}
                        </span>
                    </template>
                </el-table-column>
            </el-table>
            <div style="display: flex;justify-content: center;margin-top: 15px">
                <el-pagination background layout="prev, pager, next"
                               v-model:current-page="page" @current-change="p => loadReports(p - 1)"
                               :total="total" :page-size="15" hide-on-single-page/>
            </div>
        </card>

        <el-dialog v-model="dismiss.show" title="驳回举报" width="400px">
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
import {ElMessage} from "element-plus"
import Card from "@/components/Card.vue"
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
