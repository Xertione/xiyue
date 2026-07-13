<template>
  <div>
    <van-nav-bar title="我的订单" />
    <div class="accept-bar">
      <span class="accept-label">接单状态</span>
      <van-switch :model-value="acceptOn" @update:model-value="toggleAccept" :loading="toggling" />
      <span class="accept-state">{{ acceptOn ? '可抢单' : '休息中' }}</span>
    </div>
    <div class="accept-hint">休息中时不会被抢单大厅展示，也无法抢单</div>
    <van-tabs v-model:active="activeTab" @change="onRefresh">
      <van-tab :badge="tabBadge2" title="待服务" />
      <van-tab :badge="tabBadge3" title="服务中" />
      <van-tab :badge="tabBadgeAll" title="全部" />
    </van-tabs>
    <van-list v-model:loading="loading" :finished="finished" finished-text="没有更多了" @load="loadMore">
      <div v-if="list.length === 0 && !loading" class="empty-tip">暂无订单</div>
      <div v-for="o in list" :key="o.id" class="card" @click="router.push(`/aunt/orders/${o.id}`)">
        <div class="order-head">
          <span class="order-no">{{ o.orderNo }}</span>
          <span class="status-tag" :style="{ color: st(o.status).color, background: st(o.status).bg }">{{ st(o.status).text }}</span>
        </div>
        <div class="order-info">{{ o.serviceDate }} {{ o.startHour }}:00 / {{ o.durationHours }}小时</div>
        <div class="order-info">{{ o.address }}</div>
        <div class="price order-price">¥{{ o.amount }}</div>
      </div>
    </van-list>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { orderApi, auntApi } from '@/api'
import { orderStatus as st } from '@/utils/format'

const router = useRouter()
const list = ref<any[]>([])
const loading = ref(false)
const finished = ref(false)
const activeTab = ref(0)
const page = ref(1)
const size = 10
const acceptOn = ref(true)
const toggling = ref(false)

// tab 红点计数
const pendingCount2 = ref(0)
const pendingCount3 = ref(0)
const pendingCountAll = ref(0)

const tabBadge2 = computed(() => pendingCount2.value > 0 ? String(pendingCount2.value) : '')
const tabBadge3 = computed(() => pendingCount3.value > 0 ? String(pendingCount3.value) : '')
const tabBadgeAll = computed(() => pendingCountAll.value > 0 ? String(pendingCountAll.value) : '')

const statusMap = [2, 3, undefined]

async function loadTabCounts() {
  try {
    const [r2, r3, rAll] = await Promise.all([
      orderApi.mine({ page: 1, size: 1, status: 2 }),
      orderApi.mine({ page: 1, size: 1, status: 3 }),
      orderApi.mine({ page: 1, size: 1 })
    ])
    pendingCount2.value = (r2 as any)?.total ?? 0
    pendingCount3.value = (r3 as any)?.total ?? 0
    pendingCountAll.value = (rAll as any)?.total ?? 0
  } catch { /* ignore */ }
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await orderApi.mine({ page: page.value, size, status: statusMap[activeTab.value] })
    if (page.value === 1) list.value = []
    list.value.push(...res.records)
    if (res.records.length < size) finished.value = true
    else page.value++
  } catch { finished.value = true } finally { loading.value = false }
}

function loadMore() { if (!finished.value) loadData() }
function onRefresh() { page.value = 1; finished.value = false; list.value = []; loadData(); loadTabCounts() }

async function toggleAccept(val: boolean) {
  toggling.value = true
  try {
    await auntApi.myAcceptStatus(val ? 'AVAILABLE' : 'RESTING')
    acceptOn.value = val
    showToast(val ? '已切换为可抢单' : '已切换为休息')
  } catch {} finally { toggling.value = false }
}

onMounted(() => loadTabCounts())
</script>

<style scoped>
.accept-bar { display: flex; align-items: center; gap: 12px; padding: 12px 16px; background: #fff; border-bottom: 1px solid #f1f5f9; }
.accept-label { font-size: 14px; color: #475569; }
.accept-state { font-size: 13px; color: #0d9488; font-weight: 500; }
.accept-hint { font-size: 12px; color: #94a3b8; padding: 0 16px 8px; background: #fff; }
.order-head { display: flex; justify-content: space-between; align-items: center; }
.order-no { color: #94a3b8; font-size: 12px; }
.order-info { color: #475569; font-size: 13px; margin-top: 6px; }
.order-price { margin-top: 6px; font-size: 16px; }
</style>
