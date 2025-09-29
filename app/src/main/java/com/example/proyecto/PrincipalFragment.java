package com.example.proyecto;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
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

public class PrincipalFragment extends Fragment {

    private View logosView;          // para anclar el título debajo de los logos
    private View titleView;
    private View reunionesSection;
    private View cursosSection;
    private View actividadesSection;

    private FirebaseAuth mAuth;
    private DatabaseReference usuariosRef;

    public PrincipalFragment() {}

    public static PrincipalFragment newInstance(String p1, String p2) {
        PrincipalFragment fragment = new PrincipalFragment();
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
        View view = inflater.inflate(R.layout.fragment_principal, container, false);

        // Referencias a vistas
        logosView          = view.findViewById(R.id.logos_layout);
        titleView          = view.findViewById(R.id.title);
        reunionesSection   = view.findViewById(R.id.reuniones_section);
        cursosSection      = view.findViewById(R.id.cursos_section);
        actividadesSection = view.findViewById(R.id.actividades_section);

        // Evita parpadeo: oculta Reuniones por defecto (luego decidimos por rol)
        if (reunionesSection != null) reunionesSection.setVisibility(View.GONE);

        // Aplica reglas según el rol
        aplicarReglasPorRol();

        // Navegación (sin cambios)
        if (reunionesSection != null) {
            reunionesSection.setOnClickListener(v ->
                    Navigation.findNavController(v).navigate(R.id.navigation_reuniones));
        }
        if (cursosSection != null) {
            cursosSection.setOnClickListener(v ->
                    Navigation.findNavController(v).navigate(R.id.navigation_cursos));
        }
        if (actividadesSection != null) {
            actividadesSection.setOnClickListener(v ->
                    Navigation.findNavController(v).navigate(R.id.navigation_actividades));
        }

        return view;
    }

    /** Decide visibilidad de Reuniones y centra el título. */
    private void aplicarReglasPorRol() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Sin sesión: se muestra Reuniones normalmente
            centrarTituloDebajoDeLogos();
            mostrarReunionesDebajoDelTitulo();
            return;
        }
        String uid = currentUser.getUid();

        usuariosRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                if (!isAdded()) return;
                String rol = snap.child("categoriaCentro").getValue(String.class);

                // SIEMPRE: centrar el título debajo de los logos
                centrarTituloDebajoDeLogos();

                if (esProfesionista(rol)) {
                    // Profesionista NO ve reuniones
                    if (reunionesSection != null) reunionesSection.setVisibility(View.GONE);
                } else {
                    // Encargado, Colaborador, etc. SÍ ven reuniones
                    mostrarReunionesDebajoDelTitulo();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                // En caso de error, no bloqueamos: mostramos Reuniones
                centrarTituloDebajoDeLogos();
                mostrarReunionesDebajoDelTitulo();
                Toast.makeText(getContext(), "No se pudo verificar el rol", Toast.LENGTH_SHORT).show();
            }
        });
        
    }

    /** Coloca la tarjeta de Reuniones visible y debajo del título (centrada). */
    private void mostrarReunionesDebajoDelTitulo() {
        if (reunionesSection == null || titleView == null) return;
        RelativeLayout.LayoutParams lpReu =
                (RelativeLayout.LayoutParams) reunionesSection.getLayoutParams();
        limpiarReglas(lpReu);
        lpReu.addRule(RelativeLayout.BELOW, titleView.getId());
        lpReu.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        lpReu.setMargins(0, dp(16), 0, 0);
        reunionesSection.setLayoutParams(lpReu);
        reunionesSection.setVisibility(View.VISIBLE);
    }

    /** Deja el título centrado y anclado debajo de los logos. */
    private void centrarTituloDebajoDeLogos() {
        if (titleView == null || logosView == null) return;
        RelativeLayout.LayoutParams lpTitle =
                (RelativeLayout.LayoutParams) titleView.getLayoutParams();
        limpiarReglas(lpTitle);
        lpTitle.addRule(RelativeLayout.BELOW, logosView.getId());
        lpTitle.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        lpTitle.setMargins(0, dp(16), 0, 0);
        titleView.setLayoutParams(lpTitle);
        titleView.setVisibility(View.VISIBLE);
    }

    private boolean esProfesionista(String rol) {
        if (rol == null) return false;
        String r = rol.trim().toLowerCase();
        return r.equals("profesionista") || r.equals("profesionistas");
    }

    /** Limpia reglas típicas para evitar conflictos al re-anclar. */
    private void limpiarReglas(RelativeLayout.LayoutParams lp) {
        lp.addRule(RelativeLayout.BELOW, 0);
        lp.addRule(RelativeLayout.ABOVE, 0);
        lp.addRule(RelativeLayout.ALIGN_TOP, 0);
        lp.addRule(RelativeLayout.ALIGN_BOTTOM, 0);
        lp.addRule(RelativeLayout.ALIGN_LEFT, 0);
        lp.addRule(RelativeLayout.ALIGN_RIGHT, 0);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL, 0);
        lp.addRule(RelativeLayout.CENTER_VERTICAL, 0);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }
}

