<template>
    <div class="admin-page">
        <div class="admin-page-header">
            <div>
                <div class="admin-page-title">用户管理</div>
                <div class="admin-page-desc text-secondary">查看、搜索和管理论坛注册用户的状态与权限</div>
            </div>
            <div class="admin-page-stats" v-if="total > 0">
                <span class="stat-badge">共 {{ total }} 条</span>
            </div>
        </div>
        <div class="admin-page-filter">
            <div class="filter-group">
                <el-select v-model="status" placeholder="全部状态" style="width: 130px" clearable>
                    <el-option value="active" label="正常"/>
                    <el-option value="disabled" label="已禁用"/>
                </el-select>
                <el-input v-model="search" placeholder="搜索用户名或邮箱" style="width: 250px" clearable
                          @keyup.enter="loadUsers(0)" :prefix-icon="Search"/>
            </div>
            <el-button type="primary" @click="loadUsers(0)" :icon="Search">搜索</el-button>
        </div>
        <div class="admin-page-body">
            <el-table :data="users" stripe v-loading="loading" class="admin-table">
                <el-table-column prop="id" label="ID" width="70"/>
                <el-table-column label="头像" width="65">
                    <template #default="{ row }">
                        <el-avatar :src="store.avatarUserUrl(row.avatar)" :size="34"/>
                    </template>
                </el-table-column>
                <el-table-column label="用户名" width="150">
                    <template #default="{ row }">
                        <el-link type="primary" @click="router.push(`/admin/user-detail/${row.id}`)">
                            {{ row.username }}
                        </el-link>
                    </template>
                </el-table-column>
                <el-table-column prop="email" label="邮箱"/>
                <el-table-column label="角色" width="90">
                    <template #default="{ row }">
                        <el-tag :type="row.role === 'admin' ? 'danger' : 'info'" size="small" effect="light">
                            {{ row.role === 'admin' ? '管理员' : '用户' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="状态" width="90">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 'active' ? 'success' : 'danger'" size="small" effect="light">
                            {{ row.status === 'active' ? '正常' : '已禁用' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="注册时间" width="170">
                    <template #default="{ row }">
                        {{ new Date(row.registerTime).toLocaleString() }}
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="200">
                    <template #default="{ row }">
                        <div class="action-cell">
                            <el-button :type="row.status === 'active' ? 'danger' : 'success'" size="small" plain round
                                       @click="confirmToggle(row)">
                                {{ row.status === 'active' ? '禁用' : '启用' }}
                            </el-button>
                            <el-button type="warning" size="small" plain round
                                       @click="confirmReset(row.id)">重置密码</el-button>
                        </div>
                    </template>
                </el-table-column>
            </el-table>
            <div class="admin-pagination">
                <el-select v-model="pageSize" style="width: 110px; margin-right: 12px" @change="loadUsers(0)">
                    <el-option :value="15" label="15 条/页"/>
                    <el-option :value="30" label="30 条/页"/>
                    <el-option :value="50" label="50 条/页"/>
                    <el-option :value="100" label="100 条/页"/>
                </el-select>
                <el-pagination background layout="prev, pager, next"
                               v-model:current-page="page" @current-change="p => loadUsers(p - 1)"
                               :total="total" :page-size="pageSize"/>
            </div>
        </div>
    </div>
</template>

<script setup>
import {get, post} from "@/net"
import {ref, watch} from "vue"
import {ElMessage, ElMessageBox} from "element-plus"
import {Search} from "@element-plus/icons-vue"
import router from "@/router"
import {useStore} from "@/stores/index"
import {useRoute} from "vue-router"

const store = useStore()
const users = ref([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const pageSize = ref(15)
const search = ref('')
const status = ref('')
const route = useRoute()

function loadUsers(p) {
    loading.value = true
    page.value = p + 1
    let url = `/api/admin/users?page=${p}&pageSize=${pageSize.value}`
    if (search.value) url += `&search=${encodeURIComponent(search.value)}`
    if (status.value) url += `&status=${status.value}`
    get(url, data => {
        users.value = data.list
        total.value = data.total
        loading.value = false
    })
}

function confirmToggle(user) {
    const action = user.status === 'active' ? '禁用' : '启用'
    ElMessageBox.confirm(`确定${action}用户「${user.username}」吗？`, `${action}用户`, {
        confirmButtonText: `确定${action}`,
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => toggleStatus(user)).catch(() => {})
}

function toggleStatus(user) {
    const action = user.status === 'active' ? 'disable' : 'enable'
    post(`/api/admin/users/${user.id}/${action}`, null, () => {
        ElMessage.success(`${user.status === 'active' ? '禁用' : '启用'}成功`)
        loadUsers(page.value - 1)
    })
}

function confirmReset(id) {
    ElMessageBox.confirm('确定重置该用户密码为 123456 吗？', '重置密码', {
        confirmButtonText: '确定重置',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => resetPassword(id)).catch(() => {})
}

function resetPassword(id) {
    post(`/api/admin/users/${id}/reset-password`, null, () => {
        ElMessage.success('密码已重置为 123456')
    })
}

function applyRouteQuery() {
    search.value = typeof route.query.search === 'string' ? route.query.search : ''
    status.value = typeof route.query.status === 'string' ? route.query.status : ''
    loadUsers(0)
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

.admin-pagination {
    display: flex;
    justify-content: center;
    margin-top: 16px;
}
</style>
