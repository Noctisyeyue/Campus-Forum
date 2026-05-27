<template>
    <div class="theme-toggle" @click="toggle">
        <div class="toggle-track" :class="{ 'is-dark': store.dark }">
            <div class="toggle-thumb">
                <Transition name="toggle-icon" mode="out-in">
                    <el-icon :size="14" :key="store.dark ? 'moon' : 'sun'">
                        <Moon v-if="store.dark"/>
                        <Sunny v-else/>
                    </el-icon>
                </Transition>
            </div>
        </div>
    </div>
</template>

<script setup>
import { Moon, Sunny } from '@element-plus/icons-vue'
import { useStore } from '@/stores/index'

/** Pinia 全局状态 */
const store = useStore()

/**
 * 切换暗黑模式，使用 View Transition API 实现水滴扩散动画
 * 不支持 View Transition 的浏览器自动降级为即时切换
 *
 * @param event 鼠标点击事件，用于获取扩散圆心坐标
 */
function toggle(event) {
    const x = event.clientX
    const y = event.clientY
    /** 计算覆盖整个视口所需的最大半径 */
    const endRadius = Math.hypot(
        Math.max(x, window.innerWidth - x),
        Math.max(y, window.innerHeight - y)
    )
    /** 将扩散圆心和半径存为 CSS 自定义属性 */
    document.documentElement.style.setProperty('--theme-expand-x', x + 'px')
    document.documentElement.style.setProperty('--theme-expand-y', y + 'px')
    document.documentElement.style.setProperty('--theme-expand-r', endRadius + 'px')

    if (!document.startViewTransition) {
        store.toggleDark()
        return
    }

    const transition = document.startViewTransition(() => {
        store.toggleDark()
    })

    transition.ready.then(() => {
        const isDark = document.documentElement.classList.contains('dark')
        if (isDark) {
            // 亮→暗：new(暗色)从按钮处扩散到全屏
            document.documentElement.animate(
                { clipPath: [`circle(0% at ${x}px ${y}px)`, `circle(${endRadius}px at ${x}px ${y}px)`] },
                { duration: 500, easing: 'ease-in-out', pseudoElement: '::view-transition-new(root)' }
            )
        } else {
            // 暗→亮：new(亮色)从按钮处扩散到全屏
            document.documentElement.animate(
                { clipPath: [`circle(0% at ${x}px ${y}px)`, `circle(${endRadius}px at ${x}px ${y}px)`] },
                { duration: 500, easing: 'ease-in-out', pseudoElement: '::view-transition-new(root)' }
            )
        }
    })
}
</script>

<style scoped>
.theme-toggle {
    display: inline-flex;
    align-items: center;
    cursor: pointer;
    margin-right: 10px;
}

.toggle-track {
    width: 48px;
    height: 26px;
    border-radius: 13px;
    background: #cfd3dc;
    position: relative;
    transition: background-color 0.3s;
    box-sizing: border-box;
}

.toggle-track.is-dark {
    background: #6b7280;
}

.toggle-thumb {
    position: absolute;
    top: 2px;
    left: 2px;
    width: 22px;
    height: 22px;
    border-radius: 50%;
    background: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #606266;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
    transition: transform 0.3s, color 0.3s;
}

.is-dark .toggle-thumb {
    transform: translateX(22px);
    color: #6b7280;
}

.toggle-icon-enter-active,
.toggle-icon-leave-active {
    transition: opacity 0.15s;
}

.toggle-icon-enter-from,
.toggle-icon-leave-to {
    opacity: 0;
}
</style>
