// 订单状态映射（skill：反 AI-tell，用清晰中文，无 em-dash）
const ORDER_STATUS_MAP: Record<number, { text: string; color: string; bg: string }> = {
  0: { text: '待支付', color: '#d97706', bg: '#fef3c7' },
  1: { text: '待抢单', color: '#0d9488', bg: '#f0fdfa' },
  2: { text: '待服务', color: '#2563eb', bg: '#eff6ff' },
  3: { text: '服务中', color: '#7c3aed', bg: '#f5f3ff' },
  4: { text: '待确认', color: '#db2777', bg: '#fdf2f8' },
  5: { text: '待评价', color: '#ea580c', bg: '#fff7ed' },
  6: { text: '已完成', color: '#16a34a', bg: '#f0fdf4' },
  7: { text: '已取消', color: '#64748b', bg: '#f1f5f9' },
  8: { text: '投诉中', color: '#dc2626', bg: '#fef2f2' }
}

export function orderStatus(code: number) {
  return ORDER_STATUS_MAP[code] || { text: '未知', color: '#64748b', bg: '#f1f5f9' }
}

export function formatDate(date: string | Date | null): string {
  if (!date) return '-'
  const d = new Date(date)
  if (isNaN(d.getTime())) return '-'
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

export function formatDateTime(date: string | Date | null): string {
  if (!date) return '-'
  const d = new Date(date)
  if (isNaN(d.getTime())) return '-'
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export function maskPhone(phone: string): string {
  if (!phone || phone.length < 11) return phone
  return phone.slice(0, 3) + '****' + phone.slice(7)
}
