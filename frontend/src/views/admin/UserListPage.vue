<template>
    <div style="padding: 20px">
        <card>
            <span style="font-size: 18px;font-weight: bold">用户管理</span>
        </card>
        <card style="margin-top: 10px">
            <div style="display: flex;gap: 10px;margin-bottom: 15px">
                <el-input v-model="search" placeholder="搜索用户名或邮箱" style="width: 250px" clearable
                          @keyup.enter="loadUsers(0)"/>
                <el-button type="primary" @click="loadUsers(0)">搜索</el-button>
            </div>
            <el-table :data="users" stripe v-loading="loading">
                <el-table-column prop="id" label="ID" width="80"/>
                <el-table-column label="头像" width="70">
                    <template #default="{ row }">
                        <el-avatar :src="store.avatarUserUrl(row.avatar)" :size="35"/>
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
                <el-table-column label="角色" width="100">
                    <template #default="{ row }">
                        <el-tag :type="row.role === 'admin' ? 'danger' : 'info'" size="small">
                            {{ row.role === 'admin' ? '管理员' : '用户' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="状态" width="100">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 'active' ? 'success' : 'danger'" size="small">
                            {{ row.status === 'active' ? '正常' : '已禁用' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="注册时间" width="170">
                    <template #default="{ row }">
                        {{ new Date(row.registerTime).toLocaleString() }}
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="250">
                    <template #default="{ row }">
                        <el-popconfirm :title="`确定${row.status === 'active' ? '禁用' : '启用'}该用户吗？`"
                                       @confirm="toggleStatus(row)">
                            <template #reference>
                                <el-link :type="row.status === 'active' ? 'danger' : 'success'">
                                    &nbsp;{{ row.status === 'active' ? '禁用' : '启用' }}
                                </el-link>
                            </template>
                        </el-popconfirm>
                        <el-popconfirm title="确定重置该用户密码为 123456 吗？" @confirm="resetPassword(row.id)"
                                       style="margin-left: 15px">
                            <template #reference>
                                <el-link type="warning">&nbsp;重置密码</el-link>
                            </template>
                        </el-popconfirm>
                        <el-link type="primary" style="margin-left: 15px"
                                 @click="router.push(`/admin/user-detail/${row.id}`)">&nbsp;详情</el-link>
                    </template>
                </el-table-column>
            </el-table>
            <div style="display: flex;justify-content: center;margin-top: 15px">
                <el-pagination background layout="prev, pager, next"
                               v-model:current-page="page" @current-change="p => loadUsers(p - 1)"
                               :total="total" :page-size="10" hide-on-single-page/>
            </div>
        </card>
    </div>
</template>

<script setup>
import {get, post} from "@/net"
import {ref} from "vue"
import {ElMessage} from "element-plus"
import Card from "@/components/Card.vue"
import router from "@/router"
import {useStore} from "@/stores/index"

const store = useStore()
const users = ref([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const search = ref('')

function loadUsers(p) {
    loading.value = true
    page.value = p + 1
    let url = `/api/admin/users?page=${p}`
    if (search.value) url += `&search=${encodeURIComponent(search.value)}`
    get(url, data => {
        users.value = data
        total.value = (p + 1) * 10
        loading.value = false
    })
}
loadUsers(0)

function toggleStatus(user) {
    const action = user.status === 'active' ? 'disable' : 'enable'
    post(`/api/admin/users/${user.id}/${action}`, null, () => {
        ElMessage.success(`${user.status === 'active' ? '禁用' : '启用'}成功`)
        loadUsers(page.value - 1)
    })
}

function resetPassword(id) {
    post(`/api/admin/users/${id}/reset-password`, null, () => {
        ElMessage.success('密码已重置为 123456')
    })
}
</script>
