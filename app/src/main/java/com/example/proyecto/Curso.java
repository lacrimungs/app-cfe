package com.example.proyecto;

import java.util.List;

public class Curso {
    private String nombredelcurso;
    private String motivodelcurso;
    private String inicioyfindelcurso;
    private String sistema;
    private String recordatorio;
    private String userId; // UID del creador
    private List<String> participantes; // lista de UIDs de los inscritos

    // Constructor vacío requerido por Firebase
    public Curso() {}

    // Constructor para inicializar todos los campos
    public Curso(String nombredelcurso, String motivodelcurso, String inicioyfindelcurso,
                 String sistema, String recordatorio, String userId, List<String> participantes) {
        this.nombredelcurso = nombredelcurso;
        this.motivodelcurso = motivodelcurso;
        this.inicioyfindelcurso = inicioyfindelcurso;
        this.sistema = sistema;
        this.recordatorio = recordatorio;
        this.userId = userId;
        this.participantes = participantes;
    }

    // Getters y setters
    public String getNombredelcurso() { return nombredelcurso; }
    public void setNombredelcurso(String nombredelcurso) { this.nombredelcurso = nombredelcurso; }

    public String getMotivodelcurso() { return motivodelcurso; }
    public void setMotivodelcurso(String motivodelcurso) { this.motivodelcurso = motivodelcurso; }

    public String getInicioyfindelcurso() { return inicioyfindelcurso; }
    public void setInicioyfindelcurso(String inicioyfindelcurso) { this.inicioyfindelcurso = inicioyfindelcurso; }

    public String getSistema() { return sistema; }
    public void setSistema(String sistema) { this.sistema = sistema; }

    public String getRecordatorio() { return recordatorio; }
    public void setRecordatorio(String recordatorio) { this.recordatorio = recordatorio; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<String> getParticipantes() { return participantes; }
    public void setParticipantes(List<String> participantes) { this.participantes = participantes; }
}
