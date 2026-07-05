Component({
  // 加上这一段，开启全局样式支持
  options: {
    addGlobalClass: true 
  },
  
  properties: {
    task: { type: Object, value: {} },
    mode: { type: String, value: 'lobby' } // lobby: 接单大厅, pickup: 待取餐, delivering: 配送中, display: 仅展示
  },
  methods: {
    preventBubble() {},
    onTap() { this.triggerEvent('click', { id: this.properties.task.id }); },
    onAction(e) {
      this.triggerEvent('action', { 
        action: e.currentTarget.dataset.action, 
        task: this.properties.task 
      });
    }
  }
});