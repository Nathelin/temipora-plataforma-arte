import { createRouter, createWebHistory } from 'vue-router'
import Login from '../components/Login.vue'
import Usuarios from '../components/Usuarios.vue'
import TiposAdmin from '../components/TiposAdmin.vue'
import EtiquetasAdmin from '../components/EtiquetasAdmin.vue'
import Registro from '../components/Registro.vue'
import TOS from '../components/TOS.vue'
import MainLayout from '../layouts/MainLayout.vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import ConfiguracionLayout from '../layouts/ConfiguracionLayout.vue'
import Inicio from '../views/Inicio.vue'
import Perfil from '../views/Perfil.vue'
import Servicios from '../views/Servicios.vue'
import ConfiguracionPerfil from '../views/ConfiguracionPerfil.vue'
import ConfiguracionPrivacidad from '../views/ConfiguracionPrivacidad.vue'
import ConfiguracionTerminos from '../views/ConfiguracionTerminos.vue'
import ConfiguracionCuenta from '../views/ConfiguracionCuenta.vue'

const routes = [
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: '', 
        name: 'inicio', 
        component: Inicio,
        meta: { title: 'Temipora - inicio' }  },
      { path: 'productos', redirect: '/' },
      { path: 'servicios', name: 'servicios', component: Servicios,
        meta: { title: 'Servicios' }
      },
      { path: 'perfil/:nombreUsuario', name: 'perfil', component: Perfil,
        meta: { title: (route) => `Perfil de @${route.params.nombreUsuario}` }
      },
      {
        path: 'perfil',
        redirect: () => {
          const nombreUsuario = localStorage.getItem('nombreUsuario')
          if (nombreUsuario) {
            return { name: 'perfil', params: { nombreUsuario } }
          }
          return { name: 'inicio' }
        },
      },
      {
        path: 'configuracion',
        component: ConfiguracionLayout,
        children: [
          { path: '', redirect: '/configuracion/perfil' },
          { path: 'perfil', name: 'configuracion-perfil', component: ConfiguracionPerfil, meta: { title: 'Configuración - Perfil' } },
          { path: 'privacidad', name: 'configuracion-privacidad', component: ConfiguracionPrivacidad, meta: { title: 'Configuración - Privacidad' } },
          { path: 'terminos', name: 'configuracion-terminos', component: ConfiguracionTerminos, meta: { title: 'Configuración - Términos' } },
          { path: 'cuenta', name: 'configuracion-cuenta', component: ConfiguracionCuenta, meta: { title: 'Configuración - Cuenta' } },
        ],
      },
      { path: 'about', redirect: '/' },
      { path: 'contacto', redirect: '/' },
    ],
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAdmin: true },
    children: [
      {
        path: 'usuarios',
        name: 'usuarios',
        component: Usuarios,
        meta: { title: 'Usuarios registrados' },
      },
      {
        path: 'tipos/producto',
        name: 'tipos-producto',
        component: TiposAdmin,
        props: { tipo: 'producto' },
        meta: { title: 'Tipos de producto' },
      },
      {
        path: 'tipos/servicio',
        name: 'tipos-servicio',
        component: TiposAdmin,
        props: { tipo: 'servicio' },
        meta: { title: 'Tipos de servicio' },
      },
      {
        path: 'etiquetas',
        name: 'etiquetas-admin',
        component: EtiquetasAdmin,
        meta: { title: 'Gestión de etiquetas' },
      },
    ],
  },
  { path: '/login', name: 'login', component: Login, meta: { title: 'Iniciar sesión' } },
  { path: '/registrarse', name: 'registrarse', component: Registro, meta: { title: 'Regístrate' } },
  { path: '/terminos', name: 'terminos', component: TOS, meta: { title: 'Términos de uso y servicio' }},
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const rol = localStorage.getItem('rol')

  const routeTitle = to.meta.title;

  if (typeof routeTitle === 'function') {
    document.title = routeTitle(to); 
  } else if (routeTitle) {
    document.title = routeTitle;
  } else {
    document.title = 'Temipora';
  }

  if (to.matched.some((r) => r.meta.requiresAdmin)) {
    if (!token || rol !== 'ADMIN') {
      next({ name: 'login' })
      return
    }
  }

  if (to.name === 'login' && token) {
    next({ name: 'inicio' })
    return
  }

  next()
})

export default router