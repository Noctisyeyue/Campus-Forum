<template>
    <div style="max-width: 960px;margin: 20px auto;padding: 0 20px 20px">
        <light-card>
            <div style="display: flex;justify-content: space-between;align-items: center;gap: 10px;flex-wrap: wrap">
                <div>
                    <div style="font-size: 20px;font-weight: bold">我的帖子</div>
                    <div class="text-secondary" style="font-size: 13px;margin-top: 4px">
                        展示你自己的帖子，按状态查看；已发布可进入详情，待审核仅允许删除。
                    </div>
                </div>
                <el-button type="primary" @click="router.push('/index')">返回帖子广场</el-button>
            </div>
        </light-card>

        <light-card style="margin-top: 10px">
            <el-tabs v-model="activeTab">
                <el-tab-pane label="全部" name="all"/>
                <el-tab-pane label="已发布" name="published"/>
                <el-tab-pane label="待审核" name="pending_review"/>
                <el-tab-pane label="已删除" name="deleted"/>
            </el-tabs>
        </light-card>

        <div style="margin-top: 10px;display: flex;flex-direction: column;gap: 10px" v-loading="loading">
            <light-card v-for="item in topics" :key="item.id" class="topic-card">
                <div style="display: flex;justify-content: space-between;gap: 12px;align-items: flex-start;flex-wrap: wrap">
                    <div style="flex: 1;min-width: 0">
                        <div style="display: flex;align-items: center;gap: 8px;flex-wrap: wrap">
                            <topic-tag :type="item.type"/>
                            <el-tag :type="statusTag(item.status)" size="small">{{ statusText(item.status) }}</el-tag>
                            <el-link v-if="canOpen(item.status)" type="primary"
                                     @click="router.push(`/index/topic-detail/${item.id}`)">
                                {{ item.title }}
                            </el-link>
                            <span v-else style="font-weight: bold">{{ item.title }}</span>
                        </div>
                        <div class="topic-meta">{{ new Date(item.time).toLocaleString() }}</div>
                    </div>
                    <div style="display: flex;gap: 12px;align-items: center;flex-wrap: wrap">
                        <el-link v-if="canEdit(item.status)" type="primary" @click="openEditor(item)">
                            {{ item.status === 'rejected' ? '修改后重新提交' : '编辑帖子' }}
                        </el-link>
                        <el-popconfirm v-if="canDelete(item.status)"
                                       title="确定删除这篇帖子吗？删除后将进入“已删除”列表。"
                                       confirm-button-text="确定"
                                       cancel-button-text="取消"
                                       @confirm="deleteTopic(item.id)">
                            <template #reference>
                                <el-link type="danger">删除帖子</el-link>
                            </template>
                        </el-popconfirm>
                    </div>
                </div>

                <div class="topic-content">{{ item.text || '该帖子暂无可展示摘要' }}</div>

                <div v-if="item.images?.length" class="topic-images">
                    <el-image class="topic-image" v-for="img in item.images" :key="img" :src="img" fit="cover"/>
                </div>

                <el-alert v-if="item.status === 'rejected' && item.reviewReason"
                          :title="`拒绝原因：${item.reviewReason}`"
                          type="error" :closable="false" style="margin-top: 10px"/>
                <el-alert v-if="item.status === 'hidden' && item.hideReason"
                          :title="`下架原因：${item.hideReason}`"
                          type="warning" :closable="false" style="margin-top: 10px"/>
                <el-alert v-if="item.status === 'pending_review'"
                          title="帖子正在审核中，审核完成前不可查看详情或编辑。"
                          type="info" :closable="false" style="margin-top: 10px"/>

                <div class="topic-footer" v-if="item.status === 'published'">
                    <div>
                        <el-icon style="vertical-align: middle"><CircleCheck/></el-icon> {{ item.like }}点赞
                    </div>
                    <div>
                        <el-icon style="vertical-align: middle"><Star/></el-icon> {{ item.collect }}收藏
                    </div>
                </div>
            </light-card>

            <light-card v-if="!loading && !topics.length">
                <el-empty :image-size="90" description="当前筛选下还没有帖子"/>
            </light-card>

            <div v-if="topics.length && !end" style="display: flex;justify-content: center">
                <el-button plain :loading="loadingMore" @click="loadMore">加载更多</el-button>
            </div>
        </div>

        <topic-editor :show="editor.show"
                      header-title="编辑帖子"
                      submit-button="提交修改"
                      :default-type="editor.type"
                      :default-title="editor.title"
                      :default-text="editor.text"
                      :submit="submitUpdate"
                      @success="onEditorSuccess"
                      @close="editor.show = false"/>
    </div>
</template>

<script setup>
defineOptions({ name: 'MyTopicList' })

import {get, post} from "@/net";
import {reactive, ref, watch} from "vue";
import {CircleCheck, Star} from "@element-plus/icons-vue";
import {ElMessage} from "element-plus";
import router from "@/router";
import LightCard from "@/components/LightCard.vue";
import TopicTag from "@/components/TopicTag.vue";
import TopicEditor from "@/components/TopicEditor.vue";
import {useStore} from "@/stores/index";

/** 全局状态管理实例 */
const store = useStore()

/** 当前激活的 Tab 标签，值为状态类型或 'all' */
const activeTab = ref('all')
/** 当前筛选下的帖子列表 */
const topics = ref([])
/** 首次加载中状态 */
const loading = ref(false)
/** 加载更多中状态 */
const loadingMore = ref(false)
/** 当前页码 */
const page = ref(0)
/** 是否已加载完全部数据 */
const end = ref(false)

