Component({
  // 加上这一段，开启全局样式支持
  options: {
    addGlobalClass: true 
  },
  
  properties: {
    // 允许父页面传入自定义文案和图标
    text: {
      type: String,
      value: '暂无相关数据'
    },
    icon: {
      type: String,
      value: '/images/icons/empty.png'
    }
  }
});