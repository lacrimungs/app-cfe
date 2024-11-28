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

public class ActividadesFragment extends Fragment {

    private FirebaseAuth mAuth;
    private LinearLayout actividadesLayout;
    private static final String PREFS_NAME = "actividades_realizadas_prefs";
    private static final String KEY_REALIZADAS = "actividades_realizadas";

    public ActividadesFragment() {
        // Constructor vacío requerido
    }

    public static ActividadesFragment newInstance(String param1, String param2) {
        ActividadesFragment fragment = new ActividadesFragment();
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
        View view = inflater.inflate(R.layout.fragment_actividades, container, false);

        // Imagen de signo para agregar actividad
        ImageView signo = view.findViewById(R.id.signo);
        signo.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.navigation_formulario_actividad);
        });

        actividadesLayout = view.findViewById(R.id.actividadesLayout);
        obtenerActividades();  // Inicialmente obtener actividades

        return view;
    }

    private void obtenerActividades() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            Retrofit retrofit = ApiClient.getClient();
            ApiService apiService = retrofit.create(ApiService.class);

            Call<Map<String, Actividad>> call = apiService.getAllActividades();
            call.enqueue(new Callback<Map<String, Actividad>>() {
                @Override
                public void onResponse(Call<Map<String, Actividad>> call, Response<Map<String, Actividad>> response) {
                    if (response.isSuccessful()) {
                        Map<String, Actividad> actividadesMap = response.body();

                        actividadesLayout.removeAllViews();

                        if (actividadesMap != null) {
                            for (String actividadId : actividadesMap.keySet()) {
                                Actividad actividad = actividadesMap.get(actividadId);
                                if (!esActividadRealizada(actividadId)) {
                                    mostrarActividad(actividad, actividadId);
                                }
                            }
                        }
                    } else {
                        Toast.makeText(getContext(), "Error al obtener las actividades", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Actividad>> call, Throwable t) {
                    Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Toast.makeText(getContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarActividad(Actividad actividad, String actividadId) {
        View actividadView = LayoutInflater.from(getContext()).inflate(R.layout.item_actividad, actividadesLayout, false);

        TextView tituloTextView = actividadView.findViewById(R.id.nombredelaactividad);
        TextView motivoTextView = actividadView.findViewById(R.id.motivodelaactividad);
        TextView fechaTextView = actividadView.findViewById(R.id.fecha);
        TextView areaTextView = actividadView.findViewById(R.id.areasinvolucradas);

        tituloTextView.setText(actividad.getNombredelaactividad());
        motivoTextView.setText(actividad.getMotivodelaactividad());
        fechaTextView.setText(actividad.getFecha());
        areaTextView.setText(actividad.getAreasinvolucradas());

        ImageView palomita = actividadView.findViewById(R.id.palomita);
        palomita.setOnClickListener(v -> {
            marcarActividadComoRealizada(actividadId);
            actividadView.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Actividad marcada como realizada", Toast.LENGTH_SHORT).show();
        });

        actividadesLayout.addView(actividadView);
    }

    private void marcarActividadComoRealizada(String actividadId) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Set<String> realizadas = sharedPreferences.getStringSet(KEY_REALIZADAS, new HashSet<>());
        realizadas.add(actividadId);
        editor.putStringSet(KEY_REALIZADAS, realizadas);
        editor.apply();
    }

    private boolean esActividadRealizada(String actividadId) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> realizadas = sharedPreferences.getStringSet(KEY_REALIZADAS, new HashSet<>());
        return realizadas.contains(actividadId);
    }
}

