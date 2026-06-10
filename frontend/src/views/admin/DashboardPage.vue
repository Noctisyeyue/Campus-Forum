<template>
    <div class="dashboard-page" v-loading="loading">
        <card>
            <div class="dashboard-header">
                <div>
                    <div class="dashboard-title">数据看板</div>
                    <div class="dashboard-subtitle text-secondary">
                        聚合查看论坛总览、审核待办和社区运营数据
                    </div>
                </div>
                <el-button size="small" @click="loadDashboard" :disabled="loading">刷新数据</el-button>
            </div>
        </card>

        <div class="stat-grid">
            <card v-for="item in statCards" :key="item.label" class="stat-card-wrapper" @click="handleStatCardClick(item.key)">
                <div class="stat-card">
                    <div class="stat-icon" :style="{ background: item.bg }">
                        <el-icon :size="24" :color="item.color">
                            <component :is="item.icon"/>
                        </el-icon>
                    </div>
                    <div class="stat-body">
                        <div class="stat-value">{{ item.value ?? 0 }}</div>
                        <div class="stat-label text-secondary">{{ item.label }}</div>
                    </div>
                </div>
            </card>
        </div>

        <div class="trend-summary-grid">
            <card v-for="item in trendCards" :key="item.label">
                <div class="trend-card">
                    <div class="trend-card-head">
                        <div class="panel-title">{{ item.label }}</div>
                    </div>
                    <div class="trend-main-value">{{ item.current }}</div>
                    <div class="trend-subtitle text-secondary">
                        {{ item.desc }}
                    </div>
                </div>
            </card>
        </div>

        <div class="activity-chart-wrap">
            <card>
                <div class="panel-head trend-panel-head">
                    <div>
                        <div class="panel-title">{{ trendTitle }}</div>
                        <div class="panel-tip text-secondary">支持快捷范围和自定义日期筛选</div>
                    </div>
                    <div class="trend-toolbar">
                        <el-radio-group v-model="rangeMode" size="small" @change="handleRangeModeChange">
                            <el-radio-button label="7d">近7天</el-radio-button>
                            <el-radio-button label="30d">近30天</el-radio-button>
                        </el-radio-group>
                        <el-date-picker
                                v-model="customRange"
                                type="daterange"
                                range-separator="至"
                                start-placeholder="开始日期"
                                end-placeholder="结束日期"
                                value-format="YYYY-MM-DD"
                                :disabled-date="disableFutureDate"
                                unlink-panels
                                clearable
                                @change="handleCustomRangeChange"/>
                    </div>
                </div>
                <v-chart :option="activityTrendOption" class="activity-chart-panel" autoresize/>
            </card>
        </div>

        <div class="chart-grid">
            <card>
                <div class="panel-title">帖子状态分布</div>
                <v-chart :option="statusChartOption" class="chart-panel" autoresize/>
            </card>
            <card>
                <div class="panel-title">分类发帖 Top 5</div>
                <v-chart :option="typeChartOption" class="chart-panel" autoresize/>
            </card>
            <card>
                <div class="panel-head">
                    <div class="panel-title">举报原因分布</div>
                    <div class="report-type-tags">
                        <el-tag size="small" type="danger">帖子举报 {{ dashboard.reportTargetTypeMap.topic || 0 }}</el-tag>
                        <el-tag size="small" type="warning">评论举报 {{ dashboard.reportTargetTypeMap.comment || 0 }}</el-tag>
                    </div>
                </div>
                <v-chart :option="reportReasonChartOption" class="chart-panel" autoresize/>
            </card>
        </div>

        <div class="todo-grid">
            <card>
                <div class="panel-head">
                    <div class="panel-title">最近待审核帖子</div>
                    <el-button text @click="router.push('/admin/topics')">进入帖子管理</el-button>
                </div>
                <div v-if="dashboard.latestPendingTopics.length" class="todo-list">
                    <div v-for="item in dashboard.latestPendingTopics"
                         :key="item.id"
                         class="todo-item"
                         @click="router.push(`/admin/topic-detail/${item.id}`)">
                        <div class="todo-main">
                            <div class="todo-title">{{ item.title }}</div>
                            <div class="todo-meta text-secondary">
                                {{ item.username }} · {{ item.typeName }} · {{ formatTime(item.lastSubmitTime) }}
                            </div>
                        </div>
                        <el-tag size="small" type="warning">待审核</el-tag>
                    </div>
                </div>
                <el-empty v-else :image-size="80" description="当前没有待审核帖子"/>
            </card>

            <card>
                <div class="panel-head">
                    <div class="panel-title">最近待处理举报</div>
                    <el-button text @click="router.push('/admin/reports')">进入举报管理</el-button>
                </div>
                <div v-if="dashboard.latestPendingReports.length" class="todo-list">
                    <div v-for="item in dashboard.latestPendingReports"
                         :key="item.id"
                         class="todo-item"
                         @click="router.push('/admin/reports')">
                        <div class="todo-main">
                            <div class="todo-title">
                                {{ targetTypeName[item.targetType] || item.targetType }} · {{ item.targetSummary }}
                            </div>
                            <div class="todo-meta text-secondary">
                                举报人 {{ item.reporterName }} · 原因 {{ item.reason }} · {{ formatTime(item.time) }}
                            </div>
                        </div>
                        <el-tag size="small" type="danger">待处理</el-tag>
                    </div>
                </div>
                <el-empty v-else :image-size="80" description="当前没有待处理举报"/>
            </card>
        </div>

        <div class="ranking-grid">
            <card>
                <div class="panel-head">
                    <div class="panel-title">热门内容榜单</div>
                    <div class="text-secondary panel-tip">按浏览量优先，结合评论/点赞/收藏排序</div>
                </div>
                <div v-if="dashboard.hotTopics.length" class="ranking-list">
                    <div v-for="(item, index) in dashboard.hotTopics"
                         :key="item.id"
                         class="ranking-item"
                         @click="router.push(`/admin/topic-detail/${item.id}`)">
                        <div class="ranking-index">{{ index + 1 }}</div>
                        <div class="ranking-main">
                            <div class="ranking-title-row">
                                <div class="ranking-title">{{ item.title }}</div>
                                <el-tag size="small" :type="topicStatusTagType(item.status)">
                                    {{ statusName[item.status] || item.status }}
                                </el-tag>
                            </div>
                            <div class="ranking-meta text-secondary">
                                {{ item.username }} · {{ item.typeName }}
                            </div>
                            <div class="ranking-metrics">
                                <span>浏览 {{ item.viewCount }}</span>
                                <span>评论 {{ item.commentCount }}</span>
                                <span>点赞 {{ item.likeCount }}</span>
                                <span>收藏 {{ item.collectCount }}</span>
                            </div>
                        </div>
                    </div>
                </div>
                <el-empty v-else :image-size="80" description="暂无热门内容数据"/>
            </card>

            <card>
                <div class="panel-head">
                    <div class="panel-title">最近注册用户</div>
                    <el-button text @click="router.push('/admin/users')">进入用户管理</el-button>
                </div>
                <div v-if="dashboard.latestUsers.length" class="ranking-list">
                    <div v-for="item in dashboard.latestUsers"
                         :key="item.id"
                         class="ranking-item"
                         @click="router.push(`/admin/user-detail/${item.id}`)">
                        <el-avatar :src="store.avatarUserUrl(item.avatar)" :size="38" class="ranking-avatar"
                                   :style="!item.avatar ? { background: store.avatarColor(item.username) } : {}">
                            {{ item.avatar ? '' : store.avatarText(item.username) }}
                        </el-avatar>
                        <div class="ranking-main">
                            <div class="ranking-title-row">
                                <div class="ranking-title">{{ item.username }}</div>
                                <el-tag size="small" :type="userStatusTagType(item.status)">
                                    {{ item.status === 'active' ? '正常' : '已禁用' }}
                                </el-tag>
                            </div>
                            <div class="ranking-meta text-secondary">
                                {{ item.email }}
                            </div>
                            <div class="ranking-metrics">
                                <span>{{ item.role === 'super_admin' ? '超级管理员' : (item.role === 'admin' ? '管理员' : '普通用户') }}</span>
                                <span>注册于 {{ formatTime(item.registerTime) }}</span>
                            </div>
                        </div>
                    </div>
                </div>
                <el-empty v-else :image-size="80" description="暂无用户注册数据"/>
            </card>
        </div>
    </div>
