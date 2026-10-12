<script setup>
import { ref, onMounted, nextTick } from 'vue'
import http from '../api/http'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'

const categoryStats = ref([])
const txs = ref([])
const budget = ref(0)
const used = ref(0)
const warn = ref(false)
const exceed = ref(false)

const budgetCatVisible = ref(false)
const categories = ref([])
const budgetCatForm = ref({ categoryId: null, limitAmount: 0 })

async function load(){
  const { data } = await http.get('/api/transactions/dashboard-summary')
  categoryStats.value = data.categoryStats || []
  const all = await http.get('/api/transactions')
  txs.value = all.data || []
  const bs = await http.get('/api/budgets/monthly/status')
  budget.value = Number(bs.data.limit||0)
  used.value = Number(bs.data.used||0)
  warn.value = !!bs.data.warn
  exceed.value = !!bs.data.exceed
  await nextTick(); renderPie(); renderLine();
}

async function loadCategories(){
  const { data } = await http.get('/api/categories')
  categories.value = data || []
  if (!budgetCatForm.value.categoryId && categories.value.length) budgetCatForm.value.categoryId = categories.value[0].id
}

async function submitCatBudget(){
  await http.post('/api/budgets/category', budgetCatForm.value)
  ElMessage.success('分类预算已保存')
  budgetCatVisible.value = false
}

function renderPie(){
  const dom = document.getElementById('ana-pie'); if(!dom) return
  const chart = echarts.getInstanceByDom(dom) || echarts.init(dom)
  const data = categoryStats.value.map(it=>({value:it.totalAmount, name:`${it.categoryName}(${it.type==='INCOME'?'收入':'支出'})`}))
  chart.setOption({ tooltip:{trigger:'item'}, series:[{type:'pie', radius:'65%', data}] })
}

function renderLine(){
  const dom = document.getElementById('ana-line'); if(!dom) return
  const chart = echarts.getInstanceByDom(dom) || echarts.init(dom)
  const map = new Map()
  txs.value.forEach(t=>{ const d=(t.transactionDate||'').substring(0,10); map.set(d,(map.get(d)||0)+(t.type==='INCOME'?Number(t.amount):-Number(t.amount))) })
  const labels = Array.from(map.keys()).sort()
  const values = labels.map(k=>map.get(k))
  chart.setOption({ xAxis:{type:'category',data:labels}, yAxis:{type:'value'}, series:[{type:'line',data:values}] })
}

onMounted(()=>{ load(); loadCategories(); })

// 预算设置
const dialogMonthly = ref(false)
const monthlyAmount = ref(0)
async function openMonthly(){ dialogMonthly.value=true; monthlyAmount.value = budget.value }
async function saveMonthly(){ await http.post('/api/budgets/monthly',{limitAmount: monthlyAmount.value}); dialogMonthly.value=false; await load() }

const dialogCat = ref(false)
const catForm = ref({ categoryId:null, limitAmount:0 })
async function openCat(){
  const res = await http.get('/api/categories'); categories.value=res.data||[]; dialogCat.value = true
}
async function saveCat(){ await http.post('/api/budgets/category', catForm.value); dialogCat.value=false; await load() }
</script>

<template>
  <el-row :gutter="24">
    <el-col :span="12">
      <el-card shadow="hover" style="margin-bottom:22px;min-height:360px;">
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <b>分类占比（饼图）</b>
            <el-button type="warning" plain @click="budgetCatVisible=true">设置分类预算</el-button>
          </div>
        </template>
        <div id="ana-pie" style="width:100%;height:320px;"></div>
      </el-card>
    </el-col>
    <el-col :span="12">
      <el-card shadow="hover" style="margin-bottom:22px;min-height:360px;">
        <template #header><b>月度趋势（折线）</b></template>
        <div id="ana-line" style="width:100%;height:320px;"></div>
      </el-card>
    </el-col>
  </el-row>
  <el-row>
    <el-col :span="24">
      <el-card shadow="hover">
        <template #header><b>预算进度</b></template>
        <div>预算：{{ budget }}，已用：<span :style="{color: exceed?'#f56c6c':(warn?'#e6a700':'inherit')} ">{{ used }}</span></div>
        <el-progress :percentage="budget? Math.min(100, Math.round(used*100/budget)) : 0" :status="exceed?'exception':(warn?'warning':'success')" />
      </el-card>
    </el-col>
  </el-row>

  <el-dialog v-model="dialogMonthly" title="设置月度预算" width="420px">
    <el-form label-width="100px">
      <el-form-item label="预算金额"><el-input v-model="monthlyAmount" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogMonthly=false">取消</el-button>
      <el-button type="primary" @click="saveMonthly">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="dialogCat" title="设置分类预算" width="420px">
    <el-form label-width="100px">
      <el-form-item label="分类">
        <el-select v-model="catForm.categoryId" filterable>
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="预算金额"><el-input v-model="catForm.limitAmount" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogCat=false">取消</el-button>
      <el-button type="primary" @click="saveCat">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="budgetCatVisible" title="设置分类预算" width="420px">
    <el-form label-width="100px">
      <el-form-item label="分类">
        <el-select v-model="budgetCatForm.categoryId">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="预算上限">
        <el-input v-model="budgetCatForm.limitAmount" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="budgetCatVisible=false">取消</el-button>
      <el-button type="warning" @click="submitCatBudget">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.el-card{ border:1px solid #ffe066; }
</style>

