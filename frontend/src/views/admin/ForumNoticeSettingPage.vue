<template>
    <div style="padding: 20px;max-width: 1100px">
        <card>
            <div style="font-size: 20px;font-weight: bold">论坛公告设置</div>
            <div style="font-size: 13px;color: grey;margin-top: 6px">
                帖子广场右侧仅展示单条论坛公告，内容为纯文本。
            </div>
        </card>

        <card style="margin-top: 10px">
            <el-form label-width="100px">
                <el-form-item label="公告正文">
                    <el-input v-model="form.content"
                              type="textarea"
                              :rows="10"
                              maxlength="2000"
                              show-word-limit
                              placeholder="请输入论坛公告正文"/>
                </el-form-item>
                <el-form-item v-if="notice.updateTime" label="最近更新">
                    <div style="color: grey">
                        {{ new Date(notice.updateTime).toLocaleString() }}
                        <span v-if="notice.updateByName"> / {{ notice.updateByName }}</span>
                    </div>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" :loading="submitting" @click="submit">保存公告</el-button>
                </el-form-item>
            </el-form>
        </card>
    </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { get, post } from "@/net";
import Card from "@/components/Card.vue";
import { ElMessage } from "element-plus";

const submitting = ref(false)
const notice = reactive({
    updateTime: null,
    updateByName: ''
})
const form = reactive({
    content: ''
})

function loadNotice() {
    get('/api/admin/forum/notice', data => {
        Object.assign(notice, data || {})
        form.content = data?.content || ''
    })
}

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
