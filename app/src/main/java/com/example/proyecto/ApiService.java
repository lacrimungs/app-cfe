package com.example.proyecto;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @GET("/api/usuarios/{id}")
    Call<Usuario> getUsuario(@Path("id") String userId);

    @GET("/api/usuarios")
    Call<Map<String, Usuario>> getUsuarios();

    @GET("/api/reuniones")
    Call<Map<String, Reunion>> getAllReuniones();

    @GET("/api/cursos")
    Call<Map<String, Curso>> getAllCursos();

    @GET("/api/actividades")
    Call<Map<String, Actividad>> getAllActividades();

}




