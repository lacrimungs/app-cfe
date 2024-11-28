package com.example.proyecto;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class CampanaFragment extends Fragment {

    private ApiService apiService;

    public CampanaFragment() {
        // Constructor público vacío requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_campana, container, false);

        Retrofit retrofit = ApiClient.getClient();
        apiService = retrofit.create(ApiService.class);

        // Inicializar las listas de notificaciones
        List<String> notificacionesReuniones = new ArrayList<>();
        List<String> notificacionesActividades = new ArrayList<>();
        List<String> notificacionesCursos = new ArrayList<>();

        // Cargar notificaciones desde la API
        cargarNotificacionesDesdeApi(notificacionesReuniones, notificacionesActividades, notificacionesCursos, view);

        return view;
    }

    private void cargarNotificacionesDesdeApi(List<String> notificacionesReuniones, List<String> notificacionesActividades, List<String> notificacionesCursos, View view) {
        apiService.getAllReuniones().enqueue(new Callback<Map<String, Reunion>>() {
            @Override
            public void onResponse(Call<Map<String, Reunion>> call, Response<Map<String, Reunion>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Reunion> reuniones = response.body();
                    for (Reunion reunion : reuniones.values()) {
                        String asunto = reunion.getAsunto();
                        String fecha = reunion.getFecha();
                        if (asunto != null && fecha != null) {
                            notificacionesReuniones.add("Reunión: " + asunto + " el " + fecha);
                        }
                    }
                }

                apiService.getAllActividades().enqueue(new Callback<Map<String, Actividad>>() {
                    @Override
                    public void onResponse(Call<Map<String, Actividad>> call, Response<Map<String, Actividad>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Map<String, Actividad> actividades = response.body();
                            for (Actividad actividad : actividades.values()) {
                                String nombre = actividad.getNombredelaactividad();
                                String fecha = actividad.getFecha();
                                if (nombre != null && fecha != null) {
                                    notificacionesActividades.add("Actividad: " + nombre + " el " + fecha);
                                }
                            }
                        }

                        apiService.getAllCursos().enqueue(new Callback<Map<String, Curso>>() {
                            @Override
                            public void onResponse(Call<Map<String, Curso>> call, Response<Map<String, Curso>> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    Map<String, Curso> cursos = response.body();
                                    for (Curso curso : cursos.values()) {
                                        String nombre = curso.getNombredelcurso();
                                        String fecha = curso.getInicioyfindelcurso();
                                        if (nombre != null && fecha != null) {
                                            notificacionesCursos.add("Curso: " + nombre + " el " + fecha);
                                        }
                                    }
                                }

                                getActivity().runOnUiThread(() -> mostrarNotificaciones(notificacionesReuniones, notificacionesActividades, notificacionesCursos, view));
                            }

                            @Override
                            public void onFailure(Call<Map<String, Curso>> call, Throwable t) {
                                // Manejar error
                            }
                        });
                    }

                    @Override
                    public void onFailure(Call<Map<String, Actividad>> call, Throwable t) {
                        // Manejar error
                    }
                });
            }

            @Override
            public void onFailure(Call<Map<String, Reunion>> call, Throwable t) {
                // Manejar error
            }
        });
    }

    private void mostrarNotificaciones(List<String> notificacionesReuniones, List<String> notificacionesActividades, List<String> notificacionesCursos, View view) {
        // Obtener las actividades ya realizadas
        List<String> actividadesRealizadas = obtenerActividadesRealizadas();

        // Referencia a los contenedores
        LinearLayout notificacionLayoutReuniones = view.findViewById(R.id.notificacion_layout_1);
        LinearLayout notificacionLayoutActividades = view.findViewById(R.id.notificacion_layout_2);
        LinearLayout notificacionLayoutCursos = view.findViewById(R.id.notificacion_layout_3);

        // Agregar notificaciones filtradas (no realizadas) a los contenedores
        agregarNotificaciones(notificacionesReuniones, notificacionLayoutReuniones, actividadesRealizadas);
        agregarNotificaciones(notificacionesActividades, notificacionLayoutActividades, actividadesRealizadas);
        agregarNotificaciones(notificacionesCursos, notificacionLayoutCursos, actividadesRealizadas);
    }

    private void agregarNotificaciones(List<String> notificaciones, LinearLayout layout, List<String> actividadesRealizadas) {
        // Limpiar el layout antes de agregar nuevas notificaciones
        layout.removeAllViews();

        if (notificaciones.isEmpty()) {
            TextView noDataText = new TextView(getContext());
            noDataText.setText("No hay notificaciones.");
            noDataText.setTextSize(16);
            noDataText.setTextColor(getResources().getColor(android.R.color.black));
            noDataText.setGravity(Gravity.CENTER);
            layout.addView(noDataText);
        } else {
            for (String notificacion : notificaciones) {
                // Si la notificación ya está marcada como realizada, no la mostramos
                if (actividadesRealizadas.contains(notificacion)) {
                    continue;  // No agregar las actividades ya realizadas
                }

                LinearLayout container = new LinearLayout(getContext());
                container.setOrientation(LinearLayout.HORIZONTAL);
                container.setPadding(16, 16, 16, 16);

                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(0, 20, 0, 20);
                container.setLayoutParams(layoutParams);
                container.setBackground(getResources().getDrawable(R.drawable.rect_border));

                LinearLayout messageContainer = new LinearLayout(getContext());
                messageContainer.setOrientation(LinearLayout.VERTICAL);
                messageContainer.setGravity(Gravity.CENTER_VERTICAL);

                TextView messageTextView = new TextView(getContext());
                messageTextView.setText("Colaborador tienes programado " + notificacion);
                messageTextView.setTextSize(16);
                messageTextView.setTextColor(getResources().getColor(android.R.color.black));
                messageTextView.setPadding(8, 8, 8, 8);

                messageContainer.addView(messageTextView);

                ImageView imageView = new ImageView(getContext());
                imageView.setLayoutParams(new LinearLayout.LayoutParams(250, 250));
                if (notificacion.contains("Reunión")) {
                    imageView.setImageResource(R.drawable.meeting_icon);
                } else if (notificacion.contains("Curso")) {
                    imageView.setImageResource(R.drawable.moodle_icon);
                } else if (notificacion.contains("Actividad")) {
                    imageView.setImageResource(R.drawable.book_lightbulb_icon);
                }

                // Marcar la notificación como realizada al hacer clic en la notificación
                container.setOnClickListener(v -> {
                    marcarNotificacionComoRealizada(notificacion);
                    container.setVisibility(View.GONE);  // Ocultar el contenedor
                    Toast.makeText(getContext(), "Pendiente realizado", Toast.LENGTH_SHORT).show();
                });

                container.addView(imageView);
                container.addView(messageContainer);

                layout.addView(container);
            }
        }
    }

    private void marcarNotificacionComoRealizada(String notificacion) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("notificaciones_realizadas", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(notificacion, true);  // Marca la notificación como realizada
        editor.apply();
    }

    private List<String> obtenerActividadesRealizadas() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("notificaciones_realizadas", Context.MODE_PRIVATE);
        Map<String, ?> allEntries = sharedPreferences.getAll();
        List<String> actividadesRealizadas = new ArrayList<>();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if ((Boolean) entry.getValue()) {
                actividadesRealizadas.add(entry.getKey());
            }
        }
        return actividadesRealizadas;
    }
}

