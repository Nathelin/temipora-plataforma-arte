function redondear(valor) {
  return Math.round(valor * 100) / 100
}

export function calcularLiquidacionProducto({
  precio,
  comisionPorcentaje,
  arancelPorcentaje,
}) {
  const base = Number(precio)
  if (!Number.isFinite(base) || base <= 0) return null
  const comisionPct = Number(comisionPorcentaje ?? 0)
  const arancelPct = Number(arancelPorcentaje ?? 0)
  const comision = redondear((base * comisionPct) / 100)
  const arancel = redondear((base * arancelPct) / 100)
  const neto = redondear(base - comision)
  const recibis = redondear(base - comision - arancel)
  return { precio: base, comision, arancel, neto, recibis }
}
