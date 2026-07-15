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
      <div v-if="completeImages.length" class="detail-row"><span class="dl">完成图片</span></div>
      <div v-if="completeImages.length" class="image-row">
        <van-image
          v-for="(img, i) in completeImages"
          :key="i"
          width="80" height="80" fit="cover" radius="6"
          :src="img"
          @click="showPreview(i)"
          style="margin-right: 8px; margin-bottom: 8px;"
        />
      </div>
    </div>

    <div class="page-pad">
      <van-button v-if="order.status === 2" type="primary" block round :loading="acting" @click="start" style="margin-bottom:8px">开始服务</van-button>
      <van-button v-if="order.status === 3" type="primary" block round @click="showComplete = true" style="margin-bottom:8px">提交服务完成</van-button>
    </div>

    <van-popup v-model:show="showComplete" round position="bottom" :style="{ minHeight: '40%' }">
      <div class="popup-body">
        <div class="popup-title">提交服务完成（可上传多张）</div>
        <van-uploader
          v-model="fileList"
          :after-read="onUpload"
          accept="image/*"
          multiple
          :max-count="9"
          class="popup-uploader"
        />
        <div class="popup-hint" v-if="fileList.length">已选择 {{ fileList.length }} 张图片</div>
        <van-button type="primary" block round :loading="acting" @click="complete" style="margin-top:12px">确认提交</van-button>
        <van-button type="default" block round :loading="acting" @click="mockComplete" style="margin-top:8px">模拟提交（免传图）</van-button>
      </div>
    </van-popup>

    <van-image-preview
      v-model:show="showImagePreview"
      :images="completeImages"
      :start-position="previewIndex"
      @change="previewIndex = $event"
    />
  </div>
  <div v-else class="empty-tip">加载中...</div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { orderApi, uploadApi } from '@/api'
import { orderStatus as st } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const order = ref<any>(null)
const acting = ref(false)
const showComplete = ref(false)
const completeForm = ref({ imageUrl: '' })
const fileList = ref<any[]>([])
const showImagePreview = ref(false)
const previewIndex = ref(0)

const completeImages = computed(() => {
  if (!order.value?.completeImage) return []
  return order.value.completeImage.split(',').filter(Boolean)
})

onMounted(async () => {
  try { order.value = await orderApi.detail(Number(route.params.id)) } catch {}
})
// keep-alive 缓存下组件复用时 route param 变化不触发 onMounted，需手动 watch 重载
watch(() => route.params.id, () => { loadData() })

async function start() {
  acting.value = true
  try { await orderApi.start(order.value.id); showToast('已开始服务'); loadData() } catch {} finally { acting.value = false }
}

async function complete() {
  if (fileList.value.length === 0) return showToast('请上传完成图片')
  acting.value = true
  try {
    await orderApi.complete(order.value.id, completeForm.value.imageUrl)
    showToast('已提交完成')
    showComplete.value = false
    completeForm.value.imageUrl = ''
    fileList.value = []
    loadData()
  } catch {} finally { acting.value = false }
}

// 模拟提交：使用占位 URL，无需上传图片，方便测试
async function mockComplete() {
  acting.value = true
  try {
    const mockUrl = '/mock-uploads/mock-test-image.jpg'
    await orderApi.complete(order.value.id, mockUrl)
    showToast('模拟提交成功')
    showComplete.value = false
    completeForm.value.imageUrl = ''
    fileList.value = []
    loadData()
  } catch {} finally { acting.value = false }
}

async function onUpload(fileItem: any) {
  try {
    const res = await uploadApi.upload(fileItem.file)
    const uploadedUrl = res.url
    // 追加到已有 URL 列表（逗号分隔）
    if (completeForm.value.imageUrl) {
      completeForm.value.imageUrl += ',' + uploadedUrl
    } else {
      completeForm.value.imageUrl = uploadedUrl
    }
    // fileList 已由 v-model 自动管理，只需确保 URL 正确
    const idx = fileList.value.findIndex((f: any) => f === fileItem || f.file === fileItem.file)
    if (idx >= 0) {
      fileList.value[idx] = { ...fileList.value[idx], url: uploadedUrl, status: 'done' }
    }
  } catch {
    showToast('上传失败，请重试')
    // 移除失败项
    fileList.value = fileList.value.filter((f: any) => f.file !== fileItem.file)
  }
}

function showPreview(index: number) {
  previewIndex.value = index
  showImagePreview.value = true
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
.image-row { display: flex; flex-wrap: wrap; }
.popup-body { padding: 20px; }
.popup-title { font-weight: 600; font-size: 16px; margin-bottom: 16px; }
.popup-uploader { margin-bottom: 8px; }
.popup-hint { font-size: 13px; color: #94a3b8; margin-bottom: 4px; }
.page-pad { padding: 16px; }
.card { background: #fff; margin: 10px; border-radius: 10px; padding: 16px; }
.empty-tip { padding: 40px; text-align: center; color: #94a3b8; }
.price { color: #e74c3c; font-weight: 600; }
</style>
