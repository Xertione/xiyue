import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('xiyue_token') || '')
  const userId = ref(Number(localStorage.getItem('xiyue_user_id')) || 0)
  const phone = ref(localStorage.getItem('xiyue_phone') || '')
  const role = ref(localStorage.getItem('xiyue_role') || '')
  const nickname = ref(localStorage.getItem('xiyue_nickname') || '')

  const isLoggedIn = computed(() => !!token.value)

  function setAuth(data: { token: string; userId: number; phone: string; role: string; nickname?: string }) {
    token.value = data.token
    userId.value = data.userId
    phone.value = data.phone
    role.value = data.role
    nickname.value = data.nickname || ''
    localStorage.setItem('xiyue_token', data.token)
    localStorage.setItem('xiyue_user_id', String(data.userId))
    localStorage.setItem('xiyue_phone', data.phone)
    localStorage.setItem('xiyue_role', data.role)
    localStorage.setItem('xiyue_nickname', data.nickname || '')
  }

  function logout() {
    token.value = ''
    userId.value = 0
    phone.value = ''
    role.value = ''
    nickname.value = ''
    localStorage.removeItem('xiyue_token')
    localStorage.removeItem('xiyue_user_id')
    localStorage.removeItem('xiyue_phone')
    localStorage.removeItem('xiyue_role')
    localStorage.removeItem('xiyue_nickname')
  }

  function homePath(): string {
    if (role.value === 'ADMIN') return '/admin/aunts'
    if (role.value === 'AUNT') return '/aunt/grab-list'
    return '/user/aunts'
  }

  return { token, userId, phone, role, nickname, isLoggedIn, setAuth, logout, homePath }
})
