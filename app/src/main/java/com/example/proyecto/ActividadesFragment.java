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
import java.util.Set;

public class ActividadesFragment extends Fragment {

    private FirebaseAuth mAuth;
    private DatabaseReference actividadesRef;
    private DatabaseReference usuariosRef; // para leer rol
    private LinearLayout actividadesLayout;

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
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        actividadesRef = db.getReference("actividades");
        usuariosRef = db.getReference("usuarios");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_actividades, container, false);

        actividadesLayout = view.findViewById(R.id.actividadesLayout);

        ImageView signo = view.findViewById(R.id.signo);
        signo.setVisibility(View.GONE);
        signo.setOnClickListener(null);
        habilitarPlusSiEncargado(signo);

        obtenerActividades();
        return view;
    }

    /** Solo muestra el "+" si categoriaCentro es Encargado/Encargados */
    private void habilitarPlusSiEncargado(ImageView signo) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        String uid = currentUser.getUid();

        // Si /usuarios/<uid> es la key:
        usuariosRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                String rol = snap.child("categoriaCentro").getValue(String.class);
                if (puedeVerPlus(rol)) {
                    signo.setVisibility(View.VISIBLE);
                    signo.setOnClickListener(v ->
                            Navigation.findNavController(v)
                                    .navigate(R.id.navigation_formulario_actividad));
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

        /* Alternativa si NO usas uid como key y guardas uid en "uidFirebase":
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
                                            .navigate(R.id.navigation_formulario_actividad));
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

    private void obtenerActividades() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();

        actividadesRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                actividadesLayout.removeAllViews();
                for (DataSnapshot actividadSnapshot : dataSnapshot.getChildren()) {
                    String actividadId = actividadSnapshot.getKey();
                    Actividad actividad = actividadSnapshot.getValue(Actividad.class);
                    if (actividad != null) {
                        boolean esCreador = actividad.getUserId() != null && actividad.getUserId().equals(userId);
                        boolean esParticipante = actividad.getParticipantes() != null &&
                                actividad.getParticipantes().contains(userId);
                        if ((esCreador || esParticipante)
                                && !esActividadRealizada(requireContext(), actividadId)) {
                            mostrarActividad(actividad, actividadId);
                        }
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Error al obtener actividades", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarActividad(Actividad actividad, String actividadId) {
        View actividadView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_reunion, actividadesLayout, false);

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
            marcarActividadComoRealizada(requireContext(), actividadId);
            actividadView.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Actividad realizada", Toast.LENGTH_SHORT).show();
        });

        actividadesLayout.addView(actividadView);
    }

    public static void marcarActividadComoRealizada(Context context, String actividadId) {
        android.content.SharedPreferences sp =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = sp.edit();
        Set<String> realizadas = new HashSet<>(sp.getStringSet(KEY_ACTIVIDADES, new HashSet<>()));
        realizadas.add(actividadId);
        editor.putStringSet(KEY_ACTIVIDADES, realizadas);
        editor.apply();
    }

    public static boolean esActividadRealizada(Context context, String actividadId) {
        android.content.SharedPreferences sp =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> realizadas = sp.getStringSet(KEY_ACTIVIDADES, new HashSet<>());
        return realizadas.contains(actividadId);
    }
}
