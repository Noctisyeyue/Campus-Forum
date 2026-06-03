<template>
    <div class="message-page" :class="{ 'is-dark': store.dark }">
        <light-card>
            <div class="message-toolbar">
                <div>
                    <div class="message-title">消息列表</div>
                    <div class="message-subtitle text-secondary">
                        查看你的全部通知消息
                    </div>
                </div>
                <div class="message-toolbar-actions">
                    <template v-if="editing">
                        <el-button size="small" @click="cancelEdit">取消</el-button>
                        <el-button size="small" @click="toggleSelectAll">
                            {{ isAllSelected ? '取消全选' : '全选' }}
                        </el-button>
                        <el-button type="danger" size="small" @click="deleteSelected"
                                   :disabled="!selectedIds.length">
                            删除选中 ({{ selectedIds.length }})
                        </el-button>
                    </template>
                    <template v-else>
                        <el-button type="primary" size="small" @click="readAll" plain
                                   :disabled="!messages.length">全部标记已读</el-button>
                        <el-button size="small" @click="editing = true"
                                   :disabled="!messages.length">编辑</el-button>
                    </template>
                </div>
            </div>
        </light-card>
        <div class="message-list" v-loading="loading">
            <light-card
                v-for="item in messages"
                :key="item.id"
                class="message-card"
                :class="{
                    'is-unread': item.status === 'unread',
                    'is-editing': editing
                }">
                <div class="message-card-layout">
                    <label v-if="editing" class="message-select" @click.stop>
                        <el-checkbox
                            :model-value="selectedIds.includes(item.id)"
                            @change="val => toggleSelect(item.id, val)"/>
                    </label>
                    <div class="message-card-accent"></div>
                    <div
                        class="message-main"
                        :class="{ 'is-clickable': !editing }"
                        :role="editing ? undefined : 'button'"
                        :tabindex="editing ? -1 : 0"
                        @click="onItemClick(item)"
                        @keydown.enter.prevent="onItemClick(item)"
                        @keydown.space.prevent="onItemClick(item)">
                        <div class="message-head">
                            <div class="message-head-left">
                                <span class="message-status-dot"></span>
                                <el-tag size="small" :type="item.status === 'unread' ? 'danger' : 'info'">
                                    {{ item.status === 'unread' ? '未读' : '已读' }}
                                </el-tag>
                                <el-tag size="small" :type="item.type">消息</el-tag>
                                <span class="message-item-title">{{ item.title }}</span>
                            </div>
                            <div class="message-time text-secondary">
                                {{ formatTime(item.time) }}
                            </div>
                        </div>
                        <el-divider class="message-divider"/>
                        <div class="message-content text-secondary">
                            {{ item.content }}
                        </div>
                    </div>
                </div>
            </light-card>
        </div>
        <light-card v-if="!loading && !messages.length" style="margin-top: 10px">
            <el-empty :image-size="90" description="暂时没有任何消息"/>
        </light-card>
    </div>
</template>

<script setup>
defineOptions({ name: 'MessageList' })

import LightCard from "@/components/LightCard.vue";
import {ref, computed} from "vue";
import {get, post} from "@/net";
import {ElMessage} from "element-plus";
import router from "@/router";
import {useStore} from "@/stores/index";

/** 全局主题状态 */
const store = useStore()

/** 消息列表数据 */
const messages = ref([])
/** 选中的消息 ID 列表 */
const selectedIds = ref([])
/** 是否处于编辑模式 */
const editing = ref(false)
/** 是否全选 */
const isAllSelected = computed(() => messages.value.length > 0 && selectedIds.value.length === messages.value.length)
/** 加载状态 */
const loading = ref(true)

/** 加载全部消息 */
function loadMessages() {
    loading.value = true
    get('/api/notification/list', data => {
        messages.value = data
        loading.value = false
    }, () => loading.value = false)
}
loadMessages()

/**
 * 点击消息：编辑模式下勾选，普通模式下标记已读并跳转
 *
 * @param item 消息对象
 */
function onItemClick(item) {
    if (editing.value) {
        toggleSelect(item.id, !selectedIds.value.includes(item.id))
        return
    }
    if (item.status === 'unread') {
        get(`/api/notification/read?id=${item.id}`, () => loadMessages())
    }
    if (item.url && item.url !== 'null' && item.url !== 'undefined'
        && !item.url.endsWith('/null') && !item.url.endsWith('/undefined')) {
        router.push(item.url)
    }
}

/** 全选/取消全选 */
function toggleSelectAll() {
    if (isAllSelected.value) {
        selectedIds.value = []
    } else {
        selectedIds.value = messages.value.map(m => m.id)
    }
}

/** 勾选/取消勾选消息 */
function toggleSelect(id, checked) {
    if (checked) {
        if (!selectedIds.value.includes(id)) selectedIds.value.push(id)
    } else {
        selectedIds.value = selectedIds.value.filter(i => i !== id)
    }
}

