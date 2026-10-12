<script setup>
import { ref, onMounted } from 'vue'
import http from '../api/http'
import { ElMessage } from 'element-plus'

const form = ref({ nickname:'', avatarUrl:'' })
const loading = ref(false)

async function load(){
  try {
    const response = await http.get('/api/profile')
    const data = response.data || {}
    form.value.nickname = data.nickname || ''
    form.value.avatarUrl = data.avatarUrl || ''
  } catch (error) {
    const msg = error.response?.data?.message || error.response?.data?.error || error.message || '未知错误'
    ElMessage.error('加载个人资料失败: ' + msg)
    console.error('加载个人资料错误:', error)
  }
}

async function submit(){
  if (!form.value.nickname || form.value.nickname.trim() === '') {
    ElMessage.warning('请输入昵称')
    return
  }
  loading.value = true
  try {
    const payload = {
      nickname: form.value.nickname ? form.value.nickname.trim() : '',
      avatarUrl: form.value.avatarUrl || ''
    }
    await http.put('/api/profile', payload)
    ElMessage.success('保存成功')
    await load() // 重新加载以确保数据同步
  } catch (error) {
    const msg = error.response?.data?.message || error.response?.data?.error || error.message || '未知错误'
    ElMessage.error('保存失败: ' + msg)
    console.error('保存个人资料错误:', error)
  } finally {
    loading.value = false
  }
}

async function uploadAvatar(file){
  if (!file.raw) return
  const fd = new FormData()
  fd.append('file', file.raw)
  try {
    const { data } = await http.post('/api/auth/avatar', fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    form.value.avatarUrl = data.avatarUrl
    ElMessage.success('头像上传成功，请点击保存按钮保存更改')
  } catch (error) {
    ElMessage.error('头像上传失败: ' + (error.response?.data?.message || error.message))
  }
}

onMounted(load)
</script>

<template>
  <el-row justify="center" style="margin-top:40px;">
    <el-col :span="12">
      <el-card shadow="always">
        <template #header><b>个人资料</b></template>
        <div style="display:flex;gap:20px;align-items:center;">
          <img :src="form.avatarUrl || 'https://via.placeholder.com/96x96?text=Avatar'" style="width:96px;height:96px;border-radius:50%;object-fit:cover;border:2px solid #ffe066;box-shadow:0 2px 10px #ffdb5070"/>
          <el-upload :show-file-list="false" accept="image/*" :on-change="uploadAvatar">
            <el-button type="warning" plain>选择头像</el-button>
          </el-upload>
        </div>
        <el-form label-width="100px" style="margin-top:18px;">
          <el-form-item label="昵称">
            <el-input v-model="form.nickname" placeholder="输入昵称" maxlength="20" show-word-limit />
          </el-form-item>
          <el-form-item>
            <el-button type="warning" @click="submit" :loading="loading" :disabled="!form.nickname || form.nickname.trim() === ''">保存</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>
  </el-row>
</template>
