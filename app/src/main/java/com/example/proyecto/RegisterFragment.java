package com.example.proyecto;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterFragment extends Fragment {

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private EditText nombreCompletoField, claveField, correoField, contraseñaField;
    private Spinner spinner2, spinner3;
    private SharedPreferences prefs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("usuarios");
        prefs = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);

        // Spinners
        spinner2 = view.findViewById(R.id.spinne);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(
                getContext(), R.array.spinner_items_6, R.layout.spinner_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner2.setAdapter(adapter2);

        spinner3 = view.findViewById(R.id.spinner3);
        ArrayAdapter<CharSequence> adapter3 = ArrayAdapter.createFromResource(
                getContext(), R.array.spinner_items_7, R.layout.spinner_item);
        adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner3.setAdapter(adapter3);

        // Campos
        nombreCompletoField = view.findViewById(R.id.nombreCompletoField);
        claveField = view.findViewById(R.id.claveField);
        correoField = view.findViewById(R.id.correoField);
        contraseñaField = view.findViewById(R.id.contraseñaField);

        validarEnTiempoReal();

        // Botón
        Button registrarButton = view.findViewById(R.id.enviar);
        registrarButton.setOnClickListener(v -> registrarUsuario());

        return view;
    }

    private void validarEnTiempoReal() {
        nombreCompletoField.addTextChangedListener(new SimpleWatcher(() ->
                nombreCompletoField.setError(nombreCompletoField.getText().toString().trim().isEmpty()
                        ? "⚠️ El nombre no puede estar vacío" : null)));

        claveField.addTextChangedListener(new SimpleWatcher(() ->
                claveField.setError(claveField.getText().toString().trim().isEmpty()
                        ? "⚠️ La clave no puede estar vacía" : null)));

        correoField.addTextChangedListener(new SimpleWatcher(() -> {
            String email = correoField.getText().toString().trim();
            if (email.isEmpty()) {
                correoField.setError("⚠️ El correo no puede estar vacío");
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                correoField.setError("⚠️ Correo inválido");
            } else {
                correoField.setError(null);
            }
        }));

        contraseñaField.addTextChangedListener(new SimpleWatcher(() -> {
            String pass = contraseñaField.getText().toString();
            if (pass.isEmpty()) {
                contraseñaField.setError("⚠️ La contraseña no puede estar vacía");
            } else if (pass.length() < 6) {
                contraseñaField.setError("⚠️ Debe tener mínimo 6 caracteres");
            } else {
                contraseñaField.setError(null);
            }
        }));
    }

    private boolean hayConexion() {
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void registrarUsuario() {
        String nombreCompleto = nombreCompletoField.getText().toString().trim();
        String clave = claveField.getText().toString().trim();
        String correo = correoField.getText().toString().trim();
        String contraseña = contraseñaField.getText().toString().trim();
        String categoriaCentro = spinner2.getSelectedItem().toString();
        String centro = spinner3.getSelectedItem().toString();

        if (!hayConexion()) {
            Toast.makeText(getContext(), "❌ No hay conexión a Internet", Toast.LENGTH_SHORT).show();
            return;
        }

        if (nombreCompleto.isEmpty() || clave.isEmpty() || correo.isEmpty() || contraseña.isEmpty()) {
            Toast.makeText(getContext(), "⚠️ Complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            correoField.setError("⚠️ Correo inválido"); return;
        }
        if (contraseña.length() < 6) {
            contraseñaField.setError("⚠️ Debe tener mínimo 6 caracteres"); return;
        }

        mAuth.createUserWithEmailAndPassword(correo, contraseña)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userIdAuth = mAuth.getCurrentUser().getUid();

                        // 🔹 Generar un ID propio (aparte del UID de Firebase)
                        String idUsuario = "USR_" + System.currentTimeMillis();

                        // Crear objeto Usuario con UID de Firebase incluido
                        Usuario usuario = new Usuario(
                                idUsuario,
                                nombreCompleto,
                                categoriaCentro,
                                centro,
                                clave,
                                correo,
                                contraseña,
                                userIdAuth   // 👈 UID real de FirebaseAuth
                        );

                        // Guardar en BD
                        databaseReference.child(userIdAuth).setValue(usuario)
                                .addOnSuccessListener(aVoid -> {
                                    // Guardar en SharedPreferences
                                    prefs.edit().putString("clave_" + clave, correo).apply();

                                    Toast.makeText(getContext(), "✅ Registro exitoso", Toast.LENGTH_SHORT).show();

                                    // Vaciar campos
                                    nombreCompletoField.setText("");
                                    claveField.setText("");
                                    correoField.setText("");
                                    contraseñaField.setText("");

                                    // Ir a Login
                                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                                    startActivity(intent);
                                    getActivity().finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "❌ Error al guardar en BD", Toast.LENGTH_SHORT).show();
                                    Log.e("Registro", "Error BD: " + e.getMessage());
                                });

                    } else {
                        String error = task.getException().getMessage();
                        Log.e("Registro", "Error en Auth: " + error);

                        if (error.contains("email address is already in use")) {
                            correoField.setError("⚠️ El correo ya está en uso");
                        } else if (error.contains("badly formatted")) {
                            correoField.setError("⚠️ El correo no es válido");
                        } else if (error.contains("Password should be at least 6 characters")) {
                            contraseñaField.setError("⚠️ La contraseña debe tener al menos 6 caracteres");
                        } else if (error.contains("network error")) {
                            Toast.makeText(getContext(), "❌ Error de red. Verifique su Internet.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getContext(), "❌ Error: " + error, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // Clase auxiliar para simplificar TextWatcher
    private static class SimpleWatcher implements TextWatcher {
        private Runnable callback;
        SimpleWatcher(Runnable callback) { this.callback = callback; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { callback.run(); }
        @Override public void afterTextChanged(Editable s) {}
    }
}
