<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../servicios/api'
import { formatearMonto, formatearPrecio } from '../servicios/moneda'
import { descripcionHito } from '../servicios/pedidos'
import {
  MAX_PLAZO_POR_UNIDAD,
  UNIDADES_ENTREGA,
  aFechaISO,
  fechaEntregaEstimada,
  formatearFechaDDMMAAAA,
  gruposDeRespuestas,
  sumarPlazo,
  textoTiempoEntrega,
} from '../servicios/solicitudes'
import { useNotificaciones } from '../composables/useNotificaciones'
import ModalComoSeCalcula from '../components/ModalComoSeCalcula.vue'
import BurbujaInfo from '../components/BurbujaInfo.vue'

const route = useRoute()
const router = useRouter()
const { exito } = useNotificaciones()

const idSolicitud = computed(() => Number(route.params.idSolicitud))
const idActual = Number(localStorage.getItem('idUsuario'))
const fechaActual = new Date()
const hoy = aFechaISO(fechaActual)
const maxInicio = aFechaISO(sumarPlazo(hoy, 12, 'MESES'))

const solicitud = ref(null)
const comisionPorcentaje = ref(null)
const arancelMp = ref(null)
const configPedido = ref({
  maxHitos: 4,
  porcentajeMaxPrimerHito: 50,
  montoMinimoCuota: 1,
})
const vigenciaHoras = ref(72)
const cargando = ref(true)
const errorCarga = ref('')
const enviando = ref(false)
const errorEnvio = ref('')
const paso = ref(1)
const modalCalculoAbierto = ref(false)
const fechasVisibles = ref({
  fechaInicioEstimada: '',
  fechaVencimiento: '',
})
const fechasInvalidas = ref({
  fechaInicioEstimada: false,
  fechaVencimiento: false,
})

const pasos = [
  { numero: 1, titulo: 'Entrega' },
  { numero: 2, titulo: 'Dibujos' },
  { numero: 3, titulo: 'Hitos' },
  { numero: 4, titulo: 'Resumen' },
]

const form = ref({
  tiempoEntregaValor: 1,
  tiempoEntregaUnidad: 'SEMANAS',
  fechaInicioEstimada: '',
  fechaVencimiento: '',
  mensaje: '',
  items: [],
  hitos: [],
})

const esArtista = computed(
  () => solicitud.value?.artista?.idUsuario === idActual,
)
const estadoPresupuestable = computed(
  () => ['PENDIENTE', 'EN_LISTA_ESPERA'].includes(solicitud.value?.estado),
)
const puedePresupuestar = computed(
  () => esArtista.value
    && estadoPresupuestable.value
    && !solicitud.value?.presupuesto
    && solicitud.value?.puedePresupuestar === true,
)
const motivoBloqueo = computed(() => {
  if (cargando.value || !solicitud.value) return ''
  if (!esArtista.value) return 'No podés presupuestar esta solicitud.'
  if (solicitud.value.presupuesto) {
    return 'Ya enviaste un presupuesto para esta solicitud.'
  }
  if (!estadoPresupuestable.value) {
    return 'Esta solicitud ya no se puede presupuestar.'
  }
  return solicitud.value.motivoNoPresupuestable
    || 'No podés presupuestar esta solicitud todavía.'
})
const puedeEditarServicio = computed(
  () => solicitud.value?.estadoServicio !== 'ARCHIVADO',
)

const items = computed(() => form.value.items)
const moneda = computed(() => solicitud.value?.moneda || 'ARS')
const entregaTexto = computed(() => textoTiempoEntrega(
  form.value.tiempoEntregaValor,
  form.value.tiempoEntregaUnidad,
))
const maxPlazo = computed(
  () => MAX_PLAZO_POR_UNIDAD[form.value.tiempoEntregaUnidad] || 365,
)
const maxPlazoTexto = computed(() => textoTiempoEntrega(
  maxPlazo.value,
  form.value.tiempoEntregaUnidad,
))
const entregaSiPagaHoy = computed(() => fechaEntregaEstimada({
  fechaPago: hoy,
  fechaInicioEstimada: form.value.fechaInicioEstimada,
  valor: form.value.tiempoEntregaValor,
  unidad: form.value.tiempoEntregaUnidad,
}))
const entregaAlVencer = computed(() => {
  if (!form.value.fechaVencimiento) return null
  return fechaEntregaEstimada({
    fechaPago: form.value.fechaVencimiento,
    fechaInicioEstimada: form.value.fechaInicioEstimada,
    valor: form.value.tiempoEntregaValor,
    unidad: form.value.tiempoEntregaUnidad,
  })
})
const inicioEstimadoTexto = computed(() => form.value.fechaInicioEstimada
  ? formatearFechaDDMMAAAA(form.value.fechaInicioEstimada)
  : 'Al confirmarse el pago')
const entregaSiPagaHoyTexto = computed(
  () => formatearFechaDDMMAAAA(entregaSiPagaHoy.value),
)
const entregaAlVencerTexto = computed(
  () => formatearFechaDDMMAAAA(entregaAlVencer.value),
)

const vigenciaTexto = computed(() => {
  const horas = Number(vigenciaHoras.value) || 72
  if (horas % 24 === 0) {
    const dias = horas / 24
    return `${horas} horas (${dias} ${dias === 1 ? 'día' : 'días'})`
  }
  return `${horas} horas`
})

const vencimientoPorDefectoTexto = computed(() => {
  const horas = Number(vigenciaHoras.value) || 72
  const fecha = new Date(Date.now() + horas * 60 * 60 * 1000)
  const hora = fecha.toLocaleTimeString('es-AR', {
    hour: '2-digit',
    minute: '2-digit',
  })
  return `${formatearFechaDDMMAAAA(fecha)} a las ${hora}`
})

function formatear(valor) {
  return formatearPrecio(valor, moneda.value)
}

function actualizarFechaFormulario(campo, event) {
  const digitos = event.target.value.replace(/\D/g, '').slice(0, 8)
  const partes = []

  if (digitos.length > 0) partes.push(digitos.slice(0, 2))
  if (digitos.length > 2) partes.push(digitos.slice(2, 4))
  if (digitos.length > 4) partes.push(digitos.slice(4, 8))

  const visible = partes.join('/')
  event.target.value = visible
  fechasVisibles.value[campo] = visible

  if (digitos.length < 8) {
    form.value[campo] = ''
    fechasInvalidas.value[campo] = false
    return
  }

  const dia = Number(digitos.slice(0, 2))
  const mes = Number(digitos.slice(2, 4))
  const anio = Number(digitos.slice(4, 8))
  const fecha = new Date(anio, mes - 1, dia)
  const invalida = fecha.getFullYear() !== anio
    || fecha.getMonth() !== mes - 1
    || fecha.getDate() !== dia

  fechasInvalidas.value[campo] = invalida

  form.value[campo] = !invalida
    ? `${digitos.slice(4, 8)}-${digitos.slice(2, 4)}-${digitos.slice(0, 2)}`
    : ''
}

