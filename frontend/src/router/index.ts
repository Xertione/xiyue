import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/Login.vue') },
    { path: '/register', name: 'register', component: () => import('@/views/Register.vue') },
    {
      path: '/user',
      component: () => import('@/layouts/UserLayout.vue'),
      children: [
        { path: 'aunts', component: () => import('@/views/user/Aunts.vue') },
        { path: 'aunts/:id', component: () => import('@/views/user/AuntDetail.vue') },
        { path: 'orders/create', component: () => import('@/views/user/CreateOrder.vue') },
        { path: 'orders', component: () => import('@/views/user/Orders.vue') },
        { path: 'orders/:id', component: () => import('@/views/user/OrderDetail.vue') }
      ]
    },
    {
      path: '/aunt',
      component: () => import('@/layouts/AuntLayout.vue'),
      children: [
        { path: 'grab-list', component: () => import('@/views/aunt/GrabList.vue') },
        { path: 'mine', component: () => import('@/views/aunt/Mine.vue') },
        { path: 'orders/:id', component: () => import('@/views/aunt/OrderDetail.vue') }
      ]
    },
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      children: [
        { path: 'aunts', component: () => import('@/views/admin/Aunts.vue') },
        { path: 'orders', component: () => import('@/views/admin/Orders.vue') },
        { path: 'complaints', component: () => import('@/views/admin/Complaints.vue') }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

// 直接读 localStorage，避免 Pinia 在 router 守卫中的时序依赖问题
function getAuthState() {
  const token = localStorage.getItem('xiyue_token') || ''
  const role = localStorage.getItem('xiyue_role') || ''
  const isValidRole = role === 'USER' || role === 'AUNT' || role === 'ADMIN'
  const homePath = role === 'ADMIN' ? '/admin/aunts' : role === 'AUNT' ? '/aunt/grab-list' : '/user/aunts'
  // token 存在但 role 无效时视为未登录，避免角色路径不匹配导致无限重定向
  return { isLoggedIn: !!token && isValidRole, role, homePath }
}

router.beforeEach((to, _from, next) => {
  const auth = getAuthState()
  if (to.path === '/login' || to.path === '/register') {
    if (auth.isLoggedIn) return next(auth.homePath)
    return next()
  }
  if (!auth.isLoggedIn) return next('/login')
  if (to.path === '/') return next(auth.homePath)
  if (to.path.startsWith('/admin') && auth.role !== 'ADMIN') return next(auth.homePath)
  if (to.path.startsWith('/user') && auth.role !== 'USER') return next(auth.homePath)
  if (to.path.startsWith('/aunt') && auth.role !== 'AUNT') return next(auth.homePath)
  next()
})

export default router
