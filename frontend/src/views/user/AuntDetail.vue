<template>
  <div v-if="aunt">
    <van-nav-bar title="阿姨详情" left-arrow @click-left="router.back()" />
    <div class="card">
      <div class="aunt-head">
        <van-image round width="64" height="64" :src="aunt.avatar" />
        <div class="aunt-head-info">
          <div class="aunt-head-name">{{ aunt.name || '未设置姓名' }}</div>
          <div class="aunt-head-sub">{{ aunt.rating }}星 / {{ aunt.serviceCount }}次服务</div>
        </div>
      </div>
      <div class="price price-large detail-price">¥{{ aunt.price ?? '-' }}/小时</div>
      <van-divider />
      <div class="detail-label">技能标签</div>
      <div class="detail-text">{{ aunt.skillTags || '暂无' }}</div>
      <div class="detail-label">个人介绍</div>
      <div class="detail-text">{{ aunt.intro || '暂无介绍' }}</div>
    </div>
    <div class="page-pad">
      <van-button type="primary" block round @click="router.push('/user/orders/create')">预约服务</van-button>
    </div>
  </div>
  <div v-else class="empty-tip">加载中...</div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { auntApi } from '@/api'

const route = useRoute()
const router = useRouter()
const aunt = ref<any>(null)

onMounted(async () => {
  try {
    aunt.value = await auntApi.detail(Number(route.params.id))
  } catch { /* 拦截器提示 */ }
})
// keep-alive 缓存下组件复用时 route param 变化不触发 onMounted，需手动 watch 重载
watch(() => route.params.id, () => {
  auntApi.detail(Number(route.params.id)).then(data => aunt.value = data).catch(() => {})
})
</script>

<style scoped>
.aunt-head { display: flex; align-items: center; gap: 12px; }
.aunt-head-name { font-size: 18px; font-weight: 600; }
.aunt-head-sub { color: #64748b; font-size: 13px; margin-top: 4px; }
.detail-price { margin: 16px 0 0; }
.detail-label { color: #94a3b8; font-size: 13px; margin-top: 12px; margin-bottom: 4px; }
.detail-text { color: #334155; font-size: 14px; line-height: 1.6; }
</style>
