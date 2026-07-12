<template>
  <div>
    <van-nav-bar title="找阿姨" />
    <van-search v-model="skillTag" placeholder="搜索技能标签" @search="onRefresh" />
    <van-dropdown-menu>
      <van-dropdown-item v-model="sort" :options="sortOptions" @change="onRefresh" />
    </van-dropdown-menu>
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list v-model:loading="loading" :finished="finished" finished-text="没有更多了" @load="loadMore">
        <div v-if="list.length === 0 && !loading" class="empty-tip">暂无可预约阿姨</div>
        <div v-for="item in list" :key="item.id" class="card aunt-item" @click="router.push(`/user/aunts/${item.id}`)">
          <div class="aunt-info">
            <div class="aunt-name">{{ item.name || '未设置姓名' }}</div>
            <div class="aunt-tags">{{ item.skillTags || '暂无标签' }}</div>
          </div>
          <div class="aunt-meta">
            <div class="price price-large">¥{{ item.price ?? '-' }}/h</div>
            <div class="aunt-sub">{{ item.rating }}星 / {{ item.serviceCount }}次</div>
          </div>
        </div>
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { auntApi } from '@/api'

const router = useRouter()
const list = ref<any[]>([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const page = ref(1)
const size = 10
const skillTag = ref('')
const sort = ref('rating')
const sortOptions = [
  { text: '星级优先', value: 'rating' },
  { text: '价格升序', value: 'price_asc' }
]

async function loadData() {
  loading.value = true
  try {
    const res: any = await auntApi.list({
      page: page.value, size,
      skillTag: skillTag.value || undefined,
      sort: sort.value
    })
    if (page.value === 1) list.value = []
    list.value.push(...res.records)
    if (res.records.length < size) finished.value = true
    else page.value++
  } catch { finished.value = true } finally { loading.value = false }
}

function loadMore() { if (!finished.value) loadData() }
function onRefresh() { page.value = 1; finished.value = false; refreshing.value = false; loadData() }
</script>

<style scoped>
.aunt-item { display: flex; justify-content: space-between; align-items: center; }
.aunt-name { font-weight: 600; font-size: 16px; }
.aunt-tags { color: #64748b; font-size: 13px; margin-top: 4px; }
.aunt-meta { text-align: right; }
.aunt-sub { color: #94a3b8; font-size: 12px; margin-top: 2px; }
</style>
