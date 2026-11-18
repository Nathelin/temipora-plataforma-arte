UPDATE usuario SET coleccion_publica = pedidos_publicos
WHERE pedidos_publicos = true
  AND coleccion_publica = false;
