<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import http from '../api/http'
import * as echarts from 'echarts'
import { ElMessage, ElMessageBox } from 'element-plus'

const core = ref({ totalBalance: 0, monthlyIncome: 0, monthlyExpense: 0, balance: 0 })
const txs = ref([])
const categoryStats = ref([])
const dailySummary = ref([])
const weeklyComparison = ref(null)
const topExpenseCategories = ref([])

const budgetStatus = ref({ limit: 0, used: 0, ratio: 0, warn: false, exceed: false })
const setBudgetVisible = ref(false)
const budgetForm = ref({ limitAmount: 0 })

async function load() {
  const { data } = await http.get('/api/transactions/dashboard-summary')
  core.value = data.coreStats
  txs.value = data.recentTransactions || []
  categoryStats.value = data.categoryStats || []

  // 加载最近7天的每日汇总数据
  const now = new Date()
  const sevenDaysAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
  const { data: dailyData } = await http.get('/api/transactions/daily-summary', {
    params: {
      start: sevenDaysAgo.toISOString(),
      end: now.toISOString()
    }
  })
  dailySummary.value = dailyData || []

  // 加载每周对比数据
  const { data: weeklyData } = await http.get('/api/transactions/weekly-comparison')
  weeklyComparison.value = weeklyData

  // 加载Top5支出分类
  const monthStart = new Date(now.getFullYear(), now.getMonth(), 1)
  const { data: topData } = await http.get('/api/transactions/top-expense-categories', {
    params: {
      start: monthStart.toISOString(),
      end: now.toISOString(),
      limit: 5
    }
  })
  topExpenseCategories.value = topData || []

  // 加载预算状态
  try {
    const { data: budgetData } = await http.get('/api/budgets/monthly/status')
    budgetStatus.value = budgetData
  } catch (e) {
    // 如果预算服务不可用，忽略
  }

  await nextTick()
  renderCharts()
}

async function loadBudget() {
  try {
    const { data } = await http.get('/api/budgets/monthly/status')
    budgetStatus.value = data
  } catch (e) {}
}

async function submitBudget() {
  await http.post('/api/budgets/monthly', { limitAmount: budgetForm.value.limitAmount })
  setBudgetVisible.value = false
  await loadBudget()
}

function renderCharts() {
  renderPie()
  renderTrendLine()
  renderWeeklyComparison()
  renderRoseChart()
}

function renderPie() {
  const dom = document.getElementById('dash-pie')
  if (!dom) return
  const chart = echarts.getInstanceByDom(dom) || echarts.init(dom)
  const data = categoryStats.value.map(it => ({
    value: it.totalAmount,
    name: `${it.categoryName}(${it.type === 'INCOME' ? '收入' : '支出'})`
  }))
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    series: [{
      type: 'pie',
      radius: '60%',
      data,
      label: { formatter: '{b}: {d}%' }
    }]
  })
}