</template>

<script setup>
import {get} from "@/net"
import {computed, ref} from "vue"
import {Bell, ChatDotSquare, Document, Lock, User, Warning} from "@element-plus/icons-vue"
import Card from "@/components/Card.vue"
import {useStore} from "@/stores/index"
import router from "@/router"
import VChart from "vue-echarts"
import {use} from "echarts/core"
import {CanvasRenderer} from "echarts/renderers"
import {BarChart, LineChart, PieChart} from "echarts/charts"
import {GridComponent, LegendComponent, TooltipComponent} from "echarts/components"

use([CanvasRenderer, PieChart, BarChart, LineChart, TooltipComponent, LegendComponent, GridComponent])

const store = useStore()
/** 页面数据加载状态 */
const loading = ref(true)
/** 活跃趋势的时间范围模式（'7d' / '30d' / 'custom'） */
const rangeMode = ref('7d')
/** 自定义日期范围选择值 */
const customRange = ref([])

/** 看板聚合数据 */
const dashboard = ref({
    overview: {
        totalUsers: 0,
        totalTopics: 0,
        totalComments: 0,
        pendingTopics: 0,
        pendingReports: 0,
        disabledUsers: 0
    },
    activityTrend: {
        topicSummary: { current: 0 },
        commentSummary: { current: 0 },
        userSummary: { current: 0 },
        points: []
    },
    topicStatusMap: {},
    topicTypeTop: [],
    reportReasonMap: {},
    reportTargetTypeMap: {},
    latestPendingTopics: [],
    latestPendingReports: [],
    hotTopics: [],
    latestUsers: []
})

