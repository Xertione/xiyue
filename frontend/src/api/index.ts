import request from './request'

// ===== 认证 =====
export const authApi = {
  smsCode: (phone: string) => request.post('/auth/sms-code', { phone }),
  register: (data: { phone: string; password: string; code: string; role: string }) =>
    request.post('/auth/register', data),
  loginByPassword: (data: { phone: string; password: string }) =>
    request.post('/auth/login/password', data),
  loginByCode: (data: { phone: string; code: string }) =>
    request.post('/auth/login/code', data),
  resetPassword: (data: { phone: string; code: string; newPassword: string }) =>
    request.post('/auth/reset-password', data),
  profile: () => request.get('/auth/profile')
}

// ===== 阿姨（用户端）=====
export const auntApi = {
  list: (params: { page: number; size: number; minRating?: number; minPrice?: number; maxPrice?: number; skillTag?: string; sort?: string }) =>
    request.get('/aunts', { params }),
  detail: (id: number) => request.get(`/aunts/${id}`),
  myAcceptStatus: (status: string) => request.patch('/aunts/me/status', { acceptStatus: status })
}

// ===== 管理员-阿姨 =====
export const adminAuntApi = {
  list: (params: { page: number; size: number }) => request.get('/admin/aunts', { params }),
  detail: (id: number) => request.get(`/admin/aunts/${id}`),
  update: (id: number, data: { name?: string; avatar?: string; price?: number; skillTags?: string; intro?: string }) =>
    request.put(`/admin/aunts/${id}`, data),
  status: (id: number, adminStatus: string) => request.patch(`/admin/aunts/${id}/status`, { adminStatus }),
  delete: (id: number) => request.delete(`/admin/aunts/${id}`)
}

// ===== 订单 =====
export const orderApi = {
  create: (data: { serviceDate: string; startHour: number; durationHours: number; contactName: string; contactPhone: string; address: string; amount: number }) =>
    request.post('/orders', data),
  userList: (params: { page: number; size: number; status?: number }) => request.get('/orders', { params }),
  detail: (id: number) => request.get(`/orders/${id}`),
  pay: (id: number) => request.post(`/orders/${id}/pay`),
  cancel: (id: number) => request.post(`/orders/${id}/cancel`),
  confirm: (id: number) => request.post(`/orders/${id}/confirm`),
  grabList: (params: { page: number; size: number }) => request.get('/orders/grab-list', { params }),
  grab: (id: number) => request.post(`/orders/${id}/grab`),
  mine: (params: { page: number; size: number; status?: number }) => request.get('/orders/mine', { params }),
  start: (id: number) => request.post(`/orders/${id}/start`),
  complete: (id: number, imageUrl: string) => request.post(`/orders/${id}/complete`, { imageUrl })
}

// ===== 管理员-订单 =====
export const adminOrderApi = {
  list: (params: { page: number; size: number; status?: number }) => request.get('/admin/orders', { params }),
  assign: (id: number, auntId: number) => request.post(`/admin/orders/${id}/assign`, { auntId })
}

// ===== 评价 =====
export const reviewApi = {
  create: (data: { orderId: number; rating: number; content: string }) => request.post('/reviews', data),
  getByOrder: (orderId: number) => request.get(`/orders/${orderId}/review`)
}

// ===== 投诉 =====
export const complaintApi = {
  create: (data: { orderId: number; reason: string }) => request.post('/complaints', data),
  adminList: (params: { page: number; size: number; status?: string }) => request.get('/admin/complaints', { params }),
  handle: (id: number, handleRemark: string) => request.post(`/admin/complaints/${id}/handle`, { handleRemark })
}
