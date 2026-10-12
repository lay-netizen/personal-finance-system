<script setup>
import { ref, onMounted } from 'vue'
import http from '../api/http'
import { ElMessage } from 'element-plus'
import authStore from '../store/auth.js'

const form = ref({ username:'', password:'', captchaId:'', captchaCode:'' })
const captchaBase64 = ref('')

async function loadCaptcha(){
  const { data } = await http.get('/api/auth/captcha')
  form.value.captchaId = data.captchaId
  captchaBase64.value = data.imageBase64
}

onMounted(loadCaptcha)

async function submit(){
  try {
    const { data } = await http.post('/api/auth/login', form.value)
    localStorage.setItem('user', JSON.stringify(data))
    authStore.login(data)
    ElMessage.success('登录成功')
    
    // 获取重定向路径
    const redirect = new URLSearchParams(window.location.search).get('redirect') || '/'
    window.location.href = redirect
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '登录失败')
    loadCaptcha()
  }
}
</script>

<template>
  <el-row justify="center" style="margin-top:80px;">
    <el-col :span="8">
      <el-card shadow="hover">
        <template #header><b>登录</b></template>
        <el-form label-width="90px">
          <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
          <el-form-item label="密码"><el-input v-model="form.password" type="password" /></el-form-item>
          <el-form-item label="验证码">
            <div style="display:flex;gap:8px;align-items:center;">
              <el-input v-model="form.captchaCode" placeholder="输入验证码" />
              <img :src="captchaBase64" style="height:36px;border:1px solid #ddd;cursor:pointer;" @click="loadCaptcha" />
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="warning" @click="submit">登录</el-button>
            <el-button link @click="$router.push({name:'register'})">去注册</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>
  </el-row>
</template>
