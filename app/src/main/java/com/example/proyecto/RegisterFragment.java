package com.example.proyecto;

import android.os.Bundle;
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

    // Referencias para los campos de entrada y Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private EditText nombreCompletoField, claveField, contraseñaField;
    private Spinner spinner1, spinner2, spinner3;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflar el layout para este fragmento
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        // Configurar Firebase Authentication y Database
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("usuarios");

        // Primer Spinner
        spinner1 = view.findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(getContext(),
                R.array.spinner_items_5, android.R.layout.simple_spinner_item);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner1.setAdapter(adapter1);

        // Segundo Spinner
        spinner2 = view.findViewById(R.id.spinne);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(getContext(),
                R.array.spinner_items_6, android.R.layout.simple_spinner_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner2.setAdapter(adapter2);

        // Tercer Spinner
        spinner3 = view.findViewById(R.id.spinner3);
        ArrayAdapter<CharSequence> adapter3 = ArrayAdapter.createFromResource(getContext(),
                R.array.spinner_items_7, android.R.layout.simple_spinner_item);
        adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner3.setAdapter(adapter3);

        // Obtener referencias de los campos de entrada adicionales
        nombreCompletoField = view.findViewById(R.id.nombreCompletoField);
        claveField = view.findViewById(R.id.claveField);
        contraseñaField = view.findViewById(R.id.contraseñaField);

        // Configurar botón de registro
        Button registrarButton = view.findViewById(R.id.enviar);
        registrarButton.setOnClickListener(v -> registrarUsuario());

        return view;
    }

    private void registrarUsuario() {
        // Obtener los valores de los campos de entrada
        String nombreCompleto = nombreCompletoField.getText().toString().trim();
        String clave = claveField.getText().toString().trim();
        String contraseña = contraseñaField.getText().toString().trim();
        String tipoEmpleado = spinner1.getSelectedItem().toString();
        String categoriaCentro = spinner2.getSelectedItem().toString();
        String centro = spinner3.getSelectedItem().toString();

        // Validar campos antes de enviar
        if (nombreCompleto.isEmpty() || clave.isEmpty() || contraseña.isEmpty()) {
            Toast.makeText(getContext(), "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generar un correo falso basado en la clave
        String email = clave + "@example.com";

        // Crear un usuario en Firebase Authentication usando el correo generado
        mAuth.createUserWithEmailAndPassword(email, contraseña)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Registro exitoso en Authentication, guardar información adicional en Firebase Database
                        String userId = mAuth.getCurrentUser().getUid();
                        Usuario usuario = new Usuario(nombreCompleto, tipoEmpleado, categoriaCentro, centro, clave, contraseña);
                        databaseReference.child(userId).setValue(usuario)
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(getContext(), "Registro exitoso", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(getContext(), "Error al registrar", Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(getContext(), "Error al registrar: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
