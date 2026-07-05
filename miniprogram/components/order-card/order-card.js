Component({
  // 加上这一段，开启全局样式支持
  options: {
    addGlobalClass: true 
  },
  
  properties: {
    order: { type: Object, value: {} }
  },
  methods: {
    preventBubble() {},
    onTap() { this.triggerEvent('click', { id: this.properties.order.id }); },
    onAction(e) {
      this.triggerEvent('action', { 
        action: e.currentTarget.dataset.action, 
        order: this.properties.order 
      });
    }
  }
});