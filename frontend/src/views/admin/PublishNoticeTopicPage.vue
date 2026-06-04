<template>
    <div class="admin-page">
        <div class="admin-page-header">
            <div>
                <div class="admin-page-title">{{ editId ? '编辑教务通知' : '发布教务通知' }}</div>
                <div class="admin-page-desc text-secondary">
                    {{ editId ? '修改后直接生效' : '发布后直接进入已发布状态，详情页不显示评论区，但仍允许点赞和收藏' }}
                </div>
            </div>
            <el-button v-if="editId" :icon="ArrowLeft" size="small" plain round @click="router.back()">返回</el-button>
        </div>
        <div class="admin-form-card" v-loading="loading">
            <el-form label-width="100px" class="admin-form">
                <el-form-item label="通知标题" required>
                    <el-input v-model="form.title" maxlength="30" placeholder="请输入通知标题" show-word-limit/>
                </el-form-item>
                <el-form-item label="通知正文">
                    <div style="width: 100%">
                        <quill-editor v-model:content="form.content"
                                      content-type="delta"
                                      style="height: 360px"/>
                    </div>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" :loading="submitting" @click="submit">
                        {{ editId ? '保存修改' : '发布通知' }}
                    </el-button>
                </el-form-item>
            </el-form>
        </div>
    </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { Delta, QuillEditor } from "@vueup/vue-quill";
import '@vueup/vue-quill/dist/vue-quill.snow.css';
import { get, post } from "@/net";
import { ElMessage } from "element-plus";
import { ArrowLeft } from "@element-plus/icons-vue";
import { useRoute } from "vue-router";
import router from "@/router";

const route = useRoute()
/** 编辑模式的帖子 ID（来自路由参数） */
const editId = route.query.id ? Number(route.query.id) : null
/** 页面加载状态（编辑模式下加载数据） */
const loading = ref(!!editId)
/** 是否正在提交表单 */
const submitting = ref(false)
/** 教务通知表单数据 */
const form = reactive({
    title: '',
    content: new Delta()
})

/**
 * 编辑模式下加载帖子数据回填表单
 */
onMounted(() => {
    if (editId) {
        get(`/api/admin/forum/notice-topic/${editId}`, data => {
            if (!data) {
                ElMessage.error('帖子不存在或无权编辑')
                router.back()
                return
            }
            form.title = data.title
            form.content = new Delta(JSON.parse(data.content))
            loading.value = false
        }, () => {
            ElMessage.error('加载失败')
            router.back()
        })
    }
})

/**
 * 提交表单（新建或更新）
 */
function submit() {
    if (!form.title) {
        ElMessage.warning('请先填写通知标题')
        return
    }
    submitting.value = true
    const body = {
        title: form.title,
        content: form.content
    }
    if (editId) {
        post(`/api/admin/topics/${editId}/edit`, body, () => {
            submitting.value = false
            ElMessage.success('教务通知已更新')
            router.push(`/admin/topic-detail/${editId}`)
        }, () => {
            submitting.value = false
        })
    } else {
        post('/api/admin/forum/publish-notice-topic', body, () => {
            submitting.value = false
            ElMessage.success('教务通知发布成功')
            form.title = ''
            form.content = new Delta()
        }, () => {
            submitting.value = false
        })
    }
}
</script>

<style lang="less" scoped>
.admin-page {
    padding: 20px 24px;
    max-width: 1100px;
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

.admin-form-card {
    background: var(--el-bg-color);
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    padding: 24px;
}

.admin-form {
    max-width: 700px;
}
</style>
