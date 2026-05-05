import DefaultTheme from 'vitepress/theme'
import Login from './components/Login.vue'
import PaperList from './components/PaperList.vue'
import OnlinePractice from './components/OnlinePractice.vue'
import Empty from './components/Empty.vue'
import HomeQuickNav from './components/HomeQuickNav.vue'
import Modal from './components/Modal.vue'

export default {
  ...DefaultTheme,
  enhanceApp({ app }) {
    app.component('Login', Login)
    app.component('PaperList', PaperList)
    app.component('OnlinePractice', OnlinePractice)
    app.component('Empty', Empty)
    app.component('HomeQuickNav', HomeQuickNav)
    app.component('Modal', Modal)
  }
}
