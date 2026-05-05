// frontend/.vitepress/config.ts
import { defineConfig } from 'vitepress'

// 后端和 AI 服务地址，Docker 内用容器名，本地开发用 localhost
const backendTarget = process.env.VITE_PROXY_BACKEND || 'http://localhost:8080'
const aiTarget = process.env.VITE_PROXY_AI || 'http://localhost:8000'

export default defineConfig({
  title: 'BALA 公考',
  description: '上岸没烦恼',
  lang: 'zh-CN',
  srcDir: 'pages',
  cleanUrls: true,
  vite: {
    server: {
      allowedHosts: ['host.docker.internal'],
      proxy: {
        '/api': {
          target: backendTarget,
          changeOrigin: true,
        },
        '/ai': {
          target: aiTarget,
          changeOrigin: true,
        },
      },
    },
  },
  themeConfig: {
    nav: [
      { text: '主页', link: '/' },
      { text: '行测', link: '/题库/' },
      { text: '申论', link: '/essay-bank/' },
      { text: '专项练习', link: '/practice/special/' },
      { text: '个人中心', link: '/login/' },
      { text: '管理', link: '/admin/import/' },

    ],
    sidebar: {
      '/题库/': [
        {
          text: '题库练习',
          items: [
            { text: '💪 专项练习', link: '/practice/special/' },
            { text: '🖊️ 行测题库', link: '/题库/' },
          ]
        }
      ],
      '/practice/': [
        {
          text: '题库练习',
          items: [
            { text: '💪 专项练习', link: '/practice/special/' },
            { text: '🖊️ 行测题库', link: '/题库/' },
          ]
        }
      ],
      '/login/': [
        {
          text: '题库练习',
          items: [
            { text: '💪 专项练习', link: '/practice/special/' },
            { text: '🖊️ 行测题库', link: '/题库/' },
          ]
        }
      ],
    },
    darkModeSwitchLabel: '切换主题',
    returnToTopLabel: '返回顶部',
    sidebarMenuLabel: '菜单',
    outlineLabel: '目录',
    docFooter: {
      prev: '上一章',
      next: '下一章',
    },
  },
})
