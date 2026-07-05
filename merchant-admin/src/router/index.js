import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
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
        meta: { title: '数据看板', icon: 'DataAnalysis' }
      },
      {
        path: 'shop',
        name: 'ShopInfo',
        component: () => import('@/views/shop/ShopInfo.vue'),
        meta: { title: '店铺信息', icon: 'Shop' }
      },
      {
        path: 'goods',
        name: 'GoodsList',
        component: () => import('@/views/goods/GoodsList.vue'),
        meta: { title: '商品管理', icon: 'Goods' }
      },
      {
        path: 'goods/edit/:id?',
        name: 'GoodsEdit',
        component: () => import('@/views/goods/GoodsEdit.vue'),
        meta: { title: '商品编辑', hidden: true }
      },
      {
        path: 'order',
        name: 'OrderList',
        component: () => import('@/views/order/OrderList.vue'),
        meta: { title: '订单管理', icon: 'List' }
      },
      {
        path: 'evaluations',
        name: 'ShopEvaluation',
        component: () => import('@/views/evaluation/ShopEvaluation.vue'),
        meta: { title: '店铺评价', icon: 'Star' }
      },
      {
        path: 'coupons',
        name: 'CouponList',
        component: () => import('@/views/coupon/CouponList.vue'),
        meta: { title: '优惠券管理', icon: 'Ticket' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 商家管理后台` : '商家管理后台'
  const token = sessionStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
