<template>
    <div style="display: flex;margin: 20px auto;gap: 20px;max-width: 900px;padding: 0 20px">
        <!-- ==================== 左侧主内容区 ==================== -->
        <div style="flex: 1">
            <!-- 搜索关键词提示条，有搜索词时显示 -->
            <light-card v-if="topics.search" style="display: flex;align-items: center;justify-content: space-between">
                <span style="font-size: 14px;color: #606266">
                    搜索: <b>{{ topics.search }}</b>
                </span>
                <el-link type="info" :underline="false" @click="clearSearch">清除搜索</el-link>
            </light-card>
            <!-- 发帖入口 -->
            <light-card>
                <div class="create-topic" @click="editor = true">
                    <el-icon><EditPen/></el-icon> 点击发表主题...
                </div>
                <div class="text-secondary" style="margin-top: 10px;display: flex;gap: 13px;font-size: 18px">
                    <el-icon><Edit /></el-icon>
                    <el-icon><Document /></el-icon>
                    <el-icon><Compass /></el-icon>
                    <el-icon><Picture /></el-icon>
                    <el-icon><Microphone /></el-icon>
                </div>
            </light-card>
            <!-- 置顶帖子列表 -->
            <light-card style="margin-top: 10px;display: flex;flex-direction: column;gap: 10px">
                <div v-for="item in topics.top" class="top-topic" @click="openTopicDetail(item.id)">
                    <el-tag type="info" size="small">置顶</el-tag>
                    <div>{{item.title}}</div>
                    <div>{{new Date(item.time).toLocaleDateString()}}</div>
                </div>
            </light-card>
            <!-- 分类筛选 + 排序选择 -->
            <light-card style="margin-top: 10px;display: flex;align-items: center;gap: 7px;flex-wrap: wrap">
                <div :class="`type-select-card ${topics.type === item.id ? 'active' : ''}`"
                     v-for="item in normalTypes"
                     @click="topics.type = item.id">
                    <color-dot :color="item.color"/>
                    <span style="margin-left: 5px">{{item.name}}</span>
                </div>
                <div style="margin-left:auto">
                    <el-select v-model="topics.sort" style="width: 120px" size="small" @change="resetList">
                        <el-option value="time" label="最新发布"/>
                        <el-option value="views" label="最多浏览"/>
                        <el-option value="likes" label="最多点赞"/>
                        <el-option value="collects" label="最多收藏"/>
                        <el-option value="comments" label="最多评论"/>
                    </el-select>
                </div>
            </light-card>
            <!-- 帖子列表（无限滚动加载） -->
            <transition name="el-fade-in" mode="out-in">
                <div v-if="topics.list.length">
                    <div style="margin-top: 10px;display: flex;flex-direction: column;gap: 10px"
                         v-infinite-scroll="updateList">
                        <light-card v-for="item in topics.list" class="topic-card"
                                    @click="openTopicDetail(item.id)">
                            <!-- 帖子作者头像和名称 -->
                            <div style="display: flex">
                                <div>
                                    <el-avatar :size="30" :src="store.avatarUserUrl(item.avatar)"
                                               :style="!item.avatar ? { background: store.avatarColor(item.username) } : {}">
                                        {{ item.avatar ? '' : store.avatarText(item.username) }}
                                    </el-avatar>
                                </div>
                                <div style="margin-left: 7px;transform: translateY(-2px)">
                                    <div style="font-size: 13px;font-weight: bold">{{item.username}}</div>
                                    <div class="text-secondary" style="font-size: 12px">
                                        <el-icon><Clock/></el-icon>
                                        <div style="margin-left: 2px;display: inline-block;transform: translateY(-2px)">
                                            {{new Date(item.time).toLocaleString()}}
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <!-- 帖子分类标签 + 标题 -->
                            <div style="margin-top: 5px">
                                <topic-tag :type="item.type"/>
                                <span style="font-weight: bold;margin-left: 7px">{{item.title}}</span>
                            </div>
                            <!-- 帖子内容摘要（最多显示3行） -->
                            <div class="topic-content">{{item.text}}</div>
                            <!-- 帖子图片（三列网格） -->
                            <div style="display: grid;grid-template-columns: repeat(3, 1fr);grid-gap: 10px">
                                <el-image class="topic-image" v-for="img in item.images" :src="img" fit="cover"></el-image>
                            </div>
                            <!-- 帖子互动数据 -->
                            <div style="display: flex;gap: 20px;font-size: 13px;margin-top: 10px;opacity: 0.8">
                                <div>
                                    <el-icon style="vertical-align: middle"><View/></el-icon> {{item.viewCount}}浏览
                                </div>
                                <div>
                                    <el-icon style="vertical-align: middle"><CircleCheck/></el-icon> {{item.like}}点赞
                                </div>
                                <div>
                                    <el-icon style="vertical-align: middle"><Star/></el-icon> {{item.collect}}收藏
                                </div>
                                <div>
                                    <el-icon style="vertical-align: middle"><ChatSquare/></el-icon> {{item.comments}}评论
                                </div>
                            </div>
                        </light-card>
                    </div>
                </div>
                <!-- 无帖子时的空状态 -->
                <div v-else-if="topics.end" style="margin-top: 10px">
                    <light-card>
                        <el-empty :image-size="90" :description="topics.search ? '没有找到相关帖子' : '暂时没有帖子'"/>
                    </light-card>
                </div>
            </transition>
        </div>
        <!-- ==================== 右侧边栏 ==================== -->
        <div style="width: 280px">
            <div style="position: sticky;top: 20px">
                <!-- 我的收藏入口 -->
                <light-card>
                    <div class="collect-list-button" @click="collects = true">
                        <span><el-icon><FolderOpened /></el-icon> 查看我的收藏</span>
                        <el-icon style="transform: translateY(3px)"><ArrowRightBold/></el-icon>
                    </div>
                </light-card>
                <!-- 论坛公告 -->
                <light-card style="margin-top: 10px">
                    <div style="font-weight: bold">
                        <el-icon><CollectionTag/></el-icon>
                        论坛公告
                    </div>
                    <el-divider style="margin: 10px 0"/>
                    <div class="text-secondary" style="font-size: 14px;margin: 10px;white-space: pre-wrap;line-height: 1.7">
                        {{ notice.content || '暂无论坛公告' }}
                    </div>
                    <div v-if="notice.updateTime" class="text-secondary" style="font-size: 12px;text-align: right">
                        更新于 {{ new Date(notice.updateTime).toLocaleString() }}
                    </div>
                </light-card>
                <!-- 天气信息 -->
                <light-card style="margin-top: 10px">
                    <div style="font-weight: bold">
                        <el-icon><Calendar/></el-icon>
                        天气信息
                    </div>
                    <el-divider style="margin: 10px 0"/>
                    <weather :data="weather"/>
                </light-card>
                <!-- 日期信息 -->
                <light-card style="margin-top: 10px">
                    <div class="info-text">
                        <div>当前日期</div>
                        <div>{{today}}</div>
                    </div>
                </light-card>
                <!-- 友情链接 -->
                <div class="text-secondary" style="font-size: 14px;margin-top: 10px">
                    <el-icon><Link/></el-icon>
                    友情链接
                    <el-divider style="margin: 10px 0"/>
                </div>
                <div style="display: grid;grid-template-columns: repeat(2, 1fr);grid-gap: 10px;margin-top: 10px">
                    <div class="friend-link">
                        <el-image style="height: 100%" src="https://element-plus.org/images/js-design-banner.jpg"/>
                    </div>
                    <div class="friend-link">
                        <el-image style="height: 100%" src="https://element-plus.org/images/vform-banner.png"/>
                    </div>
                </div>
            </div>
        </div>
        <!-- 发帖编辑器弹窗 -->
        <topic-editor :show="editor" @success="onTopicCreate" @close="editor = false"/>
        <!-- 收藏列表弹窗 -->
        <topic-collect-list :show="collects" @close="collects = false"/>
    </div>
