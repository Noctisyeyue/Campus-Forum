<template>
    <div style="padding: 20px;max-width: 1100px">
        <card>
            <div style="font-size: 20px;font-weight: bold">发布教务通知</div>
            <div style="font-size: 13px;color: grey;margin-top: 6px">
                发布后直接进入已发布状态，详情页不显示评论区，但仍允许点赞和收藏。
            </div>
        </card>

        <card style="margin-top: 10px">
            <el-form label-width="100px">
                <el-form-item label="通知标题">
                    <el-input v-model="form.title" maxlength="30" placeholder="请输入通知标题"/>
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
    content: new Delta()
})

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
