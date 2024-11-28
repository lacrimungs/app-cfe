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

public class CursosFragment extends Fragment {

    private FirebaseAuth mAuth;
    private LinearLayout cursosLayout;

    public CursosFragment() {
        // Constructor vacío
    }

    public static CursosFragment newInstance(String param1, String param2) {
        CursosFragment fragment = new CursosFragment();
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
        View view = inflater.inflate(R.layout.fragment_cursos, container, false);

        // Imagen de signo
        ImageView signo = view.findViewById(R.id.signo);

        // Configura el OnClickListener para el signo
        signo.setOnClickListener(v -> {
            // Navegar al formulario de curso
            Navigation.findNavController(v).navigate(R.id.navigation_formulario_curso);
        });

        // LinearLayout para mostrar los cursos
        cursosLayout = view.findViewById(R.id.cursosLayout);
        if (cursosLayout == null) {
            Toast.makeText(getContext(), "Error: LinearLayout no encontrado", Toast.LENGTH_LONG).show();
            return view;
        }

        // Obtener cursos
        obtenerCursos();

        return view;
    }

    private void obtenerCursos() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            // Usar ApiClient para obtener Retrofit
            Retrofit retrofit = ApiClient.getClient();
            ApiService apiService = retrofit.create(ApiService.class);

            // Llamada para obtener todos los cursos
            Call<Map<String, Curso>> call = apiService.getAllCursos();
            call.enqueue(new Callback<Map<String, Curso>>() {
                @Override
                public void onResponse(Call<Map<String, Curso>> call, Response<Map<String, Curso>> response) {
                    if (response.isSuccessful()) {
                        Map<String, Curso> cursosMap = response.body();

                        // Limpiar la lista actual
                        cursosLayout.removeAllViews();

                        // Iterar sobre los cursos y mostrarlos
                        if (cursosMap != null) {
                            for (String cursoId : cursosMap.keySet()) {
                                Curso curso = cursosMap.get(cursoId);
                                if (!esCursoRealizado(cursoId)) {
                                    mostrarCurso(curso, cursoId);
                                }
                            }
                        }
                    } else {
                        Toast.makeText(getContext(), "Error al obtener los cursos", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Curso>> call, Throwable t) {
                    Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Toast.makeText(getContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarCurso(Curso curso, String cursoId) {
        View cursoView = LayoutInflater.from(getContext()).inflate(R.layout.item_curso, cursosLayout, false);

        TextView tituloTextView = cursoView.findViewById(R.id.nombredelcurso);
        TextView motivoTextView = cursoView.findViewById(R.id.motivodelcurso);
        TextView fechaTextView = cursoView.findViewById(R.id.inicioyfindelcurso);
        TextView sistemaTextView = cursoView.findViewById(R.id.sistema);
        ImageView palomita = cursoView.findViewById(R.id.palomita);

        tituloTextView.setText(curso.getNombredelcurso());
        motivoTextView.setText(curso.getMotivodelcurso());
        fechaTextView.setText(curso.getInicioyfindelcurso());
        sistemaTextView.setText(curso.getSistema());

        // Configura la acción para la palomita
        palomita.setOnClickListener(v -> {
            marcarCursoComoRealizado(cursoId);
            cursoView.setVisibility(View.GONE);  // Ocultar el curso
            Toast.makeText(getContext(), "Curso completado", Toast.LENGTH_SHORT).show();
        });

        cursosLayout.addView(cursoView);
    }

    private void marcarCursoComoRealizado(String cursoId) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("cursos_realizados", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(cursoId, true);  // Marca el curso como realizado
        editor.apply();
    }

    private boolean esCursoRealizado(String cursoId) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("cursos_realizados", Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(cursoId, false);  // Devuelve true si el curso fue realizado
    }
}
