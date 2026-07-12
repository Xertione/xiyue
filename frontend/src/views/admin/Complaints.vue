<template>
  <div>
    <div class="toolbar">
      <h2>投诉处理</h2>
      <el-select v-model="statusFilter" placeholder="全部状态" clearable @change="onFilter" style="width: 140px">
        <el-option label="待处理" value="PENDING" />
        <el-option label="已处理" value="HANDLED" />
      </el-select>
    </div>
    <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="orderId" label="订单ID" width="80" />
      <el-table-column prop="reason" label="投诉原因" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PENDING' ? 'warning' : 'success'" size="small">{{ row.status === 'PENDING' ? '待处理' : '已处理' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="handleRemark" label="处理备注" show-overflow-tooltip />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" size="small" type="primary" @click="openHandle(row)">处理</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="page" :page-size="size" :total="total"
      layout="total, prev, pager, next" @current-change="loadData"
      style="margin-top: 16px; justify-content: flex-end"
    />

    <el-dialog v-model="showHandle" title="处理投诉" width="440px">
      <el-input v-model="handleRemark" type="textarea" rows="3" placeholder="请输入处理备注" />
      <template #footer>
        <el-button @click="showHandle = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doHandle">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { complaintApi } from '@/api'

const list = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const size = 10
const total = ref(0)
const statusFilter = ref<string | undefined>(undefined)
const showHandle = ref(false)
const saving = ref(false)
const handleRemark = ref('')
const handleId = ref(0)

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const res: any = await complaintApi.adminList({ page: page.value, size, status: statusFilter.value })
    list.value = res.records
    total.value = res.total
  } catch {} finally { loading.value = false }
}

function onFilter() { page.value = 1; loadData() }
function openHandle(row: any) { handleId.value = row.id; handleRemark.value = ''; showHandle.value = true }

async function doHandle() {
  if (!handleRemark.value) return ElMessage.warning('请输入处理备注')
  saving.value = true
  try {
    await complaintApi.handle(handleId.value, handleRemark.value)
    ElMessage.success('处理成功')
    showHandle.value = false
    loadData()
  } catch {} finally { saving.value = false }
}
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.toolbar h2 { font-size: 18px; font-weight: 600; color: #0f172a; }
</style>
