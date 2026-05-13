/**
 * 欢迎页轮播素材（LABRES-111）
 * 所有配图、标题、描述、按钮文案仅此一处维护；页面与组件禁止散落硬编码同素材。
 */
import imageSmartBooking from '@/assets/welcome/carousel-smart-booking.svg'
import imageCollaboration from '@/assets/welcome/carousel-collaboration.svg'
import imageInsights from '@/assets/welcome/carousel-insights.svg'

/** @typedef {{ type: 'route', path: string, query?: Record<string, string>, requiresAuth?: boolean }} CarouselRouteAction */
/** @typedef {{ type: 'scroll', elementId: string }} CarouselScrollAction */
/** @typedef {CarouselRouteAction | CarouselScrollAction} CarouselAction */

/**
 * @typedef {object} WelcomeCarouselSlide
 * @property {string} id
 * @property {string} image — 统一由本文件 import 或受控路径，勿在组件内直接写 URL
 * @property {string} title
 * @property {string} description
 * @property {string} buttonText
 * @property {CarouselAction} [action] — 按钮行为；缺省仅展示
 */

/** @type {WelcomeCarouselSlide[]} */
export const welcomeCarouselSlides = [
  {
    id: 'smart-booking',
    image: imageSmartBooking,
    title: '智慧预约，高效利用实验室资源',
    description: '查看空闲时段、使用率与实验室详情，减少排队与冲突，一站完成预约流程。',
    buttonText: '查看实验室列表',
    action: { type: 'scroll', elementId: 'lab-list-section' }
  },
  {
    id: 'my-lab-plan',
    image: imageCollaboration,
    title: '我的预约，进度尽在掌握',
    description: '随时跟进审核状态与使用安排，重要节点不错过。',
    buttonText: '我的预约',
    action: { type: 'route', path: '/my-reservations', requiresAuth: true }
  },
  {
    id: 'notices-reminder',
    image: imageInsights,
    title: '消息通知，重要提醒不错过',
    description: '审核结果、系统公告与实验室动态集中送达，高效处理待办。',
    buttonText: '查看消息',
    action: { type: 'route', path: '/notices', requiresAuth: true }
  }
]

export const welcomeCarouselIntervalMs = 6000
