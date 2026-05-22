<template>
    <div class="admin-page" v-loading="loading">
        <div class="admin-page-header">
            <div class="admin-page-title">用户详情</div>
            <el-button :icon="ArrowLeft" size="small" plain round @click="router.push('/admin/users')">返回列表</el-button>
        </div>
        <div class="admin-form-card" v-if="user">
            <el-descriptions :column="2" border>
                <el-descriptions-item label="头像">
                    <el-avatar :src="store.avatarUserUrl(user.avatar)" :size="60"/>
                </el-descriptions-item>
                <el-descriptions-item label="ID">{{ user.id }}</el-descriptions-item>
                <el-descriptions-item label="用户名">{{ user.username }}</el-descriptions-item>
                <el-descriptions-item label="邮箱">{{ user.email }}</el-descriptions-item>
                <el-descriptions-item label="角色">
                    <el-tag :type="user.role === 'admin' ? 'danger' : 'info'" size="small">
                        {{ user.role === 'admin' ? '管理员' : '用户' }}
                    </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="注册时间">
                    {{ new Date(user.registerTime).toLocaleString() }}
                </el-descriptions-item>
            </el-descriptions>
            <div style="margin-top: 20px;display: flex;gap: 10px">
                <el-popconfirm :title="`确定${user.status === 'active' ? '禁用' : '启用'}该用户吗？`"
                               @confirm="toggleStatus">
                    <template #reference>
                        <el-button :type="user.status === 'active' ? 'danger' : 'success'" plain>
                            {{ user.status === 'active' ? '禁用用户' : '启用用户' }}
                        </el-button>
                    </template>
                </el-popconfirm>
                <el-popconfirm title="确定重置该用户密码为 123456 吗？" @confirm="resetPassword">
                    <template #reference>
                        <el-button type="warning" plain>重置密码</el-button>
                    </template>
                </el-popconfirm>
            </div>
        </div>
    </div>
</template>

<script setup>
import {get, post} from "@/net"
import {useRoute} from "vue-router"
import router from "@/router"
import {useStore} from "@/stores/index"
import {ref} from "vue"
import {ElMessage} from "element-plus"
import {ArrowLeft} from "@element-plus/icons-vue"

const route = useRoute()
const store = useStore()
const user = ref(null)
const loading = ref(true)
const uid = route.params.id

get(`/api/admin/users/${uid}`, data => {
    user.value = data
    loading.value = false
})

function toggleStatus() {
    const action = user.value.status === 'active' ? 'disable' : 'enable'
    post(`/api/admin/users/${uid}/${action}`, null, () => {
        ElMessage.success('操作成功')
        get(`/api/admin/users/${uid}`, data => user.value = data)
    })
}

function resetPassword() {
    post(`/api/admin/users/${uid}/reset-password`, null, () => {
        ElMessage.success('密码已重置为 123456')
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
</style>
