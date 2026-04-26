<template>
    <div style="padding: 20px">
        <card>
            <div style="display: flex;justify-content: space-between;align-items: center">
                <span style="font-size: 18px;font-weight: bold">分类管理</span>
                <el-button type="primary" @click="openDialog()" :icon="Plus">新增分类</el-button>
            </div>
        </card>
        <card style="margin-top: 10px">
            <el-table :data="types" stripe style="width: 100%">
                <el-table-column prop="id" label="ID" width="80"/>
                <el-table-column label="颜色" width="80">
                    <template #default="{ row }">
                        <color-dot :color="row.color"/>
                    </template>
                </el-table-column>
                <el-table-column prop="name" label="名称"/>
                <el-table-column prop="desc" label="描述"/>
                <el-table-column label="类型" width="120">
                    <template #default="{ row }">
                        <el-tag v-if="row.systemKey" type="warning">系统分类</el-tag>
                        <el-tag v-else type="info">普通分类</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="180">
                    <template #default="{ row }">
                        <el-link type="primary" @click="openDialog(row)">&nbsp;编辑</el-link>
                        <el-popconfirm v-if="!row.systemKey" title="确定删除该分类吗？" @confirm="deleteType(row.id)">
                            <template #reference>
                                <el-link type="danger" style="margin-left: 15px">&nbsp;删除</el-link>
                            </template>
                        </el-popconfirm>
                        <span v-else style="margin-left: 15px;font-size: 12px;color: grey">仅可改颜色</span>
                    </template>
                </el-table-column>
            </el-table>
        </card>
        <el-dialog v-model="dialog.show" :title="dialog.edit ? '编辑分类' : '新增分类'" width="450px">
            <el-form label-width="80px">
                <el-form-item label="名称">
                    <el-input v-model="dialog.form.name" placeholder="请输入分类名称" :disabled="dialog.form.systemKey"/>
                </el-form-item>
                <el-form-item label="描述">
                    <el-input v-model="dialog.form.desc" type="textarea" :rows="3" placeholder="请输入分类描述" :disabled="dialog.form.systemKey"/>
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
import {ElMessage} from "element-plus"
import {Plus} from "@element-plus/icons-vue"
import Card from "@/components/Card.vue"
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
