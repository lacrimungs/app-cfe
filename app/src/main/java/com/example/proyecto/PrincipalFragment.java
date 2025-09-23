package com.example.proyecto;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

public class PrincipalFragment extends Fragment {

    public PrincipalFragment() {
        // Required empty public constructor
    }

    public static PrincipalFragment newInstance(String param1, String param2) {
        PrincipalFragment fragment = new PrincipalFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_principal, container, false);

        // Encuentra las secciones
        View reunionesSection = view.findViewById(R.id.reuniones_section);
        View cursosSection = view.findViewById(R.id.cursos_section);
        View actividadesSection = view.findViewById(R.id.actividades_section);



        reunionesSection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(R.id.navigation_reuniones);
            }
        });

        cursosSection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(R.id.navigation_cursos);
            }
        });

        actividadesSection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(R.id.navigation_actividades);
            }
        });

        return view;
    }
}