function renderTrendLine() {
  const dom = document.getElementById('dash-trend-line')
  if (!dom) return
  const chart = echarts.getInstanceByDom(dom) || echarts.init(dom)
  const dates = dailySummary.value.map(item => {
    const d = new Date(item.date)
    return `${d.getMonth() + 1}/${d.getDate()}`
  })
  const incomes = dailySummary.value.map(item => Number(item.income))
  const expenses = dailySummary.value.map(item => Number(item.expense))
  chart.setOption({
    title: { text: '近7天收支趋势', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis' },
    legend: { data: ['收入', '支出'], bottom: 0, textStyle: { fontSize: 12 } },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value' },
    series: [
      { name: '收入', type: 'line', data: incomes, itemStyle: { color: '#67C23A' }, smooth: true },
      { name: '支出', type: 'line', data: expenses, itemStyle: { color: '#F56C6C' }, smooth: true }
    ],
    grid: { top: 40, bottom: 50, left: 50, right: 20 }
  })
}

function renderWeeklyComparison() {
  const dom = document.getElementById('dash-weekly-comparison')
  if (!dom || !weeklyComparison.value) return
  const chart = echarts.getInstanceByDom(dom) || echarts.init(dom)
  const thisWeek = weeklyComparison.value.thisWeek || { income: 0, expense: 0 }
  const lastWeek = weeklyComparison.value.lastWeek || { income: 0, expense: 0 }
  chart.setOption({
    title: { text: '近两周收支对比', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['上周', '本周'], bottom: 0, textStyle: { fontSize: 12 } },
    xAxis: { type: 'category', data: ['收入', '支出'] },
    yAxis: { type: 'value' },
    series: [
      {
        name: '上周',
        type: 'bar',
        data: [Number(lastWeek.income), Number(lastWeek.expense)],
        itemStyle: { color: '#E6A23C' }
      },
      {
        name: '本周',
        type: 'bar',
        data: [Number(thisWeek.income), Number(thisWeek.expense)],
        itemStyle: { color: '#409EFF' }
      }
    ],
    grid: { top: 40, bottom: 50, left: 50, right: 20 }
  })
}

function renderRoseChart() {
  const dom = document.getElementById('dash-rose-chart')
  if (!dom) return
  const chart = echarts.getInstanceByDom(dom) || echarts.init(dom)
  const data = topExpenseCategories.value.map(it => ({
    value: it.totalAmount,
    name: it.categoryName
  }))
  chart.setOption({
    title: { text: '支出分类Top5', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    series: [{
      name: '支出分类',
      type: 'pie',
      radius: ['20%', '70%'],
      center: ['50%', '50%'],
      roseType: 'area',
      itemStyle: { borderRadius: 5 },
      label: { show: true, formatter: '{b}\n¥{c}' },
      data
    }],
    grid: { top: 40, bottom: 20 }
  })
}

onMounted(() => { load(); loadBudget(); })
watch(() => categoryStats.value, renderCharts, { deep: true })

// 快速操作弹窗
const dialogVisible = ref(false)
const actionType = ref('')
const formTx = ref({ accountId: null, amount: null, categoryId: null, type: 'EXPENSE', note: '' })
const formAcc = ref({ name: '', balance: 0 })
const formCat = ref({ name: '', type: 'EXPENSE' })
const accounts = ref([])
const categories = ref([])

async function loadMeta() {
  const [accRes, catRes] = await Promise.all([
    http.get('/api/accounts'),
    http.get('/api/categories')
  ])
  accounts.value = accRes.data || []
  categories.value = catRes.data || []
  if (!formTx.value.accountId && accounts.value.length) {
    formTx.value.accountId = accounts.value[0].id
  }
}

function openAction(type) {
  actionType.value = type
  dialogVisible.value = true
}

function closeAction() {
  dialogVisible.value = false
}

async function submit() {
  try {
    if (actionType.value === '记一笔') {
      await http.post('/api/transactions', {
        account: { id: formTx.value.accountId },
        category: { id: formTx.value.categoryId },
        amount: Number(formTx.value.amount),
        type: formTx.value.type,
        note: formTx.value.note
      })
      ElMessage.success('记账成功')
    } else if (actionType.value === '新增账户') {
      await http.post('/api/accounts', {
        name: formAcc.value.name,
        balance: Number(formAcc.value.balance),
        type: 'CASH'
      })
      ElMessage.success('新增账户成功')
      await loadMeta()
    } else if (actionType.value === '新增分类') {
      await http.post('/api/categories', {
        name: formCat.value.name,
        type: formCat.value.type
      })
      ElMessage.success('新增分类成功')
      await loadMeta()
    }
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error('操作失败: ' + (error.response?.data?.message || error.message))
  }
}

onMounted(loadMeta)
</script>

<template>
  <!-- 核心概览卡片 -->
  <el-row :gutter="20" style="margin-bottom: 20px;">
    <el-col :span="6">
      <el-card shadow="hover" class="stat-card" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
        <div class="stat-content">
          <div class="stat-label">总资产</div>
          <div class="stat-value" style="color: #fff;">¥{{ (core.totalBalance || 0).toFixed(2) }}</div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="6">
      <el-card shadow="hover" class="stat-card" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
        <div class="stat-content">
          <div class="stat-label">本月收入</div>
          <div class="stat-value" style="color: #fff;">¥{{ (core.monthlyIncome || 0).toFixed(2) }}</div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="6">
      <el-card shadow="hover" class="stat-card" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);">
        <div class="stat-content">
          <div class="stat-label">本月支出</div>
          <div class="stat-value" style="color: #fff;">¥{{ (core.monthlyExpense || 0).toFixed(2) }}</div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="6">
      <el-card shadow="hover" class="stat-card" :style="{ background: core.balance >= 0 ? 'linear-gradient(135deg, #84fab0 0%, #8fd3f4 100%)' : 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)' }">
        <div class="stat-content">
          <div class="stat-label">本月结余</div>
          <div class="stat-value" style="color: #fff;">¥{{ (core.balance || 0).toFixed(2) }}</div>
        </div>
      </el-card>
    </el-col>
  </el-row>

  <!-- 预算进度条 -->
  <el-card shadow="hover" style="margin-bottom: 20px;" v-if="budgetStatus.limit > 0">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <b>月度预算</b>
        <el-button type="warning" plain size="small" @click="setBudgetVisible = true">设置预算</el-button>
      </div>
    </template>
    <el-progress
      :percentage="Math.min(100, Math.round((budgetStatus.used || 0) / (budgetStatus.limit || 1) * 100))"
      :status="budgetStatus.exceed ? 'exception' : (budgetStatus.warn ? 'warning' : 'success')"
      :stroke-width="20"
    >
      <template #default="{ percentage }">
        <span style="font-size: 14px;">已用: ¥{{ (budgetStatus.used || 0).toFixed(2) }} / ¥{{ (budgetStatus.limit || 0).toFixed(2) }} ({{ percentage }}%)</span>
      </template>
    </el-progress>
  </el-card>

  <!-- 快速操作和近期流水 -->
  <el-row :gutter="20" style="margin-bottom: 20px;">
    <el-col :span="12">
      <el-card shadow="hover">
        <template #header>
          <b>快速操作</b>
        </template>
        <div class="quick-actions">
          <el-button type="primary" size="large" @click="openAction('记一笔')" style="width: 30%;">记一笔</el-button>
          <el-button type="success" size="large" @click="openAction('新增账户')" style="width: 30%;">新增账户</el-button>
          <el-button type="warning" size="large" @click="openAction('新增分类')" style="width: 30%;">新增分类</el-button>
        </div>
      </el-card>
    </el-col>
    <el-col :span="12">
      <el-card shadow="hover">
        <template #header><b>近期流水</b></template>
        <el-table :data="txs" size="small" v-if="txs.length" :show-header="false">
          <el-table-column prop="transactionDate" label="时间" width="140">
            <template #default="{ row }">
              {{ new Date(row.transactionDate).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) }}
            </template>
          </el-table-column>
          <el-table-column prop="category.name" label="分类" width="100" />
          <el-table-column prop="amount" label="金额" align="right">
            <template #default="{ row }">
              <span :style="{ color: row.type === 'INCOME' ? '#67C23A' : '#F56C6C', fontWeight: 'bold' }">
                {{ row.type === 'INCOME' ? '+' : '-' }}¥{{ Number(row.amount).toFixed(2) }}
              </span>
            </template>
          </el-table-column>
        </el-table>
        <div v-else style="text-align: center; color: #999; padding: 20px;">暂无流水</div>
      </el-card>
    </el-col>
  </el-row>

  <!-- 图表区域：第一行 -->
  <el-row :gutter="20" style="margin-bottom: 20px;">
    <el-col :span="12">
      <el-card shadow="hover">
        <template #header><b>收支分类占比</b></template>
        <div id="dash-pie" style="width: 100%; height: 300px;"></div>
      </el-card>
    </el-col>
    <el-col :span="12">
      <el-card shadow="hover">
        <template #header><b>支出分类Top5</b></template>
        <div id="dash-rose-chart" style="width: 100%; height: 300px;"></div>
      </el-card>
    </el-col>
  </el-row>

  <!-- 图表区域：第二行 -->
  <el-row :gutter="20" style="margin-bottom: 20px;">
    <el-col :span="12">
      <el-card shadow="hover">
        <template #header><b>近7天收支趋势</b></template>
        <div id="dash-trend-line" style="width: 100%; height: 300px;"></div>
      </el-card>
    </el-col>
    <el-col :span="12">
      <el-card shadow="hover">
        <template #header><b>近两周收支对比</b></template>
        <div id="dash-weekly-comparison" style="width: 100%; height: 300px;"></div>
      </el-card>
    </el-col>
  </el-row>

  <!-- 快速操作对话框 -->
  <el-dialog v-model="dialogVisible" :title="actionType" width="520px" @close="closeAction">
    <template v-if="actionType === '记一笔'">
      <el-form label-width="100px">
        <el-form-item label="金额">
          <el-input-number v-model="formTx.amount" :min="0.01" :precision="2" :step="10" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="分类类型">
          <el-radio-group v-model="formTx.type">
            <el-radio-button label="EXPENSE">支出</el-radio-button>
            <el-radio-button label="INCOME">收入</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="formTx.categoryId" filterable placeholder="选择分类" style="width: 100%;">
            <el-option
              v-for="c in categories.filter(c => c.type === formTx.type)"
              :key="c.id"
              :label="c.name"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="账户">
          <el-select v-model="formTx.accountId" filterable placeholder="选择账户" style="width: 100%;">
            <el-option v-for="a in accounts" :key="a.id" :label="a.name" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formTx.note" placeholder="可选" />
        </el-form-item>
      </el-form>
    </template>
    <template v-else-if="actionType === '新增账户'">
      <el-form label-width="100px">
        <el-form-item label="账户名称">
          <el-input v-model="formAcc.name" placeholder="输入名称" />
        </el-form-item>
        <el-form-item label="初始余额">
          <el-input-number v-model="formAcc.balance" :min="0" :precision="2" :step="10" style="width: 100%;" />
        </el-form-item>
      </el-form>
    </template>
    <template v-else>
      <el-form label-width="100px">
        <el-form-item label="分类名称">
          <el-input v-model="formCat.name" placeholder="输入名称" />
        </el-form-item>
        <el-form-item label="分类类型">
          <el-radio-group v-model="formCat.type">
            <el-radio-button label="EXPENSE">支出</el-radio-button>
            <el-radio-button label="INCOME">收入</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
    </template>
    <template #footer>
      <el-button @click="closeAction">取消</el-button>
      <el-button type="warning" @click="submit">确认</el-button>
    </template>
  </el-dialog>

  <!-- 预算设置对话框 -->
  <el-dialog v-model="setBudgetVisible" title="设置月度预算" width="420px">
    <el-form label-width="100px">
      <el-form-item label="预算上限">
        <el-input-number v-model="budgetForm.limitAmount" :min="0" :precision="2" :step="100" style="width: 100%;" placeholder="本月总支出上限" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="setBudgetVisible = false">取消</el-button>
      <el-button type="warning" @click="submitBudget">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.stat-card {
  border: none;
  border-radius: 12px;
  overflow: hidden;
}

.stat-content {
  padding: 20px;
  text-align: center;
}

.stat-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 10px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
}

.quick-actions {
  display: flex;
  justify-content: space-around;
  gap: 10px;
  padding: 10px 0;
}

.el-card {
  border: 1px solid #ffe066;
  border-radius: 8px;
}
</style>
