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
import { ref, computed, onActivated } from 'vue'
import { orderApi } from '@/api'

const pendingTotal2 = ref(0) // 待服务
const pendingTotal3 = ref(0) // 服务中

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
    pendingTotal2.value = (r2 as any)?.data?.total ?? 0
    pendingTotal3.value = (r3 as any)?.data?.total ?? 0
  } catch { /* ignore */ }
}

onActivated(() => loadPendingCount())
</script>
