import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { public: true }
  },
  {
    path: '/goods/:id',
    name: 'GoodsDetail',
    component: () => import('../views/GoodsDetail.vue'),
    meta: { public: true }
  },
  {
    path: '/flash-delivery',
    name: 'FlashDelivery',
    component: () => import('../views/FlashDelivery.vue'),
    meta: { public: true }
  },
  {
    path: '/task',
    name: 'TaskHall',
    component: () => import('../views/TaskHall.vue'),
    meta: { public: true }
  },
  {
    path: '/automation',
    name: 'Automation',
    component: () => import('../views/Automation.vue'),
    meta: { requiresAdmin: true }
  },
  {
    path: '/market',
    name: 'Market',
    component: () => import('../views/Market.vue'),
    meta: { public: true }
  },
  {
    path: '/textbook',
    name: 'Textbook',
    component: () => import('../views/Textbook.vue'),
    meta: { public: true }
  },
  {
    path: '/donation',
    name: 'Donation',
    component: () => import('../views/Donation.vue'),
    meta: { public: true }
  },
  {
    path: '/points',
    name: 'Points',
    component: () => import('../views/Points.vue'),
    meta: { public: true }
  },
  {
    path: '/rental',
    name: 'Rental',
    component: () => import('../views/Rental.vue'),
    meta: { public: true }
  },
  {
    path: '/together',
    name: 'Together',
    component: () => import('../views/Together.vue'),
    meta: { public: true }
  },
  {
    path: '/demand',
    name: 'Demand',
    component: () => import('../views/Demand.vue'),
    meta: { public: true }
  },
  {
    path: '/wanted',
    name: 'Wanted',
    component: () => import('../views/Wanted.vue'),
    meta: { public: true }
  },
  {
    path: '/wanted/publish',
    name: 'WantedPublish',
    component: () => import('../views/WantedPublish.vue'),
    meta: { public: true }
  },
  {
    path: '/pickup',
    name: 'Pickup',
    component: () => import('../views/Pickup.vue'),
    meta: { public: true }
  },
  {
    path: '/forum',
    name: 'Forum',
    component: () => import('../views/Forum.vue'),
    meta: { public: true }
  },
  {
    path: '/jury',
    name: 'Jury',
    component: () => import('../views/Jury.vue')
  },
  {
    path: '/alumni',
    name: 'Alumni',
    component: () => import('../views/Alumni.vue')
  },
  {
    path: '/carbon',
    name: 'Carbon',
    component: () => import('../views/Carbon.vue')
  },
  {
    path: '/blindbox',
    name: 'BlindBox',
    component: () => import('../views/BlindBox.vue')
  },
  {
    path: '/hub/:group',
    name: 'ServiceHub',
    component: () => import('../views/ServiceHub.vue'),
    meta: { public: true }
  },
  {
    path: '/publish',
    name: 'Publish',
    component: () => import('../views/Publish.vue')
  },
  {
    path: '/drafts',
    name: 'Drafts',
    component: () => import('../views/Drafts.vue')
  },
  {
    path: '/ledger',
    name: 'Ledger',
    component: () => import('../views/Ledger.vue')
  },
  {
    path: '/exposure',
    name: 'Exposure',
    component: () => import('../views/Exposure.vue')
  },
  {
    path: '/subscribe',
    name: 'Subscribe',
    component: () => import('../views/Subscribe.vue')
  },
  {
    path: '/message',
    name: 'Message',
    component: () => import('../views/Message.vue')
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue')
  },
  {
    path: '/users',
    name: 'UserSearch',
    component: () => import('../views/UserSearch.vue')
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../views/Admin.vue'),
    meta: { requiresAdmin: true }
  },
  // 404 兜底，避免访问不存在路径白屏
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  // 每次进入首页（含从其他页面返回、浏览器后退）都回到顶部
  scrollBehavior(to, from, savedPosition) {
    if (to.path === '/') {
      return { top: 0 }
    }
    return savedPosition || false
  }
})

// 解析 JWT 过期时间（不校验签名，仅前端预判过期）
const isTokenExpired = (token) => {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.exp ? payload.exp * 1000 < Date.now() : false
  } catch (e) {
    return true
  }
}

// 校验本地登录态是否有效（token 存在、未过期、且能解析出 id 的用户对象）
const isValidLogin = () => {
  const token = localStorage.getItem('token')
  if (!token || isTokenExpired(token)) {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    return false
  }
  const userStr = localStorage.getItem('user')
  if (!userStr) return false
  try {
    const user = JSON.parse(userStr)
    return !!(user && user.id)
  } catch (e) {
    localStorage.removeItem('user')
    localStorage.removeItem('token')
    return false
  }
}

const getUserRole = () => {
  try {
    return JSON.parse(localStorage.getItem('user') || '{}').role
  } catch (e) {
    return null
  }
}

router.beforeEach((to, from, next) => {
  // 进店看卖家公开主页（/profile?userId=）允许未登录访问
  const isPublicShopView = to.path === '/profile' && !!to.query.userId
  // 公开页面直接放行（首页、商品详情、登录、他人公开主页）
  if (to.meta.public || isPublicShopView) {
    if (to.path === '/login' && isValidLogin()) {
      return next({ path: '/' })
    }
    return next()
  }
  if (isValidLogin()) {
    // 管理后台需 admin 角色，否则拦回首页
    if (to.meta.requiresAdmin && getUserRole() !== 'admin') {
      ElMessage.error('没有权限访问管理后台')
      return next({ path: '/' })
    }
    next()
  } else {
    // 未登录跳登录页，并记录来源地址以便登录后回跳
    next({ path: '/login', query: { redirect: to.fullPath } })
  }
})

export default router
