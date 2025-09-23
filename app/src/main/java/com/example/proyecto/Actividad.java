package com.example.proyecto;

import java.io.Serializable;
import java.util.List;

public class Actividad implements Serializable {
    private String nombredelaactividad;
    private String motivodelaactividad;
    private String fecha;
    private String areasinvolucradas;
    private String recordatorio;
    private String userId;
    private List<String> participantes;


    // Constructor vacío requerido por Firebase
    public Actividad() {}


    // Constructor para inicializar todos los campos
    public Actividad(String nombredelaactividad, String motivodelaactividad, String fecha, String areasinvolucradas, String recordatorio, String userId, List<String> participantes) {
        this.nombredelaactividad = nombredelaactividad;
        this.motivodelaactividad = motivodelaactividad;
        this.fecha = fecha;
        this.areasinvolucradas = areasinvolucradas;
        this.recordatorio = recordatorio;
        this.userId = userId;
        this.participantes = participantes;
    }

    // Métodos getters y setters
    public String getNombredelaactividad() {
        return nombredelaactividad;
    }

    public void setNombredelaactividad(String nombredelaactividad) {
        this.nombredelaactividad = nombredelaactividad;
    }

    public String getMotivodelaactividad() {
        return motivodelaactividad;
    }

    public void setMotivodelaactividad(String motivodelaactividad) {
        this.motivodelaactividad = motivodelaactividad;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getAreasinvolucradas() {
        return areasinvolucradas;
    }

    public void setAreasinvolucradas(String areasinvolucradas) {
        this.areasinvolucradas = areasinvolucradas;
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
