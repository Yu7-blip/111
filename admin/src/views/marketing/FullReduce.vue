<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :inline="true" :model="searchForm" size="default">
        <el-form-item label="活动名称">
          <el-input v-model="searchForm.name" placeholder="请输入活动名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:120px">
            <el-option label="进行中" :value="1" />
            <el-option label="未开始" :value="0" />
            <el-option label="已结束" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon> 搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-card">
      <div class="flex-between mb-16">
        <span></span>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增活动
        </el-button>
      </div>

      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="活动名称" width="200" />
        <el-table-column label="满减规则" width="180">
          <template #default="{ row }">
            <span class="rule-text">满{{ row.conditionAmount }}元减{{ row.reduceAmount }}元</span>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column prop="endTime" label="结束时间" width="170" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type" size="small">{{ statusMap[row.status]?.text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              v-if="row.status === 0"
              type="success"
              link
              size="small"
              @click="handleToggleStatus(row, 1)"
            >启用</el-button>
            <el-button
              v-if="row.status === 1"
              type="warning"
              link
              size="small"
              @click="handleToggleStatus(row, 2)"
            >停用</el-button>
            <el-popconfirm title="确定删除该活动吗？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无满减活动" />

      <div class="text-right mt-16">
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

    <!-- Add/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" @close="handleDialogClose">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="活动名称" prop="name">
          <el-input v-model="form.name" placeholder="如：满100减20" />
        </el-form-item>
        <el-form-item label="满减门槛(元)" prop="conditionAmount">
          <el-input-number v-model="form.conditionAmount" :min="1" :precision="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="减免金额(元)" prop="reduceAmount">
          <el-input-number v-model="form.reduceAmount" :min="1" :precision="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择开始时间" style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择结束时间" style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="0">未开始</el-radio>
            <el-radio :value="1">进行中</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getActivityList, createActivity, updateActivity, deleteActivity, updateActivityStatus } from '@/api/marketing'
import { ElMessage } from 'element-plus'

const statusMap = {
  0: { text: '未开始', type: 'info' },
  1: { text: '进行中', type: 'success' },
  2: { text: '已结束', type: 'default' }
}

const searchForm = reactive({ name: '', status: '' })
const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })

const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null, name: '', conditionAmount: 100, reduceAmount: 20,
  startTime: '', endTime: '', status: 0
})
const rules = {
  name: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  conditionAmount: [{ required: true, message: '请输入满减门槛' }],
  reduceAmount: [{ required: true, message: '请输入减免金额' }],
  startTime: [{ required: true, message: '请选择开始时间' }],
  endTime: [{ required: true, message: '请选择结束时间' }]
}

const dialogTitle = computed(() => form.id ? '编辑活动' : '新增活动')

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const res = await getActivityList({ ...searchForm, page: pagination.page, pageSize: pagination.pageSize })
    tableData.value = res.data.records
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchData()
}

function handleReset() {
  Object.assign(searchForm, { name: '', status: '' })
  handleSearch()
}

function handleAdd() {
  Object.assign(form, {
    id: null, name: '', conditionAmount: 100, reduceAmount: 20,
    startTime: '', endTime: '', status: 0
  })
  dialogVisible.value = true
}

function handleEdit(row) {
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

function handleDialogClose() {
  formRef.value?.resetFields()
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const { id, name, conditionAmount, reduceAmount, startTime, endTime, status } = form
    const data = { name, conditionAmount, reduceAmount, startTime, endTime, status }
    if (id) {
      await updateActivity(id, data)
      ElMessage.success('更新成功')
    } else {
      await createActivity(data)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(id) {
  await deleteActivity(id)
  ElMessage.success('删除成功')
  fetchData()
}

async function handleToggleStatus(row, newStatus) {
  await updateActivityStatus(row.id, newStatus)
  ElMessage.success(newStatus === 1 ? '已启用' : '已停用')
  fetchData()
}
</script>

<style scoped lang="scss">
.rule-text {
  color: #E6A23C;
  font-weight: 600;
}
</style>
