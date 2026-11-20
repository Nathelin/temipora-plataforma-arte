import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import AdminPageHeader from './components/admin/AdminPageHeader.vue'
import AdminTabs from './components/admin/AdminTabs.vue'
import CargandoPagina from './components/CargandoPagina.vue'

const app = createApp(App)
app.component('AdminPageHeader', AdminPageHeader)
app.component('AdminTabs', AdminTabs)
app.component('CargandoPagina', CargandoPagina)
app.use(router).mount('#app')