<template>
  <div class="page-container">
    <div class="page-header">
      <h2>优惠券管理</h2>
      <el-button type="primary" @click="handleCreate">创建优惠券</el-button>
    </div>

    <div class="content-card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column label="满减条件" width="120">
          <template #default="{ row }">满{{ row.conditionAmount }}减{{ row.reduceAmount }}</template>
        </el-table-column>
        <el-table-column label="剩余/总量" width="100">
          <template #default="{ row }">{{ row.remainCount }}/{{ row.totalCount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="有效期" width="180">
          <template #default="{ row }">{{ row.startTime || '-' }} ~ {{ row.endTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无优惠券数据" />

      <div style="margin-top: 20px; display: flex; justify-content: flex-end;">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @change="loadData"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑优惠券' : '创建优惠券'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="如：新用户专享券" />
        </el-form-item>
        <el-form-item label="满减条件" prop="conditionAmount">
          <el-input-number v-model="form.conditionAmount" :min="0" :precision="2" style="width:100%" placeholder="满多少可用" />
        </el-form-item>
        <el-form-item label="减免金额" prop="reduceAmount">
          <el-input-number v-model="form.reduceAmount" :min="0" :precision="2" style="width:100%" placeholder="减多少" />
        </el-form-item>
        <el-form-item label="发放总量" prop="totalCount">
          <el-input-number v-model="form.totalCount" :min="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="form.startTime" type="datetime" placeholder="选填" style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="form.endTime" type="datetime" placeholder="选填" style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCouponList, createCoupon, updateCoupon, deleteCoupon } from '@/api/coupon'

const tableData = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const editId = ref(null)
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })

const form = reactive({
  name: '', conditionAmount: 0, reduceAmount: 0, totalCount: 100,
  startTime: '', endTime: '', status: 1
})

const rules = {
  name: [{ required: true, message: '请输入名称' }],
  conditionAmount: [{ required: true, message: '请输入满减条件' }],
  reduceAmount: [{ required: true, message: '请输入减免金额' }],
  totalCount: [{ required: true, message: '请输入总量' }]
}

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const res = await getCouponList({ page: pagination.page, pageSize: pagination.pageSize })
    if (res.code === 200) {
      tableData.value = res.data.records || res.data || []
      pagination.total = res.data.total || 0
    } else {
      tableData.value = res.data || []
    }
  } finally { loading.value = false }
}

function handleCreate() {
  isEdit.value = false
  editId.value = null
  Object.assign(form, { name: '', conditionAmount: 0, reduceAmount: 0, totalCount: 100, startTime: '', endTime: '', status: 1 })
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    name: row.name, conditionAmount: row.conditionAmount, reduceAmount: row.reduceAmount,
    totalCount: row.totalCount, startTime: row.startTime || '', endTime: row.endTime || '', status: row.status
  })
  dialogVisible.value = true
}

async function confirmSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEdit.value) {
      await updateCoupon(editId.value, { ...form, remainCount: form.totalCount })
    } else {
      await createCoupon({ ...form, remainCount: form.totalCount })
    }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    loadData()
  } finally { saving.value = false }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该优惠券吗？', '删除确认', { type: 'warning' })
    await deleteCoupon(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* cancelled */ }
}
</script>
