import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'

// Cada pantalla se carga solo cuando se visita. El primer bundle queda con el
// shell (layout + router) y la vista inicial; el resto viaja por su cuenta.
const Login = () => import('../components/Login.vue')
const Usuarios = () => import('../components/Usuarios.vue')
const SancionesAdmin = () => import('../components/SancionesAdmin.vue')
const AdminSancionesPruebas = () => import('../views/AdminSancionesPruebas.vue')
const AnclajeAdmin = () => import('../components/AnclajeAdmin.vue')
const TiposAdmin = () => import('../components/TiposAdmin.vue')
const EtiquetasAdmin = () => import('../components/EtiquetasAdmin.vue')
const CategoriasEtiquetaAdmin = () => import('../components/CategoriasEtiquetaAdmin.vue')
const MediosAdmin = () => import('../components/MediosAdmin.vue')
const TiposArteAdmin = () => import('../components/TiposArteAdmin.vue')
const MaterialesAdmin = () => import('../components/MaterialesAdmin.vue')
const PaisesAdmin = () => import('../components/PaisesAdmin.vue')
const CotizacionAdmin = () => import('../components/CotizacionAdmin.vue')
const MonedasAdmin = () => import('../components/MonedasAdmin.vue')
const PresupuestoAdmin = () => import('../components/PresupuestoAdmin.vue')
const ComisionAdmin = () => import('../components/ComisionAdmin.vue')
const PromocionesAdmin = () => import('../components/PromocionesAdmin.vue')
const DescuentosPromocionesAdmin = () => import('../components/DescuentosPromocionesAdmin.vue')
const SustitucionesAdmin = () => import('../components/SustitucionesAdmin.vue')
const HistorialSustitucionesAdmin = () => import('../components/HistorialSustitucionesAdmin.vue')
const PedidoConfigAdmin = () => import('../components/PedidoConfigAdmin.vue')
const ConfiguracionBajaCuentaAdmin = () => import('../components/ConfiguracionBajaCuentaAdmin.vue')
const ConfiguracionMensajeriaAdmin = () => import('../components/ConfiguracionMensajeriaAdmin.vue')
const TerminosPlataformaAdmin = () => import('../components/TerminosPlataformaAdmin.vue')
const CriteriosReseniaAdmin = () => import('../components/CriteriosReseniaAdmin.vue')
const ConfiguracionReseniaAdmin = () => import('../components/ConfiguracionReseniaAdmin.vue')
const AuditoriaAdmin = () => import('../components/AuditoriaAdmin.vue')
const ReseniasReportadasAdmin = () => import('../components/ReseniasReportadasAdmin.vue')
const HistorialReportesAdmin = () => import('../components/HistorialReportesAdmin.vue')
const Registro = () => import('../components/Registro.vue')
const TOS = () => import('../components/TOS.vue')
const AdminLayout = () => import('../layouts/AdminLayout.vue')
const ConfiguracionLayout = () => import('../layouts/ConfiguracionLayout.vue')
const ArtistLayout = () => import('../layouts/ArtistLayout.vue')
const ClientLayout = () => import('../layouts/ClientLayout.vue')
const Inicio = () => import('../views/Inicio.vue')
const Explorar = () => import('../views/Explorar.vue')
const AdminResumen = () => import('../views/AdminResumen.vue')
const AdminInformeVentas = () => import('../views/AdminInformeVentas.vue')
const PanelResumen = () => import('../views/PanelResumen.vue')
const Ventas = () => import('../views/Ventas.vue')
const InformeVentas = () => import('../views/InformeVentas.vue')
const Perfil = () => import('../views/Perfil.vue')
const Favoritos = () => import('../views/Favoritos.vue')
const FavoritosArtistas = () => import('../views/FavoritosArtistas.vue')
const FavoritosServicios = () => import('../views/FavoritosServicios.vue')
const Servicios = () => import('../views/Servicios.vue')
const ServicioDetalle = () => import('../views/ServicioDetalle.vue')
const AlternativasServicio = () => import('../views/AlternativasServicio.vue')
const SolicitudServicio = () => import('../views/SolicitudServicio.vue')
const SolicitudDetalle = () => import('../views/SolicitudDetalle.vue')
const CierreDetalle = () => import('../views/CierreDetalle.vue')
const Solicitudes = () => import('../views/Solicitudes.vue')
const PresupuestoForm = () => import('../views/PresupuestoForm.vue')
const PagoPresupuesto = () => import('../views/PagoPresupuesto.vue')
const Pedidos = () => import('../views/Pedidos.vue')
const PedidoDetalle = () => import('../views/PedidoDetalle.vue')
const Notificaciones = () => import('../views/Notificaciones.vue')
const Ofertas = () => import('../views/Ofertas.vue')
const Mensajes = () => import('../views/Mensajes.vue')
const Conversacion = () => import('../views/Conversacion.vue')
const ConfiguracionPerfil = () => import('../views/ConfiguracionPerfil.vue')
const ConfiguracionApariencia = () => import('../views/ConfiguracionApariencia.vue')
const ConfiguracionPrivacidad = () => import('../views/ConfiguracionPrivacidad.vue')
const ConfiguracionEtiquetas = () => import('../views/ConfiguracionEtiquetas.vue')
const ConfiguracionFormularioPedidos = () => import('../views/ConfiguracionFormularioPedidos.vue')
const ConfiguracionTerminosArtista = () => import('../views/ConfiguracionTerminosArtista.vue')
const ConfiguracionCuenta = () => import('../views/ConfiguracionCuenta.vue')
const ConfiguracionCobros = () => import('../views/ConfiguracionCobros.vue')
const CuentaRestringida = () => import('../views/CuentaRestringida.vue')
const ApelacionesAdmin = () => import('../components/ApelacionesAdmin.vue')
const PortfolioSubir = () => import('../views/PortfolioSubir.vue')
const MisServicios = () => import('../views/MisServicios.vue')
const ServicioForm = () => import('../views/ServicioForm.vue')
const ServicioSolicitudes = () => import('../views/ServicioSolicitudes.vue')
const MisProductos = () => import('../views/MisProductos.vue')
const ProductoForm = () => import('../views/ProductoForm.vue')
const ProductoDetalle = () => import('../views/ProductoDetalle.vue')
const ProductoPublico = () => import('../views/ProductoPublico.vue')
const Productos = () => import('../views/Productos.vue')
const Reventas = () => import('../views/Reventas.vue')
const ReventaPublica = () => import('../views/ReventaPublica.vue')
const MiReventa = () => import('../views/MiReventa.vue')
const Carrito = () => import('../views/Carrito.vue')
const MisCompras = () => import('../views/MisCompras.vue')
const MiActividad = () => import('../views/MiActividad.vue')
const MiHistorial = () => import('../views/MiHistorial.vue')
const CompraDetalle = () => import('../views/CompraDetalle.vue')
const CertificadoPublico = () => import('../views/CertificadoPublico.vue')
const TerminosAceptadosPublico = () => import('../views/TerminosAceptadosPublico.vue')
const ServiciosArchivados = () => import('../views/ServiciosArchivados.vue')
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
      { path: 'explorar', name: 'explorar', component: Explorar,
        meta: { title: 'Explorar' }
      },
      { path: 'productos', name: 'productos', component: Productos,
        meta: { title: 'Productos' }
      },
      { path: 'productos/:id', name: 'producto-publico', component: ProductoPublico,
        props: (route) => ({ id: Number(route.params.id) }),
        meta: { title: 'Detalle del producto' }
      },
      { path: 'reventas', name: 'reventas', component: Reventas,
        meta: { title: 'Reventas' }
      },
      { path: 'reventas/:id', name: 'reventa-publica', component: ReventaPublica,
        props: (route) => ({ id: Number(route.params.id) }),
        meta: { title: 'Detalle de la reventa' }
      },
      { path: 'certificado/:codigo', name: 'certificado-publico', component: CertificadoPublico,
        props: (route) => ({ codigo: String(route.params.codigo) }),
        meta: { title: (route) => `Certificado ${route.params.codigo}` }
      },
      { path: 'terminos-aceptados/:codigo', name: 'terminos-aceptados-publico', component: TerminosAceptadosPublico,
        props: (route) => ({ codigo: String(route.params.codigo) }),
        meta: { title: 'Términos aceptados' }
      },
      {
        path: 'carrito',
        name: 'carrito',
        component: Carrito,
        meta: {
          title: 'Carrito',
          requiresAuth: true,
          requiresComprador: true,
        },
      },
      {
        path: 'servicios',
        name: 'servicios',
        component: Servicios,
        meta: { title: 'Servicios' }
      },
      { path: 'servicios/:id', name: 'servicio-detalle', component: ServicioDetalle,
        props: (route) => ({ id: Number(route.params.id) }),
        meta: { title: 'Detalle del servicio' }
      },
      {
        path: 'servicios/:id/alternativas',
        name: 'servicio-alternativas',
        component: AlternativasServicio,
        meta: { title: 'Artistas con cupo' },
      },
      {
        path: 'servicios/:id/solicitar',
        name: 'solicitud-servicio',
        component: SolicitudServicio,
        meta: { title: 'Solicitar servicio', requiresAuth: true, requiresComprador: true },
      },
      {
        path: 'ofertas',
        name: 'ofertas',
        component: Ofertas,
        meta: { title: 'Ofertas para vos', requiresAuth: true, requiresComprador: true },
      },
      {
        path: 'ofertas/:token',
        name: 'oferta-campana',
        component: Ofertas,
        meta: { title: 'Ofertas para vos' },
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
        path: 'mi-actividad',
        component: ClientLayout,
        meta: { requiresAuth: true },
        children: [
          {
            path: '',
            name: 'mi-actividad',
            component: MiActividad,
            meta: { title: 'Mi actividad', requiresComprador: true },
          },
          {
            path: 'historial',
            name: 'mi-historial',
            component: MiHistorial,
            meta: { title: 'Mi historial', requiresComprador: true },
          },
          {
            path: '/compras',
            name: 'compras',
            component: MisCompras,
            meta: { title: 'Mis compras', requiresComprador: true },
          },
          {
            path: '/compras/:id',
            name: 'compra-detalle',
            component: CompraDetalle,
            meta: { title: 'Detalle de compra', requiresComprador: true },
          },
          {
            path: '/reventa',
            name: 'mi-reventa',
            component: MiReventa,
            meta: { title: 'Reventa', requiresComprador: true },
          },
          {
            path: '/pedidos',
            name: 'pedidos',
            component: Pedidos,
            meta: { title: 'Mis pedidos' },
          },
          {
            path: '/pedidos/:idPedido',
            name: 'pedido-detalle',
            component: PedidoDetalle,
            meta: {
              title: (route) => `Pedido #${route.params.idPedido}`,
            },
          },
          {
            path: '/solicitudes',
            name: 'solicitudes',
            component: Solicitudes,
            meta: { title: 'Mis solicitudes' },
          },
          {
            path: '/solicitudes/:idSolicitud',
            name: 'solicitud-detalle',
            component: SolicitudDetalle,
            meta: {
              title: (route) => `Solicitud #${route.params.idSolicitud}`,
            },
          },
        ],
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
        path: 'solicitudes/:idSolicitud/resultado',
        name: 'solicitud-resultado',
        component: CierreDetalle,
        meta: { title: 'Resultado de la solicitud' },
      },
      {
        path: 'pedidos/:idPedido/resultado',
        name: 'pedido-resultado',
        component: CierreDetalle,
        meta: { title: 'Resultado del pedido' },
      },
      { path: 'mis-compras', redirect: { name: 'compras' } },
      {
        path: 'mensajes',
        name: 'mensajes',
        component: Mensajes,
        meta: { title: 'Mensajes', requiresAuth: true, requiresComprador: true },
      },
      {
        path: 'mensajes/:nombreUsuario',
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
          { path: 'productos', name: 'mis-productos', component: MisProductos, meta: { title: 'Mis productos' } },
          { path: 'productos/crear', name: 'producto-crear', component: ProductoForm, meta: { title: 'Crear producto' } },
          { path: 'productos/:id/editar', name: 'producto-editar', component: ProductoForm, meta: { title: 'Editar producto' } },
          { path: 'productos/:id', name: 'producto-previa', component: ProductoDetalle, meta: { title: 'Vista previa del producto' } },
          { path: 'solicitudes', name: 'panel-solicitudes', component: Solicitudes, meta: { title: 'Solicitudes' } },
          { path: 'solicitudes/:idSolicitud', name: 'panel-solicitud-detalle', component: SolicitudDetalle, meta: { title: 'Solicitud' } },
          { path: 'solicitudes/:idSolicitud/presupuesto', name: 'presupuesto-crear', component: PresupuestoForm, meta: { title: 'Enviar presupuesto' } },
          { path: 'pedidos', name: 'panel-pedidos', component: Pedidos, meta: { title: 'Pedidos' } },
          { path: 'pedidos/:idPedido', name: 'panel-pedido-detalle', component: PedidoDetalle, meta: { title: 'Pedido' } },
          { path: 'ventas', name: 'panel-ventas', component: Ventas, meta: { title: 'Ventas' } },
          { path: 'informes', name: 'panel-informes', component: InformeVentas, meta: { title: 'Informe de ventas' } },
          { path: 'mensajes', name: 'panel-mensajes', component: Mensajes, meta: { title: 'Mensajes' } },
          { path: 'mensajes/:nombreUsuario', name: 'panel-conversacion', component: Conversacion, meta: { title: 'Conversación' } },
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
          { path: 'pagos-y-cobros', name: 'configuracion-pagos-y-cobros', component: ConfiguracionCobros, meta: { title: 'Configuración - Pagos y cobros', requiresAuth: true } },
          { path: 'cobros', redirect: { name: 'configuracion-pagos-y-cobros' } },
        ],
      },
      { path: 'about', redirect: '/' },
      { path: 'contacto', redirect: '/' },
    ],
  },
  {
    path: '/cuenta/restringida',
    name: 'cuenta-restringida',
    component: CuentaRestringida,
    meta: { title: 'Cuenta restringida', requiresAuth: true },
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAdmin: true },
    children: [
      {
        path: '',
        name: 'admin-resumen',
        component: AdminResumen,
        meta: { title: 'Resumen de administración' },
      },
      {
        path: 'informes',
        redirect: '/admin/informes/ventas',
      },
      {
        path: 'informes/ventas',
        name: 'admin-informe-ventas',
        component: AdminInformeVentas,
        meta: { title: 'Informes - Ventas y comisiones' },
      },
      {
        path: 'usuarios',
        name: 'usuarios',
        component: Usuarios,
        meta: { title: 'Usuarios registrados' },
      },
      {
        path: 'sanciones',
        name: 'admin-sanciones',
        component: SancionesAdmin,
        meta: { title: 'Sanciones de cuentas' },
      },
      {
        path: 'sanciones/pruebas',
        name: 'admin-sanciones-pruebas',
        component: AdminSancionesPruebas,
        meta: { title: 'Pruebas de sanciones' },
      },
      {
        path: 'apelaciones',
        name: 'admin-apelaciones',
        component: ApelacionesAdmin,
        meta: { title: 'Apelaciones de sanciones' },
      },
      {
        path: 'anclaje',
        name: 'admin-anclaje',
        component: AnclajeAdmin,
        meta: { title: 'Anclaje en Bitcoin' },
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
        meta: { title: 'Comisión Temipora' },
      },
      {
        path: 'campanas/promociones',
        name: 'admin-promociones',
        component: PromocionesAdmin,
        meta: { title: 'Promociones' },
      },
      {
        path: 'campanas/promociones/descuentos',
        name: 'admin-promociones-descuentos',
        component: DescuentosPromocionesAdmin,
        meta: { title: 'Promociones - Descuentos aplicados' },
      },
      {
        path: 'campanas/sustituciones',
        name: 'admin-sustituciones',
        component: SustitucionesAdmin,
        meta: { title: 'Sustituciones de artistas' },
      },
      {
        path: 'campanas/sustituciones/historial',
        name: 'admin-sustituciones-historial',
        component: HistorialSustitucionesAdmin,
        meta: { title: 'Sustituciones - Historial' },
      },
      {
        path: 'ajustes/pedidos',
        name: 'pedidos-admin',
        component: PedidoConfigAdmin,
        meta: { title: 'Hitos y pedidos' },
      },
      {
        path: 'ajustes/bajas-cuenta',
        name: 'bajas-cuenta-admin',
        component: ConfiguracionBajaCuentaAdmin,
        meta: { title: 'Bajas de cuenta' },
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
        path: 'auditoria',
        name: 'admin-auditoria',
        component: AuditoriaAdmin,
        meta: { title: 'Auditoría' },
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
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.path !== from.path) return { top: 0 }
    return false
  },
})

