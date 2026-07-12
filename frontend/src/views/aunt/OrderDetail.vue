<template>
  <div v-if="order">
    <van-nav-bar title="订单详情" left-arrow @click-left="router.back()" />
    <div class="card">
      <div class="order-head-row">
        <span class="order-no">{{ order.orderNo }}</span>
        <span class="status-tag" :style="{ color: st(order.status).color, background: st(order.status).bg }">{{ st(order.status).text }}</span>
      </div>
      <van-divider />
      <div class="detail-row"><span class="dl">服务时间</span><span>{{ order.serviceDate }} {{ order.startHour }}:00 / {{ order.durationHours }}小时</span></div>
      <div class="detail-row"><span class="dl">联系人</span><span>{{ order.contactName }} {{ order.contactPhone }}</span></div>
      <div class="detail-row"><span class="dl">服务地址</span><span>{{ order.address }}</span></div>
      <div class="detail-row"><span class="dl">订单金额</span><span class="price">¥{{ order.amount }}</span></div>
      <div v-if="order.completeImage" class="detail-row"><span class="dl">完成图片</span><van-image width="100" height="100" :src="order.completeImage" /></div>
    </div>

    <div class="page-pad">
      <van-button v-if="order.status === 2" type="primary" block round :loading="acting" @click="start" style="margin-bottom:8px">开始服务</van-button>
      <van-button v-if="order.status === 3" type="primary" block round @click="showComplete = true" style="margin-bottom:8px">提交服务完成</van-button>
    </div>

    <van-popup v-model:show="showComplete" round position="bottom" :style="{ minHeight: '35%' }">
      <div class="popup-body">
        <div class="popup-title">提交服务完成</div>
        <van-field v-model="completeForm.imageUrl" placeholder="请输入完成图片URL" class="popup-field" />
        <van-button type="primary" block round :loading="acting" @click="complete">确认提交</van-button>
      </div>
    </van-popup>
  </div>
  <div v-else class="empty-tip">加载中...</div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { orderApi } from '@/api'
import { orderStatus as st } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const order = ref<any>(null)
const acting = ref(false)
const showComplete = ref(false)
const completeForm = ref({ imageUrl: '' })

onMounted(async () => {
  try { order.value = await orderApi.detail(Number(route.params.id)) } catch {}
})

async function start() {
  acting.value = true
  try { await orderApi.start(order.value.id); showToast('已开始服务'); loadData() } catch {} finally { acting.value = false }
}

async function complete() {
  if (!completeForm.value.imageUrl) return showToast('请输入完成图片URL')
  acting.value = true
  try {
    await orderApi.complete(order.value.id, completeForm.value.imageUrl)
    showToast('已提交完成')
    showComplete.value = false
    loadData()
  } catch {} finally { acting.value = false }
}

async function loadData() {
  order.value = await orderApi.detail(Number(route.params.id))
}
</script>

<style scoped>
.order-head-row { display: flex; justify-content: space-between; align-items: center; }
.order-no { color: #94a3b8; font-size: 12px; }
.detail-row { display: flex; justify-content: space-between; padding: 6px 0; font-size: 14px; color: #334155; }
.dl { color: #94a3b8; }
.popup-body { padding: 20px; }
.popup-title { font-weight: 600; font-size: 16px; margin-bottom: 16px; }
.popup-field { margin-bottom: 16px; border: 1px solid #e2e8f0; border-radius: 8px; }
</style>