</template>

<script setup>
/** 组件名（用于 keep-alive 缓存和 devtools 调试） */
defineOptions({ name: 'TopicList' })

import LightCard from "@/components/LightCard.vue";
import {
    Calendar,
    Clock,
    CollectionTag,
    Compass,
    Document,
    Edit,
    EditPen,
    Link,
    Picture,
    Microphone, CircleCheck, Star, FolderOpened, ArrowRightBold, View, ChatSquare
} from "@element-plus/icons-vue";
import Weather from "@/components/Weather.vue";
import {computed, onActivated, onDeactivated, reactive, ref, watch} from "vue";
import {useRoute} from "vue-router";
import {get} from "@/net";
import {ElMessage} from "element-plus";
import TopicEditor from "@/components/TopicEditor.vue";
import {useStore} from "@/stores/index";
import ColorDot from "@/components/ColorDot.vue";
import router from "@/router";
import TopicTag from "@/components/TopicTag.vue";
import TopicCollectList from "@/components/TopicCollectList.vue";
import {restoreForumScroll, saveForumScroll} from "@/utils/forumScroll";

/** Pinia 全局状态 */
const store = useStore()
/** 当前路由信息 */
const route = useRoute()

/** 天气数据（从后端API获取） */
const weather = reactive({
    location: {},
    now: {},
    hourly: [],
    success: false
})

