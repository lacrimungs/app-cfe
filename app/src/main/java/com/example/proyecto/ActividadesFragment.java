package com.example.proyecto;

import android.content.Context;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashSet;
import java.util.Set;

public class ActividadesFragment extends Fragment {

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private LinearLayout actividadesLayout;

    // 🔹 SharedPreferences para actividades realizadas
    private static final String PREFS_NAME = "actividades_realizadas";
    private static final String KEY_ACTIVIDADES = "actividades";

    public ActividadesFragment() {}

    public static ActividadesFragment newInstance() {
        return new ActividadesFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("actividades");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_actividades, container, false);

        actividadesLayout = view.findViewById(R.id.actividadesLayout);

        // Botón para ir al formulario
        ImageView signo = view.findViewById(R.id.signo);
        signo.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.navigation_formulario_actividad);
        });

        obtenerActividades();

        return view;
    }

    private void obtenerActividades() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                actividadesLayout.removeAllViews();

                for (DataSnapshot actividadSnapshot : dataSnapshot.getChildren()) {
                    String actividadId = actividadSnapshot.getKey();
                    Actividad actividad = actividadSnapshot.getValue(Actividad.class);

                    if (actividad != null) {
                        boolean esCreador = actividad.getUserId() != null && actividad.getUserId().equals(userId);
                        boolean esParticipante = actividad.getParticipantes() != null &&
                                actividad.getParticipantes().contains(userId);

                        // Mostrar solo si soy creador o participante y no está realizada
                        if ((esCreador || esParticipante) && !esActividadRealizada(getContext(), actividadId)) {
                            mostrarActividad(actividad, actividadId);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getContext(), "Error al obtener actividades", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarActividad(Actividad actividad, String actividadId) {
        // Reutilizamos item_reunion.xml
        View actividadView = LayoutInflater.from(getContext()).inflate(R.layout.item_reunion, actividadesLayout, false);

        TextView asuntoTextView = actividadView.findViewById(R.id.asunto);
        TextView motivoTextView = actividadView.findViewById(R.id.motivoReunion);
        TextView fechaTextView = actividadView.findViewById(R.id.fecha);
        TextView medioTextView = actividadView.findViewById(R.id.medio);
        ImageView palomita = actividadView.findViewById(R.id.palomita);

        asuntoTextView.setText(actividad.getNombredelaactividad());
        motivoTextView.setText("Motivo: " + actividad.getMotivodelaactividad());
        fechaTextView.setText("Fecha: " + actividad.getFecha());
        medioTextView.setText("Recordatorio: " + actividad.getRecordatorio());

        palomita.setOnClickListener(v -> {
            marcarActividadComoRealizada(getContext(), actividadId);
            actividadView.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Actividad realizada", Toast.LENGTH_SHORT).show();
        });

        actividadesLayout.addView(actividadView);
    }

    // 🔹 Métodos estáticos para CampanaFragment
    public static void marcarActividadComoRealizada(Context context, String actividadId) {
        android.content.SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = sharedPreferences.edit();

        Set<String> realizadas = new HashSet<>(sharedPreferences.getStringSet(KEY_ACTIVIDADES, new HashSet<>()));
        realizadas.add(actividadId);
        editor.putStringSet(KEY_ACTIVIDADES, realizadas);
        editor.apply();
    }

    public static boolean esActividadRealizada(Context context, String actividadId) {
        android.content.SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> realizadas = sharedPreferences.getStringSet(KEY_ACTIVIDADES, new HashSet<>());
        return realizadas.contains(actividadId);
    }
}
