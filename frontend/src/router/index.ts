import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import { pinia } from '../stores'
import { useAuthStore } from '../stores/auth'
import type { RbacMenu } from '../types/auth'
import LayoutView from '../views/LayoutView.vue'
import LoginView from '../views/LoginView.vue'

const componentMap: Record<string, RouteRecordRaw['component']> = {
  DashboardView: () => import('../views/DashboardView.vue'),
  ProductsView: () => import('../views/ProductsView.vue'),
  OrdersView: () => import('../views/OrdersView.vue')
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView
    }
  ]
})

let dynamicRoutesReady = false

function normalizeChildPath(path: string) {
  return path.replace(/^\//, '')
}

function buildMenuRoutes(menus: RbacMenu[]): RouteRecordRaw[] {
  return menus.flatMap((menu) => {
    const routes: RouteRecordRaw[] = []
    const component = componentMap[menu.component]
    if (menu.path && component) {
      routes.push({
        path: normalizeChildPath(menu.path),
        name: `menu-${menu.id}`,
        component,
        meta: {
          title: menu.name,
          permission: menu.permission,
          requiresAuth: true
        }
      })
    }
    routes.push(...buildMenuRoutes(menu.children ?? []))
    return routes
  })
}

export function setupDynamicRoutes() {
  const authStore = useAuthStore(pinia)
  if (dynamicRoutesReady || !authStore.hasMenuData) {
    return
  }

  const children = buildMenuRoutes(authStore.menus)
  if (authStore.hasPermission('order:detail')) {
    children.push({
      path: 'orders/:id',
      name: 'order-detail',
      component: () => import('../views/OrderDetailView.vue'),
      meta: {
        title: '订单详情',
        permission: 'order:detail',
        requiresAuth: true,
        hidden: true
      }
    })
  }
  children.push({
    path: '',
    redirect: normalizeChildPath(authStore.firstMenuPath)
  })

  router.addRoute({
    path: '/',
    name: 'layout',
    component: LayoutView,
    meta: {
      requiresAuth: true
    },
    children
  })
  router.addRoute({
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    redirect: '/'
  })
  dynamicRoutesReady = true
}

router.beforeEach((to) => {
  const authStore = useAuthStore(pinia)

  if (to.path === '/login') {
    if (authStore.isLoggedIn && authStore.hasMenuData) {
      setupDynamicRoutes()
      return authStore.firstMenuPath
    }
    return true
  }

  if (!authStore.isLoggedIn) {
    return {
      path: '/login',
      query: {
        redirect: to.fullPath
      }
    }
  }

  if (!authStore.hasMenuData) {
    authStore.logout()
    return {
      path: '/login',
      query: {
        redirect: to.fullPath
      }
    }
  }

  if (!dynamicRoutesReady) {
    setupDynamicRoutes()
    return {
      path: to.fullPath,
      replace: true
    }
  }

  const requiredPermission = to.meta.permission as string | undefined
  if (requiredPermission && !authStore.hasPermission(requiredPermission)) {
    return authStore.firstMenuPath
  }

  return true
})

export default router
