<template>
  <div>
    <van-nav-bar title="抢单大厅" />
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list v-model:loading="loading" :finished="finished" finished-text="没有更多了" @load="loadMore">
        <div v-if="list.length === 0 && !loading" class="empty-tip">暂时可抢订单，下拉刷新</div>
        <div v-for="o in list" :key="o.id" class="card">
          <div class="grab-time">{{ o.serviceDate }} {{ o.startHour }}:00 / {{ o.durationHours }}小时</div>
          <div class="grab-addr">{{ o.address }}</div>
          <div class="grab-contact">联系人：{{ o.contactName }} {{ o.contactPhone }}</div>
          <div class="grab-foot">
            <span class="price price-large">¥{{ o.amount }}</span>
            <van-button type="primary" size="small" round :loading="grabbing === o.id" @click="grab(o.id)">抢单</van-button>
          </div>
        </div>
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { orderApi } from '@/api'

const router = useRouter()
const list = ref<any[]>([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const grabbing = ref<number | null>(null)
const page = ref(1)
const size = 10

async function loadData() {
  loading.value = true
  try {
    const res: any = await orderApi.grabList({ page: page.value, size })
    if (page.value === 1) list.value = []
    list.value.push(...res.records)
    if (res.records.length < size) finished.value = true
    else page.value++
  } catch { finished.value = true } finally { loading.value = false }
}

function loadMore() { if (!finished.value) loadData() }
function onRefresh() { page.value = 1; finished.value = false; refreshing.value = false; loadData() }

async function grab(id: number) {
  grabbing.value = id
  try {
    await orderApi.grab(id)
    showToast('抢单成功')
    list.value = list.value.filter(o => o.id !== id)
    router.push(`/aunt/orders/${id}`)
  } catch {} finally { grabbing.value = null }
}
</script>

<style scoped>
.grab-time { font-weight: 600; color: #0f172a; }
.grab-addr { color: #475569; font-size: 13px; margin-top: 4px; }
.grab-contact { color: #64748b; font-size: 13px; margin-top: 2px; }
.grab-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; }
</style>
