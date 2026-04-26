<template>
    <div style="padding: 20px;max-width: 1100px">
        <card>
            <div style="font-size: 20px;font-weight: bold">发布校园活动</div>
            <div style="font-size: 13px;color: grey;margin-top: 6px">
                发布后直接进入已发布状态，并展示在“校园活动”独立入口。
            </div>
        </card>

        <card style="margin-top: 10px">
            <el-form label-width="100px">
                <el-form-item label="活动标题">
                    <el-input v-model="form.title" maxlength="30" placeholder="请输入活动标题"/>
                </el-form-item>
                <el-form-item label="活动时间">
                    <el-date-picker v-model="form.activityTime" type="datetime" placeholder="选择活动时间" style="width: 100%"/>
                </el-form-item>
                <el-form-item label="活动地点">
                    <el-input v-model="form.location" maxlength="100" placeholder="请输入活动地点"/>
                </el-form-item>
                <el-form-item label="主办方">
                    <el-input v-model="form.organizer" maxlength="100" placeholder="请输入主办方"/>
                </el-form-item>
                <el-form-item label="报名截止">
                    <el-date-picker v-model="form.signupDeadline" type="datetime" placeholder="可选" style="width: 100%"/>
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
        </card>
    </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { Delta, QuillEditor } from "@vueup/vue-quill";
import '@vueup/vue-quill/dist/vue-quill.snow.css';
import { post } from "@/net";
import Card from "@/components/Card.vue";
import { ElMessage } from "element-plus";

const submitting = ref(false)
const form = reactive({
    title: '',
    activityTime: '',
    location: '',
    organizer: '',
    signupDeadline: '',
    content: new Delta()
})

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