/** 是否显示发帖编辑器弹窗 */
const editor = ref(false)

/** 论坛公告内容 */
const notice = reactive({
    content: '',
    updateTime: null
})

/**
 * 帖子列表核心数据
 * - list: 当前已加载的帖子数组
 * - type: 当前选中的分类ID（0=全部）
 * - sort: 排序方式（time/views/likes/collects/comments）
 * - page: 当前页码（从0开始，无限滚动翻页）
 * - end: 是否已加载完全部帖子
 * - loading: 请求锁，true=正在请求中，防止并发重复加载  false=没在请求，可以发起新请求
 * - top: 置顶帖子列表
 * - search: 搜索关键词
 */
const topics = reactive({
    list: [],
    type: 0,
    sort: 'time',
    page: 0,
    end: false,
    loading: false,
    top: [],
    search: ''
})

/** 是否显示收藏列表弹窗 */
const collects = ref(false)

/** 非系统分类列表（过滤掉 activity/notice 等系统内置分类） */
const normalTypes = computed(() => store.forum.types.filter(item => !item.systemKey))

/** 监听分类切换，重新加载帖子列表 */
watch(() => topics.type, () => resetList())

/** 监听路由中的搜索参数变化，更新搜索关键词并重新加载 */
watch(() => route.query.search, val => {
    // val 是变化后的新值   有值就赋值，没值就设为空字符串
    topics.search = val || ''
    resetList()       // 重新加载列表
}, { immediate: true })// 组件一创建就立即执行一次

/** 页面被激活时（keep-alive 缓存恢复），如果列表为空则加载 */
onActivated(() => {
    if (!topics.list.length && !topics.end) updateList()
    restoreForumScroll('/index')
})

/** 页面被缓存切走时关闭抽屉并记录滚动位置 */
onDeactivated(() => {
    collects.value = false
    editor.value = false
    saveForumScroll('/index')
})

/** 当前日期（格式：2026 年 5 月 27 日） */
const today = computed(() => {
    const date = new Date()
    return `${date.getFullYear()} 年 ${date.getMonth() + 1} 月 ${date.getDate()} 日`
})

/** 加载置顶帖子 */
get('/api/forum/top-topic', data => topics.top = data)

/** 加载论坛公告 */
get('/api/forum/notice', data => Object.assign(notice, data || {}))

/**
 * 无限滚动加载：请求下一页帖子并追加到列表
 * 已加载完全部帖子时直接返回
 */
function updateList(){
    // 防抖：数据已全部加载完 或 正在加载中 → 直接返回，不发请求
    if(topics.end || topics.loading) return
    // 标记为"正在加载"，防止重复请求
    topics.loading = true
    let url = `/api/forum/list-topic?page=${topics.page}&type=${topics.type}&sort=${topics.sort}`
    // 如果有搜索关键词，追加 &title=xxx（encodeURIComponent 防止特殊字符破坏 URL）
    if (topics.search) url += `&title=${encodeURIComponent(topics.search)}`
    get(url, data => {
        if(data) {
            data.forEach(d => topics.list.push(d))
            topics.page++
        }
        if(!data || data.length < 10)
            topics.end = true
        topics.loading = false  // 加载完成，解除锁定
    }, () => {
        topics.loading = false   // 请求失败也要解除锁定
    })
}

/**
 * 发帖成功回调：关闭编辑器，刷新帖子列表
 */
