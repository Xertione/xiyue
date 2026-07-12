<template>
  <div class="auth-page">
    <div class="auth-logo">
      <h1>息悦生活</h1>
      <p>家政预约平台</p>
    </div>
    <van-tabs v-model:active="tab">
      <van-tab title="密码登录">
        <van-cell-group inset class="auth-form">
          <van-field v-model="form.phone" label="手机号" placeholder="请输入手机号" type="tel" maxlength="11" />
          <van-field v-model="form.password" label="密码" placeholder="请输入密码" type="password" />
        </van-cell-group>
        <div class="auth-btn">
          <van-button type="primary" block round :loading="loading" @click="loginByPassword">登录</van-button>
        </div>
      </van-tab>
      <van-tab title="验证码登录">
        <van-cell-group inset class="auth-form">
          <van-field v-model="form.phone" label="手机号" placeholder="请输入手机号" type="tel" maxlength="11" />
          <van-field v-model="form.code" label="验证码" placeholder="请输入验证码" maxlength="6">
            <template #button>
              <van-button size="small" type="primary" :disabled="counting > 0" @click="sendCode">
                {{ counting > 0 ? counting + 's' : '发送验证码' }}
              </van-button>
            </template>
          </van-field>
        </van-cell-group>
        <div class="auth-btn">
          <van-button type="primary" block round :loading="loading" @click="loginByCode">登录</van-button>
        </div>
      </van-tab>
    </van-tabs>
    <div class="auth-links">
      <router-link to="/register">没有账号？去注册</router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { authApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const tab = ref(0)
const loading = ref(false)
const counting = ref(0)
const form = ref({ phone: '', password: '', code: '' })

async function sendCode() {
  if (!/^1[3-9]\d{9}$/.test(form.value.phone)) return showToast('请输入正确的手机号')
  try {
    await authApi.smsCode(form.value.phone)
    showToast('验证码已发送')
    counting.value = 60
    const timer = setInterval(() => {
      counting.value--
      if (counting.value <= 0) clearInterval(timer)
    }, 1000)
  } catch { /* 拦截器已提示 */ }
}

async function loginByPassword() {
  if (!/^1[3-9]\d{9}$/.test(form.value.phone)) return showToast('请输入正确的手机号')
  if (!form.value.password) return showToast('请输入密码')
  loading.value = true
  try {
    const res: any = await authApi.loginByPassword({ phone: form.value.phone, password: form.value.password })
    auth.setAuth(res)
    showToast('登录成功')
    router.replace(auth.homePath())
  } catch { /* 拦截器已提示 */ } finally { loading.value = false }
}

async function loginByCode() {
  if (!/^1[3-9]\d{9}$/.test(form.value.phone)) return showToast('请输入正确的手机号')
  if (!form.value.code) return showToast('请输入验证码')
  loading.value = true
  try {
    const res: any = await authApi.loginByCode({ phone: form.value.phone, code: form.value.code })
    auth.setAuth(res)
    showToast('登录成功')
    router.replace(auth.homePath())
  } catch { /* 拦截器已提示 */ } finally { loading.value = false }
}
</script>

<style scoped>
.auth-page { min-height: 100dvh; background: #f8fafc; }
.auth-logo { text-align: center; padding: 56px 0 32px; }
.auth-logo h1 { font-size: 30px; font-weight: 700; color: #0d9488; letter-spacing: -0.5px; }
.auth-logo p { color: #64748b; margin-top: 8px; font-size: 14px; }
.auth-form { margin-top: 16px; }
.auth-btn { padding: 20px 16px 8px; }
.auth-links { text-align: center; padding: 16px; color: #0d9488; font-size: 14px; }
</style>
