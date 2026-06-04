<template>
    <div class="admin-page">
        <div class="admin-page-header">
            <div>
                <div class="admin-page-title">发布教务通知</div>
                <div class="admin-page-desc text-secondary">
                    发布后直接进入已发布状态，详情页不显示评论区，但仍允许点赞和收藏
                </div>
            </div>
        </div>
        <div class="admin-form-card">
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
                    <el-button type="primary" :loading="submitting" @click="submit">发布通知</el-button>
                </el-form-item>
            </el-form>
        </div>
    </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { Delta, QuillEditor } from "@vueup/vue-quill";
import '@vueup/vue-quill/dist/vue-quill.snow.css';
import { post } from "@/net";
import { ElMessage } from "element-plus";

/** 是否正在提交表单 */
const submitting = ref(false)
/** 教务通知发布表单数据 */
const form = reactive({
    title: '',
    content: new Delta()
})

/**
 * 提交教务通知表单
 * 校验标题非空后调用后端接口发布通知，成功后重置表单
 */
function submit() {
    if (!form.title) {
        ElMessage.warning('请先填写通知标题')
        return
    }
    submitting.value = true
    post('/api/admin/forum/publish-notice-topic', {
        title: form.title,
        content: form.content
    }, () => {
        submitting.value = false
        ElMessage.success('教务通知发布成功')
        form.title = ''
        form.content = new Delta()
    }, () => {
        submitting.value = false
    })
}
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
</style>
