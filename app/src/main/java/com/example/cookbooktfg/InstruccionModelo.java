package com.example.cookbooktfg;

import com.google.firebase.firestore.Exclude;

public class InstruccionModelo {
    @Exclude
    private String id;  // ID Firestore, opcional para uso local
    private int orden;  // número del paso
    private String paso;  // texto o descripción del paso

    // Constructor vacío necesario para Firestore
    public InstruccionModelo() {}

    // Constructor con parámetros
    public InstruccionModelo(int orden, String paso) {
        this.orden = orden;
        this.paso = paso;
    }

    // Getters y setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }

    public String getPaso() {
        return paso;
    }

    public void setPaso(String paso) {
        this.paso = paso;
    }
}
