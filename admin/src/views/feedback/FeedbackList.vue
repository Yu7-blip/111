<template>
  <div class="page-container">
    <div class="page-header"><h2>反馈管理</h2></div>

    <div class="search-bar">
      <el-select v-model="search.role" placeholder="角色" clearable style="width:120px" @change="handleSearch">
        <el-option label="用户" value="user" /><el-option label="骑手" value="delivery" /><el-option label="商家" value="merchant" />
      </el-select>
      <el-select v-model="search.type" placeholder="类型" clearable style="width:120px" @change="handleSearch">
        <el-option label="客服" value="support" /><el-option label="投诉" value="complaint" />
        <el-option label="反馈" value="feedback" /><el-option label="申诉" value="appeal" /><el-option label="其他" value="other" />
      </el-select>
      <el-select v-model="search.status" placeholder="状态" clearable style="width:120px" @change="handleSearch">
        <el-option label="待处理" :value="0" /><el-option label="已回复" :value="1" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>

    <div class="content-card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="userName" label="提交人" width="100" show-overflow-tooltip />
        <el-table-column label="角色" width="70">
          <template #default="{ row }">
            <el-tag :type="row.role === 'user' ? '' : row.role === 'delivery' ? 'warning' : 'danger'" size="small">{{ row.role === 'user' ? '用户' : row.role === 'delivery' ? '骑手' : row.role === 'merchant' ? '商家' : row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.type === 'complaint' ? 'danger' : row.type === 'support' ? 'success' : row.type === 'appeal' ? 'warning' : 'info'" size="small">
              {{ { support:'客服', complaint:'投诉', feedback:'反馈', appeal:'申诉', other:'其他' }[row.type] || row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">{{ row.status === 1 ? '已回复' : '待处理' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reply" label="回复" min-width="100" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="160" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" type="primary" link size="small" @click="handleReply(row)">回复</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无反馈" />
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

    <el-dialog v-model="replyDialog" title="回复反馈" width="450px">
      <div style="margin-bottom:12px">
        <text class="font-bold">反馈内容：</text>
        <p style="color:#666;margin-top:8px">{{ replyRow?.content }}</p>
      </div>
      <el-input v-model="replyText" type="textarea" :rows="4" placeholder="输入回复内容" />
      <template #footer>
        <el-button @click="replyDialog = false">取消</el-button>
        <el-button type="primary" @click="submitReply">确定回复</el-button>
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
const search = reactive({ role: '', type: '', status: '' })
const replyDialog = ref(false)
const replyRow = ref(null)
const replyText = ref('')

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/admin/feedback', { params: { ...search, page: pagination.page, pageSize: pagination.pageSize } })
    if (res.code === 200) {
      tableData.value = res.data.records
      pagination.total = res.data.total
    }
  } finally { loading.value = false }
}

function handleSearch() { pagination.page = 1; fetchData() }

function handleReply(row) {
  replyRow.value = row
  replyText.value = ''
  replyDialog.value = true
}

async function submitReply() {
  if (!replyText.value.trim()) return ElMessage.warning('请输入回复内容')
  try {
    await request.put(`/admin/feedback/${replyRow.value.id}/reply`, { reply: replyText.value.trim() })
    ElMessage.success('已回复')
    replyDialog.value = false
    fetchData()
  } catch(e) { /* ignore */ }
}
</script>
