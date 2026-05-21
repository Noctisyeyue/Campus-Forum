<template>
    <div style="padding: 20px">
        <card>
            <span style="font-size: 18px;font-weight: bold">数据看板</span>
        </card>
        <div style="display: grid;grid-template-columns: repeat(4, 1fr);gap: 15px;margin-top: 15px">
            <card v-for="item in statCards" :key="item.label">
                <div style="display: flex;align-items: center;gap: 15px">
                    <div :style="{ background: item.bg, borderRadius: '8px', width: '50px', height: '50px',
                                  display: 'flex', alignItems: 'center', justifyContent: 'center' }">
                        <el-icon :size="28" :color="item.color"><component :is="item.icon"/></el-icon>
                    </div>
                    <div>
                        <div style="font-size: 24px;font-weight: bold">{{ item.value }}</div>
                        <div style="font-size: 13px;" class="text-secondary">{{ item.label }}</div>
                    </div>
                </div>
            </card>
        </div>
        <div style="display: grid;grid-template-columns: 1fr 1fr;gap: 15px;margin-top: 15px">
            <card>
                <div style="font-weight: bold;margin-bottom: 10px">帖子状态分布</div>
                <v-chart :option="statusChartOption" style="height: 300px" autoresize/>
            </card>
            <card>
                <div style="font-weight: bold;margin-bottom: 10px">各分类帖子数量</div>
                <v-chart :option="typeChartOption" style="height: 300px" autoresize/>
            </card>
        </div>
    </div>
</template>

<script setup>
import {get} from "@/net"
import {ref, computed, shallowRef} from "vue"
import {ChatDotSquare, Document, User, Warning} from "@element-plus/icons-vue"
import Card from "@/components/Card.vue"
import {useStore} from "@/stores/index"
import VChart from "vue-echarts"
import {use} from "echarts/core"
import {CanvasRenderer} from "echarts/renderers"
import {PieChart, BarChart} from "echarts/charts"
import {TitleComponent, TooltipComponent, LegendComponent, GridComponent} from "echarts/components"

use([CanvasRenderer, PieChart, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const store = useStore()
const stats = ref({
    totalTopics: 0, totalUsers: 0, totalComments: 0, pendingTopics: 0,
    statusMap: {}, typeMap: {}
})

const statCards = computed(() => {
    const d = store.dark
    return [
        { label: '总帖子数', value: stats.value.totalTopics, icon: Document, color: '#409EFF', bg: d ? '#1a2a3a' : '#ecf5ff' },
        { label: '总用户数', value: stats.value.totalUsers, icon: User, color: '#67C23A', bg: d ? '#1a2a1a' : '#f0f9eb' },
        { label: '总评论数', value: stats.value.totalComments, icon: ChatDotSquare, color: '#E6A23C', bg: d ? '#2a2518' : '#fdf6ec' },
        { label: '待审核帖子', value: stats.value.pendingTopics, icon: Warning, color: '#F56C6C', bg: d ? '#2a1a1a' : '#fef0f0' }
    ]
})

const statusName = { pending_review: '待审核', published: '已发布', rejected: '已拒绝', hidden: '已隐藏', deleted: '已删除' }

const statusChartOption = computed(() => {
    const data = Object.entries(stats.value.statusMap).map(([k, v]) => ({
        name: statusName[k] || k, value: v
    }))
    return {
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { bottom: 0 },
        series: [{
            type: 'pie', radius: ['40%', '70%'], avoidLabelOverlap: false,
            itemStyle: { borderRadius: 10, borderColor: store.dark ? '#1d1e1f' : '#fff', borderWidth: 2 },
            label: { show: false }, emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
            data
        }]
    }
})

const typeChartOption = computed(() => {
    const names = Object.keys(stats.value.typeMap)
    const values = Object.values(stats.value.typeMap)
    return {
        tooltip: { trigger: 'axis' },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', data: names },
        yAxis: { type: 'value' },
        series: [{
            type: 'bar', data: values, barWidth: '50%',
            itemStyle: { borderRadius: [4, 4, 0, 0], color: '#409EFF' }
        }]
    }
})

// 加载统计数据
const statusList = ['pending_review', 'published', 'rejected', 'hidden', 'deleted']
let loaded = 0

function checkDone() {
    loaded++
    if (loaded >= 3) {
        // 统计完成
    }
}

// 加载帖子各状态数量
statusList.forEach(status => {
    get(`/api/admin/topics?page=0&status=${status}`, data => {
        stats.value.statusMap[status] = data.length
        if (status === 'pending_review') stats.value.pendingTopics = data.length
        if (status === 'published') stats.value.totalTopics = data.length
    })
})
// 简化统计：用各状态之和作为总帖子数
get('/api/admin/topics?page=0', data => {
    stats.value.totalTopics = data.length
})

get('/api/admin/users?page=0', data => {
    stats.value.totalUsers = data.length
})

get('/api/admin/comments?page=0', data => {
    stats.value.totalComments = data.length
})

// 各分类帖子数
get('/api/admin/types', types => {
    types.forEach(t => {
        get(`/api/admin/topics?page=0&type=${t.id}`, data => {
            if (data.length > 0) {
                stats.value.typeMap[t.name] = data.length
            }
        })
    })
})
</script>

<style scoped>
:global(.dark) .text-secondary {
    color: #a0a3a8 !important;
}
</style>