/** 批量删除选中的消息 */
function deleteSelected() {
    if (!selectedIds.value.length) return
    post('/api/notification/delete-batch', selectedIds.value, () => {
        ElMessage.success(`已删除 ${selectedIds.value.length} 条消息`)
        cancelEdit()
        loadMessages()
    })
}

/** 全部标记已读 */
function readAll() {
    get('/api/notification/read-all', () => {
        ElMessage.success('已全部标记为已读')
        loadMessages()
    })
}

/** 退出编辑模式 */
function cancelEdit() {
    editing.value = false
    selectedIds.value = []
}

/**
 * 格式化时间显示
 *
 * @param time 时间字符串
 * @return 格式化后的时间
 */
function formatTime(time) {
    if (!time) return ''
    return new Date(time).toLocaleString()
}
</script>

<style lang="less" scoped>
.message-page {
    max-width: 980px;
    margin: 20px auto;
    padding: 0 20px 20px;
    --message-title-color: var(--el-text-color-primary);
    --message-card-border: #e5e7eb;
    --message-card-bg: var(--el-bg-color);
    --message-card-shadow: 0 10px 30px rgba(15, 23, 42, 0.04);
    --message-unread-border: #bfdbfe;
    --message-unread-bg: linear-gradient(180deg, #fdfefe 0%, #f8fbff 100%);
    --message-editing-border: #dbe2ea;
    --message-accent-muted: #dbe2ea;
    --message-accent-active: linear-gradient(180deg, #60a5fa 0%, #2563eb 100%);
    --message-focus-color: #60a5fa;
    --message-dot-color: #94a3b8;
    --message-dot-active: #2563eb;
    --message-dot-shadow: 0 0 0 4px rgba(37, 99, 235, 0.12);
    --message-item-title-color: #1e293b;
    --message-content-color: #64748b;
}

.message-page.is-dark {
    --message-card-border: #3a414d;
    --message-card-bg: #1d1f26;
    --message-card-shadow: 0 14px 32px rgba(0, 0, 0, 0.22);
    --message-unread-border: #365a8a;
    --message-unread-bg: linear-gradient(180deg, #202633 0%, #1b2230 100%);
    --message-editing-border: #4a5160;
    --message-accent-muted: #4a5160;
    --message-focus-color: #7cb2ff;
    --message-dot-color: #7f8ea3;
    --message-dot-active: #6ea8fe;
    --message-dot-shadow: 0 0 0 4px rgba(110, 168, 254, 0.16);
    --message-item-title-color: #e5eaf3;
    --message-content-color: #aab4c3;
}

.message-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 24px;
}

.message-title {
    font-size: 22px;
    font-weight: 700;
    color: var(--message-title-color);
    letter-spacing: .02em;
}

.message-subtitle {
    margin-top: 6px;
    font-size: 13px;
}

.message-toolbar-actions {
    display: flex;
    gap: 8px;
    flex-shrink: 0;
}

.message-list {
    margin-top: 12px;
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.message-card {
    position: relative;
    overflow: hidden;
    border: 1px solid var(--message-card-border);
    background: var(--message-card-bg);
    box-shadow: var(--message-card-shadow);
    transition: border-color .2s ease, box-shadow .2s ease, transform .2s ease, background-color .2s ease;

    &.is-unread {
        border-color: var(--message-unread-border);
        background: var(--message-unread-bg);
    }

    &.is-unread .message-item-title {
        color: var(--message-title-color);
        font-weight: 700;
    }

    &.is-unread .message-status-dot {
        background: var(--message-dot-active);
        box-shadow: var(--message-dot-shadow);
    }

    &.is-editing {
        border-color: var(--message-editing-border);
        box-shadow: 0 6px 20px rgba(15, 23, 42, 0.08);
    }
}

.message-card-layout {
    display: flex;
    align-items: stretch;
    gap: 14px;
}

.message-select {
    width: 28px;
    display: flex;
    align-items: center;
    justify-content: center;
    padding-top: 4px;
    flex-shrink: 0;
}

.message-card-accent {
    width: 4px;
    border-radius: 999px;
    background: var(--message-accent-muted);
    flex-shrink: 0;
}

.is-unread .message-card-accent {
    background: var(--message-accent-active);
}

.message-main {
    flex: 1;
    min-width: 0;
    padding: 2px 2px 2px 0;
    border-radius: 12px;
}

.message-main.is-clickable {
    cursor: pointer;
}

.message-main.is-clickable:hover {
    transform: translateY(-1px);
}

.message-main.is-clickable:focus-visible {
    outline: 2px solid var(--message-focus-color);
    outline-offset: 3px;
}

.message-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
}

.message-head-left {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
}

.message-status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: var(--message-dot-color);
    flex-shrink: 0;
    transition: background-color .2s ease, box-shadow .2s ease;
}

.message-item-title {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 15px;
    font-weight: 600;
    color: var(--message-item-title-color);
}

.message-time {
    font-size: 12px;
    white-space: nowrap;
    flex-shrink: 0;
}

.message-divider {
    margin: 10px 0 8px 0;
}

.message-content {
    font-size: 13px;
    line-height: 1.7;
    color: var(--message-content-color);
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
    word-break: break-all;
}
</style>
