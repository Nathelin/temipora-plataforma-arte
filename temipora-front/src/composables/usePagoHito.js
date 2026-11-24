import { ref } from 'vue'
import api from '../servicios/api'

/**
 * Flujo de pago de un hito: sincroniza un pago pendiente, inicia el cobro y
 * redirige al checkout del proveedor cuando corresponde.
 *
 * Se usa tanto en el detalle del pedido como en la pantalla de cuenta
 * restringida (alcance PAGOS).
 */
export function usePagoHito({
  procesando: procesandoExterno,
  getPedido = () => null,
  onActualizado = () => {},
  onExito = () => {},
  onError = () => {},
  onNoAutorizado = () => {},
} = {}) {
  const procesando = procesandoExterno || ref(false)
  const hitoPagando = ref(null)
  const redirigiendoPago = ref(false)

  function urlCheckoutMercadoPago(data) {
    const pendiente = (data?.hitos || [])
      .flatMap((hito) => hito.pagos || [])
      .find((pago) => pago.estado === 'PENDIENTE')
    return pendiente?.proveedor === 'MERCADOPAGO'
      ? pendiente.instruccion
      : null
  }

  async function sincronizarPagoPendiente(pedidoActual = getPedido()) {
    const pendiente = (pedidoActual?.hitos || [])
      .flatMap((hito) => hito.pagos || [])
      .find((pago) => pago.estado === 'PENDIENTE'
        && pago.proveedor === 'MERCADOPAGO')
    if (!pendiente) return null
    try {
      const res = await api.post(`/pagos/${pendiente.idPago}/sincronizar`)
      return res.data || null
    } catch {
      // Si el proveedor no responde, seguimos con el polling normal.
      return null
    }
  }

  async function pagarHito(hito, idPedido = getPedido()?.idPedido) {
    if (procesando.value || !hito || !idPedido) return
    procesando.value = true
    hitoPagando.value = hito
    redirigiendoPago.value = true
    let redirigiendo = false
    try {
      const sincronizado = await sincronizarPagoPendiente()
      if (sincronizado) {
        onActualizado(sincronizado)
        const actualizado = (sincronizado.hitos || [])
          .find((item) => item.idHitoPedido === hito.idHitoPedido)
        if (actualizado && actualizado.estadoPago !== 'PENDIENTE') {
          onExito('El pago ya fue acreditado.')
          return
        }
      }
      const res = await api.post(
        `/pedidos/${idPedido}/hitos/${hito.idHitoPedido}/pagar`,
      )
      onActualizado(res.data)
      const url = urlCheckoutMercadoPago(res.data)
      if (url) {
        redirigiendo = true
        window.location.href = url
        return
      }
      onExito('Pago iniciado. Confirmalo para activar el hito.')
    } catch (e) {
      if (e.response?.status === 401) {
        onNoAutorizado()
        return
      }
      onError(e.response?.data?.message || 'No se pudo completar la acción.')
    } finally {
      procesando.value = false
      if (!redirigiendo) {
        redirigiendoPago.value = false
        hitoPagando.value = null
      }
    }
  }

  function irAPagarMercadoPago(pago, hito) {
    hitoPagando.value = hito
    redirigiendoPago.value = true
    window.location.href = pago.instruccion
  }

  return {
    procesando,
    hitoPagando,
    redirigiendoPago,
    pagarHito,
    irAPagarMercadoPago,
    sincronizarPagoPendiente,
  }
}
