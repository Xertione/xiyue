<template>
  <div class="profile-page">
    <van-nav-bar title="个人中心" />
    <van-cell-group inset>
      <van-cell title="手机号" :value="auth.phone" />
      <van-cell title="角色" value="阿姨" />
    </van-cell-group>

    <van-cell-group inset title="个人资料">
      <van-field v-model="form.name" label="姓名" placeholder="请输入姓名" maxlength="50" />
      <van-field v-model="form.price" label="价格（元）" type="number" placeholder="如 50" />
      <van-field v-model="form.age" label="年龄" type="digit" placeholder="18-100" />
      <van-field v-model="form.experience" label="入行年限" type="digit" placeholder="如 3" />
      <van-field v-model="form.skillTags" label="技能标签" placeholder="如：保洁,做饭,带娃" />
      <van-field v-model="form.intro" label="简介" type="textarea" placeholder="简单介绍自己" rows="3" autosize />
    </van-cell-group>

    <div class="btn-area">
      <van-button type="primary" block round :loading="saving" @click="save">保存</van-button>
    </div>

    <div class="logout-area">
      <van-button type="danger" block round @click="logout">退出登录</van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showLoadingToast, closeToast } from 'vant'
import { useAuthStore } from '@/stores/auth'
import { auntProfileApi } from '@/api'

const router = useRouter()
const auth = useAuthStore()

const form = reactive({
  name: '',
  price: '',
  age: '',
  experience: '',
  skillTags: '',
  intro: ''
})

const saving = ref(false)

onMounted(async () => {
  try {
    showLoadingToast({ message: '加载中...', forbidClick: true })
    const data = await auntProfileApi.get()
    // 响应拦截器已解包 Result.data
    if (data) {
      form.name = data.name || ''
      form.price = data.price != null ? String(data.price) : ''
      form.age = data.age != null ? String(data.age) : ''
      form.experience = data.experience != null ? String(data.experience) : ''
      form.skillTags = data.skillTags || ''
      form.intro = data.intro || ''
    }
    closeToast()
  } catch {
    closeToast()
    showToast('加载失败')
  }
})

function validate(): string | null {
  // 姓名：不能是纯数字
  if (form.name && /^\d+$/.test(form.name)) return '姓名不能为纯数字'
  // 价格：必须 > 0
  if (form.price) {
    const p = Number(form.price)
    if (isNaN(p) || p <= 0) return '价格必须大于 0'
  }
  // 年龄：18-100
  if (form.age) {
    const a = Number(form.age)
    if (isNaN(a) || a < 18 || a > 100) return '年龄需在 18-100 之间'
  }
  // 入行年限：0-60
  if (form.experience) {
    const e = Number(form.experience)
    if (isNaN(e) || e < 0 || e > 60) return '入行年限需在 0-60 之间'
  }
  return null
}

async function save() {
  const err = validate()
  if (err) return showToast(err)

  saving.value = true
  try {
    const payload: Record<string, any> = {}
    if (form.name) payload.name = form.name
    if (form.price) payload.price = Number(form.price)
    if (form.age) payload.age = Number(form.age)
    if (form.experience) payload.experience = Number(form.experience)
    if (form.skillTags) payload.skillTags = form.skillTags
    if (form.intro) payload.intro = form.intro
    await auntProfileApi.update(payload)
    showToast('保存成功')
  } catch (e: any) {
    // 显示后端返回的具体错误
    showToast(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function logout() {
  auth.logout()
  router.replace('/login')
}
</script>

<style scoped>
.profile-page { min-height: 100dvh; background: #f8fafc; }
.btn-area { padding: 24px 16px 0; }
.logout-area { padding: 12px 16px 24px; }
</style>
