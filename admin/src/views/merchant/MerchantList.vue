<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :inline="true" :model="searchForm" size="default">
        <el-form-item label="商家名称">
          <el-input v-model="searchForm.name" placeholder="请输入商家名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:120px">
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已拒绝" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon> 搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleCreate">
            <el-icon><Plus /></el-icon> 添加商家
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-card">
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="shopNo" label="编号" width="90" />
        <el-table-column prop="name" label="商家名称" width="160" />
        <el-table-column prop="contact" label="联系人" width="100" />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="address" label="地址" min-width="220" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type" size="small">{{ statusMap[row.status]?.text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="入驻时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleDetail(row)">详情</el-button>
            <el-button type="warning" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              v-if="row.status === 0"
              type="success"
              link
              size="small"
              @click="handleAudit(row, 1)"
            >通过</el-button>
            <el-button
              v-if="row.status === 0"
              type="danger"
              link
              size="small"
              @click="handleAudit(row, 2)"
            >拒绝</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无商家数据" />

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
    <el-dialog v-model="detailVisible" title="商家详情" width="550px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="商家编号">{{ detail.shopNo }}</el-descriptions-item>
        <el-descriptions-item label="商家名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ detail.contact }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.phone }}</el-descriptions-item>
        <el-descriptions-item label="登录用户名">{{ detail.username || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="登录密码">{{ detail.password || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="入驻时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ detail.address }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ detail.description }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusMap[detail.status]?.type" size="small">{{ statusMap[detail.status]?.text }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.auditRemark" label="审核备注">{{ detail.auditRemark }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- Audit Reject Dialog -->
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

    <!-- Edit Dialog -->
    <el-dialog v-model="editVisible" title="编辑商家" width="550px">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="100px">
        <el-form-item label="商家编号">
          <el-input :model-value="editForm.shopNo" disabled placeholder="自动生成" />
        </el-form-item>
        <el-form-item label="商家名称" prop="name">
          <el-input v-model="editForm.name" placeholder="请输入商家名称" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="editForm.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="联系邮箱" prop="email">
          <el-input v-model="editForm.email" placeholder="请输入联系邮箱" />
        </el-form-item>
        <el-form-item label="登录用户名">
          <el-input v-model="editForm.username" placeholder="修改登录用户名" />
        </el-form-item>
        <el-form-item label="登录密码">
          <el-input v-model="editForm.password" placeholder="留空则不修改密码" show-password />
        </el-form-item>
        <el-form-item label="店铺地址" prop="address">
          <el-input v-model="editForm.address" placeholder="请输入店铺地址">
            <template #append>
              <el-button @click="showEditMap = true">🗺️</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="坐标" v-if="editForm.latitude">
          <el-tag type="success" size="small">经度: {{ editForm.longitude }}, 纬度: {{ editForm.latitude }}</el-tag>
          <el-button type="danger" link size="small" @click="editForm.latitude = null; editForm.longitude = null" style="margin-left: 8px;">清除坐标</el-button>
        </el-form-item>
        <el-form-item label="店铺描述" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="2" placeholder="请输入店铺描述" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="editForm.status" style="width:100%">
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已拒绝" :value="2" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmEdit" :loading="editLoading">保存</el-button>
      </template>
    </el-dialog>

    <!-- Create Dialog -->
    <el-dialog v-model="createVisible" title="添加商家" width="550px">
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom:16px">
        <template #title>
          创建后商家状态为<strong>「待审核」</strong>，需到审核页面审批通过后商家方可登录小程序
        </template>
      </el-alert>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="80px">
        <el-form-item label="商家名称" prop="name">
          <el-input v-model="createForm.name" placeholder="请输入商家名称" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="createForm.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="登录用户名" prop="username">
          <el-input v-model="createForm.username" placeholder="审核通过后商家以此账号登录" />
        </el-form-item>
        <el-form-item label="登录密码" prop="password">
          <el-input v-model="createForm.password" placeholder="审核通过后商家以此密码登录" show-password />
        </el-form-item>
        <el-form-item label="联系邮箱">
          <el-input v-model="createForm.email" placeholder="请输入联系邮箱" />
        </el-form-item>
        <el-form-item label="店铺地址" prop="address">
          <el-input v-model="createForm.address" placeholder="请输入店铺地址">
            <template #append>
              <el-button @click="showCreateMap = true">🗺️</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="坐标" v-if="createForm.latitude">
          <el-tag type="success" size="small">{{ createForm.latitude }}, {{ createForm.longitude }}</el-tag>
        </el-form-item>
        <el-form-item label="配送费">
          <el-input-number v-model="createForm.deliveryFee" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="店铺描述">
          <el-input v-model="createForm.description" type="textarea" :rows="2" placeholder="请输入店铺描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCreate" :loading="createLoading">创建并提交审核</el-button>
      </template>
    </el-dialog>

    <!-- 地图选点（创建） -->
    <MapPicker v-model="showCreateMap" @pick="(pos) => { createForm.address = pos.address; createForm.latitude = pos.lat; createForm.longitude = pos.lng }" />

    <!-- 地图选点（编辑） -->
    <MapPicker v-model="showEditMap" :initial-lat="Number(editForm.latitude)" :initial-lng="Number(editForm.longitude)" @pick="(pos) => { editForm.address = pos.address; editForm.latitude = pos.lat; editForm.longitude = pos.lng }" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import MapPicker from '@/components/MapPicker.vue'
import { getMerchantList, getMerchantDetail, auditMerchant, updateMerchant, deleteMerchant, addMerchant } from '@/api/merchant'
import { ElMessage, ElMessageBox } from 'element-plus'

const statusMap = {
  0: { text: '待审核', type: 'warning' },
  1: { text: '已通过', type: 'success' },
  2: { text: '已拒绝', type: 'danger' }
}

const searchForm = reactive({ name: '', status: '' })
const loading = ref(false)
const auditLoading = ref(false)
const tableData = ref([])
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })

const detailVisible = ref(false)
const detail = ref(null)

const rejectVisible = ref(false)
const rejectForm = reactive({ id: null, remark: '' })

const editVisible = ref(false)
const editLoading = ref(false)
const editFormRef = ref(null)
const editForm = reactive({
  id: null, shopNo: '', name: '', phone: '', email: '', address: '', description: '', status: 1,
  username: '', password: '',
  latitude: null, longitude: null
})
const editRules = {
  name: [{ required: true, message: '请输入商家名称', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }]
}

const createVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref(null)
const showCreateMap = ref(false)
const showEditMap = ref(false)

const createForm = reactive({
  name: '', phone: '', username: '', password: '', email: '', address: '', deliveryFee: 0, description: '',
  latitude: null, longitude: null
})
const createRules = {
  name: [{ required: true, message: '请输入商家名称', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  address: [{ required: true, message: '请输入店铺地址', trigger: 'blur' }],
  username: [{ required: true, message: '请输入商家登录用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入商家登录密码', trigger: 'blur' }]
}

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const res = await getMerchantList({ ...searchForm, page: pagination.page, pageSize: pagination.pageSize })
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

async function handleDetail(row) {
  const res = await getMerchantDetail(row.id)
  detail.value = res.data
  detailVisible.value = true
}

function handleAudit(row, status) {
  if (status === 1) {
    auditMerchant(row.id, 1).then(() => {
      ElMessage.success('审核通过')
      fetchData()
    })
  } else {
    rejectForm.id = row.id
    rejectForm.remark = ''
    rejectVisible.value = true
  }
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

async function handleEdit(row) {
  const res = await getMerchantDetail(row.id)
  const d = res.data
  editForm.id = d.id
  editForm.shopNo = d.shopNo || ''
  editForm.name = d.name || ''
  editForm.phone = d.phone || ''
  editForm.email = d.email || ''
  editForm.address = d.address || ''
  editForm.description = d.description || ''
  editForm.status = d.status
  editForm.username = d.username || ''
  editForm.password = ''
  editForm.latitude = d.latitude || null
  editForm.longitude = d.longitude || null
  editVisible.value = true
}

async function confirmEdit() {
  const valid = await editFormRef.value.validate().catch(() => false)
  if (!valid) return
  editLoading.value = true
  try {
    await updateMerchant(editForm.id, {
      name: editForm.name,
      phone: editForm.phone,
      email: editForm.email,
      address: editForm.address,
      description: editForm.description,
      status: editForm.status,
      username: editForm.username,
      password: editForm.password || undefined,
      latitude: editForm.latitude != null ? Number(editForm.latitude) : null,
      longitude: editForm.longitude != null ? Number(editForm.longitude) : null
    })
    ElMessage.success('更新成功')
    editVisible.value = false
    fetchData()
  } finally {
    editLoading.value = false
  }
}

function handleCreate() {
  Object.assign(createForm, {
    name: '', phone: '', username: '', password: '', email: '', address: '', deliveryFee: 0, description: ''
  })
  createVisible.value = true
}

async function confirmCreate() {
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  createLoading.value = true
  try {
    const payload = { ...createForm }
    if (payload.latitude != null) payload.latitude = Number(payload.latitude)
    if (payload.longitude != null) payload.longitude = Number(payload.longitude)
    await addMerchant(payload)
    ElMessage.success('创建成功')
    createVisible.value = false
    fetchData()
  } finally {
    createLoading.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除商家「${row.name}」吗？此操作不可恢复。`, '删除确认', {
      confirmButtonText: '确认删除',
      type: 'warning'
    })
    await deleteMerchant(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch { /* cancelled */ }
}
</script>
