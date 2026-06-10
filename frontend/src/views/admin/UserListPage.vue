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
                        <el-avatar :src="store.avatarUserUrl(row.avatar)" :size="34"
                                   :style="!row.avatar ? { background: store.avatarColor(row.username) } : {}">
                            {{ row.avatar ? '' : store.avatarText(row.username) }}
                        </el-avatar>
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
                <el-table-column label="角色" width="110">
                    <template #default="{ row }">
                        <el-tag :type="roleTagType(row.role)" size="small" effect="light">
                            {{ roleName(row.role) }}
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
                <el-table-column label="操作" width="280">
                    <template #default="{ row }">
                        <!-- super_admin 目标不能操作 -->
                        <div v-if="row.role === 'super_admin'" class="text-secondary" style="font-size: 13px">—</div>
                        <!-- 当前用户是 super_admin -->
                        <div v-else-if="store.isSuperAdmin" class="action-cell">
                            <el-button v-if="row.role === 'user'" type="primary" size="small" plain round
                                       @click="confirmPromote(row)">提升为管理员</el-button>
                            <el-button v-if="row.role === 'admin'" type="info" size="small" plain round
                                       @click="confirmDemote(row)">降级为用户</el-button>
                            <el-button :type="row.status === 'active' ? 'danger' : 'success'" size="small" plain round
                                       @click="confirmToggle(row)">
                                {{ row.status === 'active' ? '禁用' : '启用' }}
                            </el-button>
                            <el-button type="warning" size="small" plain round
                                       @click="confirmReset(row.id)">重置密码</el-button>
                        </div>
                        <!-- 当前用户是普通 admin，只能操作 user -->
                        <div v-else-if="row.role === 'user'" class="action-cell">
                            <el-button :type="row.status === 'active' ? 'danger' : 'success'" size="small" plain round
                                       @click="confirmToggle(row)">
                                {{ row.status === 'active' ? '禁用' : '启用' }}
                            </el-button>
                            <el-button type="warning" size="small" plain round
                                       @click="confirmReset(row.id)">重置密码</el-button>
                        </div>
                        <!-- admin 对 admin 无操作 -->
                        <div v-else class="text-secondary" style="font-size: 13px">—</div>
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
/** 用户列表数据 */
const users = ref([])
/** 表格加载状态 */
const loading = ref(false)
/** 当前页码（1-based） */
const page = ref(1)
/** 用户总数 */
const total = ref(0)
/** 每页条数 */
const pageSize = ref(15)
/** 搜索关键词 */
const search = ref('')
/** 状态筛选值 */
const status = ref('')
const route = useRoute()

/**
 * 加载用户列表
 * @param {number} p - 页码（0-based）
 */
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

/**
 * 确认启用/禁用用户，弹出二次确认框
 * @param {Object} user - 用户行数据对象
 */
function confirmToggle(user) {
    const action = user.status === 'active' ? '禁用' : '启用'
    ElMessageBox.confirm(`确定${action}用户「${user.username}」吗？`, `${action}用户`, {
        confirmButtonText: `确定${action}`,
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => toggleStatus(user)).catch(() => {})
}

/**
 * 切换用户状态（启用/禁用）
 * @param {Object} user - 用户行数据对象
 */
function toggleStatus(user) {
    const action = user.status === 'active' ? 'disable' : 'enable'
    post(`/api/admin/users/${user.id}/${action}`, null, () => {
        ElMessage.success(`${user.status === 'active' ? '禁用' : '启用'}成功`)
        loadUsers(page.value - 1)
    })
}

/**
 * 确认重置用户密码，弹出二次确认框
 * @param {number} id - 用户 ID
 */
function confirmReset(id) {
    ElMessageBox.confirm('确定重置该用户密码为 123456 吗？', '重置密码', {
        confirmButtonText: '确定重置',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => resetPassword(id)).catch(() => {})
}

/**
 * 重置用户密码为默认密码 123456
 * @param {number} id - 用户 ID
 */
function resetPassword(id) {
    post(`/api/admin/users/${id}/reset-password`, null, () => {
        ElMessage.success('密码已重置为 123456')
    })
}

/**
 * 确认提升用户为管理员，弹出二次确认框
 * @param {Object} user - 用户行数据对象
 */
function confirmPromote(user) {
    ElMessageBox.confirm(`确定将用户「${user.username}」提升为管理员吗？`, '提升角色', {
        confirmButtonText: '确定提升',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => promoteUser(user)).catch(() => {})
}

/**
 * 提升用户为管理员
 * @param {Object} user - 用户行数据对象
 */
function promoteUser(user) {
    post(`/api/admin/users/${user.id}/promote`, null, () => {
        ElMessage.success('已提升为管理员')
        loadUsers(page.value - 1)
    })
}

/**
 * 确认将管理员降级为用户，弹出二次确认框
 * @param {Object} user - 用户行数据对象
 */
function confirmDemote(user) {
    ElMessageBox.confirm(`确定将管理员「${user.username}」降级为普通用户吗？`, '降级角色', {
        confirmButtonText: '确定降级',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => demoteUser(user)).catch(() => {})
}

/**
 * 将管理员降级为普通用户
 * @param {Object} user - 用户行数据对象
 */
function demoteUser(user) {
    post(`/api/admin/users/${user.id}/demote`, null, () => {
        ElMessage.success('已降级为普通用户')
        loadUsers(page.value - 1)
    })
}

/**
 * 根据角色返回标签类型
 * @param {string} role - 角色标识
 * @return {string} Element Plus Tag 类型
 */
function roleTagType(role) {
    if (role === 'super_admin') return 'danger'
    if (role === 'admin') return 'warning'
    return 'info'
}

/**
 * 根据角色返回中文名称
 * @param {string} role - 角色标识
 * @return {string} 角色中文名称
 */
function roleName(role) {
    if (role === 'super_admin') return '超级管理员'
    if (role === 'admin') return '管理员'
    return '用户'
}

/**
 * 从路由 query 参数中恢复筛选条件并加载用户列表
 */
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
