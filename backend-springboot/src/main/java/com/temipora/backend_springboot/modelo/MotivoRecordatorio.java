package com.temipora.backend_springboot.modelo;

public enum MotivoRecordatorio {
    PAGO_SENA,
    PAGO_7D,
    PAGO_72H,
    PAGO_24H,
    ENTREGA_7D,
    ENTREGA_1D,
    ENTREGA_HOY;

    public boolean esPago() {
        return name().startsWith("PAGO_");
    }

    public boolean esEntrega() {
        return name().startsWith("ENTREGA_");
    }

    public boolean esUrgente() {
        return this == PAGO_24H || this == ENTREGA_HOY;
    }

    public boolean esAlerta() {
        return this == ENTREGA_1D || this == PAGO_7D || this == PAGO_72H;
    }
}