function onTopicCreate() {
    editor.value = false
    resetList()
}

/**
 * 跳转到帖子详情页
 *
 * @param id 帖子ID
 */
function openTopicDetail(id) {
    saveForumScroll('/index')
    router.push('/index/topic-detail/' + id)
}

/** 重置列表状态并重新加载（切换分类/排序/搜索时调用） */
function resetList() {
    topics.page = 0
    topics.end = false
    topics.loading = false
    topics.list = []
    updateList()
}

/** 清除搜索关键词，恢复全部帖子 */
function clearSearch() {
    topics.search = ''
    router.replace({ path: '/index' })
}

/** 获取地理位置并加载天气数据（定位失败时使用北京坐标作为默认） */
navigator.geolocation.getCurrentPosition(position => {
    const longitude = position.coords.longitude
    const latitude = position.coords.latitude
    get(`/api/forum/weather?longitude=${longitude}&latitude=${latitude}`, data => {
        Object.assign(weather, data)           //把 data 的所有属性复制到 weather 上
        weather.success = true
    }, () => weather.success = true)
}, error => {
    console.info(error)
    get(`/api/forum/weather?longitude=116.40529&latitude=39.90499`, data => {
        Object.assign(weather, data)
        weather.success = true
    }, () => weather.success = true)
}, {
    timeout: 3000,             // 3秒内没定位到就算超时
    enableHighAccuracy: true   // 优先用GPS
})
</script>

<style lang="less" scoped>
/** 收藏列表入口按钮 */
.collect-list-button {
    font-size: 14px;
    display: flex;
    justify-content: space-between;
    transition: .3s;

    &:hover {
        cursor: pointer;
        opacity: 0.6;
    }
}

/** 置顶帖子行 */
.top-topic {
    display: flex;

    div:first-of-type {
        font-size: 14px;
        margin-left: 10px;
        font-weight: bold;
        opacity: 0.8;
        transition: color .3s;

        &:hover {
            color: grey;
        }
    }

    div:nth-of-type(2) {
        flex: 1;
        color: grey;
        font-size: 13px;
        text-align: right;
    }

    &:hover {
        cursor: pointer;
    }
}

/** 分类选择小卡片 */
.type-select-card {
    background-color: #f5f5f5;
    padding: 2px 7px;
    font-size: 14px;
    border-radius: 3px;
    box-sizing: border-box;
    transition: background-color .3s;

    &.active {
        border: solid 1px #ead4c4;
    }

    &:hover {
        cursor: pointer;
        background-color: #dadada;
    }
}

/** 帖子卡片 */
.topic-card {
    padding: 15px;
    transition: scale .3s;

    &:hover {
        scale: 1.015;
        cursor: pointer;
    }

    /** 帖子内容摘要，最多3行，超出部分省略号 */
    .topic-content {
        font-size: 13px;
        color: grey;
        margin: 5px 0;
        display: -webkit-box;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 3;
        overflow: hidden;
        text-overflow: ellipsis;
    }

    /** 帖子图片 */
    .topic-image {
        width: 100%;
        height: 100%;
        max-height: 110px;
        border-radius: 5px;
    }
}

/** 右侧信息文本（日期、IP等） */
.info-text {
    display: flex;
    justify-content: space-between;
    color: grey;
    font-size: 14px;
}

/** 友情链接图片容器 */
.friend-link {
    border-radius: 5px;
    overflow: hidden;
}

/** 发帖入口按钮 */
.create-topic {
    background-color: #efefef;
    border-radius: 5px;
    height: 40px;
    color: grey;
    font-size: 14px;
    line-height: 40px;
    padding: 0 10px;

    &:hover {
        cursor: pointer;
    }
}

/** 深色模式样式覆盖 */
.dark {
    .create-topic {
        background-color: #232323;
        color: #a0a3a8;
    }

    .type-select-card {
        background-color: #282828;

        &.active {
            border: solid 1px #64594b;
        }

        &:hover {
            background-color: #5e5e5e;
        }
    }

    .info-text {
        color: #a0a3a8;
    }

    .topic-card .topic-content {
        color: #a0a3a8;
    }

    .top-topic {
        div:first-of-type:hover {
            color: #a0a3a8;
        }

        div:nth-of-type(2) {
            color: #a0a3a8;
        }
    }
}
</style>
