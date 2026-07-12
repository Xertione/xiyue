<template>
  <div class="auth-page">
    <van-nav-bar title="注册" left-arrow @click-left="router.back()" />
    <van-cell-group inset class="auth-form">
      <van-field v-model="form.phone" label="手机号" placeholder="请输入手机号" type="tel" maxlength="11" />
      <van-field v-model="form.password" label="密码" placeholder="6-20位密码" type="password" />
      <van-field v-model="form.code" label="验证码" placeholder="请输入验证码" maxlength="6">
        <template #button>
          <van-button size="small" type="primary" :disabled="counting > 0" @click="sendCode">
            {{ counting > 0 ? counting + 's' : '发送验证码' }}
          </van-button>
        </template>
      </van-field>
      <van-field label="注册角色">
        <template #input>
          <van-radio-group v-model="form.role" direction="horizontal">
            <van-radio name="USER">用户</van-radio>
            <van-radio name="AUNT">阿姨</van-radio>
          </van-radio-group>
        </template>
      </van-field>
    </van-cell-group>
    <div class="auth-btn">
      <van-button type="primary" block round :loading="loading" @click="register">注册</van-button>
    </div>
    <div class="auth-links">
      <router-link to="/login">已有账号？去登录</router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { authApi } from '@/api'

const router = useRouter()
const loading = ref(false)
const counting = ref(0)
const form = ref({ phone: '', password: '', code: '', role: 'USER' })

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

async function register() {
  if (!/^1[3-9]\d{9}$/.test(form.value.phone)) return showToast('请输入正确的手机号')
  if (form.value.password.length < 6) return showToast('密码至少6位')
  if (!form.value.code) return showToast('请输入验证码')
  loading.value = true
  try {
    await authApi.register({
      phone: form.value.phone,
      password: form.value.password,
      code: form.value.code,
      role: form.value.role
    })
    showToast('注册成功，请登录')
    router.replace('/login')
  } catch { /* 拦截器已提示 */ } finally { loading.value = false }
}
</script>

<style scoped>
.auth-page { min-height: 100dvh; background: #f8fafc; }
.auth-form { margin-top: 16px; }
.auth-btn { padding: 20px 16px 8px; }
.auth-links { text-align: center; padding: 16px; color: #0d9488; font-size: 14px; }
</style>
