<template>
  <div class="login-container">
    <div class="login-card">
      <!-- Logo区域 -->
      <div class="logo-area">
        <div class="jd-logo">
          <svg viewBox="0 0 60 60" width="52" height="52">
            <rect x="4" y="4" width="52" height="52" rx="10" fill="#E1251B" />
            <path d="M20 22 L20 40 L26 40 L26 28 L32 40 L38 40 L38 22 Z" fill="white" />
            <rect x="42" y="22" width="4" height="18" fill="white" />
          </svg>
        </div>
        <h1 class="title">校园二手市场</h1>
        <p class="subtitle">闲置好物 低价淘好物</p>
      </div>

      <!-- Tab切换 -->
      <div class="tab-switch">
        <div
          class="tab-item"
          :class="{ active: activeTab === 'login' }"
          @click="activeTab = 'login'"
        >
          登录
          <span class="tab-underline"></span>
        </div>
        <div
          class="tab-item"
          :class="{ active: activeTab === 'register' }"
          @click="activeTab = 'register'"
        >
          注册
          <span class="tab-underline"></span>
        </div>
      </div>

      <!-- 登录表单 -->
      <div class="form-area" v-if="activeTab === 'login'">
        <el-form :model="loginForm" @submit.prevent="handleLogin">
          <el-form-item>
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              size="large"
              :prefix-icon="UserIcon"
            />
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="LockIcon"
              show-password
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              class="submit-btn"
              :loading="loading"
              @click="handleLogin"
            >登 录</el-button>
          </el-form-item>
        </el-form>
        <p class="form-tip">登录即代表同意《校园二手市场用户协议》</p>
      </div>

      <!-- 注册表单 -->
      <div class="form-area" v-else>
        <el-form :model="registerForm" @submit.prevent="handleRegister">
          <el-form-item>
            <el-input
              v-model="registerForm.studentNo"
              placeholder="请输入学号"
              size="large"
              :prefix-icon="SchoolIcon"
            />
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="registerForm.username"
              placeholder="请输入昵称"
              size="large"
              :prefix-icon="UserIcon"
            />
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="registerForm.phone"
              placeholder="请输入手机号"
              size="large"
              :prefix-icon="PhoneIcon"
            />
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="LockIcon"
              show-password
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              class="submit-btn"
              :loading="loading"
              @click="handleRegister"
            >注 册</el-button>
          </el-form-item>
        </el-form>
        <p class="form-tip">注册即代表同意《校园二手市场用户协议》</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, shallowRef } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '../api/request'
import { connectRealtime } from '../store/realtime'
import { ElMessage } from 'element-plus'
import { User, Lock, Phone, School } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const activeTab = ref('login')
const loading = ref(false)

// 图标引用
const UserIcon = shallowRef(User)
const LockIcon = shallowRef(Lock)
const PhoneIcon = shallowRef(Phone)
const SchoolIcon = shallowRef(School)

// 登录表单 - 后端接口参数是username
const loginForm = ref({
  username: '',
  password: ''
})

// 注册表单 - 后端User实体含studentNo字段
const registerForm = ref({
  studentNo: '',
  username: '',
  password: '',
  phone: ''
})

// 登录
const handleLogin = async () => {
  if (loading.value) return
  if (!loginForm.value.username) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!loginForm.value.password) {
    ElMessage.warning('请输入密码')
    return
  }
  loading.value = true
  try {
    const res = await request.post('/user/login', loginForm.value)
    // response拦截器已解包res.data，拿到的是 { success, user, msg }
    if (res.success) {
      localStorage.setItem('token', res.token)
      localStorage.setItem('user', JSON.stringify(res.user))
      connectRealtime()
      ElMessage.success(res.msg || '登录成功')
      router.push(route.query.redirect || '/')
    } else {
      ElMessage.error(res.msg || '登录失败')
    }
  } catch (e) {
    // 错误已由请求拦截器统一提示
  } finally {
    loading.value = false
  }
}

// 注册
const handleRegister = async () => {
  if (loading.value) return
  if (!registerForm.value.studentNo) {
    ElMessage.warning('请输入学号')
    return
  }
  if (!registerForm.value.username) {
    ElMessage.warning('请输入昵称')
    return
  }
  if (!registerForm.value.phone) {
    ElMessage.warning('请输入手机号')
    return
  }
  if (!registerForm.value.password) {
    ElMessage.warning('请输入密码')
    return
  }
  if (registerForm.value.password.length < 6) {
    ElMessage.warning('密码长度不能少于 6 位')
    return
  }
  loading.value = true
  try {
    await request.post('/user/register', registerForm.value)
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
    // 清空注册表单
    registerForm.value = {
      studentNo: '',
      username: '',
      password: '',
      phone: ''
    }
  } catch (e) {
    // 错误已由请求拦截器统一提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  width: 100vw;
  height: 100vh;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.login-card {
  width: 400px;
  background: #fff;
  padding: 40px 36px 32px;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  position: relative;
  z-index: 1;
}

/* Logo区域 */
.logo-area {
  text-align: center;
  margin-bottom: 28px;
}

.jd-logo {
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.title {
  font-size: 22px;
  font-weight: 700;
  color: #E1251B;
  margin-bottom: 6px;
  letter-spacing: 1px;
}

.subtitle {
  font-size: 13px;
  color: #999;
}

/* Tab切换 */
.tab-switch {
  display: flex;
  position: relative;
  margin-bottom: 28px;
  border-bottom: 1px solid #eee;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 12px 0;
  font-size: 15px;
  color: #666;
  cursor: pointer;
  position: relative;
  transition: color 0.2s;
  user-select: none;
}

.tab-item.active {
  color: #E1251B;
  font-weight: 600;
}

.tab-underline {
  position: absolute;
  bottom: -1px;
  left: 50%;
  transform: translateX(-50%) scaleX(0);
  width: 28px;
  height: 2px;
  background: #E1251B;
  border-radius: 2px;
  transition: transform 0.2s;
}

.tab-item.active .tab-underline {
  transform: translateX(-50%) scaleX(1);
}

/* 表单区域 */
.form-area {
  width: 100%;
}

.form-area :deep(.el-input__wrapper) {
  border-radius: 6px;
  padding: 2px 12px;
  box-shadow: 0 0 0 1px #ddd inset;
  transition: box-shadow 0.2s;
  background: #fff;
}

.form-area :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #E1251B inset;
}

.form-area :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #E1251B inset;
}

.form-area :deep(.el-form-item) {
  margin-bottom: 18px;
}

.form-area :deep(.el-input__inner) {
  height: 34px;
  font-size: 14px;
  color: #333;
}

.form-area :deep(.el-input__inner::placeholder) {
  color: #bbb;
}

.form-area :deep(.el-input__prefix .el-icon) {
  color: #999;
  font-size: 16px;
}

/* 提交按钮 */
.submit-btn {
  width: 100%;
  height: 44px;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 600;
  background: #E1251B;
  border: none;
  letter-spacing: 4px;
  margin-top: 4px;
  transition: background 0.2s;
}

.submit-btn:hover {
  background: #C81E18;
}

.submit-btn:active {
  background: #A81813;
}

.form-tip {
  text-align: center;
  font-size: 12px;
  color: #bbb;
  margin-top: 12px;
}
</style>
