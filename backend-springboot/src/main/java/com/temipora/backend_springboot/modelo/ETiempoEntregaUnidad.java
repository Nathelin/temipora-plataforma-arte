package com.temipora.backend_springboot.modelo;

public enum ETiempoEntregaUnidad {
    DIAS(1),
    SEMANAS(7),
    MESES(30);

    private final int factorDias;

    ETiempoEntregaUnidad(int factorDias) {
        this.factorDias = factorDias;
    }

    public int getFactorDias() {
        return factorDias;
    }
}
