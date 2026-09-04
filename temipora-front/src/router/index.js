import { createRouter, createWebHistory } from 'vue-router'
import Login from '../components/Login.vue'
import Usuarios from '../components/Usuarios.vue'

const routes = [
  { path: '/login', name: 'login', component: Login },
  {
    path: '/admin/usuarios',
    name: 'usuarios',
    component: Usuarios,
    meta: { requiresAdmin: true },
  },
  { path: '/', redirect: '/login' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const rol = localStorage.getItem('rol')

  if (to.meta.requiresAdmin) {
    if (!token || rol !== 'ADMIN') {
      next({ name: 'login' })
      return
    }
  }

  if (to.name === 'login' && token) {
    next({ name: 'usuarios' })
    return
  }

  next()
})

export default router