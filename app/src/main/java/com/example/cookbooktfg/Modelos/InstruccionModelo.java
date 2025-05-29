package com.example.cookbooktfg.Modelos;

import com.google.firebase.firestore.Exclude;
/**
 * Modelo de datos que representa una instrucción o paso dentro de una receta.
 * Este modelo se puede almacenar en Firebase Firestore.
 *
 *  Autor: Telma Teixeira
 *  Proyecto: CookbookTFG
 */
public class InstruccionModelo {
    @Exclude
    private String id;  // ID Firestore, opcional para uso local
    private int orden;  // número del paso
    private String paso;  // texto o descripción del paso

    /**
     * Constructor vacío requerido por Firestore para deserializar objetos.
     */
    public InstruccionModelo() {}

    /**
     * Constructor con parámetros para inicializar directamente una instrucción.
     *
     * @param orden Número de orden del paso.
     * @param paso  Texto o descripción del paso.
     */
    public InstruccionModelo(int orden, String paso) {
        this.orden = orden;
        this.paso = paso;
    }

    /**
     * Getters y setters
     * */

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
