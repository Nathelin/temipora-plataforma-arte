import { formatearFechaHora } from './fechas'

export const ETIQUETAS_ACCION = {
  ETIQUETA_CREAR: 'Creó una etiqueta',
  ETIQUETA_MODIFICAR: 'Modificó una etiqueta',
  ETIQUETA_APROBAR: 'Aprobó una etiqueta',
  ETIQUETA_RECHAZAR: 'Rechazó una etiqueta',
  ETIQUETA_BAJA: 'Dio de baja una etiqueta',
  ETIQUETA_REACTIVAR: 'Reactivó una etiqueta',
  CATEGORIA_CREAR: 'Creó una categoría',
  CATEGORIA_MODIFICAR: 'Modificó una categoría',
  CATEGORIA_BAJA: 'Dio de baja una categoría',
  CATEGORIA_REACTIVAR: 'Reactivó una categoría',
  MEDIO_CREAR: 'Creó un medio',
  MEDIO_MODIFICAR: 'Modificó un medio',
  MEDIO_BAJA: 'Dio de baja un medio',
  MEDIO_REACTIVAR: 'Reactivó un medio',
  MATERIAL_CREAR: 'Creó un material',
  MATERIAL_MODIFICAR: 'Modificó un material',
  MATERIAL_BAJA: 'Dio de baja un material',
  MATERIAL_REACTIVAR: 'Reactivó un material',
  TIPO_ARTE_CREAR: 'Creó un tipo de arte',
  TIPO_ARTE_MODIFICAR: 'Modificó un tipo de arte',
  TIPO_ARTE_BAJA: 'Dio de baja un tipo de arte',
  TIPO_ARTE_REACTIVAR: 'Reactivó un tipo de arte',
  TIPO_PRODUCTO_CREAR: 'Creó un tipo de producto',
  TIPO_PRODUCTO_MODIFICAR: 'Modificó un tipo de producto',
  TIPO_PRODUCTO_BAJA: 'Dio de baja un tipo de producto',
  TIPO_PRODUCTO_REACTIVAR: 'Reactivó un tipo de producto',
  TIPO_SERVICIO_CREAR: 'Creó un tipo de servicio',
  TIPO_SERVICIO_MODIFICAR: 'Modificó un tipo de servicio',
  TIPO_SERVICIO_BAJA: 'Dio de baja un tipo de servicio',
  TIPO_SERVICIO_REACTIVAR: 'Reactivó un tipo de servicio',
  PAIS_CREAR: 'Creó un país',
  PAIS_MODIFICAR: 'Modificó un país',
  PAIS_BAJA: 'Dio de baja un país',
  PAIS_REACTIVAR: 'Reactivó un país',
  MONEDA_CREAR: 'Creó una moneda',
  MONEDA_MODIFICAR: 'Modificó una moneda',
  MONEDA_BAJA: 'Dio de baja una moneda',
  MONEDA_REACTIVAR: 'Reactivó una moneda',
  MONEDA_PREDETERMINADA: 'Marcó la moneda predeterminada',
  COMISION_ACTUALIZAR: 'Actualizó las comisiones',
  PRESUPUESTO_CONFIG_ACTUALIZAR: 'Actualizó la vigencia de presupuestos',
  PEDIDO_CONFIG_ACTUALIZAR: 'Actualizó las reglas de pedidos e hitos',
  MENSAJERIA_CONFIG_ACTUALIZAR: 'Actualizó la configuración de mensajería',
  RESENIA_CONFIG_ACTUALIZAR: 'Actualizó la configuración de reseñas',
  INACTIVIDAD_CONFIG_ACTUALIZAR: 'Actualizó las promociones por inactividad',
  REACTIVACION_EJECUTAR: 'Ejecutó las promociones por inactividad',
  COTIZACION_ACTUALIZAR: 'Actualizó la cotización',
  COTIZACION_REFRESCAR: 'Actualizó la cotización desde la API',
  RESENIA_CREAR: 'Dejó una reseña',
  RESENIA_EDITAR: 'Editó una reseña',
  RESENIA_ELIMINAR: 'Eliminó una reseña',
  RESENIA_REPORTAR: 'Reportó una reseña',
  RESENIA_OCULTAR: 'Ocultó una reseña',
  RESENIA_RESTAURAR: 'Restauró una reseña',
  RESENIA_DESCARTAR_REPORTES: 'Descartó reportes de reseña',
  TERMINOS_PUBLICAR: 'Publicó una versión de los términos',
  TERMINOS_ACEPTAR: 'Aceptó los términos de la plataforma',
  USUARIO_CAMBIAR_CORREO: 'Cambió su correo electrónico',
  USUARIO_CAMBIAR_CONTRASENIA: 'Cambió su contraseña',
  USUARIO_CAMBIAR_TELEFONO: 'Cambió su teléfono',
  USUARIO_QUITAR_TELEFONO: 'Quitó su teléfono',
  CUENTA_SANCIONAR: 'Inhabilitó una cuenta',
  CUENTA_REVERTIR_SANCION: 'Revirtió una sanción de cuenta',
  LOGIN_FALLIDO: 'Intento de inicio de sesión fallido',
  SERVICIO_CREAR: 'Creó un servicio',
  SERVICIO_MODIFICAR: 'Modificó un servicio',
  SERVICIO_PAUSAR: 'Pausó un servicio',
  SERVICIO_REACTIVAR: 'Reactivó un servicio',
  SERVICIO_ARCHIVAR: 'Dio de baja definitiva un servicio',
  SERVICIO_RESTAURAR: 'Restauró un servicio archivado',
  PRESUPUESTO_CREAR: 'Envió un presupuesto',
  PEDIDO_CREAR: 'Aceptó un presupuesto y creó un pedido',
  PEDIDO_CANCELAR: 'Canceló un pedido',
  PEDIDO_FINALIZAR: 'Finalizó un pedido',
  PEDIDO_PUBLICACION_AUTORIZAR: 'Autorizó la publicación de un pedido',
  PEDIDO_PUBLICACION_RETIRAR: 'Retiró la publicación de un pedido',
  PEDIDO_GALERIA_OCULTAR: 'Ocultó un pedido de su galería',
  PEDIDO_GALERIA_MOSTRAR: 'Mostró un pedido en su galería',
  PEDIDO_PROCESAR: 'Procesó hitos de pedidos',
  HITO_APROBAR: 'Aprobó un hito',
  HITO_REVISION: 'Solicitó una revisión de un hito',
  HITO_APROBAR_AUTOMATICO: 'Aprobó automáticamente un hito',
  HITO_VENCER: 'Venció el pago de un hito',
  ENTREGA_CREAR: 'Subió una entrega',
  PAGO_INICIAR: 'Inició un pago',
  PAGO_CONFIRMAR: 'Confirmó un pago',
  PAGO_ESTADO_ACTUALIZAR: 'Actualizó el estado de un pago',
  PAGO_RECHAZAR: 'Rechazó un pago',
  SOLICITUD_CREAR: 'Envió una solicitud de servicio',
  SOLICITUD_RECHAZAR: 'Rechazó una solicitud de servicio',
  SOLICITUD_CANCELAR: 'Canceló una solicitud de servicio',
  SOLICITUD_RETIRAR_PRESUPUESTO: 'Retiró un presupuesto',
  CRITERIO_CREAR: 'Creó un criterio de evaluación',
  CRITERIO_MODIFICAR: 'Modificó un criterio de evaluación',
  CRITERIO_BAJA: 'Dio de baja un criterio de evaluación',
  CRITERIO_REACTIVAR: 'Reactivó un criterio de evaluación',
  CRITERIO_OPCION_CREAR: 'Agregó una opción a un criterio',
  CRITERIO_OPCION_MODIFICAR: 'Modificó una opción de un criterio',
  CRITERIO_OPCION_BAJA: 'Dio de baja una opción de un criterio',
  CRITERIO_OPCION_REACTIVAR: 'Reactivó una opción de un criterio',
}

