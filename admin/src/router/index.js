import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { noAuth: true }
  },
  {
    path: '/',
    component: () => import('@/components/Layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'DataAnalysis' }
      },
      {
        path: 'users',
        name: 'UserList',
        component: () => import('@/views/user/UserList.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'merchants',
        name: 'MerchantList',
        component: () => import('@/views/merchant/MerchantList.vue'),
        meta: { title: '商家管理', icon: 'Shop' }
      },
      {
        path: 'merchants/audit',
        name: 'MerchantAudit',
        component: () => import('@/views/merchant/MerchantAudit.vue'),
        meta: { title: '商家审核', icon: 'Checked', adminOnly: true }
      },
      {
        path: 'delivery',
        name: 'DeliveryList',
        component: () => import('@/views/delivery/DeliveryList.vue'),
        meta: { title: '骑手管理', icon: 'Van', adminOnly: true }
      },
      {
        path: 'orders',
        name: 'OrderList',
        component: () => import('@/views/order/OrderList.vue'),
        meta: { title: '订单管理', icon: 'Document' }
      },
      {
        path: 'goods',
        name: 'GoodsList',
        component: () => import('@/views/goods/GoodsList.vue'),
        meta: { title: '商品管理', icon: 'Goods', adminOnly: true }
      },
      {
        path: 'evaluations',
        name: 'EvaluationList',
        component: () => import('@/views/evaluation/EvaluationList.vue'),
        meta: { title: '评价管理', icon: 'Star', adminOnly: true }
      },
      {
        path: 'feedback',
        name: 'FeedbackList',
        component: () => import('@/views/feedback/FeedbackList.vue'),
        meta: { title: '反馈管理', icon: 'ChatLineSquare', adminOnly: true }
      },
      {
        path: 'marketing/full-reduce',
        name: 'FullReduce',
        component: () => import('@/views/marketing/FullReduce.vue'),
        meta: { title: '满减活动', icon: 'Discount', adminOnly: true }
      },
      {
        path: 'marketing/coupons',
        name: 'CouponList',
        component: () => import('@/views/marketing/CouponList.vue'),
        meta: { title: '平台优惠券', icon: 'Ticket', adminOnly: true }
      },
      {
        path: 'system/configs',
        name: 'ConfigList',
        component: () => import('@/views/system/ConfigList.vue'),
        meta: { title: '系统配置', icon: 'Setting', adminOnly: true }
      },
      {
        path: 'system/admins',
        name: 'AdminUserList',
        component: () => import('@/views/system/AdminUserList.vue'),
        meta: { title: '管理员管理', icon: 'UserFilled', adminOnly: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
  const role = userInfo?.role || ''

  if (to.meta.noAuth) {
    if (token && to.path === '/login') {
      next('/dashboard')
    } else {
      next()
    }
  } else {
    if (!token) {
      next('/login')
    } else if (to.meta.adminOnly && role !== 'admin') {
      next('/dashboard')
    } else {
      next()
    }
  }
})

export default router
