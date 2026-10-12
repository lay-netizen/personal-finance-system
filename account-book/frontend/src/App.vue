<script setup>
import { computed, onMounted } from 'vue'
import authStore from './store/auth.js'

const user = computed(() => authStore.user.value)

async function logout() {
  authStore.logout()
  localStorage.removeItem('user')
  window.location.href = '/login'
}

// 应用启动时检查登录状态
onMounted(async () => {
  const localUser = localStorage.getItem('user')
  if (localUser) {
    await authStore.checkAuth()
  }
})
</script>

<template>
  <el-container style="min-height:100vh;background:#fffbe6;">
    <el-header height="70px" style="background:#ffec99;display:flex;justify-content:space-between;align-items:center">
      <div style="font-size:2rem;color:#7d5200;font-weight:bold">记账本</div>
      <el-menu mode="horizontal" :default-active="$route.name" background-color="#ffec99" active-text-color="#e6a700">
        <el-menu-item index="dashboard" @click="$router.push({name:'dashboard'})">首页</el-menu-item>
        <el-menu-item index="transactions" @click="$router.push({name:'transactions'})">流水</el-menu-item>
        <el-menu-item index="analysis" @click="$router.push({name:'analysis'})">统计分析</el-menu-item>
        <el-menu-item index="accounts" @click="$router.push({name:'accounts'})">账户管理</el-menu-item>
        <el-menu-item index="categories" @click="$router.push({name:'categories'})">分类管理</el-menu-item>
        <el-menu-item index="profile" @click="$router.push({name:'profile'})">个人资料</el-menu-item>
      </el-menu>
      <div>
        <template v-if="user">
          <el-tag type="warning" effect="dark" style="margin-right:8px;">{{ user.username }}</el-tag>
          <el-button type="warning" round plain @click="logout">退出</el-button>
        </template>
        <template v-else>
          <el-button type="warning" round plain @click="$router.push({name:'login'})">登录</el-button>
          <el-button type="warning" round plain @click="$router.push({name:'register'})">注册</el-button>
        </template>
      </div>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<style scoped>
.el-header{ box-shadow: 0 2px 10px 0 #ffdb5070; }
</style>
