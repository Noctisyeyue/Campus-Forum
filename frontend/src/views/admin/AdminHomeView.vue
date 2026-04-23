<script setup>
import { useAppStore } from '../../stores/app'

const appStore = useAppStore()
</script>

<template>
  <main class="page-shell">
    <section class="hero-card admin-card">
      <p class="eyebrow">Admin</p>
      <h1>{{ appStore.projectName }} 后台</h1>
      <p class="description">
        这里将承接管理员端页面。第一阶段会优先落地帖子审核、用户管理、分类管理的最小闭环。
      </p>
      <div class="action-row">
        <RouterLink to="/index" class="ghost-link">返回前台骨架</RouterLink>
      </div>
    </section>
  </main>
</template>
