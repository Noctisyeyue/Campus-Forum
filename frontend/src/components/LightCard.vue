<template>
    <div class="light-card">
        <slot/>
    </div>
</template>

<style scoped>
/** 轻量卡片：无边框，只有背景色、圆角和内边距 */
.light-card {
    background-color: var(--el-bg-color);
    border-radius: 5px;
    padding: 10px;
}
</style>
