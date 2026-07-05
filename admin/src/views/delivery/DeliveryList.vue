<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :inline="true" :model="searchForm" size="default">
        <el-form-item label="骑手姓名">
          <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="searchForm.phone" placeholder="请输入手机号" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:120px">
            <el-option label="在线" :value="1" />
            <el-option label="忙碌" :value="2" />
            <el-option label="离线" :value="0" />
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
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="idCard" label="身份证号" width="180" />
        <el-table-column prop="vehicle" label="交通工具" width="100" />
        <el-table-column prop="totalDeliveries" label="总单数" width="80" />
        <el-table-column prop="onTimeRate" label="准时率" width="80">
          <template #default="{ row }">{{ row.onTimeRate || 100 }}%</template>
        </el-table-column>
        <el-table-column prop="praiseRate" label="好评率" width="80">
          <template #default="{ row }">{{ row.praiseRate || 100 }}%</template>
        </el-table-column>
        <el-table-column prop="verifyStatus" label="认证" width="85">
          <template #default="{ row }">
            <el-tag :type="verifyStatusMap[row.verifyStatus]?.type" size="small" effect="plain">
              {{ verifyStatusMap[row.verifyStatus]?.text || '未提交' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type" size="small" effect="plain">
              <span class="status-dot" :class="statusMap[row.status]?.dotClass"></span>
              {{ statusMap[row.status]?.text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 0"
              type="success"
              link
              size="small"
              @click="handleStatusChange(row, 1)"
            >上线</el-button>
            <el-button
              v-if="row.status === 1 || row.status === 2"
              type="warning"
              link
              size="small"
              @click="handleStatusChange(row, 0)"
            >下线</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无骑手数据" />

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

    <!-- Detail Dialog -->
    <el-dialog v-model="detailVisible" title="骑手详情" width="550px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="姓名">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detail.phone }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ detail.idCard }}</el-descriptions-item>
        <el-descriptions-item label="交通工具">{{ detail.vehicle }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusMap[detail.status]?.type" size="small">{{ statusMap[detail.status]?.text }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="账户余额">￥{{ Number(detail.balance || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="总配送单">{{ detail.totalDeliveries || 0 }} 单</el-descriptions-item>
        <el-descriptions-item label="准时率">{{ detail.onTimeRate || 100 }}%</el-descriptions-item>
        <el-descriptions-item label="好评率">{{ detail.praiseRate || 100 }}%</el-descriptions-item>
        <el-descriptions-item label="认证状态">
          <el-tag :type="verifyStatusMap[detail.verifyStatus]?.type" size="small">
            {{ verifyStatusMap[detail.verifyStatus]?.text || '未提交' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="真实姓名">{{ detail.realName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="骑手等级">
          <el-tag :type="['info','','warning'][detail.level] || 'info'" size="small">
            {{ ['铜牌骑手','银牌骑手','金牌骑手'][detail.level] || '铜牌骑手' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="更新时间" :span="2">{{ detail.updateTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间" :span="2">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.verifyRemark" label="审核备注" :span="2">{{ detail.verifyRemark }}</el-descriptions-item>
      </el-descriptions>

      <!-- 认证审核操作 -->
      <template #footer v-if="detail && detail.verifyStatus === 1">
        <div style="display:flex;gap:12px;justify-content:flex-end;width:100%">
          <el-input v-model="verifyRemark" placeholder="审核备注（拒绝时建议填写原因）" size="small" style="flex:1" />
          <el-button type="danger" @click="handleVerifyReview(detail.id, 0)">拒绝认证</el-button>
          <el-button type="success" @click="handleVerifyReview(detail.id, 2)">通过认证</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getDeliveryList, updateDeliveryStatus, getDeliveryDetail, reviewDeliveryVerification } from '@/api/delivery'
import { ElMessage, ElMessageBox } from 'element-plus'

const statusMap = {
  0: { text: '离线', type: 'info', dotClass: 'dot-offline' },
  1: { text: '在线', type: 'success', dotClass: 'dot-online' },
  2: { text: '忙碌', type: 'warning', dotClass: 'dot-busy' }
}

const verifyStatusMap = {
  0: { text: '未提交', type: 'info' },
  1: { text: '审核中', type: 'warning' },
  2: { text: '已通过', type: 'success' }
}

const searchForm = reactive({ name: '', phone: '', status: '' })
const loading = ref(false)
const tableData = ref([])
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })

const detailVisible = ref(false)
const detail = ref(null)
const verifyRemark = ref('')

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const res = await getDeliveryList({ ...searchForm, page: pagination.page, pageSize: pagination.pageSize })
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
  Object.assign(searchForm, { name: '', phone: '', status: '' })
  handleSearch()
}

async function handleDetail(row) {
  const res = await getDeliveryDetail(row.id)
  detail.value = res.data
  detailVisible.value = true
}

async function handleStatusChange(row, newStatus) {
  await updateDeliveryStatus(row.id, newStatus)
  const statusText = statusMap[newStatus]?.text || '未知'
  ElMessage.success(`骑手「${row.name}」已${statusText}`)
  fetchData()
}

async function handleVerifyReview(deliveryId, verifyStatus) {
  const actionText = verifyStatus === 2 ? '通过' : '拒绝'
  try {
    await ElMessageBox.confirm(
      `确定${actionText}该骑手的实名认证申请吗？`,
      `认证审核 - ${actionText}`,
      { confirmButtonText: actionText, cancelButtonText: '取消', type: verifyStatus === 2 ? 'success' : 'warning' }
    )
  } catch { return }

  await reviewDeliveryVerification(deliveryId, verifyStatus, verifyRemark.value || '')
  ElMessage.success(`已${actionText}骑手认证`)
  verifyRemark.value = ''
  detailVisible.value = false
  fetchData()
}
</script>

<style scoped lang="scss">
.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 4px;
  vertical-align: middle;

  &.dot-online { background: #67C23A; }
  &.dot-busy { background: #E6A23C; }
  &.dot-offline { background: #909399; }
}
</style>