function baseItem() {
  const base = Number(solicitud.value?.precioBase ?? 0)
  return Number.isFinite(base) && base > 0 ? base : 0
}

function valorRecargo(recargo) {
  const valor = Number(String(recargo?.valor ?? '').replace(',', '.'))
  return Number.isFinite(valor) && valor > 0 ? valor : 0
}

function autoRecargosDe(item) {
  return (item?.derechos || []).map((d) => ({
    descripcion: d.nombre,
    tipo: 'PORCENTAJE',
    valor: Number(d.porcentajeExtra) || 0,
    automatico: true,
  }))
}

function subtotalItem(item) {
  let fijos = 0
  let porcentajes = 0
  const recargos = [...autoRecargosDe(item), ...(item.recargos || [])]
  for (const recargo of recargos) {
    const valor = valorRecargo(recargo)
    if (recargo.tipo === 'PORCENTAJE') porcentajes += valor
    else fijos += valor
  }
  const base = baseItem()
  return Math.round((base + fijos + (base * porcentajes) / 100) * 100) / 100
}

const totalBase = computed(
  () => Math.round(items.value.length * baseItem() * 100) / 100,
)
const totalFinal = computed(
  () => Math.round(items.value.reduce((total, item) => total + subtotalItem(item), 0) * 100) / 100,
)
const totalRecargos = computed(
  () => Math.round((totalFinal.value - totalBase.value) * 100) / 100,
)
const comisionMonto = computed(() => {
  if (comisionPorcentaje.value == null) return null
  return Math.round(totalFinal.value * comisionPorcentaje.value) / 100
})
const arancelMpEstimado = computed(() => {
  if (arancelMp.value == null) return null
  return Math.round(totalFinal.value * arancelMp.value) / 100
})
const netoSinArancel = computed(() => {
  if (comisionMonto.value == null) return null
  return Math.round((totalFinal.value - comisionMonto.value) * 100) / 100
})
const netoArtista = computed(() => {
  if (netoSinArancel.value == null) return null
  if (arancelMpEstimado.value == null) {
    return netoSinArancel.value
  }
  return Math.round((netoSinArancel.value - arancelMpEstimado.value) * 100) / 100
})
const maxHitos = computed(() => Number(configPedido.value?.maxHitos) || 4)
const porcentajeMaxPrimerHito = computed(
  () => Number(configPedido.value?.porcentajeMaxPrimerHito) || 50,
)
const montoMinimoCuota = computed(
  () => Number(configPedido.value?.montoMinimoCuota) || 0,
)
const totalPorcentajeHitos = computed(
  () => Math.round(form.value.hitos.reduce(
    (total, hito) => total + (Number(hito.porcentaje) || 0),
    0,
  ) * 100) / 100,
)
const hitosPreview = computed(() => {
  const hitos = form.value.hitos
  const cantidad = hitos.length
  let acumulado = 0
  return hitos.map((hito, indice) => {
    const porcentaje = Number(hito.porcentaje) || 0
    const ultimo = indice === cantidad - 1
    const monto = ultimo
      ? Math.round((totalFinal.value - acumulado) * 100) / 100
      : Math.round(totalFinal.value * porcentaje) / 100
    acumulado = Math.round((acumulado + Math.max(0, monto)) * 100) / 100
    return {
      ...hito,
      monto: Math.max(0, monto),
      tipo: tipoHito(indice, cantidad),
    }
  })
})

function tipoHito(indice, cantidad) {
  if (cantidad === 1) return 'SENA'
  if (indice === 0) return 'SENA'
  if (indice === cantidad - 1) return 'ENTREGA'
  return 'CUOTA'
}

function etiquetaTipoHito(tipo, cantidad) {
  if (tipo === 'SENA') return cantidad === 1 ? 'Pago único' : 'Seña'
  if (tipo === 'ENTREGA') return 'Entrega final'
  return 'Cuota'
}

function hitosPorDefecto() {
  if (maxHitos.value < 2) {
    return [{
      nombre: 'Pago único',
      descripcion: '',
      porcentaje: 100,
      requiereEntrega: true,
    }]
  }
  const primera = Math.min(50, porcentajeMaxPrimerHito.value)
  return [
    {
      nombre: 'Seña',
      descripcion: '',
      porcentaje: primera,
      requiereEntrega: true,
    },
    {
      nombre: 'Entrega final',
      descripcion: '',
      porcentaje: Math.round((100 - primera) * 100) / 100,
      requiereEntrega: true,
    },
  ]
}

function usarPagoUnico() {
  form.value.hitos = [
    {
      nombre: 'Pago único',
      descripcion: '',
      porcentaje: 100,
      requiereEntrega: true,
    },
  ]
}

function usarSenaEntrega() {
  form.value.hitos = hitosPorDefecto()
}

function usarSkeb() {
  const primera = Math.min(50, porcentajeMaxPrimerHito.value)
  form.value.hitos = [
    {
      nombre: 'Seña',
      descripcion: '',
      porcentaje: primera,
      requiereEntrega: false,
    },
    {
      nombre: 'Entrega final',
      descripcion: '',
      porcentaje: Math.round((100 - primera) * 100) / 100,
      requiereEntrega: true,
    },
  ]
}

function usarTresEtapas() {
  if (maxHitos.value < 3) {
    usarSenaEntrega()
    return
  }
  const primera = Math.min(30, porcentajeMaxPrimerHito.value)
  const resto = Math.round((100 - primera) * 100) / 100
  const cuota = Math.round((resto / 2) * 100) / 100
  form.value.hitos = [
    {
      nombre: 'Seña',
      descripcion: '',
      porcentaje: primera,
      requiereEntrega: true,
    },
    {
      nombre: 'Cuota 2',
      descripcion: '',
      porcentaje: cuota,
      requiereEntrega: true,
    },
    {
      nombre: 'Entrega final',
      descripcion: '',
      porcentaje: Math.round((resto - cuota) * 100) / 100,
      requiereEntrega: true,
    },
  ]
}

function agregarHito() {
  if (form.value.hitos.length >= maxHitos.value) return
  form.value.hitos.push({
    nombre: `Cuota ${form.value.hitos.length + 1}`,
    descripcion: '',
    porcentaje: 0,
    requiereEntrega: true,
  })
}

