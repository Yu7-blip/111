<template>
  <div class="page-container">
    <div class="page-header"><h2>系统配置</h2></div>
    <div class="content-card">
      <div style="margin-bottom: 16px; display: flex; justify-content: space-between;">
        <el-input v-model="searchKey" placeholder="搜索配置键..." style="width: 260px;" clearable @change="fetchData" />
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon> 添加配置
        </el-button>
      </div>

      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="configKey" label="配置键" width="200" />
        <el-table-column prop="configValue" label="配置值" min-width="200">
          <template #default="{ row }">
            <el-tag>{{ row.configValue }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无配置" />

      <div style="margin-top: 20px; display: flex; justify-content: flex-end;">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @change="fetchData"
        />
      </div>
    </div>

    <!-- Edit Dialog -->
    <el-dialog v-model="editVisible" title="编辑配置" width="500px">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="80px">
        <el-form-item label="配置键" prop="configKey">
          <el-input v-model="editForm.configKey" disabled placeholder="不可修改" />
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input v-model="editForm.configValue" type="textarea" :rows="2" placeholder="请输入配置值" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="editForm.description" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmEdit" :loading="editLoading">保存</el-button>
      </template>
    </el-dialog>

    <!-- Create Dialog -->
    <el-dialog v-model="createVisible" title="添加配置" width="500px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="80px">
        <el-form-item label="配置键" prop="configKey">
          <el-input v-model="createForm.configKey" placeholder="如 site.name" />
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input v-model="createForm.configValue" type="textarea" :rows="2" placeholder="请输入配置值" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCreate" :loading="createLoading">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getConfigList, createConfig, updateConfig, deleteConfig } from '@/api/config'

const loading = ref(false)
const searchKey = ref('')
const tableData = ref([])
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })

const editVisible = ref(false)
const editLoading = ref(false)
const editFormRef = ref(null)
const editForm = reactive({ id: null, configKey: '', configValue: '', description: '' })
const editRules = {
  configValue: [{ required: true, message: '请输入配置值', trigger: 'blur' }]
}

const createVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref(null)
const createForm = reactive({ configKey: '', configValue: '', description: '' })
const createRules = {
  configKey: [{ required: true, message: '请输入配置键', trigger: 'blur' }],
  configValue: [{ required: true, message: '请输入配置值', trigger: 'blur' }]
}

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const res = await getConfigList({ page: pagination.page, pageSize: pagination.pageSize, key: searchKey.value })
    if (res.code === 200) {
      tableData.value = res.data.records
      pagination.total = res.data.total
    }
  } finally { loading.value = false }
}

function handleCreate() {
  Object.assign(createForm, { configKey: '', configValue: '', description: '' })
  createVisible.value = true
}

async function confirmCreate() {
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  createLoading.value = true
  try {
    await createConfig({ ...createForm })
    ElMessage.success('创建成功')
    createVisible.value = false
    fetchData()
  } finally { createLoading.value = false }
}

function handleEdit(row) {
  editForm.id = row.id
  editForm.configKey = row.configKey
  editForm.configValue = row.configValue
  editForm.description = row.description || ''
  editVisible.value = true
}

async function confirmEdit() {
  const valid = await editFormRef.value.validate().catch(() => false)
  if (!valid) return
  editLoading.value = true
  try {
    await updateConfig(editForm.id, { configValue: editForm.configValue, description: editForm.description })
    ElMessage.success('更新成功')
    editVisible.value = false
    fetchData()
  } finally { editLoading.value = false }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除配置「${row.configKey}」吗？`, '删除确认', {
      confirmButtonText: '确认删除',
      type: 'warning'
    })
    await deleteConfig(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch { /* cancelled */ }
}
</script>
