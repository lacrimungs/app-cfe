package com.example.proyecto;

public class Usuario {

    private String nombreCompleto;
    private String tipoEmpleado;
    private String categoriaCentro;
    private String centro;
    private String clave;
    private String contraseña;

    // Constructor vacío requerido por Firebase
    public Usuario() {}

    // Constructor para inicializar todos los campos
    public Usuario(String nombreCompleto, String tipoEmpleado, String categoriaCentro, String centro, String clave, String contraseña) {
        this.nombreCompleto = nombreCompleto;
        this.tipoEmpleado = tipoEmpleado;
        this.categoriaCentro = categoriaCentro;
        this.centro = centro;
        this.clave = clave;
        this.contraseña = contraseña;
    }

    // Getters y setters
    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getTipoEmpleado() {
        return tipoEmpleado;
    }

    public void setTipoEmpleado(String tipoEmpleado) {
        this.tipoEmpleado = tipoEmpleado;
    }

    public String getCategoriaCentro() {
        return categoriaCentro;
    }

    public void setCategoriaCentro(String categoriaCentro) {
        this.categoriaCentro = categoriaCentro;
    }

    public String getCentro() {
        return centro;
    }

    public void setCentro(String centro) {
        this.centro = centro;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getContraseña() {
        return contraseña;
    }

    public void setContraseña(String contraseña) {
        this.contraseña = contraseña;
    }
}
