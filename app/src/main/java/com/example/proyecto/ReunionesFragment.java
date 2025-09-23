package com.example.proyecto;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ReunionesFragment extends Fragment {

    private FirebaseAuth mAuth;
    private LinearLayout reunionesLayout;

    // SharedPreferences unificado
    private static final String PREFS_NAME = "tareas_realizadas";
    private static final String KEY_REUNIONES = "reuniones_realizadas";

    public ReunionesFragment() {}

    public static ReunionesFragment newInstance(String param1, String param2) {
        ReunionesFragment fragment = new ReunionesFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reuniones, container, false);

        // Imagen de signo
        ImageView signo = view.findViewById(R.id.signo);
        signo.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.navigation_formulario_reunion);
        });

        // Contenedor de reuniones
        reunionesLayout = view.findViewById(R.id.reunionesLayout);
        if (reunionesLayout == null) {
            Toast.makeText(getContext(), "Error: LinearLayout no encontrado", Toast.LENGTH_LONG).show();
            return view;
        }

        // Obtener reuniones
        obtenerReuniones();

        return view;
    }

    private void obtenerReuniones() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            Retrofit retrofit = ApiClient.getClient();
            ApiService apiService = retrofit.create(ApiService.class);

            Call<Map<String, Reunion>> call = apiService.getAllReuniones();
            call.enqueue(new Callback<Map<String, Reunion>>() {
                @Override
                public void onResponse(Call<Map<String, Reunion>> call, Response<Map<String, Reunion>> response) {
                    if (response.isSuccessful()) {
                        Map<String, Reunion> reunionesMap = response.body();

                        reunionesLayout.removeAllViews();

                        if (reunionesMap != null) {
                            for (String reunionId : reunionesMap.keySet()) {
                                Reunion reunion = reunionesMap.get(reunionId);

                                if (reunion != null) {
                                    boolean esCreador = reunion.getUserId() != null && reunion.getUserId().equals(userId);
                                    boolean esParticipante = reunion.getParticipantes() != null && reunion.getParticipantes().contains(userId);

                                    // Solo mostrar si soy creador o participo y no está marcada como realizada
                                    if ((esCreador || esParticipante) && !esReunionRealizada(getContext(), reunionId)) {
                                        mostrarReunion(reunion, reunionId);
                                    }
                                }
                            }
                        }
                    } else {
                        Toast.makeText(getContext(), "Error al obtener las reuniones", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Reunion>> call, Throwable t) {
                    Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Toast.makeText(getContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarReunion(Reunion reunion, String reunionId) {
        View reunionView = LayoutInflater.from(getContext()).inflate(R.layout.item_reunion, reunionesLayout, false);

        TextView asuntoTextView = reunionView.findViewById(R.id.asunto);
        TextView motivoReunionTextView = reunionView.findViewById(R.id.motivoReunion);
        TextView fechaTextView = reunionView.findViewById(R.id.fecha);
        TextView medioTextView = reunionView.findViewById(R.id.medio);
        ImageView palomita = reunionView.findViewById(R.id.palomita);

        asuntoTextView.setText(reunion.getAsunto());
        motivoReunionTextView.setText(reunion.getMotivodelareunion());
        fechaTextView.setText(reunion.getFecha());
        medioTextView.setText(reunion.getMedio());

        palomita.setOnClickListener(v -> {
            marcarReunionComoRealizada(getContext(), reunionId);
            reunionView.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Reunión realizada", Toast.LENGTH_SHORT).show();
        });

        reunionesLayout.addView(reunionView);
    }

    // Métodos estáticos para que también los use CampanaFragment
    public static void marcarReunionComoRealizada(Context context, String reunionId) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Set<String> realizadas = new HashSet<>(sharedPreferences.getStringSet(KEY_REUNIONES, new HashSet<>()));
        realizadas.add(reunionId);
        editor.putStringSet(KEY_REUNIONES, realizadas);
        editor.apply();
    }

    public static boolean esReunionRealizada(Context context, String reunionId) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> realizadas = sharedPreferences.getStringSet(KEY_REUNIONES, new HashSet<>());
        return realizadas.contains(reunionId);
    }
}
