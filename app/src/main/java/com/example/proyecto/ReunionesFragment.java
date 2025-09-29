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

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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

    // Para leer rol desde /usuarios
    private DatabaseReference usuariosRef;

    // SharedPreferences unificado
    private static final String PREFS_NAME = "tareas_realizadas";
    private static final String KEY_REUNIONES = "reuniones_realizadas";

    public ReunionesFragment() {}

    public static ReunionesFragment newInstance(String p1, String p2) {
        ReunionesFragment fragment = new ReunionesFragment();
        fragment.setArguments(new Bundle());
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        usuariosRef = FirebaseDatabase.getInstance().getReference("usuarios");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reuniones, container, false);

        // Botón "+"
        ImageView signo = view.findViewById(R.id.signo);
        signo.setVisibility(View.GONE);
        signo.setOnClickListener(null);
        habilitarPlusSiEncargado(signo);

        // Contenedor
        reunionesLayout = view.findViewById(R.id.reunionesLayout);
        if (reunionesLayout == null) {
            Toast.makeText(getContext(), "Error: LinearLayout no encontrado", Toast.LENGTH_LONG).show();
            return view;
        }

        obtenerReuniones();
        return view;
    }

    /** Solo muestra el "+" si categoriaCentro es Encargado/Encargados */
    private void habilitarPlusSiEncargado(ImageView signo) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        String uid = currentUser.getUid();

        usuariosRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                String rol = snap.child("categoriaCentro").getValue(String.class);
                if (puedeVerPlus(rol)) {
                    signo.setVisibility(View.VISIBLE);
                    signo.setOnClickListener(v ->
                            Navigation.findNavController(v)
                                    .navigate(R.id.navigation_formulario_reunion));
                } else {
                    signo.setVisibility(View.GONE);
                    signo.setOnClickListener(null);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                signo.setVisibility(View.GONE);
                signo.setOnClickListener(null);
            }
        });

        /* Alternativa con uidFirebase:
        usuariosRef.orderByChild("uidFirebase").equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot ds) {
                        boolean autorizado = false;
                        for (DataSnapshot u : ds.getChildren()) {
                            String rol = u.child("categoriaCentro").getValue(String.class);
                            if (puedeVerPlus(rol)) { autorizado = true; break; }
                        }
                        if (autorizado) {
                            signo.setVisibility(View.VISIBLE);
                            signo.setOnClickListener(v ->
                                    Navigation.findNavController(v)
                                            .navigate(R.id.navigation_formulario_reunion));
                        } else {
                            signo.setVisibility(View.GONE);
                            signo.setOnClickListener(null);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        signo.setVisibility(View.GONE);
                        signo.setOnClickListener(null);
                    }
                });
        */
    }

    private boolean puedeVerPlus(String rol) {
        if (rol == null) return false;
        String r = rol.trim().toLowerCase();
        return r.equals("encargado") || r.equals("encargados");
    }

    private void obtenerReuniones() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        Retrofit retrofit = ApiClient.getClient();
        ApiService apiService = retrofit.create(ApiService.class);

        Call<Map<String, Reunion>> call = apiService.getAllReuniones();
        call.enqueue(new Callback<Map<String, Reunion>>() {
            @Override
            public void onResponse(Call<Map<String, Reunion>> call, Response<Map<String, Reunion>> response) {
                if (!isAdded()) return; // evita crash si el fragment ya no está visible
                if (response.isSuccessful()) {
                    Map<String, Reunion> reunionesMap = response.body();
                    reunionesLayout.removeAllViews();

                    if (reunionesMap != null) {
                        for (String reunionId : reunionesMap.keySet()) {
                            Reunion reunion = reunionesMap.get(reunionId);
                            if (reunion != null) {
                                boolean esCreador = reunion.getUserId() != null && reunion.getUserId().equals(userId);
                                boolean esParticipante = reunion.getParticipantes() != null
                                        && reunion.getParticipantes().contains(userId);
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
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void mostrarReunion(Reunion reunion, String reunionId) {
        View reunionView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_reunion, reunionesLayout, false);

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

    public static void marcarReunionComoRealizada(Context context, String reunionId) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        Set<String> realizadas = new HashSet<>(sp.getStringSet(KEY_REUNIONES, new HashSet<>()));
        realizadas.add(reunionId);
        editor.putStringSet(KEY_REUNIONES, realizadas);
        editor.apply();
    }

    public static boolean esReunionRealizada(Context context, String reunionId) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> realizadas = sp.getStringSet(KEY_REUNIONES, new HashSet<>());
        return realizadas.contains(reunionId);
    }
}