const { subidaEnCurso, publicadasEnSesion, proteccionActiva } = useSubirImagen()
const { pedir } = useConfirmacionSalida()
const {
  hayCambios: hayCambiosConfig,
  mensaje: mensajeConfig,
  textoAlternativo: alternativaConfig,
  limpiar: limpiarConfig,
  ejecutarAlternativa: ejecutarAlternativaConfig,
} = useCambiosSinGuardar()

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
  if (to.path === from.path || !hayCambiosConfig.value) {
    return true
  }

  const permitir = await pedir(mensajeConfig.value, {
    titulo: '¿Salir sin guardar?',
    textoConfirmar: 'Salir sin guardar',
    textoCancelar: 'Seguir editando',
    textoAlternativo: alternativaConfig.value || undefined,
    alAlternar: alternativaConfig.value
      ? () => ejecutarAlternativaConfig()
      : undefined,
  })
  if (permitir) limpiarConfig()
  return permitir
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const rol = localStorage.getItem('rol')
  const tieneEnlace = typeof to.query.acceso === 'string'
    && to.query.acceso.length > 0

  const routeTitle = to.meta.title;

  if (typeof routeTitle === 'function') {
    document.title = routeTitle(to); 
  } else if (routeTitle) {
    document.title = routeTitle;
  } else {
    document.title = 'Temipora';
  }

  if (to.matched.some((r) => r.meta.requiresAuth) && !token && !tieneEnlace) {
    next({ name: 'login', query: { redirect: to.fullPath } })
    return
  }

  if (localStorage.getItem('restringido') === '1'
      && to.name !== 'cuenta-restringida') {
    next({ name: 'cuenta-restringida' })
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

const CLAVE_RECARGA_CHUNK = 'temipora-recarga-por-chunk'

router.onError((error, to) => {
  const mensaje = String(error?.message || '')
  if (!/dynamically imported module|Importing a module script failed|Failed to fetch/i.test(mensaje)) {
    return
  }
  let yaRecargo = false
  try {
    yaRecargo = sessionStorage.getItem(CLAVE_RECARGA_CHUNK) === '1'
  } catch {
    // sessionStorage no disponible: no intentamos recargar para evitar bucles.
    return
  }
  if (yaRecargo) {
    try {
      sessionStorage.removeItem(CLAVE_RECARGA_CHUNK)
    } catch {
      // sin almacenamiento, nada que limpiar.
    }
    return
  }
  try {
    sessionStorage.setItem(CLAVE_RECARGA_CHUNK, '1')
  } catch {
    return
  }
  window.location.assign(to?.fullPath || window.location.href)
})

router.afterEach(() => {
  try {
    sessionStorage.removeItem(CLAVE_RECARGA_CHUNK)
  } catch {
    // sin almacenamiento, nada que limpiar.
  }
})

const rutasPrecargadas = new Set()

/**
 * Descarga por adelantado el chunk de una ruta. Se usa al pasar el mouse o el
 * foco por un enlace para que el clic no espere la descarga por el túnel.
 */
export function precargarRuta(destino) {
  let resuelta
  try {
    resuelta = router.resolve(destino)
  } catch {
    return
  }
  for (const registro of resuelta.matched) {
    for (const componente of Object.values(registro.components || {})) {
      if (typeof componente !== 'function' || rutasPrecargadas.has(componente)) {
        continue
      }
      rutasPrecargadas.add(componente)
      componente().catch(() => {
        rutasPrecargadas.delete(componente)
      })
    }
  }
}

export default router