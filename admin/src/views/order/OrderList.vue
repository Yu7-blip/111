<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="订单列表" name="orders" />
      <el-tab-pane label="退款管理" name="refunds" />
    </el-tabs>

    <!-- Order List Tab -->
    <div v-if="activeTab === 'orders'">
      <div class="search-bar">
        <el-form :inline="true" :model="searchForm" size="default">
          <el-form-item label="订单号">
            <el-input v-model="searchForm.orderNo" placeholder="请输入订单号" clearable />
          </el-form-item>
          <el-form-item label="用户名">
            <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
          </el-form-item>
          <el-form-item label="商家">
            <el-input v-model="searchForm.merchantName" placeholder="请输入商家名" clearable />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:120px">
              <el-option v-for="(text, key) in statusMap" :key="key" :label="text" :value="Number(key)" />
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
          <el-table-column prop="orderNo" label="订单号" width="170" />
          <el-table-column prop="username" label="用户" width="100" />
          <el-table-column prop="merchantName" label="商家" width="140" />
          <el-table-column prop="deliveryName" label="骑手" width="80">
            <template #default="{ row }">{{ row.deliveryName || '-' }}</template>
          </el-table-column>
          <el-table-column prop="amount" label="金额" width="90">
            <template #default="{ row }">
              <span class="money">￥{{ Number(row.amount || 0).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="actualAmount" label="实付金额" width="100">
            <template #default="{ row }">
              <span class="money">￥{{ Number(row.actualAmount || 0).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="deliveryFee" label="配送费" width="90">
            <template #default="{ row }">
              <span class="money">￥{{ Number(row.deliveryFee || 0).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="statusType[row.status]" size="small">{{ statusMap[row.status] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="下单时间" width="170" />
          <el-table-column label="大订单" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.isLargeOrder === 1" type="warning" size="small">大订单</el-tag>
              <span v-else style="color:#C0C4CC;font-size:12px">—</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="handleDetail(row)">详情</el-button>
              <el-button
                v-if="row.isLargeOrder === 1"
                type="warning"
                link
                size="small"
                @click="handleViewChildren(row)"
              >查看拆分</el-button>
              <el-button
                v-if="row.status === 0"
                type="danger"
                link
                size="small"
                @click="handleCancel(row)"
              >取消</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="!loading && tableData.length === 0" description="暂无订单数据" />

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
    </div>

    <!-- Refund Management Tab -->
    <div v-if="activeTab === 'refunds'">
      <div class="content-card">
        <el-table :data="refundTableData" border stripe v-loading="refundLoading">
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="orderNo" label="订单号" width="170" />
          <el-table-column prop="username" label="用户" width="100" />
          <el-table-column prop="userPhone" label="手机号" width="120" />
          <el-table-column prop="shopName" label="商家" width="140" />
          <el-table-column prop="totalPrice" label="金额" width="90">
            <template #default="{ row }">
              <span class="money">￥{{ Number(row.totalPrice || 0).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="actualAmount" label="实付金额" width="100">
            <template #default="{ row }">
              <span class="money">￥{{ Number(row.actualAmount || 0).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="退款原因" width="160" />
          <el-table-column label="退款状态" width="120">
            <template #default="{ row }">
              <el-tag :type="statusType[row.status]" size="small">{{ statusMap[row.status] || statusMap[5] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="updateTime" label="申请时间" width="170" />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button type="success" link size="small" @click="handleApprove(row)">通过</el-button>
              <el-button type="danger" link size="small" @click="handleReject(row)">拒绝</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="!refundLoading && refundTableData.length === 0" description="暂无退款申请" />

        <div class="text-right mt-16">
          <el-pagination
            v-model:current-page="refundPagination.page"
            v-model:page-size="refundPagination.pageSize"
            :total="refundPagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @change="fetchRefundData"
          />
        </div>
      </div>
    </div>

    <!-- Detail Dialog -->
    <el-dialog v-model="detailVisible" title="订单详情" width="600px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="订单号" :span="2">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ detail.username }}</el-descriptions-item>
        <el-descriptions-item label="商家">{{ detail.shopName }}</el-descriptions-item>
        <el-descriptions-item label="骑手">{{ detail.deliveryName || '未分配' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType[detail.status]">{{ statusMap[detail.status] }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="总金额">
          <span class="money">￥{{ Number(detail.totalPrice || 0).toFixed(2) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="实付金额" v-if="detail.actualAmount != null">
          <span class="money">￥{{ Number(detail.actualAmount || 0).toFixed(2) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="配送费">￥{{ Number(detail.deliveryFee || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="包装费">￥{{ Number(detail.packageFee || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="下单时间" :span="2">{{ detail.createTime }}</el-descriptions-item>
      </el-descriptions>

      <!-- 商品明细 -->
      <div v-if="detail.items && detail.items.length" style="margin-top:16px">
        <h4 style="margin-bottom:8px;color:#303133">商品明细</h4>
        <el-table :data="detail.items" size="small" border>
          <el-table-column prop="goodsName" label="商品名称" />
          <el-table-column prop="goodsPrice" label="单价" width="90">
            <template #default="{ row }">￥{{ Number(row.goodsPrice || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="count" label="数量" width="60" />
          <el-table-column label="小计" width="90">
            <template #default="{ row }">
              <span class="money">￥{{ (Number(row.goodsPrice || 0) * (row.count || 0)).toFixed(2) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <!-- 子订单查看 Dialog -->
    <el-dialog v-model="childrenVisible" title="子订单列表" width="800px">
      <el-table :data="childrenData" border stripe v-loading="childrenLoading">
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="goodsDesc" label="商品概要" width="200" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="statusType[row.status]" size="small">{{ statusMap[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deliveryName" label="骑手" width="80">
          <template #default="{ row }">{{ row.deliveryName || '未分配' }}</template>
        </el-table-column>
        <el-table-column label="金额" width="90">
          <template #default="{ row }">
            <span class="money">￥{{ Number(row.actualAmount || row.totalPrice || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
      </el-table>
      <el-empty v-if="!childrenLoading && childrenData.length === 0" description="暂无子订单" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getOrderList, getOrderDetail, cancelOrder, getRefundList, approveRefund, rejectRefund, statusMap, getOrderChildren } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

const statusType = {
  0: 'info', 1: '', 2: 'warning', 3: 'success', 4: 'info', 5: 'warning', 6: 'success', 7: 'danger'
}

const activeTab = ref('orders')
const searchForm = reactive({ orderNo: '', username: '', merchantName: '', status: null })
const loading = ref(false)
const tableData = ref([])
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })

const refundLoading = ref(false)
const refundTableData = ref([])
const refundPagination = reactive({ page: 1, pageSize: 10, total: 0 })

const detailVisible = ref(false)
const childrenVisible = ref(false)
const childrenData = ref([])
const childrenLoading = ref(false)
const detail = ref(null)

onMounted(() => fetchData())

function handleTabChange(tab) {
  if (tab === 'refunds') fetchRefundData()
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getOrderList({ ...searchForm, page: pagination.page, pageSize: pagination.pageSize })
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
  Object.assign(searchForm, { orderNo: '', username: '', merchantName: '', status: null })
  handleSearch()
}

async function handleDetail(row) {
  const res = await getOrderDetail(row.id)
  detail.value = res.data
  detailVisible.value = true
}

async function handleViewChildren(row) {
  childrenVisible.value = true
  childrenLoading.value = true
  try {
    const res = await getOrderChildren(row.id)
    childrenData.value = res.data || []
  } finally {
    childrenLoading.value = false
  }
}

async function handleCancel(row) {
  try {
    await ElMessageBox.confirm(`确认取消订单「${row.orderNo}」吗？`, '取消确认', {
      confirmButtonText: '确认',
      type: 'warning'
    })
    await cancelOrder(row.id)
    ElMessage.success('订单已取消')
    fetchData()
  } catch { /* cancelled */ }
}

async function fetchRefundData() {
  refundLoading.value = true
  try {
    const res = await getRefundList({ page: refundPagination.page, pageSize: refundPagination.pageSize })
    refundTableData.value = res.data.records
    refundPagination.total = res.data.total
  } finally {
    refundLoading.value = false
  }
}

async function handleApprove(row) {
  try {
    await ElMessageBox.confirm(`确认通过「${row.orderNo}」的退款申请吗？`, '退款确认', {
      confirmButtonText: '确认通过',
      type: 'success'
    })
    await approveRefund(row.id)
    ElMessage.success('退款已通过')
    fetchRefundData()
  } catch { /* cancelled */ }
}

async function handleReject(row) {
  try {
    await ElMessageBox.confirm(`确认拒绝「${row.orderNo}」的退款申请吗？`, '拒绝确认', {
      confirmButtonText: '确认拒绝',
      type: 'warning'
    })
    await rejectRefund(row.id)
    ElMessage.success('退款申请已拒绝')
    fetchRefundData()
  } catch { /* cancelled */ }
}
</script>

<style scoped lang="scss">
.money {
  color: #E6A23C;
  font-weight: 600;
  font-family: 'Monaco', 'Menlo', monospace;
}
</style>
