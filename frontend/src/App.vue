<template>
  <header>
    <div class="wrapper">
      <router-view/>
    </div>
  </header>
</template>

<script setup>
import { useDark } from '@vueuse/core'

useDark({
  selector: 'html',
  attribute: 'class',
  valueDark: 'dark',
  valueLight: 'light'
})
</script>

<style scoped>
header {
  line-height: 1.5;
}
</style>
