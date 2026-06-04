<template>
    <div class="admin-page">
        <div class="admin-page-header">
            <div>
                <div class="admin-page-title">发布校园活动</div>
                <div class="admin-page-desc text-secondary">
                    发布后直接进入已发布状态，并展示在"校园活动"独立入口
                </div>
            </div>
        </div>
        <div class="admin-form-card">
            <el-form label-width="100px" class="admin-form">
                <el-form-item label="活动标题" required>
                    <el-input v-model="form.title" maxlength="30" placeholder="请输入活动标题" show-word-limit/>
                </el-form-item>
                <el-form-item label="活动时间" required>
                    <el-date-picker v-model="form.activityTime" type="datetime"
                                    placeholder="选择活动时间" style="width: 100%"/>
                </el-form-item>
                <el-form-item label="活动地点" required>
                    <el-input v-model="form.location" maxlength="100" placeholder="请输入活动地点"/>
                </el-form-item>
                <el-form-item label="主办方" required>
                    <el-input v-model="form.organizer" maxlength="100" placeholder="请输入主办方"/>
                </el-form-item>
                <el-form-item label="报名截止">
                    <el-date-picker v-model="form.signupDeadline" type="datetime"
                                    placeholder="可选" style="width: 100%"/>
                </el-form-item>
                <el-form-item label="活动正文">
                    <div style="width: 100%">
                        <quill-editor v-model:content="form.content"
                                      content-type="delta"
                                      style="height: 360px"/>
                    </div>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" :loading="submitting" @click="submit">发布活动</el-button>
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
/** 活动发布表单数据 */
const form = reactive({
    title: '',
    activityTime: '',
    location: '',
    organizer: '',
    signupDeadline: '',
    content: new Delta()
})

/**
 * 提交校园活动表单
 * 校验必填字段后调用后端接口发布活动，成功后重置表单
 */
function submit() {
    if (!form.title || !form.activityTime || !form.location || !form.organizer) {
        ElMessage.warning('请先填写完整的活动信息')
        return
    }
    submitting.value = true
    post('/api/admin/forum/publish-activity', {
        title: form.title,
        activityTime: form.activityTime,
        location: form.location,
        organizer: form.organizer,
        signupDeadline: form.signupDeadline || null,
        content: form.content
    }, () => {
        submitting.value = false
        ElMessage.success('校园活动发布成功')
        form.title = ''
        form.activityTime = ''
        form.location = ''
        form.organizer = ''
        form.signupDeadline = ''
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
