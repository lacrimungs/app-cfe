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

public class CursosFragment extends Fragment {

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private LinearLayout cursosLayout;

    // 🔹 SharedPreferences para cursos realizados
    private static final String PREFS_NAME = "cursos_realizados";
    private static final String KEY_CURSOS = "cursos";

    public CursosFragment() {}

    public static CursosFragment newInstance() {
        return new CursosFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("cursos");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cursos, container, false);

        cursosLayout = view.findViewById(R.id.cursosLayout);

        // Botón para ir al formulario
        ImageView signo = view.findViewById(R.id.signo);
        signo.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.navigation_formulario_curso);
        });

        obtenerCursos();

        return view;
    }

    private void obtenerCursos() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                cursosLayout.removeAllViews();

                for (DataSnapshot cursoSnapshot : dataSnapshot.getChildren()) {
                    String cursoId = cursoSnapshot.getKey();
                    Curso curso = cursoSnapshot.getValue(Curso.class);

                    if (curso != null) {
                        boolean esCreador = curso.getUserId() != null && curso.getUserId().equals(userId);
                        boolean esParticipante = curso.getParticipantes() != null && curso.getParticipantes().contains(userId);

                        // Mostrar solo si soy creador o participante y no está realizado
                        if ((esCreador || esParticipante) && !esCursoRealizado(getContext(), cursoId)) {
                            mostrarCurso(curso, cursoId);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getContext(), "Error al obtener cursos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarCurso(Curso curso, String cursoId) {
        // Reutilizamos item_reunion.xml
        View cursoView = LayoutInflater.from(getContext()).inflate(R.layout.item_reunion, cursosLayout, false);

        TextView asuntoTextView = cursoView.findViewById(R.id.asunto);
        TextView motivoTextView = cursoView.findViewById(R.id.motivoReunion);
        TextView fechaTextView = cursoView.findViewById(R.id.fecha);
        TextView medioTextView = cursoView.findViewById(R.id.medio);
        ImageView palomita = cursoView.findViewById(R.id.palomita);

        asuntoTextView.setText("📘 " + curso.getNombredelcurso());
        motivoTextView.setText("Motivo: " + curso.getMotivodelcurso());
        fechaTextView.setText("Fecha(s): " + curso.getInicioyfindelcurso());
        medioTextView.setText("Sistema: " + curso.getSistema());

        palomita.setOnClickListener(v -> {
            marcarCursoComoRealizado(getContext(), cursoId);
            cursoView.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Curso completado", Toast.LENGTH_SHORT).show();
        });

        cursosLayout.addView(cursoView);
    }

    // 🔹 Métodos estáticos para CampanaFragment
    public static void marcarCursoComoRealizado(Context context, String cursoId) {
        android.content.SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = sharedPreferences.edit();

        Set<String> realizados = new HashSet<>(sharedPreferences.getStringSet(KEY_CURSOS, new HashSet<>()));
        realizados.add(cursoId);
        editor.putStringSet(KEY_CURSOS, realizados);
        editor.apply();
    }

    public static boolean esCursoRealizado(Context context, String cursoId) {
        android.content.SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> realizados = sharedPreferences.getStringSet(KEY_CURSOS, new HashSet<>());
        return realizados.contains(cursoId);
    }
}
