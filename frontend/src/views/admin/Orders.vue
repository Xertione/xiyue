<template>
  <div>
    <div class="toolbar">
      <h2>订单管理</h2>
      <el-select v-model="statusFilter" placeholder="全部状态" clearable @change="onFilter" style="width: 140px">
        <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>
    </div>
    <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="orderNo" label="订单号" width="180" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="tagType(row.status)" size="small">{{ st(row.status).text }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="服务时间" width="170">
        <template #default="{ row }">{{ row.serviceDate }} {{ row.startHour }}:00 / {{ row.durationHours }}h</template>
      </el-table-column>
      <el-table-column label="金额" width="80"><template #default="{ row }">¥{{ row.amount }}</template></el-table-column>
      <el-table-column label="阿姨" width="100"><template #default="{ row }">{{ row.auntName || '-' }}</template></el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 1" size="small" type="primary" @click="openAssign(row)">指派</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="page" :page-size="size" :total="total"
      layout="total, prev, pager, next" @current-change="loadData"
      style="margin-top: 16px; justify-content: flex-end"
    />

    <el-dialog v-model="showAssign" title="指派阿姨" width="400px">
      <el-form label-width="80px">
        <el-form-item label="阿姨ID"><el-input-number v-model="assignAuntId" :min="1" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAssign = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doAssign">确认指派</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminOrderApi } from '@/api'
import { orderStatus as st } from '@/utils/format'

const list = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const size = 10
const total = ref(0)
const statusFilter = ref<number | undefined>(undefined)
const showAssign = ref(false)
const saving = ref(false)
const assignAuntId = ref(1)
const assignOrderId = ref(0)

const statusOptions = [
  { label: '待支付', value: 0 }, { label: '待抢单', value: 1 }, { label: '待服务', value: 2 },
  { label: '服务中', value: 3 }, { label: '待确认', value: 4 }, { label: '待评价', value: 5 },
  { label: '已完成', value: 6 }, { label: '已取消', value: 7 }, { label: '投诉中', value: 8 }
]

function tagType(s: number) {
  return ({ 0: 'warning', 1: 'primary', 2: 'info', 3: 'info', 4: 'info', 5: 'warning', 6: 'success', 7: 'info', 8: 'danger' } as any)[s] || 'info'
}

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const res: any = await adminOrderApi.list({ page: page.value, size, status: statusFilter.value })
    list.value = res.records
    total.value = res.total
  } catch {} finally { loading.value = false }
}

function onFilter() { page.value = 1; loadData() }
function openAssign(row: any) { assignOrderId.value = row.id; assignAuntId.value = 1; showAssign.value = true }

async function doAssign() {
  saving.value = true
  try {
    await adminOrderApi.assign(assignOrderId.value, assignAuntId.value)
    ElMessage.success('指派成功')
    showAssign.value = false
    loadData()
  } catch {} finally { saving.value = false }
}
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.toolbar h2 { font-size: 18px; font-weight: 600; color: #0f172a; }
</style>
