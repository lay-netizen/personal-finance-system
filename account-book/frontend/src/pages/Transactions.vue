<script setup>
import { ref, onMounted } from 'vue'
import http from '../api/http'
import { ElMessage, ElMessageBox } from 'element-plus'

const rows = ref([])
const selection = ref([])

// 元数据
const categories = ref([])
const accounts = ref([])

// 筛选表单
const query = ref({ keyword:'', type:'', categoryId:'', accountId:'', amountMin:'', amountMax:'', dates:[] })

async function loadMeta(){
  const [cat, acc] = await Promise.all([ http.get('/api/categories'), http.get('/api/accounts') ])
  categories.value = cat.data||[]
  accounts.value = acc.data||[]
}

async function load(){
  const params = {}
  if (query.value.keyword) params.keyword = query.value.keyword
  if (query.value.type) params.type = query.value.type
  if (query.value.categoryId) params.categoryId = query.value.categoryId
  if (query.value.accountId) params.accountId = query.value.accountId
  if (query.value.amountMin) params.amountMin = query.value.amountMin
  if (query.value.amountMax) params.amountMax = query.value.amountMax
  if (query.value.dates && query.value.dates.length===2) {
    params.start = new Date(query.value.dates[0]).toISOString()
    params.end = new Date(query.value.dates[1]).toISOString()
  }
  const { data } = await http.get('/api/transactions/search', { params })
  rows.value = data || []
}

function reset(){
  query.value = { keyword:'', type:'', categoryId:'', accountId:'', amountMin:'', amountMax:'', dates:[] }
  load()
}

onMounted(()=>{ loadMeta(); load() })

function handleSelectionChange(val){ selection.value = val }

async function batchDelete(){
  if (!selection.value.length) return ElMessage.warning('请先选择记录')
  try{
    await ElMessageBox.confirm(`确认删除选中的 ${selection.value.length} 条记录吗？`, '提示', { type:'warning' })
    await http.delete('/api/transactions', { data: selection.value.map(x=>x.id) })
    ElMessage.success('删除成功')
    await load()
  }catch{}
}

// 编辑
const editVisible = ref(false)
const editing = ref(null)
function openEdit(row){ editing.value = JSON.parse(JSON.stringify(row)); editVisible.value = true }
async function submitEdit(){
  const id = editing.value.id
  const payload = {
    amount: Number(editing.value.amount),
    type: editing.value.type,
    note: editing.value.note,
    transactionDate: editing.value.transactionDate,
    account: editing.value.account? { id: editing.value.account.id }: null,
    category: editing.value.category? { id: editing.value.category.id }: null,
  }
  await http.put(`/api/transactions/${id}`, payload)
  ElMessage.success('保存成功')
  editVisible.value = false
  await load()
}

// 导出CSV（保留）
function exportCsv(){}

// 导出选中为Excel
function exportSelectedXlsx(){
  if (!selection.value.length) return ElMessage.warning('请先选择记录')
  const ids = selection.value.map(x=>x.id).join(',')
  const a = document.createElement('a')
  a.href = `/api/transactions/export/xlsx?ids=${encodeURIComponent(ids)}`
  a.download = 'transactions.xlsx'
  a.click()
}

// 导出Excel模板功能已移除

async function importExcel(e){
  const file = e.raw
  const fd = new FormData()
  fd.append('file', file)
  await http.post('/api/transactions/import/xlsx', fd)
  ElMessage.success('导入成功')
  await load()
}
</script>

<template>
  <el-card shadow="hover" style="margin-bottom:12px;">
    <template #header><b>高级筛选</b></template>
    <el-form inline>
      <el-form-item label="关键字"><el-input v-model="query.keyword" placeholder="备注/分类" /></el-form-item>
      <el-form-item label="类型">
        <el-select v-model="query.type" placeholder="全部" style="width:120px;">
          <el-option label="全部" value="" />
          <el-option label="支出" value="EXPENSE" />
          <el-option label="收入" value="INCOME" />
        </el-select>
      </el-form-item>
      <el-form-item label="分类">
        <el-select v-model="query.categoryId" placeholder="全部" style="width:140px;" filterable>
          <el-option label="全部" value="" />
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="账户">
        <el-select v-model="query.accountId" placeholder="全部" style="width:140px;">
          <el-option label="全部" value="" />
          <el-option v-for="a in accounts" :key="a.id" :label="a.name" :value="a.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="金额区间">
        <el-input v-model="query.amountMin" placeholder="最小" style="width:100px;" /> -
        <el-input v-model="query.amountMax" placeholder="最大" style="width:100px;margin-left:6px;" />
      </el-form-item>
      <el-form-item label="日期范围">
        <el-date-picker v-model="query.dates" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" />
      </el-form-item>
      <el-form-item>
        <el-button type="warning" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>
  </el-card>

  <el-card shadow="hover">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <b>流水列表</b>
        <div style="display:flex;gap:8px;">
          <el-button type="warning" plain size="default" @click="exportSelectedXlsx">导出选中</el-button>
          <el-upload :show-file-list="false" accept=".xlsx" :on-change="importExcel">
            <el-button type="warning" plain size="default">导入</el-button>
          </el-upload>
          <el-button type="danger" plain size="default" @click="batchDelete">批量删除</el-button>
        </div>
      </div>
    </template>
    <el-table :data="rows" size="small" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="48" />
      <el-table-column prop="transactionDate" label="时间" width="160" />
      <el-table-column prop="account.name" label="账户" width="120" />
      <el-table-column prop="category.name" label="分类" width="120" />
      <el-table-column prop="type" label="类型" width="100">
        <template #default="{row}">
          <el-tag :type="row.type==='INCOME'?'success':'danger'">{{ row.type==='INCOME'?'收入':'支出' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="amount" label="金额" width="120">
        <template #default="{row}">
          <span :style="{color: row.type==='INCOME'?'#67c23a':'#f56c6c'}">{{ row.amount }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="note" label="备注" />
      <el-table-column fixed="right" label="操作" width="100">
        <template #default="{row}">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="editVisible" title="编辑记录" width="520px">
    <div v-if="editing">
      <el-form label-width="100px">
        <el-form-item label="金额"><el-input v-model="editing.amount" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="editing.type">
            <el-option label="支出" value="EXPENSE" />
            <el-option label="收入" value="INCOME" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间"><el-input v-model="editing.transactionDate" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="editing.note" /></el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="editVisible=false">取消</el-button>
      <el-button type="warning" @click="submitEdit">保存</el-button>
    </template>
  </el-dialog>
</template>

