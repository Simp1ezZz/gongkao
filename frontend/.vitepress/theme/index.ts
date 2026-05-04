import DefaultTheme from 'vitepress/theme'
import Login from './components/Login.vue'

export default {
  ...DefaultTheme,
  enhanceApp({ app }) {
    app.component('Login', Login)
  }
}
