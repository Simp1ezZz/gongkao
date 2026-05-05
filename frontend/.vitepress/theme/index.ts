import DefaultTheme from 'vitepress/theme'
import { onMounted, watch, nextTick } from 'vue'
import { useRoute } from 'vitepress'
import Login from './components/Login.vue'
import PaperList from './components/PaperList.vue'
import OnlinePractice from './components/OnlinePractice.vue'
import Empty from './components/Empty.vue'
import HomeQuickNav from './components/HomeQuickNav.vue'
import Modal from './components/Modal.vue'
import QuestionImport from './components/QuestionImport.vue'

function isAdmin() {
  try {
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    return user.role === 'admin'
  } catch { return false }
}

function updateAdminNav() {
  const navItems = document.querySelectorAll('.VPNav .VPLink')
  navItems.forEach(item => {
    const link = item.querySelector('a')
    if (link && link.getAttribute('href') === '/admin/import/') {
      item.closest('.VPNavBarMenuLink, .VPNavMenuLink, li')?.remove()
    }
  })
}

export default {
  ...DefaultTheme,
  enhanceApp({ app }) {
    app.component('Login', Login)
    app.component('PaperList', PaperList)
    app.component('OnlinePractice', OnlinePractice)
    app.component('Empty', Empty)
    app.component('HomeQuickNav', HomeQuickNav)
    app.component('Modal', Modal)
    app.component('QuestionImport', QuestionImport)
  },
  setup() {
    const route = useRoute()

    function injectAdminNav() {
      if (!isAdmin()) return
      const menu = document.querySelector('.VPNavBarMenu')
      if (!menu) return
      // Already injected
      if (menu.querySelector('a[href="/admin/import/"]')) return
      const link = document.createElement('a')
      link.className = 'VPNavBarMenuLink'
      link.href = '/admin/import/'
      link.innerHTML = '<span class="vp-link-text">题库导入</span>'
      menu.appendChild(link)
    }

    onMounted(() => {
      injectAdminNav()
    })

    watch(() => route.path, () => {
      nextTick(() => injectAdminNav())
    })
  }
}
