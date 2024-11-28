package com.example.proyecto;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CalendarioFragment extends Fragment {
    private static final String TAG = "CalendarioFragment";

    private LinearLayout eventosContainer;
    private ProgressBar progressBar;
    private ApiService apiService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendario, container, false);

        // Asocia el contenedor de eventos y el ProgressBar con los elementos del XML
        eventosContainer = view.findViewById(R.id.eventosContainer);
        progressBar = view.findViewById(R.id.progressBar);

        // Configuración del calendario
        CalendarView calendarView = view.findViewById(R.id.calendarView);

        // Aplicar el selector de fecha que tiene el círculo verde
        calendarView.setBackgroundResource(R.drawable.selector_fecha);

        // Configuración de Retrofit
        apiService = ApiClient.getClient().create(ApiService.class);

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            String fecha = dayOfMonth + "/" + (month + 1) + "/" + year; // Ajustamos el formato a DD/MM/YYYY
            Log.d(TAG, "Fecha seleccionada: " + fecha);

            // Comprobar si la fecha es del pasado
            if (isFechaPasada(fecha)) {
                mostrarMensajeError("Lo sentimos, no se pueden mostrar eventos pasados.");
            } else {
                obtenerEventos(fecha);
            }
        });

        return view;
    }

    private boolean isFechaPasada(String fecha) {
        try {
            // Formato de fecha a comparar
            SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy");
            Date fechaSeleccionada = sdf.parse(fecha);

            // Obtener la fecha actual sin horas, minutos y segundos
            Date fechaActual = new Date();
            fechaActual = sdf.parse(sdf.format(fechaActual)); // Esto limpia la hora, minutos y segundos

            // Comparar solo la parte de la fecha (día, mes, año)
            return fechaSeleccionada.before(fechaActual);
        } catch (Exception e) {
            Log.e(TAG, "Error al comparar fechas: " + e.getMessage());
            return false;
        }
    }

    private void mostrarMensajeError(String mensaje) {
        // Mostrar el mensaje de error en el contenedor de eventos
        TextView mensajeTextView = new TextView(getContext());
        mensajeTextView.setText(mensaje);
        mensajeTextView.setTextSize(16);
        mensajeTextView.setPadding(8, 8, 8, 8);

        // Establecer el color de texto a blanco
        mensajeTextView.setTextColor(getResources().getColor(android.R.color.white));

        // Establecer el fondo de color rojo (para indicar error)
        mensajeTextView.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));

        // Limpiar y agregar el TextView con el mensaje de error a la vista
        eventosContainer.removeAllViews();
        eventosContainer.addView(mensajeTextView);

        // Ocultar el ProgressBar
        progressBar.setVisibility(View.GONE);
    }

    private void obtenerEventos(String fecha) {
        // Mostrar el ProgressBar mientras se cargan los eventos
        progressBar.setVisibility(View.VISIBLE);
        eventosContainer.removeAllViews();  // Limpiar contenedor de eventos previo

        // Crear un StringBuilder para almacenar los eventos
        StringBuilder eventosTexto = new StringBuilder();

        // Contador para asegurarse de que todos los eventos se procesen antes de mostrar
        final int[] responsesLeft = {3}; // Tres llamadas a la API

        // Función que actualiza la vista cuando todos los eventos se han procesado
        Runnable checkAndShowEvents = new Runnable() {
            @Override
            public void run() {
                if (responsesLeft[0] == 0) {
                    mostrarEventos(eventosTexto.toString());
                }
            }
        };

        // Obtener actividades
        apiService.getAllActividades().enqueue(new Callback<Map<String, Actividad>>() {
            @Override
            public void onResponse(Call<Map<String, Actividad>> call, Response<Map<String, Actividad>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Actividad actividad : response.body().values()) {
                        String fechaActividad = actividad.getFecha(); // "30/11/2024"
                        Log.d(TAG, "Fecha actividad: " + fechaActividad + " Comparando con: " + fecha);

                        if (fechaActividad.equals(fecha)) {
                            eventosTexto.append("Actividad: ").append(actividad.getMotivodelaactividad()).append("\n");
                        }
                    }
                } else {
                    Log.e(TAG, "Error en la respuesta de actividades");
                    eventosTexto.append("Error al obtener actividades.\n");
                }

                // Decrementar el contador y revisar si es momento de mostrar eventos
                responsesLeft[0]--;
                checkAndShowEvents.run();
            }

            @Override
            public void onFailure(Call<Map<String, Actividad>> call, Throwable t) {
                Log.e(TAG, "Error al obtener actividades: " + t.getMessage());
                eventosTexto.append("Error al obtener actividades.\n");

                // Decrementar el contador y revisar si es momento de mostrar eventos
                responsesLeft[0]--;
                checkAndShowEvents.run();
            }
        });

        // Obtener reuniones
        apiService.getAllReuniones().enqueue(new Callback<Map<String, Reunion>>() {
            @Override
            public void onResponse(Call<Map<String, Reunion>> call, Response<Map<String, Reunion>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Reunion reunion : response.body().values()) {
                        String fechaReunion = reunion.getFecha(); // "29/11/2024"
                        Log.d(TAG, "Fecha reunión: " + fechaReunion + " Comparando con: " + fecha);

                        if (fechaReunion.equals(fecha)) {
                            eventosTexto.append("Reunión: ").append(reunion.getAsunto()).append("\n");
                        }
                    }
                } else {
                    Log.e(TAG, "Error en la respuesta de reuniones");
                    eventosTexto.append("Error al obtener reuniones.\n");
                }

                // Decrementar el contador y revisar si es momento de mostrar eventos
                responsesLeft[0]--;
                checkAndShowEvents.run();
            }

            @Override
            public void onFailure(Call<Map<String, Reunion>> call, Throwable t) {
                Log.e(TAG, "Error al obtener reuniones: " + t.getMessage());
                eventosTexto.append("Error al obtener reuniones.\n");

                // Decrementar el contador y revisar si es momento de mostrar eventos
                responsesLeft[0]--;
                checkAndShowEvents.run();
            }
        });

        // Obtener cursos
        apiService.getAllCursos().enqueue(new Callback<Map<String, Curso>>() {
            @Override
            public void onResponse(Call<Map<String, Curso>> call, Response<Map<String, Curso>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Curso curso : response.body().values()) {
                        String fechaCurso = curso.getInicioyfindelcurso(); // "30/11/2024"
                        Log.d(TAG, "Fecha curso: " + fechaCurso + " Comparando con: " + fecha);

                        if (fechaCurso.equals(fecha)) {
                            eventosTexto.append("Curso: ").append(curso.getNombredelcurso()).append("\n");
                        }
                    }
                } else {
                    Log.e(TAG, "Error en la respuesta de cursos");
                    eventosTexto.append("Error al obtener cursos.\n");
                }

                // Decrementar el contador y revisar si es momento de mostrar eventos
                responsesLeft[0]--;
                checkAndShowEvents.run();
            }

            @Override
            public void onFailure(Call<Map<String, Curso>> call, Throwable t) {
                Log.e(TAG, "Error al obtener cursos: " + t.getMessage());
                eventosTexto.append("Error al obtener cursos.\n");

                // Decrementar el contador y revisar si es momento de mostrar eventos
                responsesLeft[0]--;
                checkAndShowEvents.run();
            }
        });
    }

    private void mostrarEventos(String eventos) {
        // Crear un nuevo TextView para mostrar los eventos
        TextView eventosTextView = new TextView(getContext());
        eventosTextView.setText(eventos.isEmpty() ? "No hay pendientes para esta fecha." : eventos);
        eventosTextView.setTextSize(16);
        eventosTextView.setPadding(8, 8, 8, 8);

        // Establecer el color de texto a blanco
        eventosTextView.setTextColor(getResources().getColor(android.R.color.white));

        // Cambiar el fondo dependiendo de si hay eventos o no
        if (eventos.isEmpty()) {
            // Si no hay eventos, el fondo será verde
            eventosTextView.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
        } else {
            // Si hay eventos, el fondo será azul
            eventosTextView.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light)); // Para amarillo
        }

        // Mostrar los eventos en el contenedor
        eventosContainer.addView(eventosTextView);

        // Ocultar el ProgressBar después de cargar los eventos
        progressBar.setVisibility(View.GONE);
    }
}































