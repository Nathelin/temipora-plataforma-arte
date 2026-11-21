import { ref } from 'vue'

const conectado = ref(false)
const handlers = new Set()

let cliente = null
let intentosReconexion = 0
let cargandoCliente = null
let conectando = false
let deseado = false

// El cliente STOMP se descarga recién cuando hay una sesión que lo usa, así no
// pesa en el arranque de Explorar ni en las pantallas públicas.
function cargarCliente() {
  if (!cargandoCliente) {
    cargandoCliente = import('@stomp/stompjs')
      .then((modulo) => modulo.Client)
      .catch((error) => {
        cargandoCliente = null
        throw error
      })
  }
  return cargandoCliente
}

function urlSocket() {
  const protocolo = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocolo}//${window.location.host}/ws`
}

async function conectar() {
  if (cliente?.active || conectando) return
  if (!localStorage.getItem('token')) return

  deseado = true
  conectando = true
  let Client
  try {
    Client = await cargarCliente()
  } catch {
    conectando = false
    return
  }
  conectando = false

  if (!deseado || !localStorage.getItem('token') || cliente?.active) {
    return
  }

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
      intentosReconexion = 0
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
      intentosReconexion += 1
      // Backoff acotado: 1s, 2s, 4s... hasta 30s. Evita martillar el túnel
      // cuando el backend está reiniciándose.
      nuevo.reconnectDelay = Math.min(
        30000,
        1000 * 2 ** Math.min(intentosReconexion, 5),
      )
    },
    onStompError: () => {
      conectado.value = false
    },
  })

  cliente = nuevo
  nuevo.activate()
}

async function desconectar() {
  deseado = false
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

async function reconectarAhora() {
  if (!localStorage.getItem('token')) return
  const activo = cliente
  cliente = null
  conectado.value = false
  if (activo) {
    try {
      await activo.deactivate()
    } catch {
      // La conexión ya estaba cerrada.
    }
  }
  await conectar()
}

if (typeof window !== 'undefined') {
  // Al volver del bfcache el WebSocket puede quedar muerto aunque stomp
  // siga marcado como activo. Forzamos una conexión nueva.
  window.addEventListener('pageshow', (evento) => {
    if (evento.persisted) void reconectarAhora()
  })
  window.addEventListener('online', () => {
    if (!conectado.value) void reconectarAhora()
  })
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