/** 举报目标类型名称映射 */
const targetTypeName = {
    topic: '帖子',
    comment: '评论'
}

/** 帖子状态名称映射 */
const statusName = {
    pending_review: '待审核',
    published: '已发布',
    rejected: '已拒绝',
    hidden: '已隐藏',
    deleted: '已删除'
}

/** 顶部统计卡片配置列表，根据看板概览数据动态生成 */
const statCards = computed(() => {
    const d = store.dark
    const overview = dashboard.value.overview
    return [
        { key: 'topics', label: '总帖子数', value: overview.totalTopics, icon: Document, color: '#409EFF', bg: d ? '#1b3044' : '#ecf5ff' },
        { key: 'users', label: '总用户数', value: overview.totalUsers, icon: User, color: '#67C23A', bg: d ? '#203322' : '#f0f9eb' },
        { key: 'comments', label: '总评论数', value: overview.totalComments, icon: ChatDotSquare, color: '#E6A23C', bg: d ? '#382d1d' : '#fdf6ec' },
        { key: 'pendingTopics', label: '待审核帖子', value: overview.pendingTopics, icon: Warning, color: '#F56C6C', bg: d ? '#3b2225' : '#fef0f0' },
        { key: 'pendingReports', label: '待处理举报', value: overview.pendingReports, icon: Bell, color: '#F56C6C', bg: d ? '#3a2024' : '#fef0f0' },
        { key: 'disabledUsers', label: '已禁用用户', value: overview.disabledUsers, icon: Lock, color: '#909399', bg: d ? '#2a2d33' : '#f4f4f5' }
    ]
})

/** 活跃趋势汇总卡片配置列表（发帖、评论、注册） */
const trendCards = computed(() => {
    const trend = dashboard.value.activityTrend
    const label = trendSummaryLabel.value
    return [
        buildTrendCard(`${label}发帖`, trend.topicSummary, '统计区间内累计发帖数'),
        buildTrendCard(`${label}评论`, trend.commentSummary, '统计区间内累计评论数'),
        buildTrendCard(`${label}注册`, trend.userSummary, '统计区间内累计注册数')
    ]
})

/** 当前选中时间范围的中文标签 */
const rangeLabel = computed(() => {
    if (rangeMode.value === '30d') return '近 30 天'
    if (rangeMode.value === 'custom') {
        const [startDate, endDate] = customRange.value || []
        if (startDate && endDate) return `${startDate} 至 ${endDate}`
        return '自定义区间'
    }
    return '近 7 天'
})

