<template>
  <div class="register-container">
    <!-- Animated aurora background -->
    <div class="aurora">
      <div class="aurora__glow aurora__glow--1"></div>
      <div class="aurora__glow aurora__glow--2"></div>
      <div class="aurora__glow aurora__glow--3"></div>
    </div>

    <!-- Floating particles -->
    <div class="particles">
      <span v-for="n in 20" :key="n" class="particle" :style="particleStyle(n)"></span>
    </div>

    <!-- Glass card -->
    <div class="glass-card">
      <div class="glass-card__shine"></div>
      <div class="glass-card__content">
        <div class="brand">
          <div class="brand__icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
            </svg>
          </div>
          <h1 class="brand__title">{{ configStore.get('site.name', 'Create Account') }}</h1>
          <p class="brand__subtitle">{{ configStore.get('site.logo') || 'Join us today' }}</p>
        </div>

        <el-form v-if="registerEnabled" ref="formRef" :model="form" :rules="rules" class="register-form" size="large">
          <el-form-item prop="username" class="form-item">
            <div class="input-wrapper">
              <span class="input-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                  <circle cx="12" cy="7" r="4" />
                </svg>
              </span>
              <el-input v-model="form.username" placeholder="Username (3-20 chars)" class="custom-input" />
            </div>
          </el-form-item>

          <el-form-item prop="email" class="form-item">
            <div class="input-wrapper">
              <span class="input-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" />
                  <polyline points="22,6 12,13 2,6" />
                </svg>
              </span>
              <el-input v-model="form.email" placeholder="Email (optional)" class="custom-input" />
            </div>
          </el-form-item>

          <el-form-item prop="password" class="form-item">
            <div class="input-wrapper">
              <span class="input-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                  <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                </svg>
              </span>
              <el-input
                v-model="form.password"
                type="password"
                placeholder="Password (min 6 chars)"
                show-password
                class="custom-input"
              />
            </div>
          </el-form-item>

          <el-form-item prop="confirmPassword" class="form-item">
            <div class="input-wrapper">
              <span class="input-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                  <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                  <path d="M9 17v2" />
                  <path d="M15 17v2" />
                </svg>
              </span>
              <el-input
                v-model="form.confirmPassword"
                type="password"
                placeholder="Confirm password"
                show-password
                class="custom-input"
              />
            </div>
          </el-form-item>

          <el-form-item class="form-item">
            <el-button type="primary" :loading="loading" class="register-btn" @click="handleRegister">
              <span v-if="!loading">Create Account</span>
              <span v-else class="loading-dots"> <span></span><span></span><span></span> </span>
            </el-button>
          </el-form-item>
        </el-form>

        <el-result v-else icon="warning" title="注册已关闭" sub-title="管理员已关闭注册功能" />

        <div class="switch-page">
          <span class="switch-page__text">Already have an account?</span>
          <button class="switch-page__link" @click="goToLogin">Sign in</button>
        </div>
      </div>
    </div>

    <!-- Decorative corner elements -->
    <div class="corner corner--tl"></div>
    <div class="corner corner--tr"></div>
    <div class="corner corner--bl"></div>
    <div class="corner corner--br"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '../api/auth'
import { useConfigStore } from '../stores/config'

const router = useRouter()
const configStore = useConfigStore()
const formRef = ref(null)
const loading = ref(false)

