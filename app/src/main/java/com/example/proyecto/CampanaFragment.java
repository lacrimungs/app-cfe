package com.example.proyecto;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class CampanaFragment extends Fragment {
    private ApiService apiService;
    private final Set<String> notificacionesMostradas = new HashSet<>();

    private FirebaseAuth mAuth;

    private LinearLayout notificacionLayoutReuniones;
    private LinearLayout notificacionLayoutActividades;
    private LinearLayout notificacionLayoutCursos;
    private ProgressBar progressBar;

    public CampanaFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_campana, container, false);

        Retrofit retrofit = ApiClient.getClient();
        apiService = retrofit.create(ApiService.class);

        mAuth = FirebaseAuth.getInstance();

        notificacionLayoutReuniones = view.findViewById(R.id.notificacion_layout_1);
        notificacionLayoutActividades = view.findViewById(R.id.notificacion_layout_2);
        notificacionLayoutCursos = view.findViewById(R.id.notificacion_layout_3);
        progressBar = view.findViewById(R.id.progressBar); // asegúrate que exista en el XML

        cargarNotificacionesDesdeApi();

        return view;
    }

    private void cargarNotificacionesDesdeApi() {
        String currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (currentUid == null) return;

        progressBar.setVisibility(View.VISIBLE);

        // 🔹 Reuniones
        apiService.getAllReuniones().enqueue(new Callback<Map<String, Reunion>>() {
            @Override
            public void onResponse(Call<Map<String, Reunion>> call, Response<Map<String, Reunion>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Map.Entry<String, Reunion> entry : response.body().entrySet()) {
                        String reunionId = entry.getKey();
                        Reunion reunion = entry.getValue();

                        if ((reunion.getUserId() != null && reunion.getUserId().equals(currentUid)) ||
                                (reunion.getParticipantes() != null && reunion.getParticipantes().contains(currentUid))) {

                            if (!ReunionesFragment.esReunionRealizada(getContext(), reunionId)) {
                                agregarNotificacion(notificacionLayoutReuniones,
                                        reunionId,
                                        "Reunión",
                                        reunion.getAsunto(),
                                        reunion.getFecha());
                            }
                        }
                    }
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(Call<Map<String, Reunion>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
            }
        });

        // 🔹 Actividades
        apiService.getAllActividades().enqueue(new Callback<Map<String, Actividad>>() {
            @Override
            public void onResponse(Call<Map<String, Actividad>> call, Response<Map<String, Actividad>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Map.Entry<String, Actividad> entry : response.body().entrySet()) {
                        String actividadId = entry.getKey();
                        Actividad actividad = entry.getValue();

                        if ((actividad.getUserId() != null && actividad.getUserId().equals(currentUid)) ||
                                (actividad.getParticipantes() != null && actividad.getParticipantes().contains(currentUid))) {

                            if (!ActividadesFragment.esActividadRealizada(getContext(), actividadId)) {
                                agregarNotificacion(notificacionLayoutActividades,
                                        actividadId,
                                        "Actividad",
                                        actividad.getNombredelaactividad(),
                                        actividad.getFecha());
                            }
                        }
                    }
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(Call<Map<String, Actividad>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
            }
        });

        // 🔹 Cursos
        apiService.getAllCursos().enqueue(new Callback<Map<String, Curso>>() {
            @Override
            public void onResponse(Call<Map<String, Curso>> call, Response<Map<String, Curso>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Map.Entry<String, Curso> entry : response.body().entrySet()) {
                        String cursoId = entry.getKey();
                        Curso curso = entry.getValue();

                        if ((curso.getUserId() != null && curso.getUserId().equals(currentUid)) ||
                                (curso.getParticipantes() != null && curso.getParticipantes().contains(currentUid))) {

                            if (!CursosFragment.esCursoRealizado(getContext(), cursoId)) {
                                agregarNotificacion(notificacionLayoutCursos,
                                        cursoId,
                                        "Curso",
                                        curso.getNombredelcurso(),
                                        curso.getInicioyfindelcurso());
                            }
                        }
                    }
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(Call<Map<String, Curso>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void agregarNotificacion(LinearLayout layout, String id, String tipo, String nombre, String fecha) {
        if (notificacionesMostradas.contains(id)) return;
        notificacionesMostradas.add(id);

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
        messageTextView.setText("Colaborador tienes programado " + tipo + ": " + nombre + " el " + fecha);
        messageTextView.setTextSize(16);
        messageTextView.setTextColor(getResources().getColor(android.R.color.black));
        messageTextView.setPadding(8, 8, 8, 8);

        messageContainer.addView(messageTextView);

        ImageView imageView = new ImageView(getContext());
        imageView.setLayoutParams(new LinearLayout.LayoutParams(250, 250));
        if (tipo.equals("Reunión")) {
            imageView.setImageResource(R.drawable.meeting_icon);
        } else if (tipo.equals("Curso")) {
            imageView.setImageResource(R.drawable.moodle_icon);
        } else if (tipo.equals("Actividad")) {
            imageView.setImageResource(R.drawable.book_lightbulb_icon);
        }

        container.setOnClickListener(v -> {
            if (tipo.equals("Actividad")) {
                ActividadesFragment.marcarActividadComoRealizada(getContext(), id);
            } else if (tipo.equals("Curso")) {
                CursosFragment.marcarCursoComoRealizado(getContext(), id);
            } else if (tipo.equals("Reunión")) {
                ReunionesFragment.marcarReunionComoRealizada(getContext(), id);
            }
            container.setVisibility(View.GONE);
            notificacionesMostradas.remove(id);
            Toast.makeText(getContext(), "Pendiente realizado", Toast.LENGTH_SHORT).show();
        });

        container.addView(imageView);
        container.addView(messageContainer);

        layout.addView(container);
    }
}

