<template>
  <el-dialog v-model="visible" title="📍 地图选点" width="750px" :close-on-click-modal="false" @opened="initMap" @closed="destroyMap">
    <!-- Search bar -->
    <div style="display:flex;gap:10px;margin-bottom:12px">
      <el-input v-model="searchText" placeholder="输入地址搜索定位" clearable @keyup.enter="searchAddress" style="flex:1" />
      <el-button type="primary" @click="searchAddress" :loading="searching">搜索</el-button>
    </div>

    <!-- Map container -->
    <div ref="mapContainer" style="width:100%;height:420px;border-radius:8px;border:1px solid #dcdfe6"></div>

    <!-- Info bar -->
    <div style="margin-top:12px;padding:10px 14px;background:#f5f7fa;border-radius:6px;display:flex;align-items:center;gap:16px;flex-wrap:wrap">
      <span style="color:#606266">
        📍 <strong>{{ pickedAddress || '点击地图选点或搜索地址' }}</strong>
      </span>
      <span v-if="pickedLat" style="color:#909399;font-size:13px">
        经度 {{ pickedLng }} &nbsp; 纬度 {{ pickedLat }}
      </span>
      <el-tag v-if="pickedLat" type="success" size="small">已选点</el-tag>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="confirmPick" :disabled="!pickedLat">确认此位置</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  initialLat: { type: Number, default: null },
  initialLng: { type: Number, default: null }
})

const emit = defineEmits(['update:modelValue', 'pick'])

const visible = ref(false)
watch(() => props.modelValue, v => { visible.value = v })
watch(visible, v => { emit('update:modelValue', v) })

const mapContainer = ref(null)
const searchText = ref('')
const searching = ref(false)
const pickedLat = ref(null)
const pickedLng = ref(null)
const pickedAddress = ref('')

let mapInstance = null
let markerInstance = null
let geocoderInstance = null

// Default center: 贵阳市
const DEFAULT_CENTER = { lat: 26.6470, lng: 106.6300 }

function initMap() {
  const TMap = window.TMap
  if (!TMap) {
    ElMessage.error('地图SDK加载失败，请刷新页面重试')
    return
  }

  const centerLat = props.initialLat || DEFAULT_CENTER.lat
  const centerLng = props.initialLng || DEFAULT_CENTER.lng

  mapInstance = new TMap.Map(mapContainer.value, {
    center: new TMap.LatLng(centerLat, centerLng),
    zoom: 16,
    viewMode: '2D'
  })

  // If initial position, show it
  if (props.initialLat && props.initialLng) {
    placeMarker(props.initialLat, props.initialLng)
  }

  // Click to pick
  mapInstance.on('click', async (evt) => {
    const lat = evt.latLng.getLat().toFixed(6)
    const lng = evt.latLng.getLng().toFixed(6)
    pickedLat.value = parseFloat(lat)
    pickedLng.value = parseFloat(lng)

    placeMarker(pickedLat.value, pickedLng.value)

    // Reverse geocode
    try {
      const res = await request.get('/admin/dashboard/reverse-geocode', {
        params: { lat: pickedLat.value, lng: pickedLng.value }
      })
      if (res.code === 200) {
        pickedAddress.value = res.data.address
      } else {
        pickedAddress.value = `${lat}, ${lng}`
      }
    } catch {
      pickedAddress.value = `${lat}, ${lng}`
    }
  })
}

function placeMarker(lat, lng) {
  const TMap = window.TMap
  if (markerInstance) {
    markerInstance.setMap(null)
  }
  markerInstance = new TMap.MultiMarker({
    map: mapInstance,
    styles: {
      default: new TMap.MarkerStyle({
        width: 32,
        height: 42,
        anchor: { x: 16, y: 42 },
        src: 'https://mapapi.qq.com/web/lbs/javascriptGL/demo/img/markerDefault.png'
      })
    },
    geometries: [{ id: 'pick', styleId: 'default', position: new TMap.LatLng(lat, lng) }]
  })
  mapInstance.setCenter(new TMap.LatLng(lat, lng))
}

async function searchAddress() {
  const addr = searchText.value.trim()
  if (!addr) return
  searching.value = true
  try {
    const res = await request.get('/admin/dashboard/geocode', {
      params: { address: addr }
    })
    if (res.code === 200 && res.data) {
      const { lat, lng } = res.data
      pickedLat.value = lat
      pickedLng.value = lng
      pickedAddress.value = addr
      placeMarker(lat, lng)
    } else {
      ElMessage.warning('未找到该地址，请尝试更详细的地址')
    }
  } catch {
    ElMessage.error('地址搜索失败')
  } finally {
    searching.value = false
  }
}

function confirmPick() {
  if (!pickedLat.value) return
  emit('pick', {
    lat: pickedLat.value,
    lng: pickedLng.value,
    address: pickedAddress.value
  })
  visible.value = false
}

function destroyMap() {
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
    markerInstance = null
    pickedLat.value = null
    pickedLng.value = null
    pickedAddress.value = ''
  }
}
</script>