/** 趋势汇总卡片的前缀标签（区间/近 N 天） */
const trendSummaryLabel = computed(() => rangeMode.value === 'custom' ? '区间' : rangeLabel.value)

/** 活跃趋势面板标题 */
const trendTitle = computed(() => `${rangeLabel.value}活跃趋势`)

/** 活跃趋势折线图 ECharts 配置（发帖/评论/注册） */
const activityTrendOption = computed(() => {
    const points = dashboard.value.activityTrend.points
    return {
        tooltip: { trigger: 'axis' },
        legend: {
            top: 0,
            textStyle: { color: store.dark ? '#cfd3dc' : '#606266' }
        },
        grid: { left: '3%', right: '3%', top: '16%', bottom: '4%', containLabel: true },
        xAxis: {
            type: 'category',
            data: points.map(item => item.date),
            boundaryGap: false,
            axisLabel: { color: store.dark ? '#cfd3dc' : '#606266' }
        },
        yAxis: {
            type: 'value',
            splitLine: { lineStyle: { color: store.dark ? '#31343b' : '#ebeef5' } }
        },
        series: [
            {
                name: '发帖',
                type: 'line',
                smooth: true,
                data: points.map(item => item.topics),
                symbolSize: 7,
                lineStyle: { width: 3, color: '#409EFF' },
                itemStyle: { color: '#409EFF' }
            },
            {
                name: '评论',
                type: 'line',
                smooth: true,
                data: points.map(item => item.comments),
                symbolSize: 7,
                lineStyle: { width: 3, color: '#E6A23C' },
                itemStyle: { color: '#E6A23C' }
            },
            {
                name: '注册',
                type: 'line',
                smooth: true,
                data: points.map(item => item.users),
                symbolSize: 7,
                lineStyle: { width: 3, color: '#67C23A' },
                itemStyle: { color: '#67C23A' }
            }
        ]
    }
})

/** 帖子状态分布饼图 ECharts 配置 */
const statusChartOption = computed(() => {
    const data = Object.entries(dashboard.value.topicStatusMap).map(([key, value]) => ({
        name: statusName[key] || key,
        value
    }))
    return {
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { bottom: 0, textStyle: { color: store.dark ? '#cfd3dc' : '#606266' } },
        series: [{
            type: 'pie',
            radius: ['42%', '70%'],
            data,
            label: { show: false },
            itemStyle: {
                borderRadius: 10,
                borderColor: store.dark ? '#1d1e1f' : '#fff',
                borderWidth: 2
            },
            emphasis: {
                label: { show: true, fontSize: 15, fontWeight: 'bold' }
            }
        }]
    }
})

/** 分类发帖 Top 5 横向柱状图 ECharts 配置 */
const typeChartOption = computed(() => {
    const rows = [...dashboard.value.topicTypeTop].reverse()
    return {
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: '4%', right: '4%', top: '4%', bottom: '3%', containLabel: true },
        xAxis: {
            type: 'value',
            splitLine: { lineStyle: { color: store.dark ? '#31343b' : '#ebeef5' } }
        },
        yAxis: {
            type: 'category',
            data: rows.map(item => item.name),
            axisLabel: { color: store.dark ? '#cfd3dc' : '#606266' }
        },
        series: [{
            type: 'bar',
            data: rows.map(item => item.value),
            barWidth: 18,
            itemStyle: {
                borderRadius: [0, 6, 6, 0],
                color: '#409EFF'
            }
        }]
    }
})

/** 举报原因分布横向柱状图 ECharts 配置 */
const reportReasonChartOption = computed(() => {
    const data = Object.entries(dashboard.value.reportReasonMap)
        .map(([name, value]) => ({ name, value }))
        .sort((a, b) => b.value - a.value)
    return {
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: '4%', right: '4%', top: '4%', bottom: '3%', containLabel: true },
        xAxis: {
            type: 'value',
            splitLine: { lineStyle: { color: store.dark ? '#31343b' : '#ebeef5' } }
        },
        yAxis: {
            type: 'category',
            data: [...data].reverse().map(item => item.name),
            axisLabel: { color: store.dark ? '#cfd3dc' : '#606266' }
        },
        series: [{
            type: 'bar',
            data: [...data].reverse().map(item => item.value),
            barWidth: 18,
            itemStyle: {
                borderRadius: [0, 6, 6, 0],
                color: '#E6A23C'
            }
        }]
    }
})

