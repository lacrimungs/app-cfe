package com.example.proyecto;

import java.io.Serializable;
import java.util.List;

public class Reunion implements Serializable {
    private String asunto;
    private String motivodelareunion;
    private String fecha;
    private String medio;
    private String recordatorio;
    private String userId; // UID del creador de la reunión
    private List<String> participantes; // lista de UIDs de los participantes

    // Constructor vacío requerido por Firebase
    public Reunion() {}

    // Constructor para inicializar todos los campos
    public Reunion(String asunto, String motivodelareunion, String fecha, String medio, String recordatorio, String userId, List<String> participantes) {
        this.asunto = asunto;
        this.motivodelareunion = motivodelareunion;
        this.fecha = fecha;
        this.medio = medio;
        this.recordatorio = recordatorio;
        this.userId = userId;
        this.participantes = participantes;
    }

    // Métodos getters y setters
    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getMotivodelareunion() {
        return motivodelareunion;
    }

    public void setMotivodelareunion(String motivodelareunion) {
        this.motivodelareunion = motivodelareunion;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getMedio() {
        return medio;
    }

    public void setMedio(String medio) {
        this.medio = medio;
    }

    public String getRecordatorio() {
        return recordatorio;
    }

    public void setRecordatorio(String recordatorio) {
        this.recordatorio = recordatorio;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<String> getParticipantes() {
        return participantes;
    }

    public void setParticipantes(List<String> participantes) {
        this.participantes = participantes;
    }
}
