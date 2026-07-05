<template>
  <div class="page-container">
    <div class="page-header">
      <h2>订单管理</h2>
    </div>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="订单列表" name="orders" />
      <el-tab-pane label="退款审核" name="refunds" />
    </el-tabs>

    <template v-if="activeTab === 'orders'">
    <div class="search-bar">
      <el-input v-model="search.keyword" placeholder="订单号/客户名/手机号" clearable style="width: 220px;" @keyup.enter="handleSearch" />
      <el-select v-model="search.status" placeholder="订单状态" clearable style="width: 140px;" @change="handleSearch">
        <el-option label="待付款" :value="0" />
        <el-option label="已付款" :value="1" />
        <el-option label="配送中" :value="2" />
        <el-option label="已完成" :value="3" />
        <el-option label="已取消" :value="4" />
        <el-option label="退款中" :value="5" />
        <el-option label="已退款" :value="6" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>
    <div class="content-card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="订单号" width="160" />
        <el-table-column prop="customerName" label="客户" width="100" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <span v-for="(item, i) in row.items" :key="i">
              {{ item.goodsName }} x{{ item.count }}<span v-if="i < row.items.length - 1">, </span>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="100">
          <template #default="{ row }">¥{{ Number(row.total || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="实付" width="100">
          <template #default="{ row }">¥{{ Number(row.actualAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="配送费" width="90">
          <template #default="{ row }">¥{{ Number(row.deliveryFee || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="orderStatusTag(row.status)" size="small">{{ orderStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="170" />
        <el-table-column label="大订单" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.isLargeOrder === 1" type="warning" size="small">大订单</el-tag>
            <span v-else style="color:#C0C4CC;font-size:12px">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleDetail(row)">详情</el-button>
            <el-button
              v-if="row.isLargeOrder === 1"
              type="warning"
              link
              size="small"
              @click="handleViewChildren(row)"
            >查看拆分</el-button>
            <el-popconfirm
              v-if="row.isLargeOrder === 1"
              title="确认拆分为多个子订单？"
              @confirm="handleSplitOrder(row)"
            >
              <template #reference>
                <el-button type="success" link size="small">拆分</el-button>
              </template>
            </el-popconfirm>
            <!-- 已支付 → 接单出餐（配送中） -->
            <el-button
              v-if="row.status === 1"
              type="primary" link size="small"
              @click="handleStatusChange(row, 2)"
            >接单出餐</el-button>
            <!-- 配送中 → 确认送达 -->
            <el-button
              v-if="row.status === 2"
              type="success" link size="small"
              @click="handleStatusChange(row, 3)"
            >确认送达</el-button>
            <!-- 待支付/已支付 → 取消 -->
            <el-popconfirm
              v-if="row.status === 0 || row.status === 1"
              title="确定取消该订单吗？"
              @confirm="() => handleStatusChange(row, '4')"
            >
              <template #reference>
                <el-button type="danger" link size="small">取消订单</el-button>
              </template>
            </el-popconfirm>
            <!-- 已完成/已取消/退款等 → 无操作 -->
            <span v-if="row.status >= 3" style="color:#909399;font-size:12px">—</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && tableData.length === 0" description="暂无订单数据" />
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
    </template>

    <template v-if="activeTab === 'refunds'">
    <div class="content-card">
      <el-table :data="refundData" v-loading="refundLoading" stripe>
        <el-table-column prop="orderNo" label="订单号" width="160" />
        <el-table-column prop="username" label="客户" width="100" />
        <el-table-column prop="userPhone" label="手机号" width="130" />
        <el-table-column label="金额" width="100">
          <template #default="{ row }">¥{{ Number(row.actualAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="退款原因" min-width="200" />
        <el-table-column prop="createTime" label="申请时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-popconfirm title="确定同意退款吗？" @confirm="() => handleRefundApprove(row)">
              <template #reference>
                <el-button type="success" link size="small">同意退款</el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm title="拒绝退款将提交平台裁定，确定？" @confirm="() => handleRefundReject(row)">
              <template #reference>
                <el-button type="danger" link size="small">拒绝并提交平台</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!refundLoading && refundData.length === 0" description="暂无退款申请" />
      <div style="margin-top: 20px; display: flex; justify-content: flex-end;">
        <el-pagination
          v-model:current-page="refundPagination.page"
          v-model:page-size="refundPagination.pageSize"
          :total="refundPagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @change="loadRefunds"
        />
      </div>
    </div>
    </template>

    <!-- Detail Dialog -->
    <el-dialog v-model="detailVisible" title="订单详情" width="600px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="订单号" :span="2">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ detail.customerName }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detail.phone }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="orderStatusTag(detail.status)" size="small">{{ orderStatusText(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="总金额">￥{{ Number(detail.total || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="实付金额">￥{{ Number(detail.actualAmount || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="配送费">￥{{ Number(detail.deliveryFee || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="下单时间" :span="2">{{ detail.createTime }}</el-descriptions-item>
      </el-descriptions>

      <!-- 商品明细 -->
      <div v-if="detail.items && detail.items.length" style="margin-top:16px">
        <h4 style="margin-bottom:8px;color:#303133">商品明细</h4>
        <el-table :data="detail.items" size="small" border>
          <el-table-column prop="goodsName" label="商品名称" />
          <el-table-column prop="goodsPrice" label="单价" width="90">
            <template #default="{ row: item }">￥{{ Number(item.goodsPrice || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="count" label="数量" width="60" />
          <el-table-column label="小计" width="90">
            <template #default="{ row: item }">
              ￥{{ (Number(item.goodsPrice || 0) * (item.count || 0)).toFixed(2) }}
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
            <el-tag :type="orderStatusTag(row.status)" size="small">{{ orderStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deliveryName" label="骑手" width="80">
          <template #default="{ row }">{{ row.deliveryName || '未分配' }}</template>
        </el-table-column>
        <el-table-column label="金额" width="90">
          <template #default="{ row }">¥{{ Number(row.actualAmount || row.totalPrice || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
      </el-table>
      <el-empty v-if="!childrenLoading && childrenData.length === 0" description="暂无子订单" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getOrderList, getOrderDetail, updateOrderStatus, getRefundList, approveRefund, rejectRefund, getOrderChildren, splitLargeOrder } from '@/api/order'
import { orderStatusText, orderStatusTag } from '@/utils/format'

const activeTab = ref('orders')
const tableData = ref([])
const loading = ref(false)
const refundData = ref([])
const refundLoading = ref(false)
const detailVisible = ref(false)
const childrenVisible = ref(false)
const childrenData = ref([])
const childrenLoading = ref(false)
const detail = ref(null)

const search = reactive({ keyword: '', status: null })
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })
const refundPagination = reactive({ page: 1, pageSize: 10, total: 0 })