/** 编辑器弹窗状态及待编辑帖子数据 */
const editor = reactive({
    /** 是否显示编辑器 */
    show: false,
    /** 待编辑帖子 ID */
    id: null,
    /** 待编辑帖子类型 */
    type: null,
    /** 待编辑帖子标题 */
    title: '',
    /** 待编辑帖子内容 */
    text: ''
})

/** 监听 Tab 切换，重置并重新加载列表 */
watch(activeTab, () => resetList(), { immediate: true })

/** 首次加载时获取帖子分类列表并存入全局 store */
if (!store.forum.types.length) {
    get('/api/forum/types', data => {
        const array = []
        array.push({name: '全部', id: 0, color: 'linear-gradient(45deg, white, red, orange, gold, green, blue)'})
        data.forEach(d => array.push(d))
        store.forum.types = array
    })
}

/**
 * 重置列表状态（清空数据、重置页码）并重新加载
 *
 * @return {void}
 */
function resetList() {
    topics.value = []
    page.value = 0
    end.value = false
    loadList(false)
}

/**
 * 加载我的帖子列表，支持追加或替换模式
 *
 * @param {boolean} append 是否追加模式（加载更多时为 true）
 * @return {void}
 */
function loadList(append) {
    if (end.value) return
    const status = activeTab.value === 'all' ? '' : `&status=${activeTab.value}`
    const request = `/api/forum/my-topics?page=${page.value}${status}`
    if (append) loadingMore.value = true
    else loading.value = true
    get(request, data => {
        const list = data || []
        topics.value = append ? [...topics.value, ...list] : list
        page.value++
        end.value = list.length < 10
        loading.value = false
        loadingMore.value = false
    }, () => {
        loading.value = false
        loadingMore.value = false
    })
}

/**
 * 加载更多帖子（追加模式）
 *
 * @return {void}
 */
function loadMore() {
    loadList(true)
}

/**
 * 根据帖子状态返回对应的 Tag 类型（用于 el-tag 组件）
 *
 * @param {string} status 帖子状态
 * @return {string} el-tag 类型名称
 */
function statusTag(status) {
    const map = {
        pending_review: 'warning',
        published: 'success',
        rejected: 'danger',
        hidden: 'info',
        deleted: 'info'
    }
    return map[status] || 'info'
}

/**
 * 根据帖子状态返回中文显示文本
 *
 * @param {string} status 帖子状态
 * @return {string} 状态中文描述
 */
function statusText(status) {
    const map = {
        pending_review: '待审核',
        published: '已发布',
        rejected: '已拒绝',
        hidden: '已下架',
        deleted: '已删除'
    }
    return map[status] || status
}

/**
 * 判断帖子是否可以打开详情
 *
 * @param {string} status 帖子状态
 * @return {boolean} 是否可打开
 */
function canOpen(status) {
    return status === 'published'
}

/**
 * 判断帖子是否可以编辑
 *
 * @param {string} status 帖子状态
 * @return {boolean} 是否可编辑
 */
function canEdit(status) {
    return status === 'published' || status === 'rejected'
}

/**
 * 判断帖子是否可以删除
 *
 * @param {string} status 帖子状态
 * @return {boolean} 是否可删除
 */
function canDelete(status) {
    return status === 'published' || status === 'pending_review'
}

/**
 * 打开编辑器，先从后端获取帖子完整内容再填充编辑器表单
 *
 * @param {Object} item 帖子摘要对象
 * @return {void}
 */
function openEditor(item) {
    get(`/api/forum/my-topic?tid=${item.id}`, data => {
        if (!data) {
            ElMessage.error('帖子不存在或你无权编辑')
            return
        }
        editor.id = data.id
        editor.type = data.type
        editor.title = data.title
        editor.text = data.content
        editor.show = true
    })
}

/**
 * 提交帖子更新，调用后端更新接口
 *
 * @param {Object} form 编辑器表单数据
 * @param {Function} success 更新成功后的回调
 * @return {void}
 */
function submitUpdate(form, success) {
    post('/api/forum/update-topic', {
        id: editor.id,
        type: form.type.id,
        title: form.title,
        content: form.text
    }, () => {
        ElMessage.success('帖子已更新，等待管理员审核！')
        success()
    })
}

/**
 * 编辑器提交成功后的回调，关闭编辑器并刷新列表
 *
 * @return {void}
 */
function onEditorSuccess() {
    editor.show = false
    resetList()
}

/**
 * 删除指定帖子，成功后刷新列表
 *
 * @param {number} id 帖子 ID
 * @return {void}
 */
function deleteTopic(id) {
    post(`/api/forum/delete-topic?tid=${id}`, null, () => {
        ElMessage.success('帖子已删除')
        resetList()
    })
}
</script>

<style scoped lang="less">
.topic-card {
    padding: 16px;
}

.topic-meta {
    font-size: 13px;
    color: grey;
    margin-top: 8px;
}

:global(.dark) .topic-meta {
    color: #a0a3a8;
}

.topic-content {
    font-size: 13px;
    color: grey;
    margin: 10px 0 0;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 3;
    overflow: hidden;
    text-overflow: ellipsis;
}

:global(.dark) .topic-content {
    color: #a0a3a8;
}

.topic-images {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 10px;
    margin-top: 10px;
}

.topic-image {
    width: 100%;
    height: 110px;
    border-radius: 5px;
}

.topic-footer {
    display: flex;
    gap: 20px;
    font-size: 13px;
    margin-top: 12px;
    opacity: 0.8;
}
</style>
