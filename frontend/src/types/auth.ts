export interface LoginRequest {
  username: string
  password: string
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
}

export interface RbacMenu {
  id: number
  parentId: number
  name: string
  path: string
  component: string
  icon?: string
  permission?: string
  children?: RbacMenu[]
}

export interface LoginResponse {
  token: string
  tokenType: string
  expiresIn: number
  user: UserInfo
  roles: string[]
  permissions: string[]
  menus: RbacMenu[]
}
