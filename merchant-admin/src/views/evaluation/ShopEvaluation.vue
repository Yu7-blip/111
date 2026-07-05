<template>
  <div class="page-container">
    <div class="page-header"><h2>店铺评价</h2></div>
    <div class="content-card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="订单" width="140">
          <template #default="{ row }">{{ row.orderNo }}</template>
        </el-table-column>
        <el-table-column label="用户" width="100">
          <template #default="{ row }">{{ row.userName }}</template>
        </el-table-column>
        <el-table-column label="评分" width="180">
          <template #default="{ row }">
            <el-rate v-model="row.rating" disabled show-score size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'danger' : 'success'" size="small">{{ row.status === 1 ? '已撤销' : '正常' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="160" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.rating <= 2 && row.status !== 1" type="danger" link size="small" @click="handleAppeal(row)">申诉</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无评价" />
      <div style="margin-top:20px;display:flex;justify-content:flex-end">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10,20,50]"
          layout="total,sizes,prev,pager,next"
          @change="fetchData"
        />
      </div>
    </div>

    <el-dialog v-model="appealDialog" title="申诉评价" width="400px">
      <p style="color:#666;margin-bottom:12px">评价内容：{{ appealRow?.content }}</p>
      <el-input v-model="appealReason" type="textarea" :rows="4" placeholder="请说明申诉理由（至少5个字符）" />
      <template #footer>
        <el-button @click="appealDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAppeal">提交申诉</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const loading = ref(false)
const tableData = ref([])
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })
const appealDialog = ref(false)
const appealRow = ref(null)
const appealReason = ref('')

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/merchant/evaluations', { params: { page: pagination.page, pageSize: pagination.pageSize } })
    if (res.code === 200) {
      tableData.value = res.data.records
      pagination.total = res.data.total
    }
  } finally { loading.value = false }
}

function handleAppeal(row) {
  appealRow.value = row
  appealReason.value = ''
  appealDialog.value = true
}

async function submitAppeal() {
  const reason = appealReason.value.trim()
  if (reason.length < 5) return ElMessage.warning('申诉理由至少5个字符')
  try {
    await request.post('/merchant/appeal', { evaluationId: appealRow.value.id, reason })
    ElMessage.success('申诉已提交')
    appealDialog.value = false
  } catch(e) { /* ignore */ }
}
</script>
