<script setup>
import { useAppStore } from '../../stores/app'

const appStore = useAppStore()
</script>

<template>
  <main class="page-shell">
    <section class="hero-card">
      <p class="eyebrow">Front</p>
      <h1>{{ appStore.projectName }} 前台</h1>
      <p class="description">
        这里将承接用户端页面。当前阶段我们先把新项目骨架搭好，后面再逐模块迁移登录、论坛、用户中心等功能。
      </p>
      <div class="action-row">
        <RouterLink to="/admin" class="ghost-link">进入后台骨架</RouterLink>
      </div>
    </section>
  </main>
</template>
