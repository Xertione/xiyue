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
      <div v-if="order.auntName" class="detail-row"><span class="dl">服务阿姨</span><span>{{ order.auntName }}</span></div>
      <div v-if="order.payNo" class="detail-row"><span class="dl">支付流水</span><span class="mono">{{ order.payNo }}</span></div>
      <div v-if="order.refundNo" class="detail-row"><span class="dl">退款流水</span><span class="mono">{{ order.refundNo }}</span></div>
      <div v-if="order.completeImage" class="detail-row"><span class="dl">完成图片</span><van-image width="100" height="100" :src="order.completeImage" /></div>
    </div>

    <div class="page-pad">
      <van-button v-if="order.status === 0" type="primary" block round :loading="acting" @click="pay" style="margin-bottom:8px">模拟支付</van-button>
      <van-button v-if="order.status === 4" type="primary" block round :loading="acting" @click="confirm" style="margin-bottom:8px">确认服务完成</van-button>
      <van-button v-if="order.status === 5" type="primary" block round @click="showReview = true" style="margin-bottom:8px">评价</van-button>
      <van-button v-if="order.status === 5" type="warning" block round @click="showComplaint = true" style="margin-bottom:8px">投诉</van-button>
      <van-button v-if="order.status === 6 && review" type="default" block round @click="showReviewDetail = true" style="margin-bottom:8px">查看评价</van-button>
      <van-button v-if="[0,1,2].includes(order.status)" type="danger" plain block round :loading="acting" @click="cancel">取消订单</van-button>
    </div>

    <van-popup v-model:show="showReview" round position="bottom" :style="{ minHeight: '40%' }">
      <div class="popup-body">
        <div class="popup-title">服务评价</div>
        <van-rate v-model="reviewForm.rating" :count="5" />
        <van-field v-model="reviewForm.content" placeholder="说说服务怎么样" type="textarea" rows="3" class="popup-field" />
        <van-button type="primary" block round :loading="acting" @click="submitReview">提交评价</van-button>
      </div>
    </van-popup>

    <van-popup v-model:show="showComplaint" round position="bottom" :style="{ minHeight: '35%' }">
      <div class="popup-body">
        <div class="popup-title">提交投诉</div>
        <van-field v-model="complaintForm.reason" placeholder="请描述投诉原因" type="textarea" rows="3" class="popup-field" />
        <van-button type="primary" block round :loading="acting" @click="submitComplaint">提交投诉</van-button>
      </div>
    </van-popup>

    <van-dialog v-model:show="showReviewDetail" title="订单评价" :show-confirm-button="true" confirm-button-text="关闭">
      <div style="padding: 16px" v-if="review">
        <van-rate :model-value="review.rating" readonly />
        <div style="margin-top: 8px; color: #475569">{{ review.content }}</div>
      </div>
    </van-dialog>
  </div>
  <div v-else class="empty-tip">加载中...</div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { orderApi, reviewApi, complaintApi } from '@/api'
import { orderStatus as st } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const order = ref<any>(null)
const review = ref<any>(null)
const acting = ref(false)
const showReview = ref(false)
const showComplaint = ref(false)
const showReviewDetail = ref(false)
const reviewForm = ref({ rating: 5, content: '' })
const complaintForm = ref({ reason: '' })

onMounted(loadData)

async function loadData() {
  try {
    order.value = await orderApi.detail(Number(route.params.id))
    if (order.value.status === 6) {
      try { review.value = await reviewApi.getByOrder(Number(route.params.id)) } catch { /* 无评价 */ }
    }
  } catch { /* 拦截器提示 */ }
}

async function pay() {
  acting.value = true
  try { await orderApi.pay(order.value.id); showToast('支付成功'); loadData() } catch {} finally { acting.value = false }
}

async function cancel() {
  try {
    await showConfirmDialog({ title: '确认取消', message: '确定取消该订单吗？' })
    acting.value = true
    await orderApi.cancel(order.value.id)
    showToast('已取消')
    loadData()
  } catch {} finally { acting.value = false }
}

async function confirm() {
  acting.value = true
  try { await orderApi.confirm(order.value.id); showToast('已确认'); loadData() } catch {} finally { acting.value = false }
}

async function submitReview() {
  if (!reviewForm.value.content) return showToast('请输入评价内容')
  acting.value = true
  try {
    await reviewApi.create({ orderId: order.value.id, rating: reviewForm.value.rating, content: reviewForm.value.content })
    showToast('评价成功')
    showReview.value = false
    loadData()
  } catch {} finally { acting.value = false }
}

async function submitComplaint() {
  if (!complaintForm.value.reason) return showToast('请输入投诉原因')
  acting.value = true
  try {
    await complaintApi.create({ orderId: order.value.id, reason: complaintForm.value.reason })
    showToast('投诉已提交')
    showComplaint.value = false
    loadData()
  } catch {} finally { acting.value = false }
}
</script>

<style scoped>
.order-head-row { display: flex; justify-content: space-between; align-items: center; }
.order-no { color: #94a3b8; font-size: 12px; }
.detail-row { display: flex; justify-content: space-between; padding: 6px 0; font-size: 14px; color: #334155; }
.dl { color: #94a3b8; }
.mono { font-family: monospace; font-size: 12px; }
.popup-body { padding: 20px; }
.popup-title { font-weight: 600; font-size: 16px; margin-bottom: 16px; }
.popup-field { margin: 16px 0; border: 1px solid #e2e8f0; border-radius: 8px; }
</style>
