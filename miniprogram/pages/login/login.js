const app = getApp();
import { http } from '../../utils/request';

Page({
  data: {
    role: 'user',
    loginMode: 'sms',
    phone: '',
    code: '',
    password: '',
    showPassword: false,
    counting: false,
    countdown: 60,
    loading: false
  },

  switchRole(e) {
    const role = e.currentTarget.dataset.role;
    if (role === this.data.role || this.data.loading) return;
    // 轻触觉反馈
    wx.vibrateShort({ type: 'light' });
    this.setData({ role, code: '', password: '' });
  },

  switchLoginMode(e) {
    const mode = e.currentTarget.dataset.mode;
    if (mode === this.data.loginMode) return;
    wx.vibrateShort({ type: 'light' });
    this.setData({ loginMode: mode, code: '', password: '' });
  },

  onPhoneInput(e) {
    this.setData({ phone: e.detail.value });
  },

  onCodeInput(e) {
    this.setData({ code: e.detail.value });
  },

  onPasswordInput(e) {
    this.setData({ password: e.detail.value });
  },

  toggleShowPassword() {
    wx.vibrateShort({ type: 'light' });
    this.setData({ showPassword: !this.data.showPassword });
  },

  sendCode() {
    const phone = this.data.phone;
    if (!/^1[3-9]\d{9}$/.test(phone)) {
      this.shakeField();
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '发送中', mask: true });

    http.post('/wx/send-code?phone=' + encodeURIComponent(phone), {}).then((code) => {
      wx.hideLoading();
      wx.showToast({ title: '验证码: ' + (code || '123456'), icon: 'success', duration: 3000 });
      this.startTimer();
    }).catch(() => {
      wx.hideLoading();
      wx.showToast({ title: '发送失败，请重试', icon: 'none' });
    });
  },

  startTimer() {
    this.setData({ counting: true, countdown: 60 });
    if (this.timer) clearInterval(this.timer);
    this.timer = setInterval(() => {
      if (this.data.countdown <= 1) {
        clearInterval(this.timer);
        this.timer = null;
        this.setData({ counting: false, countdown: 60 });
      } else {
        this.setData({ countdown: this.data.countdown - 1 });
      }
    }, 1000);
  },

  shakeField() {
    wx.vibrateShort({ type: 'heavy' });
  },

  handleLogin() {
    if (this.data.loading) return;

    const { phone, code, password, loginMode, role } = this.data;
    if (!/^1[3-9]\d{9}$/.test(phone)) {
      this.shakeField();
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' });
      return;
    }

    if (loginMode === 'password') {
      if (!password || password.length < 6) {
        this.shakeField();
        wx.showToast({ title: '密码至少6位', icon: 'none' });
        return;
      }
    } else {
      if (code.length !== 6) {
        this.shakeField();
        wx.showToast({ title: '请输入6位验证码', icon: 'none' });
        return;
      }
    }

    this.setData({ loading: true });
    wx.showLoading({ title: loginMode === 'password' ? '登录中' : '验证中', mask: true });

    const loginData = {
      phone: phone,
      nickname: role === 'user' ? '外卖吃货' : '金牌骑手',
      role: role
    };

    if (loginMode === 'password') {
      loginData.password = password;
    } else {
      loginData.code = code;
    }

    http.post('/wx/login', loginData).then(res => {
      wx.hideLoading();
      this.setData({ loading: false });
      wx.vibrateShort({ type: 'heavy' });
      app.setLogin({
        token: res.token,
        role: res.user.role,
        userInfo: res.user
      });
      wx.showToast({ title: '🎉 登录成功', icon: 'success' });
      setTimeout(() => {
        wx.switchTab({ url: '/pages/index/index' });
      }, 800);
    }).catch(() => {
      wx.hideLoading();
      this.setData({ loading: false });
      this.shakeField();
      wx.showToast({
        title: loginMode === 'password' ? '手机号或密码错误' : '验证码错误',
        icon: 'error'
      });
    });
  },

  onUnload() {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
  }
});
