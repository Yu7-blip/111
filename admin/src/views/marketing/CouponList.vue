<template>
  <div class="page-container">
    <div class="page-header">
      <h2>平台优惠券</h2>
      <el-button type="primary" @click="handleCreate">创建平台券</el-button>
    </div>
    <div class="content-card">
      <el-table :data="pagedData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column label="满减" width="140">
          <template #default="{ row }">满{{ row.conditionAmount }}减{{ row.reduceAmount }}</template>
        </el-table-column>
        <el-table-column label="剩余/总量" width="100">
          <template #default="{ row }">{{ row.remainCount }}/{{ row.totalCount }}</template>
        </el-table-column>
        <el-table-column label="有效期" width="180">
          <template #default="{ row }">{{ row.startTime || '-' }} ~ {{ row.endTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && tableData.length === 0" description="暂无优惠券" />

      <div class="text-right mt-16" v-if="tableData.length > 0">
        <el-pagination
          v-model:current-page="couponPage"
          v-model:page-size="couponPageSize"
          :total="tableData.length"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑' : '创建'" width="450px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="满减条件" prop="conditionAmount">
          <el-input-number v-model="form.conditionAmount" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="减免金额" prop="reduceAmount">
          <el-input-number v-model="form.reduceAmount" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="总量" prop="totalCount">
          <el-input-number v-model="form.totalCount" :min="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="form.startTime" type="datetime" placeholder="选填" style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="form.endTime" type="datetime" placeholder="选填" style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCouponList, createCoupon, updateCoupon, deleteCoupon } from '@/api/coupon'

const tableData = ref([])
const loading = ref(false)
const couponPage = ref(1)
const couponPageSize = ref(10)

const pagedData = computed(() => {
  const start = (couponPage.value - 1) * couponPageSize.value
  return tableData.value.slice(start, start + couponPageSize.value)
})
const saving = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const editId = ref(null)
const form = reactive({ name: '', conditionAmount: 0, reduceAmount: 0, totalCount: 500, startTime: '', endTime: '', status: 1 })
const rules = {
  name: [{ required: true, message: '请输入名称' }],
  conditionAmount: [{ required: true, type: 'number', min: 0.01, message: '满减条件必须大于0' }],
  reduceAmount: [{ required: true, type: 'number', min: 0.01, message: '减免金额必须大于0' }]
}

onMounted(loadData)

async function loadData() {
  loading.value = true
  try { const res = await getCouponList(); tableData.value = res.data || [] } finally { loading.value = false }
}

function handleCreate() {
  isEdit.value = false; editId.value = null
  Object.assign(form, { name: '', conditionAmount: 0, reduceAmount: 0, totalCount: 500, startTime: '', endTime: '', status: 1 })
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true; editId.value = row.id
  Object.assign(form, { name: row.name, conditionAmount: row.conditionAmount, reduceAmount: row.reduceAmount, totalCount: row.totalCount, startTime: row.startTime || '', endTime: row.endTime || '', status: row.status })
  dialogVisible.value = true
}

async function confirmSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEdit.value) { await updateCoupon(editId.value, { ...form, remainCount: form.totalCount }) }
    else { await createCoupon({ ...form, remainCount: form.totalCount }) }
    ElMessage.success('保存成功'); dialogVisible.value = false; loadData()
  } finally { saving.value = false }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' })
    await deleteCoupon(row.id); ElMessage.success('删除成功'); loadData()
  } catch { /* cancelled */ }
}
</script>