const registerEnabled = computed(() => configStore.getBool('register.enabled', true))

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('Passwords do not match'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: 'Please enter username', trigger: 'blur' },
    { min: 3, max: 20, message: 'Username must be 3-20 characters', trigger: 'blur' },
  ],
  password: [
    { required: true, message: 'Please enter password', trigger: 'blur' },
    { min: 6, message: 'Password must be at least 6 characters', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: 'Please confirm password', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

const particleStyle = (n) => {
  const size = Math.random() * 4 + 2
  const left = Math.random() * 100
  const delay = Math.random() * 20
  const duration = Math.random() * 10 + 15
  return {
    '--size': `${size}px`,
    '--left': `${left}%`,
    '--delay': `${delay}s`,
    '--duration': `${duration}s`,
  }
}

onMounted(async () => {
  await configStore.fetchConfigs()
})

const handleRegister = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await authApi.register({
          username: form.username,
          password: form.password,
          email: form.email,
        })
        if (res.data.code === 200) {
          ElMessage.success('Account created! Please sign in.')
          router.push('/login')
        } else {
          ElMessage.error(res.data.message || 'Registration failed')
        }
      } catch (error) {
        ElMessage.error(error.response?.data?.message || 'Registration failed')
      } finally {
        loading.value = false
      }
    }
  })
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=Sora:wght@300;400;500;600&display=swap');

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.register-container {
  min-height: 100vh;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: var(--bg-darker);
  font-family: 'Outfit', sans-serif;
}

/* Aurora Background */
.aurora {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.aurora__glow {
  position: absolute;
  border-radius: 50%;
  opacity: 0.5;
  animation: float 20s ease-in-out infinite;
}

.aurora__glow--1 {
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, var(--neon-magenta) 0%, var(--neon-magenta-dim) 50%, transparent 70%);
  top: -200px;
  right: -100px;
  animation-delay: 0s;
  filter: blur(80px);
  box-shadow:
    0 0 120px var(--neon-magenta),
    0 0 200px var(--neon-magenta-dim);
}

.aurora__glow--2 {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, var(--neon-cyan) 0%, var(--neon-cyan-dim) 50%, transparent 70%);
  bottom: -150px;
  left: -100px;
  animation-delay: -7s;
  filter: blur(80px);
  box-shadow:
    0 0 120px var(--neon-cyan),
    0 0 200px var(--neon-cyan-dim);
}

.aurora__glow--3 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, var(--neon-green) 0%, var(--neon-green-dim) 50%, transparent 70%);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation-delay: -14s;
  filter: blur(80px);
  box-shadow:
    0 0 100px var(--neon-green),
    0 0 180px var(--neon-green-dim);
}

@keyframes float {
  0%,
  100% {
    transform: translate(0, 0) scale(1);
  }
  25% {
    transform: translate(-30px, -30px) scale(1.1);
  }
  50% {
    transform: translate(20px, 20px) scale(0.95);
  }
  75% {
    transform: translate(-20px, 10px) scale(1.05);
  }
}

