<template>
  <div>
    <div class="toolbar">
      <h2>阿姨管理</h2>
      <el-button @click="loadData" :icon="Refresh">刷新</el-button>
    </div>
    <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="姓名" width="100">
        <template #default="{ row }">{{ row.name || '未设置' }}</template>
      </el-table-column>
      <el-table-column prop="price" label="标价" width="80">
        <template #default="{ row }">{{ row.price ? '¥' + row.price : '-' }}</template>
      </el-table-column>
      <el-table-column prop="rating" label="评分" width="80" />
      <el-table-column prop="serviceCount" label="服务次数" width="90" />
      <el-table-column prop="skillTags" label="技能标签" show-overflow-tooltip />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="warning" @click="cycleStatus(row)">改状态</el-button>
          <el-button size="small" type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="page" :page-size="size" :total="total"
      layout="total, prev, pager, next" @current-change="loadData"
      style="margin-top: 16px; justify-content: flex-end"
    />

    <el-dialog v-model="showEdit" title="编辑阿姨" width="480px">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="姓名"><el-input v-model="editForm.name" /></el-form-item>
        <el-form-item label="标价"><el-input-number v-model="editForm.price" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="技能标签"><el-input v-model="editForm.skillTags" placeholder="逗号分隔" /></el-form-item>
        <el-form-item label="个人介绍"><el-input v-model="editForm.intro" type="textarea" rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { adminAuntApi } from '@/api'

const list = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const size = 10
const total = ref(0)
const showEdit = ref(false)
const saving = ref(false)
const editForm = ref<any>({ id: 0, name: '', price: 0, skillTags: '', intro: '' })

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const res: any = await adminAuntApi.list({ page: page.value, size })
    list.value = res.records
    total.value = res.total
  } catch {} finally { loading.value = false }
}

function openEdit(row: any) {
  editForm.value = { id: row.id, name: row.name || '', price: row.price || 0, skillTags: row.skillTags || '', intro: row.intro || '' }
  showEdit.value = true
}

async function saveEdit() {
  saving.value = true
  try {
    await adminAuntApi.update(editForm.value.id, {
      name: editForm.value.name, price: editForm.value.price,
      skillTags: editForm.value.skillTags, intro: editForm.value.intro
    })
    ElMessage.success('保存成功')
    showEdit.value = false
    loadData()
  } catch {} finally { saving.value = false }
}

const statusCycle = ['AVAILABLE', 'OFF_SHELF', 'DISABLED']
const statusLabel: Record<string, string> = { AVAILABLE: '可用', OFF_SHELF: '下架', DISABLED: '禁用' }

async function cycleStatus(row: any) {
  try {
    const { value } = await ElMessageBox.confirm(
      `当前阿姨 ID: ${row.id}，请选择新状态`, '修改管理状态',
      { confirmButtonText: '可用', cancelButtonText: '取消', distinguishCancelAndClose: true }
    )
    await adminAuntApi.status(row.id, 'AVAILABLE')
    ElMessage.success('已设为可用')
    loadData()
  } catch (action: any) {
    if (action === 'cancel') {
      try {
        await ElMessageBox.confirm('设为下架？', '确认', { confirmButtonText: '下架', cancelButtonText: '禁用' })
        await adminAuntApi.status(row.id, 'OFF_SHELF')
        ElMessage.success('已下架')
        loadData()
      } catch {
        await adminAuntApi.status(row.id, 'DISABLED')
        ElMessage.success('已禁用')
        loadData()
      }
    }
  }
}

async function del(row: any) {
  try {
    await ElMessageBox.confirm(`确定逻辑删除阿姨 ${row.name || row.id} 吗？`, '确认删除', { type: 'warning' })
    await adminAuntApi.delete(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch {}
}
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.toolbar h2 { font-size: 18px; font-weight: 600; color: #0f172a; }
</style>
