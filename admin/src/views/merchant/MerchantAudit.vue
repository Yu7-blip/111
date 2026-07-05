<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :inline="true" :model="searchForm" size="default">
        <el-form-item label="商家名称">
          <el-input v-model="searchForm.name" placeholder="请输入商家名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon> 搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-alert title="以下为待审核的商家申请，请仔细核对商家信息后进行审核操作。" type="info" show-icon :closable="false" style="margin-top:10px" />
    </div>

    <div class="content-card">
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="shopNo" label="编号" width="90" />
        <el-table-column prop="name" label="商家名称" width="160" />
        <el-table-column prop="contact" label="联系人" width="100" />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="address" label="地址" min-width="220" />
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="createTime" label="申请时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="success" size="small" @click="handleApprove(row)">通过</el-button>
            <el-button type="danger" size="small" @click="handleRejectClick(row)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无待审核商家" />

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

    <!-- Reject Dialog -->
    <el-dialog v-model="rejectVisible" title="审核拒绝" width="450px">
      <el-form :model="rejectForm">
        <el-form-item label="拒绝原因">
          <el-input v-model="rejectForm.remark" type="textarea" :rows="3" placeholder="请输入拒绝原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject" :loading="auditLoading">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getMerchantList, auditMerchant } from '@/api/merchant'
import { ElMessage, ElMessageBox } from 'element-plus'

const searchForm = reactive({ name: '' })
const loading = ref(false)
const auditLoading = ref(false)
const tableData = ref([])
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })

const rejectVisible = ref(false)
const rejectForm = reactive({ id: null, remark: '' })

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const res = await getMerchantList({
      ...searchForm,
      status: 0,
      page: pagination.page,
      pageSize: pagination.pageSize
    })
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
  searchForm.name = ''
  handleSearch()
}

async function handleApprove(row) {
  try {
    await ElMessageBox.confirm(`确认通过商家「${row.name}」的入驻申请吗？`, '审核确认', {
      confirmButtonText: '确认通过',
      type: 'success'
    })
    await auditMerchant(row.id, 1)
    ElMessage.success('审核通过')
    fetchData()
  } catch { /* cancelled */ }
}

function handleRejectClick(row) {
  rejectForm.id = row.id
  rejectForm.remark = ''
  rejectVisible.value = true
}

async function confirmReject() {
  auditLoading.value = true
  try {
    await auditMerchant(rejectForm.id, 2, rejectForm.remark)
    ElMessage.success('已拒绝')
    rejectVisible.value = false
    fetchData()
  } finally {
    auditLoading.value = false
  }
}
</script>
