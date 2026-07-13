<template>
  <div class="page">
    <router-view v-slot="{ Component }">
      <keep-alive>
        <component :is="Component" />
      </keep-alive>
    </router-view>
    <van-tabbar route>
      <van-tabbar-item to="/aunt/grab-list" icon="gift-o">抢单大厅</van-tabbar-item>
      <van-tabbar-item to="/aunt/mine" icon="orders-o" :badge="pendingBadge">我的订单</van-tabbar-item>
      <van-tabbar-item to="/aunt/profile" icon="user-o">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onActivated } from 'vue'
import { orderApi } from '@/api'

const pendingTotal2 = ref(0)
const pendingTotal3 = ref(0)

const pendingBadge = computed(() => {
  const total = pendingTotal2.value + pendingTotal3.value
  return total > 0 ? String(total) : ''
})

async function loadPendingCount() {
  try {
    const [r2, r3] = await Promise.all([
      orderApi.mine({ page: 1, size: 1, status: 2 }),
      orderApi.mine({ page: 1, size: 1, status: 3 })
    ])
    // 响应拦截器已解包 Result.data，直接拿 total
    pendingTotal2.value = (r2 as any)?.total ?? 0
    pendingTotal3.value = (r3 as any)?.total ?? 0
  } catch { /* ignore */ }
}

// onMounted 处理首次进入，onActivated 处理 keep-alive 切回
onMounted(() => loadPendingCount())
onActivated(() => loadPendingCount())
</script>

<style scoped>
.page {
  min-height: 100dvh;
  overflow-y: auto;
}
</style>