/**
 * 加载看板数据，根据当前时间范围向后台请求聚合数据
 */
function loadDashboard() {
    loading.value = true
    const params = buildRangeQuery()
    get(`/api/admin/dashboard${params}`, data => {
        dashboard.value = data
        loading.value = false
    }, () => loading.value = false)
}

/**
 * 将时间字符串格式化为本地可读时间
 * @param {string} time - ISO 时间字符串
 * @return {string} 格式化后的本地时间，无值时返回 '暂无时间'
 */
function formatTime(time) {
    if (!time) return '暂无时间'
    return new Date(time).toLocaleString()
}

/**
 * 禁用未来日期，用于日期选择器的 disabled-date 回调
 * @param {Date} date - 待判断的日期
 * @return {boolean} 是否为未来日期
 */
function disableFutureDate(date) {
    return date.getTime() > Date.now()
}

/**
 * 根据帖子状态返回对应的 Tag 类型名
 * @param {string} status - 帖子状态枚举值
 * @return {string} Element Plus Tag 类型（warning/success/danger/info）
 */
function topicStatusTagType(status) {
    return ({
        pending_review: 'warning',
        published: 'success',
        rejected: 'danger',
        hidden: 'info',
        deleted: 'info'
    })[status] || 'info'
}

/**
 * 根据用户状态返回对应的 Tag 类型名
 * @param {string} status - 用户状态枚举值（active/其他）
 * @return {string} Element Plus Tag 类型（success/danger）
 */
function userStatusTagType(status) {
    return status === 'active' ? 'success' : 'danger'
}

/**
 * 统计卡片点击处理，根据卡片 key 跳转到对应管理页面
 * @param {string} key - 卡片标识（topics/users/comments/pendingTopics/pendingReports/disabledUsers）
 */
function handleStatCardClick(key) {
    switch (key) {
        case 'topics':
            router.push('/admin/topics')
            break
        case 'users':
            router.push('/admin/users')
            break
        case 'comments':
            router.push('/admin/comments')
            break
        case 'pendingTopics':
            router.push({ path: '/admin/topics', query: { status: 'pending_review' } })
            break
        case 'pendingReports':
            router.push({ path: '/admin/reports', query: { status: 'pending' } })
            break
        case 'disabledUsers':
            router.push({ path: '/admin/users', query: { status: 'disabled' } })
            break
    }
}

/**
 * 构建趋势汇总卡片对象
 * @param {string} label - 卡片标题
 * @param {Object} summary - 汇总数据对象，包含 current 字段
 * @param {string} desc - 卡片描述文字
 * @return {{ label: string, current: number, desc: string }} 趋势卡片配置
 */
function buildTrendCard(label, summary = {}, desc) {
    const current = summary.current ?? 0
    return {
        label,
        current,
        desc
    }
}

/**
 * 根据当前时间范围模式构建查询参数字符串
 * @return {string} 以 ? 开头的 URL 查询参数
 */
function buildRangeQuery() {
    const { startDate, endDate } = resolveRange()
    return `?startDate=${startDate}&endDate=${endDate}`
}

/**
 * 根据当前范围模式解析出具体的起止日期
 * @return {{ startDate: string, endDate: string }} 起止日期字符串（YYYY-MM-DD 格式）
 */
function resolveRange() {
    const today = new Date()
    const format = date => {
        const year = date.getFullYear()
        const month = `${date.getMonth() + 1}`.padStart(2, '0')
        const day = `${date.getDate()}`.padStart(2, '0')
        return `${year}-${month}-${day}`
    }
    const shiftDate = offset => {
        const date = new Date(today)
        date.setDate(date.getDate() + offset)
        return date
    }
    if (rangeMode.value === '30d') {
        return { startDate: format(shiftDate(-29)), endDate: format(today) }
    }
    if (rangeMode.value === 'custom' && customRange.value?.length === 2) {
        return { startDate: customRange.value[0], endDate: customRange.value[1] }
    }
    return { startDate: format(shiftDate(-6)), endDate: format(today) }
}

