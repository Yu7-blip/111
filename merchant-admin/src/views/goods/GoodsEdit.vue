<template>
  <div class="page-container">
    <div class="page-header">
      <h2>{{ isEdit ? '编辑商品' : '新增商品' }}</h2>
    </div>
    <div class="content-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 600px;">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="商品分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%;">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" :step="1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="商品描述">
          <el-input v-model="form.desc" type="textarea" :rows="3" placeholder="请输入商品描述" />
        </el-form-item>
        <el-form-item label="富文本描述">
          <RichTextEditor v-model="form.richDesc" :height="300" placeholder="请输入菜品富文本描述（支持加粗/图片/列表等）" />
        </el-form-item>
        <el-form-item label="上架状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="上架" inactive-text="下架" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getGoodsById, createGoods, updateGoods } from '@/api/goods'
import request from '@/api/request'
import RichTextEditor from '@/components/RichTextEditor.vue'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const saving = ref(false)
const categories = ref([])

const isEdit = computed(() => !!route.params.id)

const form = reactive({
  name: '',
  category: '',
  price: 0,
  stock: 0,
  desc: '',
  richDesc: '',
  status: 1
})

const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }]
}

async function loadGoods() {
  if (!isEdit.value) return
  try {
    const res = await getGoodsById(route.params.id)
    if (res.code === 200) {
      const d = res.data
      Object.assign(form, {
        name: d.name,
        category: d.categoryId,
        price: d.price,
        stock: d.stock,
        desc: d.description,
        richDesc: d.richDesc || '',
        status: d.status
      })
    }
  } catch (e) {
    ElMessage.error('商品不存在')
    router.back()
  }
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const api = isEdit.value ? updateGoods : createGoods
    const args = isEdit.value ? [route.params.id, { ...form }] : [{ ...form }]
    const res = await api(...args)
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
      router.push('/goods')
    }
  } catch (e) { /* ignore */ } finally {
    saving.value = false
  }
}

async function loadCategories() {
  try {
    const res = await request.get('/merchant/goods/categories')
    if (res.code === 200) {
      categories.value = res.data || []
    }
  } catch (e) { /* ignore */ }
}

onMounted(() => {
  loadCategories()
  loadGoods()
})
</script>
