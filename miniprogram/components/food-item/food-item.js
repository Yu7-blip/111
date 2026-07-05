Component({
  // 加上这一段，开启全局样式支持
  options: {
    addGlobalClass: true 
  },
  
  properties: {
    food: { type: Object, value: {} }
  },
  methods: {
    preventBubble() {},
    onPlus() {
      this.triggerEvent('change', { type: 'plus', food: this.properties.food });
    },
    onMinus() {
      this.triggerEvent('change', { type: 'minus', food: this.properties.food });
    },
    onCountTap() {
      this.triggerEvent('change', { type: 'startEdit', food: this.properties.food });
    },
    onCountConfirm(e) {
      const newCount = parseInt(e.detail.value) || 0;
      this.triggerEvent('change', { type: 'inputCount', food: this.properties.food, count: Math.min(Math.max(newCount, 0), 99) });
    }
  }
});