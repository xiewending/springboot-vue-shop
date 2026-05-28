<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Box, House, Menu as MenuIcon, SwitchButton, Tickets } from '@element-plus/icons-vue'

import { useAuthStore } from '../stores/auth'

const route = useRoute()
const authStore = useAuthStore()

const activePath = computed(() => route.path)
const iconMap = {
  House,
  Box,
  Tickets
}

function resolveIcon(icon?: string) {
  return icon ? iconMap[icon as keyof typeof iconMap] ?? MenuIcon : MenuIcon
}

function handleLogout() {
  authStore.logout()
  window.location.href = '/login'
}
</script>

<template>
  <el-container class="admin-layout">
    <el-aside class="admin-aside" width="220px">
      <div class="admin-brand">
        <strong>ShopPilot</strong>
        <span>运营后台</span>
      </div>
      <el-menu class="admin-menu" :default-active="activePath" router>
        <template v-for="menu in authStore.menus" :key="menu.id">
          <el-sub-menu v-if="menu.children?.length" :index="menu.path || String(menu.id)">
            <template #title>
              <el-icon><component :is="resolveIcon(menu.icon)" /></el-icon>
              <span>{{ menu.name }}</span>
            </template>
            <el-menu-item v-for="child in menu.children" :key="child.id" :index="child.path">
              <el-icon><component :is="resolveIcon(child.icon)" /></el-icon>
              <span>{{ child.name }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="menu.path">
            <el-icon><component :is="resolveIcon(menu.icon)" /></el-icon>
            <span>{{ menu.name }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <div>
          <strong>{{ route.meta.title ?? '工作台' }}</strong>
        </div>
        <div class="admin-user">
          <span>{{ authStore.user?.nickname ?? authStore.user?.username }}</span>
          <el-button :icon="SwitchButton" @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="admin-main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>