/**
 * 快捷范围模式切换处理，切换到预设模式时清空自定义范围并刷新数据
 */
function handleRangeModeChange() {
    if (rangeMode.value === '7d' || rangeMode.value === '30d') {
        customRange.value = []
        loadDashboard()
    }
}

/**
 * 自定义日期范围变更处理，选择完整范围后切换到 custom 模式并刷新
 */
function handleCustomRangeChange() {
    if (customRange.value?.length === 2) {
        rangeMode.value = 'custom'
        loadDashboard()
        return
    }
    if (!customRange.value?.length && rangeMode.value === 'custom') {
        rangeMode.value = '7d'
        loadDashboard()
    }
}

loadDashboard()
</script>

<style scoped>
.dashboard-page {
    padding: 20px;
}

.dashboard-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
}

.dashboard-title {
    font-size: 20px;
    font-weight: 700;
}

.dashboard-subtitle {
    margin-top: 6px;
    font-size: 13px;
}

.stat-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 15px;
    margin-top: 15px;
}

.stat-card {
    display: flex;
    align-items: center;
    gap: 14px;
}

.stat-card-wrapper {
    cursor: pointer;
    transition: transform .2s ease;
}

.stat-card-wrapper:hover {
    transform: translateY(-1px);
}

.stat-icon {
    width: 50px;
    height: 50px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
}

.stat-body {
    min-width: 0;
}

.stat-value {
    font-size: 24px;
    font-weight: 700;
    line-height: 1.1;
}

.stat-label {
    margin-top: 4px;
    font-size: 13px;
}

.chart-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 15px;
    margin-top: 15px;
}

.trend-summary-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 15px;
    margin-top: 15px;
}

.trend-card-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
}

.trend-main-value {
    margin-top: 16px;
    font-size: 30px;
    font-weight: 700;
    line-height: 1.1;
}

.trend-subtitle {
    margin-top: 8px;
    font-size: 12px;
}

.activity-chart-wrap {
    margin-top: 15px;
}

.trend-panel-head {
    margin-bottom: 14px;
}

.trend-toolbar {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
}

.activity-chart-panel {
    height: 360px;
}

.chart-panel {
    height: 320px;
}

.panel-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    margin-bottom: 10px;
}

.panel-title {
    font-size: 15px;
    font-weight: 700;
}

.panel-tip {
    font-size: 12px;
}

.report-type-tags {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
}

.todo-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 15px;
    margin-top: 15px;
}

.ranking-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 15px;
    margin-top: 15px;
}

.ranking-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.ranking-item {
    display: flex;
    gap: 12px;
    padding: 12px 14px;
    border-radius: 10px;
    background: var(--el-fill-color-light);
    cursor: pointer;
    transition: background-color .2s ease, transform .2s ease;
}

.ranking-item:hover {
    background: var(--el-fill-color);
    transform: translateY(-1px);
}

.ranking-index {
    width: 26px;
    height: 26px;
    border-radius: 50%;
    background: #409EFF;
    color: #fff;
    font-size: 13px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    margin-top: 2px;
}

.ranking-main {
    min-width: 0;
    flex: 1;
}

.ranking-avatar {
    flex-shrink: 0;
    margin-top: 2px;
}

.ranking-title-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
}

.ranking-title {
    min-width: 0;
    font-size: 14px;
    font-weight: 600;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.ranking-meta {
    margin-top: 6px;
    font-size: 12px;
}

.ranking-metrics {
    display: flex;
    gap: 16px;
    flex-wrap: wrap;
    margin-top: 8px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
}

.todo-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.todo-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 12px 14px;
    border-radius: 10px;
    background: var(--el-fill-color-light);
    cursor: pointer;
    transition: background-color .2s ease, transform .2s ease;
}

.todo-item:hover {
    background: var(--el-fill-color);
    transform: translateY(-1px);
}

.todo-main {
    min-width: 0;
}

.todo-title {
    font-size: 14px;
    font-weight: 600;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.todo-meta {
    margin-top: 6px;
    font-size: 12px;
    line-height: 1.5;
}

:global(.dark) .text-secondary {
    color: #a0a3a8 !important;
}
</style>
