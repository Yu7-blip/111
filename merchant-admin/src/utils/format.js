export function formatPrice(price) {
  if (price == null) return '-'
  return '¥' + Number(price).toFixed(2)
}

export function formatTime(time) {
  if (!time) return '-'
  const d = new Date(time)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

export function orderStatusText(status) {
  const map = {
    0: '待付款',
    1: '已付款',
    2: '配送中',
    3: '已完成',
    4: '已取消',
    5: '退款中',
    6: '已退款',
    7: '商家已拒绝'
  }
  return map[status] || '未知'
}

export function orderStatusTag(status) {
  const map = {
    0: 'warning',
    1: 'primary',
    2: '',
    3: 'success',
    4: 'info',
    5: 'danger',
    6: 'info',
    7: 'danger'
  }
  return map[status] || 'info'
}
