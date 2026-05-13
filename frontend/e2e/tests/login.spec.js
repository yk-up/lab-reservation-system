import { expect, test } from '@playwright/test'
import { LoginPage } from '../pages/login.page'

test.describe('登录模块自动化测试', () => {
  test('正常用户登录成功后跳转到实验室列表', async ({ page }) => {
    const loginPage = new LoginPage(page)
    await loginPage.mockLoginSuccess(0)
    await loginPage.goto()
    await loginPage.login('student001', '123456')

    await expect(page).toHaveURL(/\/labs$/)
    await expect
      .poll(async () => page.evaluate(() => localStorage.getItem('token')))
      .toBe('mock-jwt-token')
  })

  test('管理员登录成功后跳转到管理端看板', async ({ page }) => {
    const loginPage = new LoginPage(page)
    await loginPage.mockLoginSuccess(1)
    await loginPage.goto()
    await loginPage.login('admin', '123456')

    await expect(page).toHaveURL(/\/admin\/dashboard$/)
  })

  test('登录失败时显示后端错误提示并停留登录页', async ({ page }) => {
    const loginPage = new LoginPage(page)
    await loginPage.mockLoginFailure('用户名或密码错误')
    await loginPage.goto()
    await loginPage.login('student001', 'wrong-password')

    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByText('用户名或密码错误')).toBeVisible()
  })
})