/* Particles */
.particles {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.particle {
  position: absolute;
  width: var(--size);
  height: var(--size);
  border-radius: 50%;
  left: var(--left);
  bottom: -10px;
  animation: rise var(--duration) linear infinite;
  animation-delay: var(--delay);
  opacity: 0;
  box-shadow:
    0 0 6px var(--color),
    0 0 12px var(--color);
}

.particle:nth-child(3n + 1) {
  background: var(--neon-magenta);
  --color: var(--neon-magenta);
}
.particle:nth-child(3n + 2) {
  background: var(--neon-cyan);
  --color: var(--neon-cyan);
}
.particle:nth-child(3n) {
  background: var(--neon-green);
  --color: var(--neon-green);
}

@keyframes rise {
  0% {
    opacity: 0;
    transform: translateY(0) scale(0);
  }
  10% {
    opacity: 0.8;
  }
  90% {
    opacity: 0.3;
  }
  100% {
    opacity: 0;
    transform: translateY(-100vh) scale(1);
  }
}

/* Glass Card */
.glass-card {
  position: relative;
  width: 420px;
  padding: 2px;
  background: linear-gradient(135deg, var(--neon-magenta), var(--neon-cyan), var(--neon-green));
  border-radius: 24px;
  box-shadow:
    0 0 40px var(--border-glow-magenta),
    0 0 80px rgba(0, 255, 255, 0.2),
    0 20px 60px rgba(0, 0, 0, 0.5);
  animation: slideUp 0.8s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  opacity: 0;
  transform: translateY(30px);
}

@keyframes slideUp {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.glass-card__shine {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 50%;
  background: linear-gradient(to bottom, rgba(255, 255, 255, 0.1) 0%, transparent 100%);
  border-radius: 24px 24px 0 0;
  pointer-events: none;
}

.glass-card__content {
  position: relative;
  background: var(--glass-bg);
  border-radius: 22px;
  padding: 48px 40px;
}

/* Brand */
.brand {
  text-align: center;
  margin-bottom: 36px;
}

.brand__icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 20px;
  background: linear-gradient(135deg, var(--neon-magenta) 0%, var(--neon-cyan) 100%);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow:
    0 0 30px rgba(255, 0, 255, 0.5),
    0 0 60px rgba(0, 255, 255, 0.3);
  animation: pulse 3s ease-in-out infinite;
}

@keyframes pulse {
  0%,
  100% {
    box-shadow:
      0 0 30px rgba(255, 0, 255, 0.5),
      0 0 60px rgba(0, 255, 255, 0.3);
  }
  50% {
    box-shadow:
      0 0 40px rgba(255, 0, 255, 0.7),
      0 0 80px rgba(0, 255, 255, 0.5);
  }
}

.brand__icon svg {
  width: 32px;
  height: 32px;
  color: var(--text-primary);
}

.brand__title {
  font-family: 'Sora', sans-serif;
  font-size: 28px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
  letter-spacing: -0.5px;
}

.brand__subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 400;
}

/* Form */
.register-form {
  margin-bottom: 32px;
}

.form-item {
  margin-bottom: 16px;
}

.form-item :deep(.el-form-item__error) {
  font-family: 'Outfit', sans-serif;
  padding-top: 6px;
  color: var(--neon-magenta);
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 16px;
  z-index: 1;
  color: var(--text-secondary);
  pointer-events: none;
}

.input-icon svg {
  width: 18px;
  height: 18px;
}

.custom-input {
  --el-input-bg-color: var(--glass-bg);
  --el-input-border-color: var(--border-glow-magenta);
  --el-input-hover-border-color: rgba(0, 255, 255, 0.4);
  --el-input-focus-border-color: var(--neon-magenta);
  --el-input-text-color: var(--text-primary);
  --el-input-placeholder-color: rgba(255, 255, 255, 0.3);
  --el-input-height: 52px;
}

.custom-input :deep(.el-input__wrapper) {
  background-color: var(--el-input-bg-color);
  border: 1px solid var(--el-input-border-color);
  border-radius: 12px;
  box-shadow: none;
  padding-left: 48px;
  transition: all var(--transition-fast) ease;
}

.custom-input :deep(.el-input__wrapper:hover) {
  border-color: var(--el-input-hover-border-color);
}

.custom-input :deep(.el-input__wrapper.is-focus) {
  border-color: var(--el-input-focus-border-color);
  box-shadow:
    0 0 0 3px var(--border-glow-magenta),
    0 0 20px var(--border-glow-magenta);
}

.custom-input :deep(.el-input__inner) {
  color: var(--el-input-text-color);
  font-family: 'Outfit', sans-serif;
  font-size: 15px;
}

.custom-input :deep(.el-input__inner::placeholder) {
  color: var(--el-input-placeholder-color);
}

/* Register Button */
.register-btn {
  width: 100%;
  height: 52px;
  background: linear-gradient(135deg, var(--neon-magenta) 0%, var(--neon-cyan) 100%);
  border: none;
  border-radius: 12px;
  font-family: 'Outfit', sans-serif;
  font-size: 16px;
  font-weight: 600;
  color: var(--bg-darker);
  cursor: pointer;
  transition: all var(--transition-fast) ease;
  position: relative;
  overflow: hidden;
}

