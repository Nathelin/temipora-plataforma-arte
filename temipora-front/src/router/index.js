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
import PresupuestoAdmin from '../components/PresupuestoAdmin.vue'
import ComisionAdmin from '../components/ComisionAdmin.vue'
import PedidoConfigAdmin from '../components/PedidoConfigAdmin.vue'
import ConfiguracionMensajeriaAdmin from '../components/ConfiguracionMensajeriaAdmin.vue'
import TerminosPlataformaAdmin from '../components/TerminosPlataformaAdmin.vue'
import CriteriosReseniaAdmin from '../components/CriteriosReseniaAdmin.vue'
import ConfiguracionReseniaAdmin from '../components/ConfiguracionReseniaAdmin.vue'
import ReseniasReportadasAdmin from '../components/ReseniasReportadasAdmin.vue'
import HistorialReportesAdmin from '../components/HistorialReportesAdmin.vue'
import Registro from '../components/Registro.vue'
import TOS from '../components/TOS.vue'
import MainLayout from '../layouts/MainLayout.vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import ConfiguracionLayout from '../layouts/ConfiguracionLayout.vue'
import ArtistLayout from '../layouts/ArtistLayout.vue'
import Inicio from '../views/Inicio.vue'
import PanelResumen from '../views/PanelResumen.vue'
import Perfil from '../views/Perfil.vue'
import Favoritos from '../views/Favoritos.vue'
import FavoritosArtistas from '../views/FavoritosArtistas.vue'
import FavoritosServicios from '../views/FavoritosServicios.vue'
import Servicios from '../views/Servicios.vue'
import ServicioDetalle from '../views/ServicioDetalle.vue'
import SolicitudServicio from '../views/SolicitudServicio.vue'
import SolicitudDetalle from '../views/SolicitudDetalle.vue'
import Solicitudes from '../views/Solicitudes.vue'
import PresupuestoForm from '../views/PresupuestoForm.vue'
import PagoPresupuesto from '../views/PagoPresupuesto.vue'
import Pedidos from '../views/Pedidos.vue'
import PedidoDetalle from '../views/PedidoDetalle.vue'
import Notificaciones from '../views/Notificaciones.vue'
import Mensajes from '../views/Mensajes.vue'
import Conversacion from '../views/Conversacion.vue'
import ConfiguracionPerfil from '../views/ConfiguracionPerfil.vue'
import ConfiguracionApariencia from '../views/ConfiguracionApariencia.vue'
import ConfiguracionPrivacidad from '../views/ConfiguracionPrivacidad.vue'
import ConfiguracionEtiquetas from '../views/ConfiguracionEtiquetas.vue'
import ConfiguracionFormularioPedidos from '../views/ConfiguracionFormularioPedidos.vue'
import ConfiguracionTerminosArtista from '../views/ConfiguracionTerminosArtista.vue'
import ConfiguracionCuenta from '../views/ConfiguracionCuenta.vue'
import ConfiguracionCobros from '../views/ConfiguracionCobros.vue'
import PortfolioSubir from '../views/PortfolioSubir.vue'
import MisServicios from '../views/MisServicios.vue'
import ServicioForm from '../views/ServicioForm.vue'
import ServicioSolicitudes from '../views/ServicioSolicitudes.vue'
import ServiciosArchivados from '../views/ServiciosArchivados.vue'
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
      {
        path: 'servicios/:id/solicitar',
        name: 'solicitud-servicio',
        component: SolicitudServicio,
        meta: { title: 'Solicitar servicio', requiresAuth: true, requiresComprador: true },
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
        meta: { title: 'Favoritos', requiresAuth: true },
      },
      {
        path: 'favoritos/artistas',
        name: 'favoritos-artistas',
        component: FavoritosArtistas,
        meta: { title: 'Artistas favoritos', requiresAuth: true },
      },
      {
        path: 'favoritos/servicios',
        name: 'favoritos-servicios',
        component: FavoritosServicios,
        meta: { title: 'Servicios favoritos', requiresAuth: true },
      },
      {
        path: 'notificaciones',
        name: 'notificaciones',
        component: Notificaciones,
        meta: { title: 'Notificaciones', requiresAuth: true },
      },
      {
        path: 'solicitudes',
        name: 'solicitudes',
        component: Solicitudes,
        meta: { title: 'Mis solicitudes', requiresAuth: true },
      },
      {
        path: 'solicitudes/:idSolicitud',
        name: 'solicitud-detalle',
        component: SolicitudDetalle,
        meta: {
          title: (route) => `Solicitud #${route.params.idSolicitud}`,
          requiresAuth: true,
        },
      },
      {
        path: 'solicitudes/:idSolicitud/pagar',
        name: 'pago-presupuesto',
        component: PagoPresupuesto,
        meta: {
          title: 'Pagar presupuesto',
          requiresAuth: true,
          requiresComprador: true,
        },
      },
      {
        path: 'pedidos',
        name: 'pedidos',
        component: Pedidos,
        meta: { title: 'Pedidos', requiresAuth: true },
      },
      {
        path: 'pedidos/:idPedido',
        name: 'pedido-detalle',
        component: PedidoDetalle,
        meta: {
          title: (route) => `Pedido #${route.params.idPedido}`,
          requiresAuth: true,
        },
      },
      {
        path: 'mensajes',
        name: 'mensajes',
        component: Mensajes,
        meta: { title: 'Mensajes', requiresAuth: true, requiresComprador: true },
      },
      {
        path: 'mensajes/:idConversacion',
        name: 'conversacion',
        component: Conversacion,
        meta: {
          title: 'Conversación',
          requiresAuth: true,
          requiresComprador: true,
        },
      },
      {
        path: 'panel',
        component: ArtistLayout,
        meta: { requiresAuth: true, requiresArtist: true },
        children: [
          { path: '', name: 'panel-resumen', component: PanelResumen, meta: { title: 'Mi panel' } },
          { path: 'servicios', name: 'mis-servicios', component: MisServicios, meta: { title: 'Mis servicios' } },
          { path: 'servicios/crear', name: 'servicio-crear', component: ServicioForm, meta: { title: 'Crear servicio' } },
          { path: 'servicios/:id/editar', name: 'servicio-editar', component: ServicioForm, meta: { title: 'Editar servicio' } },
          { path: 'servicios/:id/solicitudes', name: 'servicio-solicitudes', component: ServicioSolicitudes, meta: { title: 'Solicitudes del servicio' } },
          { path: 'solicitudes', name: 'panel-solicitudes', component: Solicitudes, meta: { title: 'Solicitudes' } },
          { path: 'solicitudes/:idSolicitud/presupuesto', name: 'presupuesto-crear', component: PresupuestoForm, meta: { title: 'Enviar presupuesto' } },
          { path: 'pedidos', name: 'panel-pedidos', component: Pedidos, meta: { title: 'Pedidos' } },
          { path: 'pedidos/:idPedido', name: 'panel-pedido-detalle', component: PedidoDetalle, meta: { title: 'Pedido' } },
          { path: 'mensajes', name: 'panel-mensajes', component: Mensajes, meta: { title: 'Mensajes' } },
          { path: 'mensajes/:idConversacion', name: 'panel-conversacion', component: Conversacion, meta: { title: 'Conversación' } },
          { path: 'formulario-pedidos', name: 'panel-formulario-pedidos', component: ConfiguracionFormularioPedidos, meta: { title: 'Formulario de pedidos' } },
          { path: 'terminos', name: 'panel-terminos', component: ConfiguracionTerminosArtista, meta: { title: 'Términos y condiciones' } },
          { path: 'etiquetas', name: 'panel-etiquetas', component: ConfiguracionEtiquetas, meta: { title: 'Mis etiquetas' } },
          { path: 'portfolio', name: 'portfolio-subir', component: PortfolioSubir, meta: { title: 'Mi portfolio' } },
        ],
      },
      { path: 'portfolio/subir', redirect: { name: 'portfolio-subir' } },
      { path: 'mis-servicios', redirect: { name: 'mis-servicios' } },
      { path: 'mis-servicios/crear', redirect: { name: 'servicio-crear' } },
      {
        path: 'mis-servicios/:id/editar',
        redirect: (to) => ({ name: 'servicio-editar', params: { id: to.params.id } }),
      },
      {
        path: 'mis-servicios/:id/solicitudes',
        redirect: (to) => ({ name: 'servicio-solicitudes', params: { id: to.params.id } }),
      },
      { path: 'configuracion/etiquetas', redirect: { name: 'panel-etiquetas' } },
      { path: 'configuracion/formulario-pedidos', redirect: { name: 'panel-formulario-pedidos' } },
      { path: 'configuracion/terminos-artista', redirect: { name: 'panel-terminos' } },
      {
        path: 'configuracion',
        component: ConfiguracionLayout,
        children: [
          { path: '', redirect: '/configuracion/perfil' },
          { path: 'perfil', name: 'configuracion-perfil', component: ConfiguracionPerfil, meta: { title: 'Configuración - Perfil' } },
          { path: 'apariencia', name: 'configuracion-apariencia', component: ConfiguracionApariencia, meta: { title: 'Configuración - Apariencia' } },
          { path: 'privacidad', name: 'configuracion-privacidad', component: ConfiguracionPrivacidad, meta: { title: 'Configuración - Privacidad' } },
          { path: 'cuenta', name: 'configuracion-cuenta', component: ConfiguracionCuenta, meta: { title: 'Configuración - Cuenta' } },
          { path: 'cobros', name: 'configuracion-cobros', component: ConfiguracionCobros, meta: { title: 'Configuración - Cobros', requiresArtist: true } },
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
        path: 'servicios/archivados',
        name: 'servicios-archivados',
        component: ServiciosArchivados,
        meta: { title: 'Servicios archivados' },
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
        path: 'resenas/reportadas',
        name: 'admin-resenias-reportadas',
        component: ReseniasReportadasAdmin,
        meta: { title: 'Moderación de reseñas' },
      },
      {
        path: 'resenas/historial',
        name: 'admin-resenias-historial',
        component: HistorialReportesAdmin,
        meta: { title: 'Historial de reportes' },
      },
      {
        path: 'resenas/criterios',
        name: 'criterios-resenia-admin',
        component: CriteriosReseniaAdmin,
        meta: { title: 'Criterios de evaluación' },
      },
      {
        path: 'resenas/configuracion',
        name: 'configuracion-resenia-admin',
        component: ConfiguracionReseniaAdmin,
        meta: { title: 'Configuración de reseñas' },
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
      {
        path: 'ajustes/presupuestos',
        name: 'presupuestos-admin',
        component: PresupuestoAdmin,
        meta: { title: 'Presupuestos' },
      },
      {
        path: 'ajustes/comisiones',
        name: 'comisiones-admin',
        component: ComisionAdmin,
        meta: { title: 'Comisión de la plataforma' },
      },
      {
        path: 'ajustes/pedidos',
        name: 'pedidos-admin',
        component: PedidoConfigAdmin,
        meta: { title: 'Hitos y pedidos' },
      },
      {
        path: 'ajustes/mensajeria',
        name: 'configuracion-mensajeria-admin',
        component: ConfiguracionMensajeriaAdmin,
        meta: { title: 'Configuración de mensajería' },
      },
      {
        path: 'pedidos/:idPedido',
        name: 'admin-pedido-detalle',
        component: PedidoDetalle,
        meta: { title: (route) => `Pedido #${route.params.idPedido}` },
      },
      {
        path: 'ajustes/terminos-plataforma',
        name: 'terminos-plataforma-admin',
        component: TerminosPlataformaAdmin,
        meta: { title: 'Términos de la plataforma' },
      },
      {
        path: 'notificaciones',
        name: 'admin-notificaciones',
        component: Notificaciones,
        meta: { title: 'Notificaciones' },
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
  const saliendoDeSubir = from.path === '/panel/portfolio' && to.path !== from.path
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

  if (to.matched.some((r) => r.meta.requiresComprador)) {
    if (!token || (rol !== 'CLIENTE' && rol !== 'ARTISTA')) {
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