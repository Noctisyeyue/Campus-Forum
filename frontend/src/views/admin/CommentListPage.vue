<template>
    <div class="admin-page">
        <div class="admin-page-header">
            <div>
                <div class="admin-page-title">评论管理</div>
                <div class="admin-page-desc text-secondary">查看和管理论坛中所有帖子的评论内容</div>
            </div>
            <div class="admin-page-stats" v-if="total > 0">
                <span class="stat-badge">共 {{ total }} 条</span>
            </div>
        </div>
        <div class="admin-page-filter">
            <div class="filter-group">
                <el-select v-model="filter.status" placeholder="全部状态" style="width: 120px" clearable>
                    <el-option value="normal" label="正常"/>
                    <el-option value="deleted" label="已删除"/>
                </el-select>
                <el-input v-model="filter.content" placeholder="评论内容" style="width: 150px" clearable
                          :prefix-icon="Search"/>
                <el-input v-model="filter.author" placeholder="用户名" style="width: 120px" clearable/>
                <el-input v-model="filter.topicTitle" placeholder="帖子标题" style="width: 150px" clearable/>
            </div>
            <el-button type="primary" @click="loadComments(0)" :icon="Search">搜索</el-button>
        </div>
        <div class="admin-page-body">
            <el-table :data="comments" stripe v-loading="loading" class="admin-table">
                <el-table-column prop="id" label="ID" width="80"/>
                <el-table-column prop="username" label="用户" width="120"/>
                <el-table-column label="帖子" width="200">
                    <template #default="{ row }">
                        <el-link type="primary" @click="router.push(`/admin/topic-detail/${row.tid}`)">
                            {{ row.topicTitle || '帖子#' + row.tid }}
                        </el-link>
                    </template>
                </el-table-column>
                <el-table-column label="内容">
                    <template #default="{ row }">
                        <el-tooltip :content="row.content" placement="top" :disabled="row.content.length <= 50">
                            {{ row.content.length > 50 ? row.content.substring(0, 50) + '...' : row.content }}
                        </el-tooltip>
                    </template>
                </el-table-column>
                <el-table-column label="时间" width="170">
                    <template #default="{ row }">
                        {{ new Date(row.time).toLocaleString() }}
                    </template>
                </el-table-column>
                <el-table-column label="状态" width="90">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 'normal' ? 'success' : 'danger'" size="small" effect="light">
                            {{ row.status === 'normal' ? '正常' : '已删除' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="90">
                    <template #default="{ row }">
                        <el-button type="danger" size="small" plain round
                                   @click="confirmDelete(row.id)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
            <div class="admin-pagination">
                <el-select v-model="pageSize" style="width: 110px; margin-right: 12px" @change="loadComments(0)">
                    <el-option :value="15" label="15 条/页"/>
                    <el-option :value="30" label="30 条/页"/>
                    <el-option :value="50" label="50 条/页"/>
                    <el-option :value="100" label="100 条/页"/>
                </el-select>
                <el-pagination background layout="prev, pager, next"
                               v-model:current-page="page" @current-change="p => loadComments(p - 1)"
                               :total="total" :page-size="pageSize"/>
            </div>
        </div>
    </div>
</template>

<script setup>
import {get, post} from "@/net"
import {reactive, ref} from "vue"
import {ElMessage, ElMessageBox} from "element-plus"
import {Search} from "@element-plus/icons-vue"
import router from "@/router"

const comments = ref([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const pageSize = ref(15)

const filter = reactive({ status: '', content: '', author: '', topicTitle: '' })

function loadComments(p) {
    loading.value = true
    page.value = p + 1
    let url = `/api/admin/comments?page=${p}&pageSize=${pageSize.value}`
    if (filter.status) url += `&status=${filter.status}`
    if (filter.content) url += `&content=${encodeURIComponent(filter.content)}`
    if (filter.author) url += `&author=${encodeURIComponent(filter.author)}`
    if (filter.topicTitle) url += `&topicTitle=${encodeURIComponent(filter.topicTitle)}`
    get(url, data => {
        comments.value = data.list
        total.value = data.total
        loading.value = false
    })
}
loadComments(0)

function confirmDelete(id) {
    const row = comments.value.find(c => c.id === id)
    const isDeleted = row && row.status === 'deleted'
    ElMessageBox.confirm(
        isDeleted ? '该评论已被用户删除，确定要彻底删除吗？此操作不可逆。' : '确定删除该评论吗？此操作不可逆。',
        '删除评论',
        { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    ).then(() => deleteComment(id)).catch(() => {})
}

function deleteComment(id) {
    post(`/api/admin/comments/${id}/delete`, null, () => {
        ElMessage.success('删除成功')
        loadComments(page.value - 1)
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

.admin-pagination {
    display: flex;
    justify-content: center;
    margin-top: 16px;
}
</style>
