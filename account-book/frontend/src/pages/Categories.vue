<script setup>
import { ref, onMounted } from 'vue'
import http from '../api/http'
import { ElMessage, ElMessageBox } from 'element-plus'

const rows = ref([])
const form = ref({ name:'', type:'EXPENSE' })

async function load(){
  const { data } = await http.get('/api/categories')
  rows.value = data || []
}

async function submit(){
  await http.post('/api/categories', form.value)
  form.value = { name:'', type:'EXPENSE' }
  await load()
}

async function remove(id){
  try{
    await ElMessageBox.confirm('确认删除该分类吗？','提示',{type:'warning'})
    await http.delete(`/api/categories/${id}`)
    ElMessage.success('删除成功')
    await load()
  }catch{}
}

onMounted(load)
</script>

<template>
  <el-row :gutter="24">
    <el-col :span="12">
      <el-card shadow="hover">
        <template #header><b>分类列表</b></template>
        <el-table :data="rows" size="small">
          <el-table-column prop="name" label="分类名称" />
          <el-table-column prop="type" label="类型" />
          <el-table-column label="操作" width="100">
            <template #default="{row}">
              <el-button link type="danger" @click="remove(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-col>
    <el-col :span="12">
      <el-card shadow="hover">
        <template #header><b>新增分类</b></template>
        <el-form label-width="100px">
          <el-form-item label="分类名称"><el-input v-model="form.name" /></el-form-item>
          <el-form-item label="类型">
            <el-select v-model="form.type">
              <el-option label="支出" value="EXPENSE" />
              <el-option label="收入" value="INCOME" />
            </el-select>
          </el-form-item>
          <el-form-item><el-button type="warning" @click="submit">保存</el-button></el-form-item>
        </el-form>
      </el-card>
    </el-col>
  </el-row>
</template>
