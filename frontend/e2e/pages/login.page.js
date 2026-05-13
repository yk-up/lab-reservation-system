export class LoginPage {
  /**
   * @param {import('@playwright/test').Page} page
   */
  constructor(page) {
    this.page = page
    this.usernameInput = page.getByPlaceholder('请输入学号 / 工号')
    this.passwordInput = page.getByPlaceholder('请输入密码')
    this.submitButton = page.getByRole('button', { name: '登录' })
  }

  async goto() {
    await this.page.goto('/login')
  }

  async login(username, password) {
    await this.usernameInput.fill(username)
    await this.passwordInput.fill(password)
    await this.submitButton.click()
  }

  async mockLoginSuccess(role = 0) {
    await this.page.route('**/api/auth/login', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200,
          message: 'success',
          data: {
            token: 'mock-jwt-token',
            userId: 1001,
            username: role === 1 ? 'admin' : 'student001',
            realName: role === 1 ? '管理员' : '测试学生',
            role
          }
        })
      })
    })
  }

  async mockLoginFailure(message = '用户名或密码错误') {
    await this.page.route('**/api/auth/login', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 400,
          message
        })
      })
    })
  }
}
