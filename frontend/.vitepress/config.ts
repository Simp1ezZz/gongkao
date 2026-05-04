// frontend/.vitepress/config.ts
import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'BALA 公考',
  description: '上岸没烦恼',
  lang: 'zh-CN',
  srcDir: 'pages',
  cleanUrls: true,
  themeConfig: {
    nav: [
      { text: '主页', link: '/' },
      { text: '行测', link: '/题库/' },
      { text: '申论', link: '/essay-bank/' },
      { text: '专项练习', link: '/practice/special/' },
      { text: '个人中心', link: '/login/' },
    ],
    sidebar: {},
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
