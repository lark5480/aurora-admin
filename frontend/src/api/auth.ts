import api from '../utils/request'

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  email?: string
}

export interface AuthResponse {
  token: string
  username: string
  email?: string
  role?: string
}

export interface UserInfo {
  id: number
  username: string
  email?: string
  role?: string
}

export const authApi = {
  register(data: RegisterRequest) {
    return api.post<AuthResponse>('/auth/register', data)
  },

  login(data: LoginRequest) {
    return api.post<AuthResponse>('/auth/login', data)
  },

  getUserInfo() {
    return api.get<UserInfo>('/user/info')
  },
}