export const ETIQUETAS_ENTIDAD = {
  Etiqueta: 'Etiqueta',
  CategoriaEtiqueta: 'Categoría de etiqueta',
  Medio: 'Medio',
  Material: 'Material',
  TipoArte: 'Tipo de arte',
  TipoProducto: 'Tipo de producto',
  TipoServicio: 'Tipo de servicio',
  Pais: 'País',
  Moneda: 'Moneda',
  ConfiguracionComision: 'Comisión',
  ConfiguracionPresupuesto: 'Reglas de presupuestos',
  ConfiguracionPedido: 'Reglas de pedidos',
  ConfiguracionMensajeria: 'Mensajería',
  ConfiguracionResenia: 'Configuración de reseñas',
  ConfiguracionInactividad: 'Promociones',
  Resenia: 'Reseña',
  TerminosPlataforma: 'Términos de la plataforma',
  Servicio: 'Servicio',
  CriterioResenia: 'Criterio de evaluación',
  ConfiguracionCotizacion: 'Cotización',
  Pedido: 'Pedido',
  Presupuesto: 'Presupuesto',
  Solicitud: 'Solicitud de servicio',
  Pago: 'Pago',
  Entrega: 'Entrega',
  HitoPedido: 'Hito del pedido',
  ReporteResenia: 'Reporte de reseña',
  Usuario: 'Usuario',
  SancionCuenta: 'Sanción de cuenta',
}

