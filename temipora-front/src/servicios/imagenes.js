const URL_VERIFICADA = /^(?:https?:\/\/[^/]+)?\/api\/portfolio\/verificadas\/archivos\/\d+\/preview$/

export function esImagenVerificada(url) {
  return typeof url === 'string' && URL_VERIFICADA.test(url)
}
