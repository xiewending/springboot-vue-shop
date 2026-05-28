import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { loginApi } from '../api/auth'
import type { RbacMenu, UserInfo } from '../types/auth'

const TOKEN_KEY = 'shop_pilot_token'
const USER_KEY = 'shop_pilot_user'
const ROLES_KEY = 'shop_pilot_roles'
const PERMISSIONS_KEY = 'shop_pilot_permissions'
const MENUS_KEY = 'shop_pilot_menus'

function readJson<T>(key: string, fallback: T): T {
  const value = localStorage.getItem(key)
  if (!value) {
    return fallback
  }
  try {
    return JSON.parse(value) as T
  } catch {
    localStorage.removeItem(key)
    return fallback
  }
}

function flattenMenus(menus: RbacMenu[]): RbacMenu[] {
  return menus.flatMap((menu) => [menu, ...flattenMenus(menu.children ?? [])])
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) ?? '')
  const user = ref<UserInfo | null>(readJson<UserInfo | null>(USER_KEY, null))
  const roles = ref<string[]>(readJson<string[]>(ROLES_KEY, []))
  const permissions = ref<string[]>(readJson<string[]>(PERMISSIONS_KEY, []))
  const menus = ref<RbacMenu[]>(readJson<RbacMenu[]>(MENUS_KEY, []))

  const isLoggedIn = computed(() => Boolean(token.value))
  const hasMenuData = computed(() => menus.value.length > 0)
  const menuRoutes = computed(() => flattenMenus(menus.value).filter((menu) => Boolean(menu.path && menu.component)))
  const firstMenuPath = computed(() => menuRoutes.value[0]?.path ?? '/dashboard')

  async function login(username: string, password: string) {
    const { data } = await loginApi({ username, password })
    token.value = data.data.token
    user.value = data.data.user
    roles.value = data.data.roles
    permissions.value = data.data.permissions
    menus.value = data.data.menus
    persist()
  }

  function hasRole(role: string) {
    return roles.value.includes(role)
  }

  function hasPermission(permission: string) {
    return permissions.value.includes(permission)
  }

  function persist() {
    localStorage.setItem(TOKEN_KEY, token.value)
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
    localStorage.setItem(ROLES_KEY, JSON.stringify(roles.value))
    localStorage.setItem(PERMISSIONS_KEY, JSON.stringify(permissions.value))
    localStorage.setItem(MENUS_KEY, JSON.stringify(menus.value))
  }

  function logout() {
    token.value = ''
    user.value = null
    roles.value = []
    permissions.value = []
    menus.value = []
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    localStorage.removeItem(ROLES_KEY)
    localStorage.removeItem(PERMISSIONS_KEY)
    localStorage.removeItem(MENUS_KEY)
  }

  return {
    token,
    user,
    roles,
    permissions,
    menus,
    isLoggedIn,
    hasMenuData,
    menuRoutes,
    firstMenuPath,
    login,
    hasRole,
    hasPermission,
    logout
  }
})
