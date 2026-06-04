<template>
    <div class="topic-type"
         :style="{
             color: store.findTypeById(type)?.color + 'EE',
             'border-color': store.findTypeById(type)?.color + '77',
             'background': store.findTypeById(type)?.color + '33'
         }">
        {{store.findTypeById(type)?.name}}
    </div>
</template>

<script setup>
import {useStore} from "@/stores/index";

/** Pinia 全局状态 */
const store = useStore()

/** 组件属性定义 */
defineProps({
    /** 分类ID */
    type: Number
})
</script>

<style scoped>
.topic-type {
    display: inline-block;
    border: solid 0.5px grey;
    border-radius: 3px;
    font-size: 12px;
    padding: 0 5px;
    height: 18px;
}
</style>
