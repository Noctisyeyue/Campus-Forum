<template>
    <div class="admin-page">
        <div class="admin-page-header">
            <div>
                <div class="admin-page-title">分类管理</div>
                <div class="admin-page-desc text-secondary">管理帖子分类的类型、颜色和描述信息</div>
            </div>
            <el-button type="primary" @click="openDialog()" :icon="Plus">新增分类</el-button>
        </div>
        <div class="admin-page-body">
            <el-table :data="types" stripe class="admin-table">
                <el-table-column prop="id" label="ID" width="70"/>
                <el-table-column label="颜色" width="70">
                    <template #default="{ row }">
                        <color-dot :color="row.color"/>
                    </template>
                </el-table-column>
                <el-table-column prop="name" label="名称"/>
                <el-table-column prop="desc" label="描述"/>
                <el-table-column label="类型" width="100">
                    <template #default="{ row }">
                        <el-tag v-if="row.systemKey" type="warning" size="small" effect="light">系统分类</el-tag>
                        <el-tag v-else type="info" size="small" effect="light">普通分类</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="170">
                    <template #default="{ row }">
                        <div class="action-cell">
                            <el-button type="primary" size="small" plain round @click="openDialog(row)">编辑</el-button>
                            <el-button v-if="!row.systemKey" type="danger" size="small" plain round
                                       @click="confirmDeleteType(row.id)">删除</el-button>
                        </div>
                    </template>
                </el-table-column>
            </el-table>
        </div>
        <el-dialog v-model="dialog.show" :title="dialog.edit ? '编辑分类' : '新增分类'" width="450px">
            <el-form label-width="80px">
                <el-form-item label="名称">
                    <el-input v-model="dialog.form.name" placeholder="请输入分类名称" :disabled="dialog.form.systemKey"/>
                </el-form-item>
                <el-form-item label="描述">
                    <el-input v-model="dialog.form.desc" type="textarea" :rows="3" placeholder="请输入分类描述"
                              :disabled="dialog.form.systemKey"/>
                </el-form-item>
                <el-form-item label="颜色">
                    <el-color-picker v-model="dialog.form.color"/>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialog.show = false">取消</el-button>
                <el-button type="primary" @click="submitType">确定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import {get, post} from "@/net"
import axios from "axios"
import {accessHeader} from "@/net"
import {reactive, ref} from "vue"
import {ElMessage, ElMessageBox} from "element-plus"
import {Plus} from "@element-plus/icons-vue"
import ColorDot from "@/components/ColorDot.vue"

const types = ref([])

const dialog = reactive({
    show: false,
    edit: false,
    id: null,
    form: { name: '', desc: '', color: '#409EFF', systemKey: null }
})

function loadTypes() {
    get('/api/admin/types', data => types.value = data)
}
loadTypes()

function openDialog(row) {
    if (row) {
        dialog.edit = true
        dialog.id = row.id
        dialog.form = { name: row.name, desc: row.desc, color: row.color, systemKey: row.systemKey }
    } else {
        dialog.edit = false
        dialog.id = null
        dialog.form = { name: '', desc: '', color: '#409EFF', systemKey: null }
    }
    dialog.show = true
}

function submitType() {
    if (!dialog.form.name) {
        ElMessage.warning('请填写分类名称')
        return
    }
    const params = `name=${encodeURIComponent(dialog.form.name)}&desc=${encodeURIComponent(dialog.form.desc)}&color=${encodeURIComponent(dialog.form.color)}`
    if (dialog.edit) {
        axios.put(`/api/admin/types/${dialog.id}?${params}`, null, { headers: accessHeader() })
            .then(({data}) => {
                if (data.code === 200) {
                    ElMessage.success('修改成功')
                    dialog.show = false
                    loadTypes()
                } else {
                    ElMessage.error(data.message)
                }
            })
    } else {
        post(`/api/admin/types?${params}`, null, () => {
            ElMessage.success('新增成功')
            dialog.show = false
            loadTypes()
        })
    }
}

function confirmDeleteType(id) {
    ElMessageBox.confirm('确定删除该分类吗？', '删除分类', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => deleteType(id)).catch(() => {})
}

function deleteType(id) {
    axios.delete(`/api/admin/types/${id}`, { headers: accessHeader() })
        .then(({data}) => {
            if (data.code === 200) {
                ElMessage.success('删除成功')
                loadTypes()
            } else {
                ElMessage.error(data.message)
            }
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
