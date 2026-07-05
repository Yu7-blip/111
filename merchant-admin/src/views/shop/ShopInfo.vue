<template>
  <div class="page-container">
    <div class="page-header">
      <h2>店铺信息</h2>
    </div>
    <div class="content-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 600px;">
        <el-form-item label="店铺编号">
          <el-input :model-value="form.shopNo" disabled placeholder="系统自动分配" />
        </el-form-item>
        <el-form-item label="店铺名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入店铺名称" />
        </el-form-item>
        <el-form-item label="店铺Logo" prop="logo">
          <el-input v-model="form.logo" placeholder="请输入Logo图片URL" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="联系邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-divider content-position="left">🔑 登录账号</el-divider>
        <el-form-item label="登录用户名">
          <el-input v-model="form.username" placeholder="修改登录用户名" />
        </el-form-item>
        <el-form-item label="登录密码">
          <el-input v-model="form.password" placeholder="留空则不修改密码" show-password />
        </el-form-item>
        <el-divider content-position="left">🏪 店铺信息</el-divider>
        <el-form-item label="店铺简介">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入店铺简介" />
        </el-form-item>
        <el-form-item label="店铺公告">
          <el-input v-model="form.notice" type="textarea" :rows="2" placeholder="请输入店铺公告" />
        </el-form-item>
        <el-form-item label="起送价(元)">
          <el-input-number v-model="form.minPrice" :min="0" :precision="2" style="width: 200px;" />
        </el-form-item>
        <el-form-item label="配送费(元)">
          <el-input-number v-model="form.deliveryFee" :min="0" :precision="2" style="width: 200px;" />
        </el-form-item>
        <el-form-item label="评分">
          <span style="color:#E6A23C;font-weight:600">{{ form.rating || 0 }} 分</span>
        </el-form-item>
        <el-form-item label="月销量">
          <span style="font-weight:600">{{ form.sales || 0 }} 单</span>
        </el-form-item>
        <!-- 位置坐标 -->
        <el-divider content-position="left">📍 位置坐标（骑手导航必需）</el-divider>
        <el-form-item label="店铺地址" prop="address">
          <el-input v-model="form.address" placeholder="输入地址后点击「地址解析」自动填坐标，或点击「地图选点」在地图上点选">
            <template #append>
              <el-button @click="geocodeAddress" :loading="geoCoding">地址解析</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="经度 (lng)">
          <el-input v-model="form.longitude" placeholder="如 106.63" style="width: 200px;" />
        </el-form-item>
        <el-form-item label="纬度 (lat)">
          <el-input v-model="form.latitude" placeholder="如 26.65" style="width: 200px;" />
        </el-form-item>
        <el-form-item label=" ">
          <el-button type="warning" @click="showMapPicker = true" :icon="MapLocation">🗺️ 打开地图选点</el-button>
          <el-alert v-if="!form.latitude || !form.longitude"
            title="⚠️ 未设置坐标，骑手将无法导航到您的店铺！请输入地址后点「地址解析」，或点「地图选点」"
            type="warning" :closable="false" show-icon style="margin-top:8px" />
          <el-alert v-else title="✅ 坐标已设置" type="success" :closable="false" show-icon style="margin-top:8px" />
        </el-form-item>

        <!-- 地图选点弹窗 -->
        <MapPicker v-model="showMapPicker" :initial-lat="Number(form.latitude)" :initial-lng="Number(form.longitude)" @pick="onMapPick" />

        <el-form-item label="营业状态">
          <el-switch v-model="form.businessStatus" :active-value="1" :inactive-value="0"
                     active-text="营业中" inactive-text="休息中"
                     @change="handleToggleBusinessStatus" />
        </el-form-item>
        <el-form-item label="营业时间">
          <el-time-select v-model="form.openTime" start="08:00" step="00:30" end="12:00" placeholder="开始时间" style="width: 140px;" />
          <span style="margin: 0 8px;">至</span>
          <el-time-select v-model="form.closeTime" start="12:00" step="00:30" end="23:00" placeholder="结束时间" style="width: 140px;" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
          <el-button @click="loadShopInfo">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { MapLocation } from '@element-plus/icons-vue'
import { getShopInfo, updateShopInfo, toggleBusinessStatus } from '@/api/shop'
import request from '@/api/request'
import MapPicker from '@/components/MapPicker.vue'

const formRef = ref(null)
const saving = ref(false)
const showMapPicker = ref(false)
const geoCoding = ref(false)

const form = reactive({
  shopNo: '',
  name: '',
  logo: '',
  phone: '',
  email: '',
  username: '',
  password: '',
  address: '',
  description: '',
  notice: '',
  minPrice: 0,
  deliveryFee: 0,
  rating: 0,
  sales: 0,
  latitude: null,
  longitude: null,
  openTime: '09:00',
  closeTime: '22:00',
  businessStatus: 1
})

const rules = {
  name: [{ required: true, message: '请输入店铺名称', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入电话', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入有效邮箱', trigger: 'blur' }]
}

// 地址解析：输入地址 → 自动填坐标
async function geocodeAddress() {
  const addr = form.address?.trim()
  if (!addr) {
    ElMessage.warning('请先输入店铺地址')
    return
  }
  geoCoding.value = true
  try {
    const res = await request.get('/admin/dashboard/geocode', { params: { address: addr } })
    if (res.code === 200 && res.data) {
      form.latitude = res.data.lat
      form.longitude = res.data.lng
      ElMessage.success('地址解析成功，坐标已自动填入')
    } else {
      ElMessage.warning('地址解析失败，请使用地图选点手动定位')
    }
  } catch {
    ElMessage.error('地址解析失败')
  } finally {
    geoCoding.value = false
  }
}

// 地图选点回调
function onMapPick(pos) {
  form.latitude = pos.lat
  form.longitude = pos.lng
  if (pos.address && pos.address !== form.address) {
    form.address = pos.address
  }
  ElMessage.success('位置已更新')
}

async function loadShopInfo() {
  try {
    const res = await getShopInfo()
    if (res.code === 200) {
      Object.assign(form, res.data)
    }
  } catch (e) { /* ignore */ }
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const res = await updateShopInfo({ ...form })
    if (res.code === 200) {
      ElMessage.success('保存成功')
      loadShopInfo()
    }
  } catch (e) { /* ignore */ } finally {
    saving.value = false
  }
}

async function handleToggleBusinessStatus(val) {
  try {
    await toggleBusinessStatus(val)
    ElMessage.success(val === 1 ? '已切换为营业中' : '已切换为休息中')
  } catch (e) {
    form.businessStatus = val === 1 ? 0 : 1
  }
}

onMounted(loadShopInfo)
</script>
