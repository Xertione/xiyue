<template>
  <div>
    <van-nav-bar title="我的订单" />
    <van-tabs v-model:active="activeTab" @change="onRefresh">
      <van-tab title="全部" />
      <van-tab title="待支付" />
      <van-tab title="待评价" />
      <van-tab title="已完成" />
    </van-tabs>
    <van-list v-model:loading="loading" :finished="finished" finished-text="没有更多了" @load="loadMore">
      <div v-if="list.length === 0 && !loading" class="empty-tip">暂无订单</div>
      <div v-for="o in list" :key="o.id" class="card" @click="router.push(`/user/orders/${o.id}`)">
        <div class="order-head">
          <span class="order-no">{{ o.orderNo }}</span>
          <span class="status-tag" :style="{ color: st(o.status).color, background: st(o.status).bg }">{{ st(o.status).text }}</span>
        </div>
        <div class="order-info">{{ o.serviceDate }} {{ o.startHour }}:00 / {{ o.durationHours }}小时</div>
        <div v-if="o.auntName" class="order-info">阿姨：{{ o.auntName }}</div>
        <div class="price order-price">¥{{ o.amount }}</div>
      </div>
    </van-list>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { orderApi } from '@/api'
import { orderStatus as st } from '@/utils/format'

const router = useRouter()
const list = ref<any[]>([])
const loading = ref(false)
const finished = ref(false)
const activeTab = ref(0)
const page = ref(1)
const size = 10

const statusMap = [undefined, 0, 5, 6]

async function loadData() {
  loading.value = true
  try {
    const res: any = await orderApi.userList({ page: page.value, size, status: statusMap[activeTab.value] })
    if (page.value === 1) list.value = []
    list.value.push(...res.records)
    if (res.records.length < size) finished.value = true
    else page.value++
  } catch { finished.value = true } finally { loading.value = false }
}

function loadMore() { if (!finished.value) loadData() }
function onRefresh() { page.value = 1; finished.value = false; list.value = []; loadData() }
</script>

<style scoped>
.order-head { display: flex; justify-content: space-between; align-items: center; }
.order-no { color: #94a3b8; font-size: 12px; }
.order-info { color: #475569; font-size: 13px; margin-top: 6px; }
.order-price { margin-top: 6px; font-size: 16px; }
</style>