export const ETIQUETAS_CATEGORIA = {
  COMERCIO: 'Comercio',
  USUARIOS: 'Usuarios y seguridad',
  MODERACION: 'Moderación',
  CATALOGO: 'Catálogo',
  CONFIGURACION: 'Configuración',
  LEGAL: 'Legal',
  SISTEMA: 'Sistema',
  OTROS: 'Otros',
}

export const ETIQUETAS_ORIGEN = {
  ADMIN: 'Administración',
  ARTISTA: 'Artista',
  CLIENTE: 'Cliente',
  SISTEMA: 'Sistema',
}

const fechaRelativa = new Intl.RelativeTimeFormat('es', {
  numeric: 'auto',
})

export function nombreAccion(codigo) {
  return ETIQUETAS_ACCION[codigo] || codigo
}

export function nombreEntidad(codigo) {
  if (!codigo) return '—'
  return ETIQUETAS_ENTIDAD[codigo] || codigo
}

export function nombreCategoria(codigo) {
  if (!codigo) return '—'
  return ETIQUETAS_CATEGORIA[codigo] || codigo
}

export function nombreOrigen(codigo) {
  if (!codigo) return '—'
  return ETIQUETAS_ORIGEN[codigo] || codigo
}

export function formatearFecha(valor) {
  return formatearFechaHora(valor) || '—'
}

export function formatearFechaRelativa(valor) {
  if (!valor) return '—'
  const fecha = new Date(valor)
  if (Number.isNaN(fecha.getTime())) return '—'
  const segundos = Math.round((fecha.getTime() - Date.now()) / 1000)
  const absolutos = Math.abs(segundos)
  if (absolutos < 60) return fechaRelativa.format(segundos, 'second')
  const minutos = Math.round(segundos / 60)
  if (Math.abs(minutos) < 60) return fechaRelativa.format(minutos, 'minute')
  const horas = Math.round(minutos / 60)
  if (Math.abs(horas) < 24) return fechaRelativa.format(horas, 'hour')
  const dias = Math.round(horas / 24)
  if (Math.abs(dias) < 30) return fechaRelativa.format(dias, 'day')
  const meses = Math.round(dias / 30)
  if (Math.abs(meses) < 12) return fechaRelativa.format(meses, 'month')
  return fechaRelativa.format(Math.round(meses / 12), 'year')
}

export function actorVisible(evento) {
  if (!evento.actorNombre) {
    return evento.origen === 'SISTEMA' ? 'Sistema' : 'Sin actor'
  }
  return `@${evento.actorNombre}`
}

export function resumenCambios(evento) {
  const cambios = evento.cambios || []
  if (!cambios.length) return 'Sin cambios de campos'
  if (cambios.length === 1) {
    const [cambio] = cambios
    return `${cambio.etiqueta}: ${cambio.anterior ?? '—'} → ${cambio.nuevo ?? '—'}`
  }
  return `${cambios.length} campos modificados`
}
