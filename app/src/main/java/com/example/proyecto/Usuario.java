package com.example.proyecto;

import java.io.Serializable;

public class Usuario implements Serializable {
    private String idUsuario;       // ID personalizado que generas (USR_...)
    private String nombreCompleto;
    private String categoriaCentro;
    private String centro;
    private String clave;
    private String correo;
    private String contraseña;

    // 🔹 Nuevo campo: UID real de FirebaseAuth
    private String uidFirebase;

    // Constructor vacío requerido por Firebase
    public Usuario() {
    }

    // Constructor completo
    public Usuario(String idUsuario, String nombreCompleto, String categoriaCentro,
                   String centro, String clave, String correo, String contraseña,
                   String uidFirebase) {
        this.idUsuario = idUsuario;
        this.nombreCompleto = nombreCompleto;
        this.categoriaCentro = categoriaCentro;
        this.centro = centro;
        this.clave = clave;
        this.correo = correo;
        this.contraseña = contraseña;
        this.uidFirebase = uidFirebase;
    }

    // Getters y setters
    public String getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
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

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContraseña() {
        return contraseña;
    }

    public void setContraseña(String contraseña) {
        this.contraseña = contraseña;
    }

    public String getUidFirebase() {
        return uidFirebase;
    }

    public void setUidFirebase(String uidFirebase) {
        this.uidFirebase = uidFirebase;
    }
}
