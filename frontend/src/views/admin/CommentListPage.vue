<template>
    <div style="padding: 20px">
        <card>
            <span style="font-size: 18px;font-weight: bold">评论管理</span>
        </card>
        <card style="margin-top: 10px">
            <div style="display: flex;gap: 10px;margin-bottom: 15px">
                <el-input v-model="filter.tid" placeholder="帖子 ID" style="width: 150px" clearable/>
                <el-input v-model="filter.uid" placeholder="用户 ID" style="width: 150px" clearable/>
                <el-button type="primary" @click="loadComments(0)">搜索</el-button>
            </div>
            <el-table :data="comments" stripe v-loading="loading">
                <el-table-column prop="id" label="ID" width="100"/>
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
                        <el-tag :type="row.status === 'normal' ? 'success' : 'danger'" size="small">
                            {{ row.status === 'normal' ? '正常' : '已删除' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="100">
                    <template #default="{ row }">
                        <el-popconfirm title="确定删除该评论吗？" @confirm="deleteComment(row.id)"
                                       v-if="row.status === 'normal'">
                            <template #reference>
                                <el-link type="danger">&nbsp;删除</el-link>
                            </template>
                        </el-popconfirm>
                    </template>
                </el-table-column>
            </el-table>
            <div style="display: flex;justify-content: center;margin-top: 15px">
                <el-pagination background layout="prev, pager, next"
                               v-model:current-page="page" @current-change="p => loadComments(p - 1)"
                               :total="total" :page-size="15" hide-on-single-page/>
            </div>
        </card>
    </div>
</template>

<script setup>
import {get, post} from "@/net"
import {reactive, ref} from "vue"
import {ElMessage} from "element-plus"
import Card from "@/components/Card.vue"
import router from "@/router"

const comments = ref([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)

const filter = reactive({ tid: '', uid: '' })

function loadComments(p) {
    loading.value = true
    page.value = p + 1
    let url = `/api/admin/comments?page=${p}`
    if (filter.tid) url += `&tid=${filter.tid}`
    if (filter.uid) url += `&uid=${filter.uid}`
    get(url, data => {
        comments.value = data
        // 无法从列表接口获取总数，用返回条数判断是否有下一页
        total.value = (p + 1) * 15
        loading.value = false
    })
}
loadComments(0)

function deleteComment(id) {
    post(`/api/admin/comments/${id}/delete`, null, () => {
        ElMessage.success('删除成功')
        loadComments(page.value - 1)
    })
}
</script>
