<template>
    <div class="dot" :style="{background: color}"></div>
</template>

<script setup>
/** 组件属性定义 */
defineProps({
  /** 圆点颜色 */
  color: String
})
</script>

<style scoped>
.dot {
    width: 10px;
    height: 10px;
    border-radius: 5px;
    display: inline-block;
}
</style>
