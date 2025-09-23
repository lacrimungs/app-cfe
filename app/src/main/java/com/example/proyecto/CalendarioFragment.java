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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendario, container, false);

        eventosContainer = view.findViewById(R.id.eventosContainer);
        progressBar = view.findViewById(R.id.progressBar);

        CalendarView calendarView = view.findViewById(R.id.calendarView);
        calendarView.setBackgroundResource(R.drawable.selector_fecha);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        apiService = ApiClient.getClient().create(ApiService.class);

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            String fecha = dayOfMonth + "/" + (month + 1) + "/" + year;
            Log.d(TAG, "Fecha seleccionada: " + fecha);

            boolean fechaPasada = isFechaPasada(fecha);
            obtenerEventos(fecha, fechaPasada);
        });

        return view;
    }

    private boolean isFechaPasada(String fecha) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy");
            Date fechaSeleccionada = sdf.parse(fecha);
            Date fechaActual = new Date();
            fechaActual = sdf.parse(sdf.format(fechaActual));
            return fechaSeleccionada.before(fechaActual);
        } catch (Exception e) {
            Log.e(TAG, "Error al comparar fechas: " + e.getMessage());
            return false;
        }
    }

    private void obtenerEventos(String fecha, boolean esPasada) {
        progressBar.setVisibility(View.VISIBLE);
        eventosContainer.removeAllViews();

        StringBuilder eventosTexto = new StringBuilder();
        final int[] responsesLeft = {3};

        Runnable checkAndShowEvents = () -> {
            if (responsesLeft[0] == 0) {
                mostrarEventos(eventosTexto.toString(), esPasada);
            }
        };

        // 🔹 ACTIVIDADES
        apiService.getAllActividades().enqueue(new Callback<Map<String, Actividad>>() {
            @Override
            public void onResponse(Call<Map<String, Actividad>> call, Response<Map<String, Actividad>> response) {
                if (response.isSuccessful() && response.body() != null && currentUser != null) {
                    for (Actividad actividad : response.body().values()) {
                        String fechaActividad = actividad.getFecha();
                        if (fechaActividad.equals(fecha)) {
                            // Solo si es creador o participante
                            if (actividad.getUserId().equals(currentUser.getUid()) ||
                                    actividad.getParticipantes().contains(currentUser.getUid())) {
                                eventosTexto.append("Actividad: ").append(actividad.getMotivodelaactividad()).append("\n");
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "Error en la respuesta de actividades");
                }
                responsesLeft[0]--;
                checkAndShowEvents.run();
            }

            @Override
            public void onFailure(Call<Map<String, Actividad>> call, Throwable t) {
                Log.e(TAG, "Error al obtener actividades: " + t.getMessage());
                responsesLeft[0]--;
                checkAndShowEvents.run();
            }
        });

        // 🔹 REUNIONES
        apiService.getAllReuniones().enqueue(new Callback<Map<String, Reunion>>() {
            @Override
            public void onResponse(Call<Map<String, Reunion>> call, Response<Map<String, Reunion>> response) {
                if (response.isSuccessful() && response.body() != null && currentUser != null) {
                    for (Reunion reunion : response.body().values()) {
                        String fechaReunion = reunion.getFecha();
                        if (fechaReunion.equals(fecha)) {
                            if (reunion.getUserId().equals(currentUser.getUid()) ||
                                    reunion.getParticipantes().contains(currentUser.getUid())) {
                                eventosTexto.append("Reunión: ").append(reunion.getAsunto()).append("\n");
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "Error en la respuesta de reuniones");
                }
                responsesLeft[0]--;
                checkAndShowEvents.run();
            }

            @Override
            public void onFailure(Call<Map<String, Reunion>> call, Throwable t) {
                Log.e(TAG, "Error al obtener reuniones: " + t.getMessage());
                responsesLeft[0]--;
                checkAndShowEvents.run();
            }
        });

        // 🔹 CURSOS
        apiService.getAllCursos().enqueue(new Callback<Map<String, Curso>>() {
            @Override
            public void onResponse(Call<Map<String, Curso>> call, Response<Map<String, Curso>> response) {
                if (response.isSuccessful() && response.body() != null && currentUser != null) {
                    for (Curso curso : response.body().values()) {
                        String fechaCurso = curso.getInicioyfindelcurso();
                        if (fechaCurso.equals(fecha)) {
                            if (curso.getUserId().equals(currentUser.getUid()) ||
                                    curso.getParticipantes().contains(currentUser.getUid())) {
                                eventosTexto.append("Curso: ").append(curso.getNombredelcurso()).append("\n");
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "Error en la respuesta de cursos");
                }
                responsesLeft[0]--;
                checkAndShowEvents.run();
            }

            @Override
            public void onFailure(Call<Map<String, Curso>> call, Throwable t) {
                Log.e(TAG, "Error al obtener cursos: " + t.getMessage());
                responsesLeft[0]--;
                checkAndShowEvents.run();
            }
        });
    }

    private void mostrarEventos(String eventos, boolean esPasada) {
        TextView eventosTextView = new TextView(getContext());
        eventosTextView.setText(eventos.isEmpty() ? "No hay pendientes para esta fecha." : eventos);
        eventosTextView.setTextSize(16);
        eventosTextView.setPadding(8, 8, 8, 8);
        eventosTextView.setTextColor(getResources().getColor(android.R.color.white));

        if (esPasada) {
            // 🔴 Fecha pasada
            eventosTextView.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
        } else {
            if (eventos.isEmpty()) {
                // 🟢 Futuro sin pendientes
                eventosTextView.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
            } else {
                // 🟡 Futuro con pendientes
                eventosTextView.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
            }
        }

        eventosContainer.addView(eventosTextView);
        progressBar.setVisibility(View.GONE);
    }
}