.register-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, var(--neon-cyan) 0%, var(--neon-magenta) 100%);
  opacity: 0;
  transition: opacity var(--transition-fast) ease;
}

.register-btn:hover {
  transform: translateY(-2px);
  box-shadow:
    0 0 30px var(--border-glow-magenta),
    0 0 60px rgba(0, 255, 255, 0.3);
}

.register-btn:hover::before {
  opacity: 1;
}

.register-btn span {
  position: relative;
  z-index: 1;
}

.register-btn:active {
  transform: translateY(0);
}

/* Loading dots */
.loading-dots {
  display: flex;
  gap: 4px;
  justify-content: center;
}

.loading-dots span {
  width: 6px;
  height: 6px;
  background: var(--text-primary);
  border-radius: 50%;
  animation: bounce 1.4s ease-in-out infinite both;
}

.loading-dots span:nth-child(1) {
  animation-delay: -0.32s;
}
.loading-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes bounce {
  0%,
  80%,
  100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

/* Switch Page */
.switch-page {
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.switch-page__text {
  color: var(--text-secondary);
  font-size: 14px;
}

.switch-page__link {
  background: none;
  border: none;
  color: var(--neon-magenta);
  font-size: 14px;
  font-weight: 600;
  font-family: 'Outfit', sans-serif;
  cursor: pointer;
  transition: all var(--transition-fast) ease;
  padding: 4px 8px;
  border-radius: 4px;
}

.switch-page__link:hover {
  color: var(--neon-cyan);
  background: var(--border-glow-magenta);
  text-shadow: 0 0 10px var(--border-glow-magenta);
}

/* Corner decorations */
.corner {
  position: absolute;
  width: 100px;
  height: 100px;
  pointer-events: none;
}

.corner::before,
.corner::after {
  content: '';
  position: absolute;
  background: linear-gradient(135deg, var(--border-glow-magenta) 0%, transparent 100%);
}

.corner--tl {
  top: 20px;
  left: 20px;
}
.corner--tl::before {
  width: 60px;
  height: 2px;
  top: 0;
  left: 0;
  box-shadow: 0 0 10px var(--border-glow-magenta);
}
.corner--tl::after {
  width: 2px;
  height: 60px;
  top: 0;
  left: 0;
  box-shadow: 0 0 10px var(--border-glow-magenta);
}

.corner--tr {
  top: 20px;
  right: 20px;
}
.corner--tr::before {
  width: 60px;
  height: 2px;
  top: 0;
  right: 0;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.5);
}
.corner--tr::after {
  width: 2px;
  height: 60px;
  top: 0;
  right: 0;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.5);
}

.corner--bl {
  bottom: 20px;
  left: 20px;
}
.corner--bl::before {
  width: 60px;
  height: 2px;
  bottom: 0;
  left: 0;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.5);
}
.corner--bl::after {
  width: 2px;
  height: 60px;
  bottom: 0;
  left: 0;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.5);
}

.corner--br {
  bottom: 20px;
  right: 20px;
}
.corner--br::before {
  width: 60px;
  height: 2px;
  bottom: 0;
  right: 0;
  box-shadow: 0 0 10px var(--border-glow-magenta);
}
.corner--br::after {
  width: 2px;
  height: 60px;
  bottom: 0;
  right: 0;
  box-shadow: 0 0 10px var(--border-glow-magenta);
}

/* Message styling */
:deep(.el-message) {
  --el-message-bg-color: var(--bg-dark);
  --el-message-border-color: var(--border-glow-magenta);
  border-radius: 12px;
  backdrop-filter: blur(10px);
}

/* Reduced motion support */
@media (prefers-reduced-motion: reduce) {
  .aurora__glow,
  .particle,
  .glass-card,
  .brand__icon,
  .register-btn,
  .loading-dots span {
    animation: none;
  }

  .custom-input :deep(.el-input__wrapper),
  .register-btn,
  .switch-page__link {
    transition: none;
  }
}
</style>
