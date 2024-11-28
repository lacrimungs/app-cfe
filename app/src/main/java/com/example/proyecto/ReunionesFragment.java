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

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ReunionesFragment extends Fragment {

    private FirebaseAuth mAuth;
    private LinearLayout reunionesLayout;

    public ReunionesFragment() {
        // Constructor vacío
    }

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

        // Configura el OnClickListener para el signo
        signo.setOnClickListener(v -> {
            // Navegar al formulario de reunión
            Navigation.findNavController(v).navigate(R.id.navigation_formulario_reunion);
        });

        // LinearLayout para mostrar las reuniones
        reunionesLayout = view.findViewById(R.id.reunionesLayout);
        if (reunionesLayout == null) {
            Toast.makeText(getContext(), "Error: LinearLayout no encontrado", Toast.LENGTH_LONG).show();
            return view;
        }

        // Obtener reuniones del usuario autenticado
        obtenerReuniones();

        return view;
    }

    private void obtenerReuniones() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            // Usar ApiClient para obtener Retrofit
            Retrofit retrofit = ApiClient.getClient();
            ApiService apiService = retrofit.create(ApiService.class);

            // Llamada para obtener todas las reuniones
            Call<Map<String, Reunion>> call = apiService.getAllReuniones();
            call.enqueue(new Callback<Map<String, Reunion>>() {
                @Override
                public void onResponse(Call<Map<String, Reunion>> call, Response<Map<String, Reunion>> response) {
                    if (response.isSuccessful()) {
                        Map<String, Reunion> reunionesMap = response.body();

                        // Limpiar la lista actual
                        reunionesLayout.removeAllViews();

                        // Iterar sobre las reuniones y filtrar por userId
                        if (reunionesMap != null) {
                            for (String reunionId : reunionesMap.keySet()) {
                                Reunion reunion = reunionesMap.get(reunionId);
                                if (reunion.getUserId().equals(userId) && !esReunionRealizada(reunionId)) {
                                    mostrarReunion(reunion, reunionId);
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

        // Configura la acción para la palomita
        palomita.setOnClickListener(v -> {
            marcarReunionComoRealizada(reunionId);
            reunionView.setVisibility(View.GONE);  // Ocultar la reunión
            Toast.makeText(getContext(), "Reunión realizada", Toast.LENGTH_SHORT).show();
        });

        reunionesLayout.addView(reunionView);
    }

    private void marcarReunionComoRealizada(String reunionId) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("reuniones_realizadas", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(reunionId, true);  // Marca la reunión como realizada
        editor.apply();
    }

    private boolean esReunionRealizada(String reunionId) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("reuniones_realizadas", Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(reunionId, false);  // Devuelve true si la reunión fue realizada
    }
}

