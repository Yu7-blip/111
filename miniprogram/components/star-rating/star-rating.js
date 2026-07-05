Component({
  // 加上这一段，开启全局样式支持
  options: {
    addGlobalClass: true 
  },
  
  properties: {
    rating: {
      type: Number,
      value: 0
    },
    size: {
      type: Number,
      value: 30 // 默认大小 30rpx
    },
    readonly: {
      type: Boolean,
      value: true // 默认只读（列表页展示用），评价页需设为 false
    }
  },
  methods: {
    onTapStar(e) {
      if (this.properties.readonly) return;
      const val = e.currentTarget.dataset.val;
      // 触发自定义事件，通知父页面分数改变
      this.triggerEvent('change', { value: val });
    }
  }
});