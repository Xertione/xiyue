<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="admin-aside">
      <div class="admin-brand">息悦生活后台</div>
      <el-menu :default-active="route.path" router background-color="#0f172a" text-color="#cbd5e1" active-text-color="#2dd4bf">
        <el-menu-item index="/admin/aunts"><el-icon><User /></el-icon><span>阿姨管理</span></el-menu-item>
        <el-menu-item index="/admin/orders"><el-icon><Document /></el-icon><span>订单管理</span></el-menu-item>
        <el-menu-item index="/admin/complaints"><el-icon><Warning /></el-icon><span>投诉处理</span></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="admin-header">
        <span class="admin-user">{{ auth.phone }}</span>
        <el-button text type="primary" @click="logout">退出登录</el-button>
      </el-header>
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.admin-layout { height: 100dvh; }
.admin-aside { background: #0f172a; overflow-y: auto; }
.admin-brand { padding: 20px 24px; color: #fff; font-weight: 600; font-size: 16px; border-bottom: 1px solid #1e293b; }
.admin-header { background: #fff; display: flex; align-items: center; justify-content: flex-end; border-bottom: 1px solid #e2e8f0; }
.admin-user { margin-right: 16px; color: #475569; font-size: 14px; }
.admin-main { background: #f8fafc; }
</style>
