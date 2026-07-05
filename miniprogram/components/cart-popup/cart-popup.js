Component({
  // 加上这一段，开启全局样式支持
  options: {
    addGlobalClass: true 
  },
  
  properties: {
    show: { type: Boolean, value: false },
    cartList: { type: Array, value: [] }
  },
  methods: {
    onClose() { this.triggerEvent('close'); },
    onClear() { this.triggerEvent('clear'); },
    onPlus(e) { this.triggerEvent('change', { type: 'plus', food: e.currentTarget.dataset.item }); },
    onMinus(e) { this.triggerEvent('change', { type: 'minus', food: e.currentTarget.dataset.item }); },
    onCountTap(e) { this.triggerEvent('edit-count', { food: e.currentTarget.dataset.item }); },
    onCountConfirm(e) {
      const item = e.currentTarget.dataset.item;
      const newCount = parseInt(e.detail.value) || 0;
      this.triggerEvent('confirm-count', { food: item, count: Math.min(Math.max(newCount, 0), 99) });
    }
  }
});