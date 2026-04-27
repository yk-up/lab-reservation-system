import request from './request'

// ========== 认证 ==========
export const authApi = {
  login: (data) => request.post('/auth/login', data),
  register: (data) => request.post('/auth/register', data)
}

// ========== 实验室 ==========
export const labApi = {
  list: () => request.get('/labs'),
  listAll: () => request.get('/labs/all'),
  usage: () => request.get('/labs/usage'),
  getSlots: (id, date) => request.get(`/labs/${id}/slots`, { params: { date } }),
  add: (data) => request.post('/labs', data),
  update: (id, data) => request.put(`/labs/${id}`, data),
  toggleStatus: (id, status) => request.put(`/labs/${id}/status`, null, { params: { status } })
}

// ========== 预约 ==========
export const reservationApi = {
  create: (data) => request.post('/reservations', data),
  myList: () => request.get('/reservations/my'),
  cancel: (id) => request.put(`/reservations/${id}/cancel`),
  pending: () => request.get('/reservations/pending'),
  adminList: (params) => request.get('/reservations/admin-list', { params }),
  audit: (id, data) => request.put(`/reservations/${id}/audit`, data),
  batchAudit: (data) => request.put('/reservations/audit/batch', data)
}

// ========== 通知 ==========
export const noticeApi = {
  list: () => request.get('/notices'),
  unreadCount: () => request.get('/notices/unread-count'),
  readAll: () => request.put('/notices/read-all'),
  read: (id) => request.put(`/notices/${id}/read`)
}

// ========== 管理员 ==========
export const adminApi = {
  dashboard: () => request.get('/admin/dashboard'),
  labUsage: () => request.get('/admin/lab-usage'),
  reservationTrend: (params) => request.get('/admin/reservation-trend', { params }),
  blacklist: () => request.get('/admin/blacklist'),
  /** @param {{ keyword: string, limit?: number }} params */
  searchUsers: (params) => request.get('/admin/users/search', { params }),
  addBlacklist: (data) => request.post('/admin/blacklist', data),
  removeBlacklist: (id) => request.delete(`/admin/blacklist/${id}`)
}
