import axios from 'axios'
import { showToast } from 'vant'

const request = axios.create({ baseURL: '/api', timeout: 15000 })

request.interceptors.request.use(config => {
  const token = localStorage.getItem('xiyue_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

request.interceptors.response.use(
  res => {
    const d = res.data
    if (d.code === 200) return d.data
    showToast(d.message || '操作失败')
    return Promise.reject(new Error(d.message || 'error'))
  },
  err => {
    const status = err.response?.status
    const msg = err.response?.data?.message
    if (status === 401) {
      localStorage.removeItem('xiyue_token')
      localStorage.removeItem('xiyue_role')
      localStorage.removeItem('xiyue_user_id')
      localStorage.removeItem('xiyue_phone')
      showToast('登录已过期，请重新登录')
      setTimeout(() => { window.location.href = '/login' }, 800)
    } else if (status === 403) {
      showToast(msg || '无权限访问')
    } else {
      showToast(msg || '网络错误，请稍后重试')
    }
    return Promise.reject(err)
  }
)

export default request