function quitarHito(indice) {
  if (form.value.hitos.length <= 1) return
  form.value.hitos.splice(indice, 1)
}

function agregarRecargo(item) {
  if (item.recargos.length >= 20) return
  item.recargos.push({
    descripcion: '',
    tipo: 'PORCENTAJE',
    valor: '',
  })
}

function quitarRecargo(item, indice) {
  item.recargos.splice(indice, 1)
}

function resumenRespuestas(grupo) {
  if (grupo.codigoTipo === 'SUBIR_IMAGEN') {
    const cantidad = grupo.valores.length
    return `${cantidad} ${cantidad === 1 ? 'imagen adjunta' : 'imágenes adjuntas'}`
  }
  return grupo.valores.join(', ')
}

async function cargar() {
  cargando.value = true
  errorCarga.value = ''
  try {
    const [
      res,
      resComision,
      resConfig,
      resConfigPresupuesto,
      resArancel,
    ] = await Promise.all([
      api.get(`/solicitudes-servicio/${idSolicitud.value}`),
      api.get('/comisiones/vigentes').catch(() => null),
      api.get('/configuracion-pedido').catch(() => null),
      api.get('/presupuestos/configuracion-vencimiento').catch(() => null),
      api.get('/pagos/arancel-estimado').catch(() => null),
    ])
    solicitud.value = res.data
    const porcentaje = Number(resComision?.data?.porcentajeServicio)
    if (Number.isFinite(porcentaje)) {
      comisionPorcentaje.value = porcentaje
    }
    const porcentajeArancel = Number(resArancel?.data?.porcentajeTotal)
    if (Number.isFinite(porcentajeArancel)) {
      arancelMp.value = porcentajeArancel
    }
    if (resConfig?.data) {
      configPedido.value = {
        maxHitos: Number(resConfig.data.maxHitos) || 4,
        porcentajeMaxPrimerHito:
          Number(resConfig.data.porcentajeMaxPrimerHito) || 50,
        montoMinimoCuota: Number(resConfig.data.montoMinimoCuota) || 0,
      }
    }
    const horas = Number(resConfigPresupuesto?.data?.vigenciaHoras)
    if (Number.isFinite(horas) && horas > 0) {
      vigenciaHoras.value = horas
    }

    if (!puedePresupuestar.value) return

    form.value.items = (res.data.items || []).map((item) => ({
      idItemSolicitud: item.idItemSolicitud,
      nombre: item.nombre,
      nota: '',
      recargos: [],
      derechos: item.derechos || [],
      respuestas: gruposDeRespuestas(item),
    }))
    form.value.hitos = hitosPorDefecto()
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    if (e.response?.status === 403 || e.response?.status === 404) {
      errorCarga.value = 'No podés presupuestar esta solicitud o ya no existe.'
    } else {
      errorCarga.value = e.response?.data?.message || 'No se pudo cargar la solicitud.'
    }
  } finally {
    cargando.value = false
  }
}

function validarPaso1() {
  errorEnvio.value = ''
  const valor = Number(form.value.tiempoEntregaValor)
  if (!Number.isFinite(valor) || valor < 1) {
    errorEnvio.value = 'Ingresá un tiempo máximo de entrega de al menos 1.'
    return false
  }
  if (!form.value.tiempoEntregaUnidad) {
    errorEnvio.value = 'Elegí la unidad del tiempo de entrega.'
    return false
  }
  if (valor > maxPlazo.value) {
    errorEnvio.value = `El plazo máximo es de un año (${maxPlazoTexto.value}).`
    return false
  }
  if (fechasInvalidas.value.fechaInicioEstimada || fechasInvalidas.value.fechaVencimiento) {
    errorEnvio.value = 'Ingresá fechas válidas con el formato DD/MM/AAAA.'
    return false
  }
  if (form.value.fechaInicioEstimada) {
    if (form.value.fechaInicioEstimada < hoy) {
      errorEnvio.value = 'La fecha de inicio estimada no puede ser anterior a hoy.'
      return false
    }
    if (form.value.fechaInicioEstimada > maxInicio) {
      errorEnvio.value = 'La fecha de inicio estimada no puede superar un año desde hoy.'
      return false
    }
  }
  if (form.value.fechaVencimiento) {
    const fecha = new Date(`${form.value.fechaVencimiento}T23:59:59`)
    if (Number.isNaN(fecha.getTime()) || fecha <= new Date()) {
      errorEnvio.value = 'La fecha de vencimiento debe ser futura.'
      return false
    }
  }
  return true
}

function validarPaso2() {
  errorEnvio.value = ''
  for (const item of items.value) {
    if ((item.nota || '').length > 250) {
      errorEnvio.value = `La nota de "${item.nombre}" no puede superar los 250 caracteres.`
      return false
    }
    for (const recargo of item.recargos) {
      if (!recargo.descripcion?.trim()) {
        errorEnvio.value = `Completá la descripción del recargo de "${item.nombre}".`
        return false
      }
      if (valorRecargo(recargo) <= 0) {
        errorEnvio.value = `Ingresá un valor mayor a cero en el recargo de "${item.nombre}".`
        return false
      }
      if (recargo.tipo === 'PORCENTAJE' && valorRecargo(recargo) > 1000) {
        errorEnvio.value = 'El porcentaje de un recargo no puede superar el 1000%.'
        return false
      }
    }
  }
  return true
}

function validarPasoHitos() {
  errorEnvio.value = ''
  const hitos = form.value.hitos
  if (!hitos.length) {
    errorEnvio.value = 'Definí al menos un hito para el plan de pagos.'
    return false
  }
  if (hitos.length > maxHitos.value) {
    errorEnvio.value = `Podés definir como máximo ${maxHitos.value} hitos.`
    return false
  }
  for (const hito of hitos) {
    if ((hito.descripcion || '').length > 250) {
      errorEnvio.value = 'El alcance de un hito no puede superar los 250 caracteres.'
      return false
    }
    const porcentaje = Number(hito.porcentaje)
    if (!Number.isFinite(porcentaje) || porcentaje <= 0 || porcentaje > 100) {
      errorEnvio.value = 'Cada hito debe tener un porcentaje mayor a 0 y hasta 100%.'
      return false
    }
    if (Math.round(porcentaje * 100) !== porcentaje * 100) {
      errorEnvio.value = 'Los porcentajes admiten hasta 2 decimales.'
      return false
    }
  }
  if (Math.abs(totalPorcentajeHitos.value - 100) > 0.001) {
    errorEnvio.value = `Los porcentajes de los hitos deben sumar 100%. Ahora suman ${totalPorcentajeHitos.value}%.`
    return false
  }
  if (hitos.length > 1
    && Number(hitos[0].porcentaje) > porcentajeMaxPrimerHito.value) {
    errorEnvio.value = `La primera cuota no puede superar el ${porcentajeMaxPrimerHito.value}%.`
    return false
  }
  return true
}

