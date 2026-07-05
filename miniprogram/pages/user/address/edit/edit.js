import { http } from '../../../../utils/request';

Page({
  data: {
    isEdit: false,
    editId: null,
    locationText: '',
    hasCoords: false,
    locating: false,
    form: {
      name: '',
      phone: '',
      province: '',
      city: '',
      district: '',
      detail: '',
      latitude: null,
      longitude: null,
      isDefault: 0
    }
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ isEdit: true, editId: options.id });
      wx.setNavigationBarTitle({ title: '编辑地址' });
      this.loadAddress(options.id);
    } else {
      wx.setNavigationBarTitle({ title: '新增地址' });
      // 新增时自动获取当前GPS位置
      this.autoGetCurrentLocation();
    }
  },

  autoGetCurrentLocation() {
    wx.getLocation({
      type: 'gcj02',
      success: (res) => {
        this.setData({
          'form.latitude': res.latitude,
          'form.longitude': res.longitude,
          hasCoords: true,
          locationText: '已自动定位: ' + res.latitude.toFixed(6) + ', ' + res.longitude.toFixed(6)
        });
        // Reverse geocode to fill address fields
        this.reverseGeocodeAddress(res.latitude, res.longitude);
      },
      fail: () => {
        // GPS失败不阻塞，用户仍可手动输入
      }
    });
  },

  // 逆地理编码：坐标 → 省市区+地址
  reverseGeocodeAddress(lat, lng) {
    http.get('/admin/dashboard/reverse-geocode', { lat: lat, lng: lng }).then(res => {
      if (res && res.address) {
        const addrParts = res.address || '';
        // Tencent reverse geocode returns format like "广东省深圳市南山区..."
        const provinceMatch = addrParts.match(/^(.+?省)/);
        const cityMatch = addrParts.match(/省(.+?市)/);
        const districtMatch = addrParts.match(/市(.+?区)/);
        this.setData({
          'form.province': provinceMatch ? provinceMatch[1] : (res.province || ''),
          'form.city': cityMatch ? cityMatch[1] : (res.city || ''),
          'form.district': districtMatch ? districtMatch[1] : (res.district || ''),
          'form.detail': addrParts || ''
        });
      }
    }).catch(() => {});
  },

  loadAddress(id) {
    http.get('/wx/address').then(list => {
      const addr = (list || []).find(a => a.id == id);
      if (addr) {
        const hasCoords = !!(addr.latitude && addr.longitude);
        this.setData({
          hasCoords: hasCoords,
          form: {
            name: addr.name || '',
            phone: addr.phone || '',
            province: addr.province || '',
            city: addr.city || '',
            district: addr.district || '',
            detail: addr.detail || '',
            latitude: addr.latitude || null,
            longitude: addr.longitude || null,
            isDefault: addr.isDefault || 0
          }
        });
        if (hasCoords) {
          this.setData({
            locationText: '✅ 已定位: ' + addr.latitude.toFixed(6) + ', ' + addr.longitude.toFixed(6)
          });
        }
      }
    }).catch(() => {});
  },

  onNameInput(e) { this.setData({ 'form.name': e.detail.value }); },
  onPhoneInput(e) { this.setData({ 'form.phone': e.detail.value }); },
  onProvinceInput(e) { this.setData({ 'form.province': e.detail.value }); },
  onCityInput(e) { this.setData({ 'form.city': e.detail.value }); },
  onDistrictInput(e) { this.setData({ 'form.district': e.detail.value }); },
  onDetailInput(e) { this.setData({ 'form.detail': e.detail.value }); },

  onDefaultChange(e) {
    this.setData({ 'form.isDefault': e.detail.value ? 1 : 0 });
  },

  // GPS 快速定位
  locateMe() {
    this.setData({ locating: true });
    wx.getLocation({
      type: 'gcj02',
      success: (res) => {
        wx.showToast({ title: '定位成功', icon: 'success' });
        this.setData({
          'form.latitude': res.latitude,
          'form.longitude': res.longitude,
          hasCoords: true,
          locationText: '✅ GPS定位: ' + res.latitude.toFixed(6) + ', ' + res.longitude.toFixed(6),
          locating: false
        });
      },
      fail: () => {
        wx.showToast({ title: '定位失败，请授予位置权限', icon: 'none' });
        this.setData({ locating: false });
      }
    });
  },

  // 微信地图选点
  chooseLocation() {
    wx.chooseLocation({
      success: (res) => {
        const addr = res.address || '';
        const name = res.name || '';
        const lat = res.latitude;
        const lng = res.longitude;
        this.setData({
          'form.province': res.province || '',
          'form.city': res.city || '',
          'form.district': res.district || '',
          'form.detail': addr,
          'form.latitude': lat,
          'form.longitude': lng,
          hasCoords: true,
          locationText: '✅ 地图选点: ' + lat.toFixed(6) + ', ' + lng.toFixed(6)
        });
        if (name && !addr.includes(name)) {
          this.setData({ 'form.detail': name + ' ' + addr });
        }
      }
    });
  },

  validate() {
    const { name, phone, detail } = this.data.form;
    if (!name.trim()) {
      wx.showToast({ title: '请输入收货人姓名', icon: 'none' });
      return false;
    }
    if (!phone.trim() || !/^1\d{10}$/.test(phone.trim())) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' });
      return false;
    }
    if (!detail.trim()) {
      wx.showToast({ title: '请输入详细地址', icon: 'none' });
      return false;
    }
    return true;
  },

  save() {
    if (!this.validate()) return;

    // 检查是否有坐标，没有则提示
    if (!this.data.form.latitude) {
      wx.showModal({
        title: '缺少位置信息',
        content: '建议使用地图选点或GPS定位，以便骑手准确找到您的地址。确定继续保存吗？',
        confirmColor: '#ff6b6b',
        success: (res) => {
          if (res.confirm) {
            this.doSave();
          }
        }
      });
    } else {
      this.doSave();
    }
  },

  doSave() {
    wx.showLoading({ title: '保存中' });

    const data = { ...this.data.form };
    const promise = this.data.isEdit
      ? http.put(`/wx/address/${this.data.editId}`, data)
      : http.post('/wx/address', data);

    promise.then(() => {
      wx.hideLoading();
      wx.showToast({ title: '保存成功', icon: 'success' });
      setTimeout(() => wx.navigateBack(), 1000);
    }).catch(() => {
      wx.hideLoading();
    });
  },

  deleteAddress() {
    wx.showModal({
      title: '确认删除',
      content: '确定要删除该地址吗？',
      confirmColor: '#ff6b6b',
      success: (res) => {
        if (res.confirm) {
          http.delete(`/wx/address/${this.data.editId}`).then(() => {
            wx.showToast({ title: '已删除', icon: 'success' });
            setTimeout(() => wx.navigateBack(), 1000);
          }).catch(() => {});
        }
      }
    });
  }
});
