<template>
  <header>
    <div class="wrapper">
      <router-view/>
    </div>
  </header>
</template>

<script setup>
import { useDark, useToggle } from '@vueuse/core'
import { useStore } from '@/stores/index'

const isDark = useDark({
  selector: 'html',
  attribute: 'class',
  valueDark: 'dark',
  valueLight: 'light'
})
const toggleDark = useToggle(isDark)

const store = useStore()
store.dark = isDark
store.toggleDark = toggleDark
</script>

<style scoped>
header {
  line-height: 1.5;
}
</style>