function validarPaso3() {
  errorEnvio.value = ''
  if ((form.value.mensaje || '').length > 500) {
    errorEnvio.value = 'El mensaje no puede superar los 500 caracteres.'
    return false
  }
  return true
}

function siguiente() {
  if (paso.value === 1 && !validarPaso1()) return
  if (paso.value === 2 && !validarPaso2()) return
  if (paso.value === 3 && !validarPasoHitos()) return
  paso.value = Math.min(4, paso.value + 1)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function anterior() {
  errorEnvio.value = ''
  paso.value = Math.max(1, paso.value - 1)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function volver() {
  router.push({
    name: 'panel-solicitud-detalle',
    params: { idSolicitud: idSolicitud.value },
  })
}

async function enviar() {
  if (enviando.value) return
  if (!validarPaso1() || !validarPaso2() || !validarPasoHitos() || !validarPaso3()) return

  enviando.value = true
  errorEnvio.value = ''
  try {
    const payload = {
      tiempoEntregaValor: Number(form.value.tiempoEntregaValor),
      tiempoEntregaUnidad: form.value.tiempoEntregaUnidad,
      fechaInicioEstimada: form.value.fechaInicioEstimada || null,
      fechaVencimiento: form.value.fechaVencimiento || null,
      mensaje: form.value.mensaje?.trim() || null,
      items: items.value.map((item) => ({
        idItemSolicitud: item.idItemSolicitud,
        nota: item.nota?.trim() || null,
        recargos: (item.recargos || []).map((recargo) => ({
          descripcion: recargo.descripcion.trim(),
          tipo: recargo.tipo,
          valor: valorRecargo(recargo),
        })),
      })),
      hitos: form.value.hitos.map((hito) => ({
        nombre: hito.nombre?.trim() || null,
        descripcion: hito.descripcion?.trim() || null,
        porcentaje: Number(hito.porcentaje),
        requiereEntrega: hito.requiereEntrega !== false,
      })),
    }

    await api.post(`/solicitudes-servicio/${idSolicitud.value}/presupuesto`, payload)
    exito('Presupuesto enviado. El cliente ya recibió la notificación.')
    router.push({
      name: 'panel-solicitud-detalle',
      params: { idSolicitud: idSolicitud.value },
    })
  } catch (e) {
    if (e.response?.status === 401) {
      router.push({ name: 'login' })
      return
    }
    errorEnvio.value = e.response?.data?.message || 'No se pudo enviar el presupuesto.'
  } finally {
    enviando.value = false
  }
}

onMounted(cargar)
</script>

<template>
  <div>
    <button
      type="button"
      class="text-sm font-medium text-cafe/70 transition hover:text-coral"
      @click="volver"
    >
      ← Volver a la solicitud
    </button>

    <div
      v-if="cargando"
      class="mt-8 rounded-2xl border border-rosa/50 bg-blanco px-6 py-16 text-center text-sm text-cafe/60"
    >
      Cargando solicitud...
    </div>

    <div
      v-else-if="errorCarga"
      class="mt-8 rounded-2xl border border-rosa/50 bg-blanco px-6 py-16 text-center"
    >
      <p class="text-sm text-cafe/70">{{ errorCarga }}</p>
      <div class="mt-4 flex flex-wrap justify-center gap-2">
        <button
          type="button"
          class="rounded-full border border-rosa bg-blanco px-4 py-2 text-sm font-semibold text-granate transition hover:bg-rosa/20"
          @click="cargar"
        >
          Reintentar
        </button>
        <button
          type="button"
          class="rounded-full bg-coral px-4 py-2 text-sm font-semibold text-white transition hover:bg-rojizo"
          @click="volver"
        >
          Volver a la solicitud
        </button>
      </div>
    </div>

    <div
      v-else-if="!puedePresupuestar"
      class="mt-8 rounded-2xl border border-durazno bg-durazno/25 px-6 py-12 text-center"
    >
      <p class="text-lg font-semibold text-granate">No podés presupuestar esta solicitud</p>
      <p class="mx-auto mt-2 max-w-md text-sm leading-relaxed text-cafe/70">
        {{ motivoBloqueo }}
      </p>
      <div class="mt-5 flex flex-wrap justify-center gap-2">
        <router-link
          v-if="puedeEditarServicio"
          :to="{ name: 'servicio-editar', params: { id: solicitud.idServicio } }"
          class="rounded-full bg-coral px-4 py-2 text-sm font-semibold text-white transition hover:bg-rojizo"
        >
          Editar servicio para ampliar cupos
        </router-link>
        <button
          type="button"
          class="rounded-full border border-rosa bg-blanco px-4 py-2 text-sm font-semibold text-granate transition hover:bg-rosa/20"
          @click="volver"
        >
          Volver a la solicitud
        </button>
      </div>
    </div>

    <template v-else>
      <header class="mt-6">
        <p class="text-sm font-semibold uppercase tracking-[0.18em] text-coral">Enviar presupuesto</p>
        <h1 class="mt-1 font-display text-3xl font-extrabold tracking-[-0.02em] text-granate sm:text-4xl">
          Solicitud #{{ idSolicitud }}
        </h1>
        <p class="mt-2 text-sm text-cafe/65">
          {{ solicitud.nombreServicio || 'Servicio' }}
          <span aria-hidden="true"> · </span>
          {{ solicitud.cantidadDibujos }} {{ solicitud.cantidadDibujos === 1 ? 'dibujo' : 'dibujos' }}
          <span aria-hidden="true"> · </span>
          Precio base {{ formatear(solicitud.precioBase) }} por dibujo
        </p>
      </header>

      <ol class="mt-6 flex items-center gap-2" aria-label="Pasos del presupuesto">
        <li
          v-for="p in pasos"
          :key="p.numero"
          class="flex min-w-0 flex-1 items-center gap-2"
        >
          <span
            class="flex h-7 w-7 shrink-0 items-center justify-center rounded-full border text-xs font-semibold"
            :class="paso >= p.numero
              ? 'border-coral bg-coral text-white'
              : 'border-rosa bg-blanco text-cafe/50'"
          >
            {{ p.numero }}
          </span>
          <span
            class="hidden truncate text-xs sm:block"
            :class="paso === p.numero ? 'font-semibold text-granate' : 'text-cafe/50'"
          >
            {{ p.titulo }}
          </span>
          <span
            v-if="p.numero < pasos.length"
            class="h-px flex-1 bg-rosa/60"
            aria-hidden="true"
          ></span>
        </li>
      </ol>

      <form class="mt-6 space-y-5" @submit.prevent="enviar">
        <section
          v-if="paso === 1"
          class="rounded-2xl border border-rosa/50 bg-blanco p-5 sm:p-6"
        >
          <h2 class="text-xl font-semibold text-granate">Tiempo de entrega</h2>
          <p class="mt-1 text-sm text-cafe/60">
            Definí cuándo podés empezar y cuánto vas a tardar como máximo. La fecha de
            entrega se recalcula al confirmarse el pago.
          </p>

          <div class="mt-5">
            <label for="inicio-estimado" class="block text-sm text-cafe">
              Inicio estimado <span class="text-cafe/40">(opcional)</span>
            </label>
            <input
              id="inicio-estimado"
              :value="fechasVisibles.fechaInicioEstimada"
              type="text"
              inputmode="numeric"
              autocomplete="off"
              maxlength="10"
              placeholder="DD/MM/AAAA"
              class="mt-2 block w-full rounded-lg border-0 px-3 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral sm:max-w-xs"
              @input="actualizarFechaFormulario('fechaInicioEstimada', $event)"
            />
            <p class="mt-2 text-xs leading-relaxed text-cafe/50">
              Si no elegís una fecha, el plazo se cuenta desde el pago. Podés indicar
              hasta un año a futuro.
            </p>
          </div>

          <div class="mt-5 grid gap-4 sm:grid-cols-2">
            <div>
              <label for="tiempo-entrega" class="block text-sm text-cafe">
                Plazo máximo <span class="text-rojizo">*</span>
              </label>
              <input
                id="tiempo-entrega"
                v-model.number="form.tiempoEntregaValor"
                type="number"
                min="1"
                :max="maxPlazo"
                step="1"
                required
                class="mt-2 block w-full rounded-lg border-0 px-3 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              />
            </div>

            <div>
              <label for="unidad-entrega" class="block text-sm text-cafe">
                Unidad <span class="text-rojizo">*</span>
              </label>
              <select
                id="unidad-entrega"
                v-model="form.tiempoEntregaUnidad"
                required
                class="mt-2 block w-full rounded-lg border-0 bg-blanco px-3 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              >
                <option
                  v-for="(info, codigo) in UNIDADES_ENTREGA"
                  :key="codigo"
                  :value="codigo"
                >
                  {{ info.plural }}
                </option>
              </select>
            </div>
          </div>

          <p class="mt-3 text-xs text-cafe/50">
            Máximo un año: {{ maxPlazoTexto }}.
          </p>

          <div class="mt-6 border-t border-rosa/40 pt-5">
            <label for="fecha-vencimiento" class="block text-sm text-cafe">
              Oferta válida hasta <span class="text-cafe/40">(opcional)</span>
            </label>
            <input
              id="fecha-vencimiento"
              :value="fechasVisibles.fechaVencimiento"
              type="text"
              inputmode="numeric"
              autocomplete="off"
              maxlength="10"
              placeholder="DD/MM/AAAA"
              class="mt-2 block w-full rounded-lg border-0 px-3 py-2.5 text-base text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral sm:max-w-xs"
              @input="actualizarFechaFormulario('fechaVencimiento', $event)"
            />
            <p class="mt-2 text-xs leading-relaxed text-cafe/50">
              Si no elegís una fecha, se usa la vigencia por defecto del sistema:
              <span class="font-medium text-cafe">{{ vigenciaTexto }}</span>.
              <span v-if="!form.fechaVencimiento" class="mt-0.5 block">
                La oferta vencería el
                <span class="font-medium text-cafe">{{ vencimientoPorDefectoTexto }}</span>.
              </span>
            </p>
          </div>

          <div class="mt-6 rounded-xl border border-coral/25 bg-coral/5 p-4">
            <p class="text-sm font-semibold text-granate">Fecha de entrega estimada</p>
            <dl class="mt-3 space-y-2 text-sm">
              <div class="flex items-start justify-between gap-4">
                <dt class="text-cafe/55">Inicio estimado</dt>
                <dd class="text-right font-medium text-cafe">{{ inicioEstimadoTexto }}</dd>
              </div>
              <div class="flex items-start justify-between gap-4">
                <dt class="text-cafe/55">Si el cliente paga hoy</dt>
                <dd class="text-right font-semibold text-granate">{{ entregaSiPagaHoyTexto }}</dd>
              </div>
              <div v-if="form.fechaVencimiento" class="flex items-start justify-between gap-4">
                <dt class="text-cafe/55">Si paga al vencer la oferta</dt>
                <dd class="text-right font-semibold text-granate">{{ entregaAlVencerTexto }}</dd>
              </div>
            </dl>
            <p class="mt-3 text-xs leading-relaxed text-cafe/50">
              La fecha se recalcula al confirmarse el pago de la seña. Si el pago llega
              después del inicio estimado, el plazo se cuenta desde el pago. Si más
              adelante una cuota se paga tarde, la entrega se corre esos días; el atraso
              del artista no mueve la fecha.
            </p>
          </div>
        </section>

        <section v-else-if="paso === 2" class="space-y-4">
          <div class="rounded-2xl border border-rosa/50 bg-blanco p-5 sm:p-6">
            <h2 class="text-xl font-semibold text-granate">Recargos por dibujo</h2>
            <p class="mt-1 text-sm text-cafe/60">
              Podés agregar recargos fijos o porcentuales a cada dibujo. La base es
              {{ formatear(solicitud.precioBase) }} por dibujo.
            </p>
          </div>

          <article
            v-for="item in items"
            :key="item.idItemSolicitud"
            class="rounded-2xl border border-rosa/50 bg-blanco p-5 sm:p-6"
          >
            <div class="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/45">
                  Dibujo
                </p>
                <h3 class="mt-1 text-lg font-semibold text-granate">{{ item.nombre }}</h3>
              </div>
              <div class="text-right">
                <p class="text-xs text-cafe/50">Subtotal</p>
                <p class="text-lg font-semibold text-granate">{{ formatear(subtotalItem(item)) }}</p>
              </div>
            </div>

            <p class="mt-2 text-xs text-cafe/50">
              Base {{ formatear(baseItem()) }}
            </p>

            <div
              v-if="autoRecargosDe(item).length"
              class="mt-4 rounded-xl border border-coral/30 bg-coral/5 px-3 py-2.5"
            >
              <p class="text-xs font-semibold text-granate">Derechos de uso solicitados</p>
              <ul class="mt-2 space-y-1">
                <li
                  v-for="(recargo, autoIndice) in autoRecargosDe(item)"
                  :key="`auto-${item.idItemSolicitud}-${autoIndice}`"
                  class="flex items-center justify-between gap-3 text-xs"
                >
                  <span class="min-w-0 text-cafe">{{ recargo.descripcion }}</span>
                  <span class="shrink-0 font-semibold text-granate">+{{ recargo.valor }}%</span>
                </li>
              </ul>
              <p class="mt-2 text-xs leading-relaxed text-cafe/55">
                Estos recargos se agregan automáticamente sobre el precio base y no se pueden quitar.
              </p>
            </div>

            <details
              v-if="item.respuestas.length"
              class="mt-4 rounded-xl border border-rosa/40 bg-rosa/5 px-3 py-2"
            >
              <summary class="cursor-pointer text-xs font-semibold text-coral">
                Ver respuestas del cliente
              </summary>
              <dl class="mt-3 space-y-3 text-xs">
                <div v-for="grupo in item.respuestas" :key="grupo.clave">
                  <dt class="font-semibold text-cafe/70">{{ grupo.enunciado }}</dt>
                  <dd class="mt-0.5 whitespace-pre-line leading-relaxed text-cafe/60">
                    {{ resumenRespuestas(grupo) }}
                  </dd>
                </div>
              </dl>
            </details>

            <div class="mt-5 space-y-3">
              <div
                v-for="(recargo, indice) in item.recargos"
                :key="`${item.idItemSolicitud}-recargo-${indice}`"
                class="grid gap-2 rounded-xl border border-rosa/40 bg-rosa/5 p-3 sm:grid-cols-[minmax(0,1fr)_10rem_8rem_auto]"
              >
                <div>
                  <label class="block text-xs text-cafe/60">Descripción</label>
                  <input
                    v-model="recargo.descripcion"
                    type="text"
                    maxlength="100"
                    placeholder="Ej. Personaje extra"
                    class="mt-1 block w-full rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
                  />
                </div>

                <div>
                  <label class="block text-xs text-cafe/60">Tipo</label>
                  <select
                    v-model="recargo.tipo"
                    class="mt-1 block w-full rounded-lg border-0 bg-blanco px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
                  >
                    <option value="PORCENTAJE">Porcentaje (%)</option>
                    <option value="FIJO">Monto fijo</option>
                  </select>
                </div>

                <div>
                  <label class="block text-xs text-cafe/60">
                    {{ recargo.tipo === 'PORCENTAJE' ? 'Porcentaje' : 'Monto' }}
                  </label>
                  <input
                    v-model="recargo.valor"
                    type="text"
                    inputmode="decimal"
                    placeholder="0"
                    class="mt-1 block w-full rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
                  />
                </div>

                <div class="flex items-end">
                  <button
                    type="button"
                    class="inline-flex min-h-9 items-center justify-center rounded-full border border-rojizo/30 px-3 text-xs font-semibold text-rojizo transition hover:bg-rojizo/10"
                    @click="quitarRecargo(item, indice)"
                  >
                    Quitar
                  </button>
                </div>
              </div>

              <button
                type="button"
                :disabled="item.recargos.length >= 20"
                class="rounded-full border border-coral/40 bg-coral/5 px-4 py-2 text-xs font-semibold text-coral transition hover:bg-coral/10 disabled:cursor-not-allowed disabled:opacity-50"
                @click="agregarRecargo(item)"
              >
                + Agregar recargo
              </button>
            </div>

            <div class="mt-5">
              <label :for="`nota-${item.idItemSolicitud}`" class="block text-sm text-cafe">
                Nota para el cliente <span class="text-cafe/40">(opcional)</span>
              </label>
              <textarea
                :id="`nota-${item.idItemSolicitud}`"
                v-model="item.nota"
                rows="2"
                maxlength="250"
                placeholder="Ej. Incluye dos personajes y fondo simple."
                class="mt-2 block w-full rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              ></textarea>
              <p class="mt-1 text-right text-[11px] text-cafe/40">
                {{ (item.nota || '').length }}/250
              </p>
            </div>
          </article>
        </section>

        <section v-else-if="paso === 3" class="space-y-4">
          <div class="rounded-2xl border border-rosa/50 bg-blanco p-5 sm:p-6">
            <h2 class="text-xl font-semibold text-granate">Plan de pagos</h2>
            <p class="mt-1 text-sm text-cafe/60">
              Podés cobrar todo junto o dividir el trabajo en hitos. La primera cuota la paga
              el cliente al aceptar el presupuesto; las siguientes se habilitan al aprobar
              cada entregable.
            </p>

            <div class="mt-4 flex flex-wrap gap-2">
              <button
                type="button"
                class="rounded-full border border-coral/40 bg-coral/5 px-4 py-2 text-xs font-semibold text-coral transition hover:bg-coral/10"
                @click="usarPagoUnico"
              >
                100% al inicio
              </button>
              <button
                type="button"
                class="rounded-full border border-coral/40 bg-coral/5 px-4 py-2 text-xs font-semibold text-coral transition hover:bg-coral/10"
                @click="usarSenaEntrega"
              >
                Seña y entrega
              </button>
              <button
                type="button"
                class="rounded-full border border-coral/40 bg-coral/5 px-4 py-2 text-xs font-semibold text-coral transition hover:bg-coral/10"
                @click="usarSkeb"
              >
                Skeb (sin revisiones)
              </button>
              <button
                type="button"
                class="rounded-full border border-coral/40 bg-coral/5 px-4 py-2 text-xs font-semibold text-coral transition hover:bg-coral/10"
                @click="usarTresEtapas"
              >
                Tres etapas
              </button>
            </div>
          </div>

          <article
            v-for="(hito, indice) in hitosPreview"
            :key="indice"
            class="rounded-2xl border border-rosa/50 bg-blanco p-5 sm:p-6"
          >
            <div class="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p class="text-xs font-semibold uppercase tracking-[0.14em] text-cafe/45">
                  {{ etiquetaTipoHito(hito.tipo, form.hitos.length) }}
                </p>
                <p class="mt-1 text-lg font-semibold text-granate">{{ formatear(hito.monto) }}</p>
              </div>
              <button
                v-if="form.hitos.length > 1"
                type="button"
                class="inline-flex min-h-9 items-center justify-center rounded-full border border-rojizo/30 px-3 text-xs font-semibold text-rojizo transition hover:bg-rojizo/10"
                @click="quitarHito(indice)"
              >
                Quitar
              </button>
            </div>

            <div class="mt-4 grid gap-3 sm:grid-cols-[minmax(0,1fr)_8rem]">
              <div>
                <label :for="`hito-nombre-${indice}`" class="block text-xs text-cafe/60">
                  Nombre
                </label>
                <input
                  :id="`hito-nombre-${indice}`"
                  v-model="form.hitos[indice].nombre"
                  type="text"
                  maxlength="60"
                  placeholder="Ej. Boceto"
                  class="mt-1 block w-full rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
                />
              </div>
              <div>
                <label :for="`hito-porcentaje-${indice}`" class="block text-xs text-cafe/60">
                  Porcentaje
                </label>
                <input
                  :id="`hito-porcentaje-${indice}`"
                  v-model.number="form.hitos[indice].porcentaje"
                  type="number"
                  min="0.01"
                  max="100"
                  step="0.01"
                  class="mt-1 block w-full rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
                />
              </div>
            </div>

            <div class="mt-3">
              <label :for="`hito-alcance-${indice}`" class="block text-xs text-cafe/60">
                Alcance <span class="text-cafe/40">(opcional)</span>
              </label>
              <textarea
                :id="`hito-alcance-${indice}`"
                v-model="form.hitos[indice].descripcion"
                rows="2"
                maxlength="250"
                placeholder="Ej. Bocetos de los 3 dibujos, o los dibujos 1 y 2 terminados."
                class="mt-1 block w-full rounded-lg border-0 px-3 py-2 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
              ></textarea>
              <p class="mt-1 text-right text-[11px] text-cafe/40">
                {{ (form.hitos[indice].descripcion || '').length }}/250
              </p>
            </div>

            <p class="mt-2 text-xs text-cafe/50">
              {{ descripcionHito(hito) }}
            </p>

            <label
              v-if="hito.tipo === 'SENA' && form.hitos.length > 1"
              class="mt-3 flex items-start gap-2 rounded-lg bg-rosa/10 px-3 py-2 text-xs text-cafe/70"
            >
              <input
                v-model="form.hitos[indice].requiereEntrega"
                type="checkbox"
                class="mt-0.5 h-4 w-4 rounded border-rosa text-coral focus:ring-coral"
              />
              <span>
                Pedir aprobación de un avance antes de la siguiente cuota.
                Desmarcalo para encargos tipo skeb.
              </span>
            </label>
            <p
              v-else-if="hito.tipo === 'SENA'"
              class="mt-2 text-xs text-cafe/45"
            >
              El pago único incluye la entrega final, que el cliente aprueba.
            </p>
            <p v-else class="mt-2 text-xs text-cafe/45">
              Requiere un entregable que el cliente aprueba.
            </p>
          </article>

          <div class="rounded-2xl border border-rosa/50 bg-blanco p-5 sm:p-6">
            <div class="flex flex-wrap items-center justify-between gap-3">
              <button
                type="button"
                :disabled="form.hitos.length >= maxHitos"
                class="rounded-full border border-coral/40 bg-coral/5 px-4 py-2 text-xs font-semibold text-coral transition hover:bg-coral/10 disabled:cursor-not-allowed disabled:opacity-50"
                @click="agregarHito"
              >
                + Agregar hito
              </button>
              <p
                class="text-sm font-semibold"
                :class="Math.abs(totalPorcentajeHitos - 100) < 0.001 ? 'text-green-700' : 'text-rojizo'"
              >
                Suma: {{ totalPorcentajeHitos }}%
              </p>
            </div>
            <p class="mt-3 text-xs leading-relaxed text-cafe/50">
              Máximo {{ maxHitos }} hitos. La primera cuota no puede superar el
              {{ porcentajeMaxPrimerHito }}%. Cuando hay más de un hito, cada cuota debe ser
              de al menos $ {{ formatearMonto(montoMinimoCuota) }} en moneda de cobro.
            </p>
          </div>
        </section>

        <section v-else class="space-y-4">
          <div class="rounded-2xl border border-rosa/50 bg-blanco p-5 sm:p-6">
            <h2 class="text-xl font-semibold text-granate">Mensaje para el cliente</h2>
            <p class="mt-1 text-sm text-cafe/60">
              Podés explicar el presupuesto o dejar una aclaración.
            </p>
            <textarea
              v-model="form.mensaje"
              rows="4"
              maxlength="500"
              placeholder="Hola, te paso el presupuesto de tu solicitud..."
              class="mt-4 block w-full rounded-lg border-0 px-3 py-2.5 text-sm text-cafe shadow-sm ring-1 ring-inset ring-rosa focus:outline-none focus:ring-2 focus:ring-inset focus:ring-coral"
            ></textarea>
            <p class="mt-1 text-right text-[11px] text-cafe/40">
              {{ (form.mensaje || '').length }}/500
            </p>
          </div>

          <div class="rounded-2xl border border-coral/30 bg-coral/5 p-5 sm:p-6">
            <h2 class="text-xl font-semibold text-granate">Resumen</h2>

            <dl class="mt-4 space-y-3 text-sm">
              <div class="flex items-start justify-between gap-4">
                <dt class="flex items-center gap-1.5 text-cafe/55">
                  Plazo de entrega
                  <BurbujaInfo etiqueta="Cómo se calcula la fecha de entrega">
                    Es una duración, no una fecha fija. Se cuenta desde el pago
                    de la seña y se corre los días en que el cliente pague tarde
                    una cuota. El atraso del artista no la mueve.
                  </BurbujaInfo>
                </dt>
                <dd class="text-right font-medium text-cafe">{{ entregaTexto }}</dd>
              </div>
              <div class="flex items-start justify-between gap-4">
                <dt class="text-cafe/55">Inicio estimado</dt>
                <dd class="text-right font-medium text-cafe">{{ inicioEstimadoTexto }}</dd>
              </div>
              <div class="flex items-start justify-between gap-4">
                <dt class="text-cafe/55">Entrega si paga hoy</dt>
                <dd class="text-right font-medium text-cafe">{{ entregaSiPagaHoyTexto }}</dd>
              </div>
              <div v-if="form.fechaVencimiento" class="flex items-start justify-between gap-4">
                <dt class="text-cafe/55">Si paga al vencer</dt>
                <dd class="text-right font-medium text-cafe">{{ entregaAlVencerTexto }}</dd>
              </div>
              <div class="flex items-start justify-between gap-4">
                <dt class="text-cafe/55">Oferta válida hasta</dt>
                <dd class="text-right font-medium text-cafe">
                  {{ form.fechaVencimiento
                    ? formatearFechaDDMMAAAA(form.fechaVencimiento)
                    : `Por defecto: ${vencimientoPorDefectoTexto}` }}
                </dd>
              </div>
              <div class="flex items-start justify-between gap-4 border-t border-coral/15 pt-3">
                <dt class="text-cafe/55">Precio base</dt>
                <dd class="text-right font-medium text-cafe">{{ formatear(totalBase) }}</dd>
              </div>
              <div class="flex items-start justify-between gap-4">
                <dt class="text-cafe/55">Recargos</dt>
                <dd class="text-right font-medium text-cafe">{{ formatear(totalRecargos) }}</dd>
              </div>
              <div class="flex items-start justify-between gap-4 border-t border-coral/15 pt-3">
                <dt class="text-base font-semibold text-granate">Precio del presupuesto</dt>
                <dd class="text-right text-2xl font-bold text-granate">{{ formatear(totalFinal) }}</dd>
              </div>
              <template v-if="comisionMonto !== null">
                <div class="flex items-start justify-between gap-4 border-t border-coral/15 pt-3">
                  <dt class="text-cafe/55">
                    Comisión Temipora ({{ formatearMonto(comisionPorcentaje) }}%)
                  </dt>
                  <dd class="text-right font-medium text-cafe">− {{ formatear(comisionMonto) }}</dd>
                </div>
                <div class="flex items-start justify-between gap-4 border-t border-coral/15 pt-3">
                  <dt class="font-semibold text-granate">Tu neto</dt>
                  <dd class="text-right font-semibold text-granate">
                    {{ formatear(netoSinArancel) }}
                  </dd>
                </div>
                <div
                  v-if="arancelMpEstimado !== null"
                  class="flex items-start justify-between gap-4"
                >
                  <dt class="text-cafe/55">
                    Arancel MercadoPago (estimado) ({{ formatearMonto(arancelMp) }}%)
                  </dt>
                  <dd class="text-right font-medium text-cafe">
                    − {{ formatear(arancelMpEstimado) }}
                  </dd>
                </div>
                <div class="flex items-start justify-between gap-4">
                  <dt class="text-base font-semibold text-granate">Recibís (estimado)</dt>
                  <dd class="text-right text-xl font-bold text-granate">{{ formatear(netoArtista) }}</dd>
                </div>
                <p class="text-xs leading-relaxed text-cafe/50">
                  El cliente paga el precio del presupuesto. La comisión Temipora y el
                  arancel de MercadoPago se descuentan de tu parte.
                </p>
                <div class="pt-1">
                  <button
                    type="button"
                    class="text-xs font-semibold text-coral underline-offset-2 transition hover:text-rojizo hover:underline"
                    @click="modalCalculoAbierto = true"
                  >
                    ¿Cómo se calcula?
                  </button>
                </div>
              </template>
            </dl>
          </div>

          <div class="rounded-2xl border border-rosa/50 bg-blanco p-5 sm:p-6">
            <h3 class="text-sm font-semibold text-granate">Plan de pagos</h3>
            <ul class="mt-3 space-y-2">
              <li
                v-for="(hito, indice) in hitosPreview"
                :key="indice"
                class="flex items-start justify-between gap-3 rounded-xl bg-rosa/10 px-3 py-2 text-sm"
              >
                <span class="min-w-0 text-cafe">
                  <span class="block truncate">
                    {{ hito.nombre || etiquetaTipoHito(hito.tipo, form.hitos.length) }} · {{ hito.porcentaje }}%
                  </span>
                  <span v-if="hito.descripcion" class="mt-0.5 block text-xs leading-relaxed text-cafe/55">
                    {{ hito.descripcion }}
                  </span>
                </span>
                <span class="shrink-0 font-semibold text-granate">
                  {{ formatear(hito.monto) }}
                </span>
              </li>
            </ul>
            <p class="mt-3 text-xs leading-relaxed text-cafe/50">
              La primera cuota se paga al aceptar el presupuesto. Las siguientes se habilitan
              cuando el cliente aprueba el entregable anterior.
            </p>
          </div>

          <div class="rounded-2xl border border-rosa/50 bg-blanco p-5 sm:p-6">
            <h3 class="text-sm font-semibold text-granate">Detalle por dibujo</h3>
            <ul class="mt-3 space-y-2">
              <li
                v-for="item in items"
                :key="item.idItemSolicitud"
                class="flex items-center justify-between gap-3 rounded-xl bg-rosa/10 px-3 py-2 text-sm"
              >
                <span class="min-w-0 truncate text-cafe">{{ item.nombre }}</span>
                <span class="shrink-0 font-semibold text-granate">
                  {{ formatear(subtotalItem(item)) }}
                </span>
              </li>
            </ul>
          </div>
        </section>

        <p
          v-if="errorEnvio"
          class="rounded-xl border border-rojizo/25 bg-rojizo/5 px-4 py-3 text-sm leading-relaxed text-rojizo"
          role="alert"
        >
          {{ errorEnvio }}
        </p>

        <div class="flex flex-col-reverse gap-2 pt-2 sm:flex-row sm:justify-between">
          <button
            v-if="paso > 1"
            type="button"
            class="inline-flex min-h-10 items-center justify-center rounded-full border border-rosa bg-blanco px-5 text-sm font-semibold text-granate transition hover:bg-rosa/20"
            @click="anterior"
          >
            Anterior
          </button>
          <span v-else></span>

          <button
            v-if="paso < 4"
            type="button"
            class="inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-5 text-sm font-semibold text-white transition hover:bg-rojizo"
            @click="siguiente"
          >
            Siguiente
          </button>
          <button
            v-else
            type="submit"
            :disabled="enviando"
            class="inline-flex min-h-10 items-center justify-center rounded-full bg-coral px-6 text-sm font-semibold text-white transition hover:bg-rojizo disabled:cursor-not-allowed disabled:bg-coral/40"
          >
            {{ enviando ? 'Enviando...' : 'Enviar presupuesto' }}
          </button>
        </div>
      </form>
    </template>

    <ModalComoSeCalcula
      :abierto="modalCalculoAbierto"
      contexto="presupuesto"
      :moneda="moneda"
      :precio="totalFinal"
      :comision-porcentaje="comisionPorcentaje"
      :comision-monto="comisionMonto"
      :neto="netoSinArancel"
      :arancel-porcentaje="arancelMp"
      :arancel-monto="arancelMpEstimado"
      :recibis="netoArtista"
      estimado
      @cerrar="modalCalculoAbierto = false"
    />
  </div>
</template>
