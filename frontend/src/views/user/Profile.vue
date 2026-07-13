<template>
  <div class="profile-page">
    <van-cell-group inset>
      <van-cell title="手机号" :value="auth.phone" />
      <van-cell title="角色" value="用户" />
    </van-cell-group>
    <div class="logout-area">
      <van-button type="danger" block round @click="logout">退出登录</van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

function logout() {
  auth.logout()
  router.replace('/login')
}
</script>

<style scoped>
.profile-page { min-height: 100dvh; background: #f8fafc; padding-top: 20px; }
.logout-area { padding: 24px 16px; }
</style>
