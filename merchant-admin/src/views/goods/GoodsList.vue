<template>
  <div class="page-container">
    <div class="page-header">
      <h2>商品管理</h2>
      <div class="header-actions">
        <el-button @click="showCategoryDialog = true">管理分类</el-button>
        <el-button type="primary" @click="$router.push('/goods/edit')">新增商品</el-button>
      </div>
    </div>
    <div class="search-bar">
      <el-input v-model="search.name" placeholder="商品名称" clearable style="width: 200px;" @keyup.enter="handleSearch" />
      <el-select v-model="search.status" placeholder="状态" clearable style="width: 120px;" @change="handleSearch">
        <el-option label="上架" :value="1" />
        <el-option label="下架" :value="0" />
      </el-select>
      <el-select v-model="search.category" placeholder="分类" clearable style="width: 140px;" @change="handleSearch">
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.name" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>
    <div class="content-card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column label="图片" width="70">
          <template #default="{ row }">
            <el-image v-if="row.image" :src="row.image" style="width:40px;height:40px" fit="cover" :preview-src-list="[row.image]" />
            <el-avatar v-else :size="40" shape="square">
              <el-icon><Picture /></el-icon>
            </el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="商品名称" min-width="150" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">¥{{ Number(row.price || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="80" />
        <el-table-column prop="sales" label="销量" width="80" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              :type="row.status === 1 ? 'warning' : 'success'"
              link size="small"
              @click="handleToggle(row)"
            >
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-popconfirm title="确定删除该商品吗？" @confirm="handleDelete(row)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && tableData.length === 0" description="暂无商品，点击右上角新增" />
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

    <!-- 分类管理弹窗 -->
    <el-dialog v-model="showCategoryDialog" title="分类管理" width="500px">
      <div style="margin-bottom: 16px;">
        <el-input v-model="newCatName" placeholder="新分类名称" style="width: 260px;" maxlength="20" />
        <el-button type="primary" style="margin-left: 12px;" @click="addCategory" :disabled="!newCatName.trim()">新增</el-button>
      </div>
      <el-table :data="categories" max-height="300" stripe>
        <el-table-column prop="name" label="分类名称" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="startEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除该分类吗？" @confirm="deleteCategory(row.id)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-dialog v-model="showEditDialog" title="编辑分类" width="350px" append-to-body>
        <el-input v-model="editCatName" placeholder="分类名称" maxlength="20" />
        <template #footer>
          <el-button @click="showEditDialog = false">取消</el-button>
          <el-button type="primary" @click="updateCategory">确定</el-button>
        </template>
      </el-dialog>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getGoodsList, deleteGoods, toggleGoodsStatus } from '@/api/goods'
import request from '@/api/request'

const router = useRouter()
const tableData = ref([])
const loading = ref(false)
const categories = ref([])

const search = reactive({ name: '', status: '', category: '' })
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })

// Category management
const showCategoryDialog = ref(false)
const showEditDialog = ref(false)
const newCatName = ref('')
const editCatName = ref('')
const editingCat = ref(null)

async function loadData() {
  loading.value = true
  try {
    const res = await getGoodsList({ ...search, page: pagination.page, pageSize: pagination.pageSize })
    if (res.code === 200) {
      tableData.value = res.data.records
      pagination.total = res.data.total
    }
  } catch (e) { /* ignore */ } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function handleReset() {
  search.name = ''
  search.status = ''
  search.category = ''
  handleSearch()
}

function handleEdit(row) {
  router.push('/goods/edit/' + row.id)
}

async function handleToggle(row) {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    const res = await toggleGoodsStatus(row.id, newStatus)
    if (res.code === 200) {
      ElMessage.success(res.message)
      loadData()
    }
  } catch (e) { /* ignore */ }
}

async function handleDelete(row) {
  try {
    const res = await deleteGoods(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    }
  } catch (e) { /* ignore */ }
}

async function loadCategories() {
  try {
    const res = await request.get('/merchant/goods/categories')
    if (res.code === 200) {
      categories.value = res.data || []
    }
  } catch (e) { /* ignore */ }
}

// Category CRUD
async function addCategory() {
  const name = newCatName.value.trim()
  if (!name) return
  try {
    const res = await request.post('/merchant/goods/categories', { name, sort: 0 })
    if (res.code === 200) {
      ElMessage.success('添加成功')
      newCatName.value = ''
      loadCategories()
    }
  } catch (e) { /* ignore */ }
}

function startEdit(row) {
  editingCat.value = row
  editCatName.value = row.name
  showEditDialog.value = true
}

async function updateCategory() {
  const name = editCatName.value.trim()
  if (!name) return
  try {
    const res = await request.put(`/merchant/goods/categories/${editingCat.value.id}`, { name })
    if (res.code === 200) {
      ElMessage.success('更新成功')
      showEditDialog.value = false
      loadCategories()
    }
  } catch (e) { /* ignore */ }
}

async function deleteCategory(id) {
  try {
    const res = await request.delete(`/merchant/goods/categories/${id}`)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadCategories()
    }
  } catch (e) { /* ignore */ }
}

onMounted(() => {
  loadCategories()
  loadData()
})
</script>

<style scoped>
.header-actions {
  display: flex;
  gap: 12px;
}
</style>