async function loadData() {
  loading.value = true
  try {
    const res = await getOrderList({ ...search, page: pagination.page, pageSize: pagination.pageSize })
    if (res.code === 200) {
      tableData.value = res.data.records
      pagination.total = res.data.total
    }
  } catch (e) { /* ignore */ } finally {
    loading.value = false
  }
}

async function loadRefunds() {
  refundLoading.value = true
  try {
    const res = await getRefundList({ page: refundPagination.page, pageSize: refundPagination.pageSize })
    if (res.code === 200) {
      refundData.value = res.data.records
      refundPagination.total = res.data.total
    }
  } catch (e) { /* ignore */ } finally {
    refundLoading.value = false
  }
}

function onTabChange(tab) {
  if (tab === 'refunds') loadRefunds()
  else loadData()
}

async function handleRefundApprove(row) {
  try {
    await approveRefund(row.id)
    ElMessage.success('已同意退款')
    loadRefunds()
  } catch (e) { /* ignore */ }
}

async function handleRefundReject(row) {
  try {
    await rejectRefund(row.id)
    ElMessage.success('已拒绝退款，提交平台裁定')
    loadRefunds()
  } catch (e) { /* ignore */ }
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function handleReset() {
  search.keyword = ''
  search.status = null
  handleSearch()
}

async function handleDetail(row) {
  try {
    const res = await getOrderDetail(row.orderId || row.id)
    if (res.code === 200) {
      detail.value = res.data
      detailVisible.value = true
    }
  } catch (e) { /* ignore */ }
}

async function handleViewChildren(row) {
  childrenVisible.value = true
  childrenLoading.value = true
  try {
    const res = await getOrderChildren(row.orderId || row.id)
    childrenData.value = res.data || []
  } finally {
    childrenLoading.value = false
  }
}

async function handleSplitOrder(row) {
  try {
    await splitLargeOrder(row.orderId || row.id)
    ElMessage.success('大订单已拆分为多个子订单')
    loadData()
  } catch { /* ignore */ }
}

async function handleStatusChange(row, status) {
  try {
    const res = await updateOrderStatus(row.orderId, Number(status))
    if (res.code === 200) {
      ElMessage.success(res.message)
      loadData()
    }
  } catch (e) { /* ignore */ }
}

onMounted(loadData)
</script>
