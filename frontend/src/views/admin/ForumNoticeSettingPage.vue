<template>
    <div class="admin-page">
        <div class="admin-page-header">
            <div>
                <div class="admin-page-title">论坛公告设置</div>
                <div class="admin-page-desc text-secondary">
                    帖子广场右侧仅展示单条论坛公告，内容为纯文本
                </div>
            </div>
        </div>
        <div class="admin-form-card">
            <el-form label-width="100px" class="admin-form">
                <el-form-item label="公告正文" required>
                    <el-input v-model="form.content"
                              type="textarea"
                              :rows="10"
                              maxlength="2000"
                              show-word-limit
                              placeholder="请输入论坛公告正文"/>
                </el-form-item>
                <el-form-item v-if="notice.updateTime" label="最近更新">
                    <div class="update-info">
                        {{ new Date(notice.updateTime).toLocaleString() }}
                        <span v-if="notice.updateByName"> / {{ notice.updateByName }}</span>
                    </div>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" :loading="submitting" @click="submit">保存公告</el-button>
                </el-form-item>
            </el-form>
        </div>
    </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { get, post } from "@/net";
import { ElMessage } from "element-plus";

/** 是否正在提交表单 */
const submitting = ref(false)
/** 论坛公告元信息（更新时间、更新人） */
const notice = reactive({
    updateTime: null,
    updateByName: ''
})
/** 论坛公告表单数据 */
const form = reactive({
    content: ''
})

/**
 * 从后端加载当前论坛公告内容和元信息
 * @param {Function} callback - 请求成功后的回调函数，接收公告数据对象
 * @return {void}
 */
function loadNotice() {
    get('/api/admin/forum/notice', data => {
        Object.assign(notice, data || {})
        form.content = data?.content || ''
    })
}

/**
 * 提交论坛公告
 * 校验内容非空后调用后端接口保存公告，成功后重新加载公告数据
 * @return {void}
 */
function submit() {
    if (!form.content.trim()) {
        ElMessage.warning('公告正文不能为空')
        return
    }
    submitting.value = true
    post('/api/admin/forum/notice', { content: form.content }, () => {
        submitting.value = false
        ElMessage.success('论坛公告已保存')
        loadNotice()
    }, () => {
        submitting.value = false
    })
}

loadNotice()
</script>

<style lang="less" scoped>
.admin-page {
    padding: 20px 24px;
    max-width: 1100px;
}

.admin-page-header {
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

.update-info {
    color: var(--el-text-color-secondary);
    font-size: 13px;
}
</style>
