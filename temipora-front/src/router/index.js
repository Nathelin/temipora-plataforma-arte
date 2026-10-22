import { createRouter, createWebHistory } from 'vue-router'
import Login from '../components/Login.vue'
import Usuarios from '../components/Usuarios.vue'
import TiposAdmin from '../components/TiposAdmin.vue'
import EtiquetasAdmin from '../components/EtiquetasAdmin.vue'
import CategoriasEtiquetaAdmin from '../components/CategoriasEtiquetaAdmin.vue'
import MediosAdmin from '../components/MediosAdmin.vue'
import TiposArteAdmin from '../components/TiposArteAdmin.vue'
import MaterialesAdmin from '../components/MaterialesAdmin.vue'
import PaisesAdmin from '../components/PaisesAdmin.vue'
import CotizacionAdmin from '../components/CotizacionAdmin.vue'
import MonedasAdmin from '../components/MonedasAdmin.vue'
import Registro from '../components/Registro.vue'
import TOS from '../components/TOS.vue'
import MainLayout from '../layouts/MainLayout.vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import ConfiguracionLayout from '../layouts/ConfiguracionLayout.vue'
import Inicio from '../views/Inicio.vue'
import Perfil from '../views/Perfil.vue'
import Favoritos from '../views/Favoritos.vue'
import Servicios from '../views/Servicios.vue'
import ServicioDetalle from '../views/ServicioDetalle.vue'
import ConfiguracionPerfil from '../views/ConfiguracionPerfil.vue'
import ConfiguracionApariencia from '../views/ConfiguracionApariencia.vue'
import ConfiguracionPrivacidad from '../views/ConfiguracionPrivacidad.vue'
import ConfiguracionEtiquetas from '../views/ConfiguracionEtiquetas.vue'
import ConfiguracionFormularioPedidos from '../views/ConfiguracionFormularioPedidos.vue'
import ConfiguracionTerminosArtista from '../views/ConfiguracionTerminosArtista.vue'
import ConfiguracionCuenta from '../views/ConfiguracionCuenta.vue'
import PortfolioSubir from '../views/PortfolioSubir.vue'
import MisServicios from '../views/MisServicios.vue'
import ServicioForm from '../views/ServicioForm.vue'
import { useSubirImagen } from '../composables/useSubirImagen'
import { useConfirmacionSalida } from '../composables/useConfirmacionSalida'
import { useCambiosSinGuardar } from '../composables/useCambiosSinGuardar'

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
      { path: 'servicios/:id', name: 'servicio-detalle', component: ServicioDetalle,
        props: (route) => ({ id: Number(route.params.id) }),
        meta: { title: 'Detalle del servicio' }
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
        path: 'favoritos',
        name: 'favoritos',
        component: Favoritos,
        meta: { title: 'Artistas favoritos', requiresAuth: true },
      },
      {
        path: 'portfolio/subir',
        name: 'portfolio-subir',
        component: PortfolioSubir,
        meta: { title: 'Subir imagen al portfolio', requiresArtist: true },
      },
      {
        path: 'mis-servicios',
        name: 'mis-servicios',
        component: MisServicios,
        meta: { title: 'Mis servicios', requiresArtist: true },
      },
      {
        path: 'mis-servicios/crear',
        name: 'servicio-crear',
        component: ServicioForm,
        meta: { title: 'Crear servicio', requiresArtist: true },
      },
      {
        path: 'mis-servicios/:id/editar',
        name: 'servicio-editar',
        component: ServicioForm,
        meta: { title: 'Editar servicio', requiresArtist: true },
      },
      {
        path: 'configuracion',
        component: ConfiguracionLayout,
        children: [
          { path: '', redirect: '/configuracion/perfil' },
          { path: 'perfil', name: 'configuracion-perfil', component: ConfiguracionPerfil, meta: { title: 'Configuración - Perfil' } },
          { path: 'apariencia', name: 'configuracion-apariencia', component: ConfiguracionApariencia, meta: { title: 'Configuración - Apariencia' } },
          { path: 'privacidad', name: 'configuracion-privacidad', component: ConfiguracionPrivacidad, meta: { title: 'Configuración - Privacidad' } },
          { path: 'etiquetas', name: 'configuracion-etiquetas', component: ConfiguracionEtiquetas, meta: { title: 'Configuración - Mis etiquetas' } },
          { path: 'formulario-pedidos', name: 'configuracion-formulario-pedidos', component: ConfiguracionFormularioPedidos, meta: { title: 'Configuración - Formulario de pedidos', requiresArtist: true } },
          { path: 'terminos-artista', name: 'configuracion-terminos-artista', component: ConfiguracionTerminosArtista, meta: { title: 'Configuración - Términos y condiciones', requiresArtist: true } },
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
        meta: { title: 'Etiquetas' },
      },
      {
        path: 'etiquetas/categorias',
        name: 'categorias-admin',
        component: CategoriasEtiquetaAdmin,
        meta: { title: 'Categorías' },
      },
      {
        path: 'arte/medios',
        name: 'medios-admin',
        component: MediosAdmin,
        meta: { title: 'Medios' },
      },
      {
        path: 'arte/tipos',
        name: 'tipos-arte-admin',
        component: TiposArteAdmin,
        meta: { title: 'Tipos de arte' },
      },
      {
        path: 'arte/materiales',
        name: 'materiales-admin',
        component: MaterialesAdmin,
        meta: { title: 'Materiales' },
      },
      {
        path: 'ajustes/paises',
        name: 'paises-admin',
        component: PaisesAdmin,
        meta: { title: 'Países' },
      },
      {
        path: 'ajustes/cotizacion',
        name: 'cotizacion-admin',
        component: CotizacionAdmin,
        meta: { title: 'Cotización de monedas' },
      },
      {
        path: 'ajustes/monedas',
        name: 'monedas-admin',
        component: MonedasAdmin,
        meta: { title: 'Monedas' },
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

const { subidaEnCurso, publicadasEnSesion, proteccionActiva } = useSubirImagen()
const { pedir } = useConfirmacionSalida()
const { hayCambios: hayCambiosConfig, mensaje: mensajeConfig, limpiar: limpiarConfig } = useCambiosSinGuardar()

router.beforeEach(async (to, from) => {
  const saliendoDeSubir = from.path === '/portfolio/subir' && to.path !== from.path
  if (!saliendoDeSubir || !proteccionActiva.value) {
    return true
  }

  let mensaje = ''
  const publicadas = publicadasEnSesion.value
  if (publicadas === 1) {
    mensaje += 'Ya publicaste 1 imagen de este lote: esa información sí se guardó en tu portfolio. '
  } else if (publicadas > 1) {
    mensaje += `Ya publicaste ${publicadas} imágenes de este lote: esa información sí se guardó en tu portfolio. `
  }
  if (subidaEnCurso.value) {
    mensaje += 'Hay imágenes en plena subida que se interrumpirán. '
  }
  mensaje += 'Las imágenes restantes no se publicarán y sus archivos subidos se eliminarán.'

  return await pedir(mensaje)
})

router.beforeEach(async (to, from) => {
  const saliendoDeConfig = from.path.startsWith('/configuracion') && to.path !== from.path
  if (!saliendoDeConfig || !hayCambiosConfig.value) {
    return true
  }

  const permitir = await pedir(mensajeConfig.value, {
    titulo: '¿Salir sin guardar?',
    textoConfirmar: 'Salir sin guardar',
    textoCancelar: 'Seguir editando',
  })
  if (permitir) limpiarConfig()
  return permitir
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

  if (to.matched.some((r) => r.meta.requiresAuth) && !token) {
    next({ name: 'login' })
    return
  }

  if (to.matched.some((r) => r.meta.requiresAdmin)) {
    if (!token || rol !== 'ADMIN') {
      next({ name: 'login' })
      return
    }
  }

  if (to.matched.some((r) => r.meta.requiresArtist)) {
    if (!token || rol !== 'ARTISTA') {
      next({ name: 'inicio' })
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