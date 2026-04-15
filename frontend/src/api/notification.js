import request from '@/api/request'

// 获取消息列表
export function getNotificationList(params) {
  return request({
    url: '/notifications',
    method: 'get',
    params
  })
}

// 获取未读数量
export function getUnreadCount() {
  return request({
    url: '/notifications/unread-count',
    method: 'get'
  })
}

// 标记已读
export function markAsRead(id) {
  return request({
    url: `/notifications/${id}/read`,
    method: 'put'
  })
}

// 全部已读
export function markAllRead() {
  return request({
    url: '/notifications/read-all',
    method: 'put'
  })
}
