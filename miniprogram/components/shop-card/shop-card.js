Component({
// 加上这一段，开启全局样式支持
options: {
  addGlobalClass: true 
},

  properties: {
    shop: {
      type: Object,
      value: {}
    }
  },
  methods: {
    onTapCard() {
      // 抛出点击事件，携带商家 ID，由父页面决定跳转逻辑
      this.triggerEvent('click', { id: this.properties.shop.id });
    }
  }
});