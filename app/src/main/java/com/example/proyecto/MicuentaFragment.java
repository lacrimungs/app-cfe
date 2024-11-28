package com.example.proyecto;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MicuentaFragment extends Fragment {
    private SharedPreferences sharedPreferences;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private TextView userNameTextView, userEmailTextView, userRoleTextView, userCategoryTextView, userCenterTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mi_cuenta, container, false);

        // Inicializar Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Inicializar Realtime Database
        databaseReference = FirebaseDatabase.getInstance().getReference("usuarios");

        // Inicializar SharedPreferences
        sharedPreferences = getActivity().getSharedPreferences("UserSession", getContext().MODE_PRIVATE);

        // Inicializar Vistas
        userNameTextView = view.findViewById(R.id.user_name);
        userEmailTextView = view.findViewById(R.id.user_email);
        userRoleTextView = view.findViewById(R.id.user_role);
        userCategoryTextView = view.findViewById(R.id.user_category);
        userCenterTextView = view.findViewById(R.id.user_center);

        Button logoutButton = view.findViewById(R.id.cerrar);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Lógica de cerrar sesión
                mAuth.signOut();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("isLoggedIn", false);
                editor.apply();

                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
                getActivity().finish();
            }
        });

        // Obtener y mostrar información del usuario
        cargarInformacionUsuario();

        return view;
    }

    private void cargarInformacionUsuario() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            databaseReference.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Usuario usuario = dataSnapshot.getValue(Usuario.class);
                        if (usuario != null) {
                            userNameTextView.setText(usuario.getNombreCompleto());
                            userEmailTextView.setText(usuario.getClave());
                            userRoleTextView.setText(usuario.getTipoEmpleado());
                            userCategoryTextView.setText(usuario.getCategoriaCentro());
                            userCenterTextView.setText(usuario.getCentro());
                        } else {
                            Toast.makeText(getContext(), "Error al cargar la información del usuario", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Usuario no encontrado", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(getContext(), "Error al acceder a la base de datos: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}

