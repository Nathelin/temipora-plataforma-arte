import { ref } from 'vue'
import { Client } from '@stomp/stompjs'

const conectado = ref(false)
const handlers = new Set()

let cliente = null

function urlSocket() {
  const protocolo = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocolo}//${window.location.host}/ws`
}

function conectar() {
  if (cliente?.active) return
  if (!localStorage.getItem('token')) return

  const nuevo = new Client({
    brokerURL: urlSocket(),
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    beforeConnect: () => {
      const token = localStorage.getItem('token')
      if (!token) {
        nuevo.deactivate()
        return
      }
      nuevo.connectHeaders = { Authorization: `Bearer ${token}` }
    },
    onConnect: () => {
      conectado.value = true
      nuevo.subscribe('/usuario/cola/mensajeria', (frame) => {
        let evento
        try {
          evento = JSON.parse(frame.body)
        } catch {
          return
        }
        for (const handler of handlers) {
          try {
            handler(evento)
          } catch {
            // Un handler roto no debe interrumpir al resto.
          }
        }
      })
    },
    onDisconnect: () => {
      conectado.value = false
    },
    onWebSocketClose: () => {
      conectado.value = false
    },
    onStompError: () => {
      conectado.value = false
    },
  })

  cliente = nuevo
  nuevo.activate()
}

async function desconectar() {
  conectado.value = false
  const activo = cliente
  cliente = null
  if (activo) {
    try {
      await activo.deactivate()
    } catch {
      // La conexión ya estaba cerrada.
    }
  }
}

function suscribir(handler) {
  handlers.add(handler)
  return () => {
    handlers.delete(handler)
  }
}

function enviarEscribiendo(idConversacion) {
  if (!cliente || !conectado.value) return false
  try {
    cliente.publish({
      destination: `/app/conversacion/${idConversacion}/escribiendo`,
      body: '',
    })
    return true
  } catch {
    return false
  }
}

export function useMensajeriaSocket() {
  return {
    conectado,
    conectar,
    desconectar,
    suscribir,
    enviarEscribiendo,
  }
}
