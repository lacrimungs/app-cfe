package com.example.proyecto;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        // Cambiar color de la barra de estado (parte superior)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(getResources().getColor(R.color.cfe_green));  // Establecer el color CFE verde
        }

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("usuarios");

        emailEditText = findViewById(R.id.clave);
        passwordEditText = findViewById(R.id.contraseña);

        TextView crearCuentaTextView = findViewById(R.id.crear_cuenta);
        crearCuentaTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Iniciar RegisterActivity
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        Button loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validarCredenciales();
            }
        });
    }

    private void validarCredenciales() {
        String clave = emailEditText.getText().toString().trim();
        String contraseña = passwordEditText.getText().toString().trim();

        if (clave.isEmpty() || contraseña.isEmpty()) {
            Toast.makeText(this, "Por favor, ingresa tu clave y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = clave + "@example.com"; // Generar el correo basado en la clave

        // Autenticar con Firebase Authentication
        mAuth.signInWithEmailAndPassword(email, contraseña)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        verificarUsuarioEnBaseDeDatos(user.getUid());
                    } else {
                        Toast.makeText(LoginActivity.this, "Lo sentimos no puede ingresar: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void verificarUsuarioEnBaseDeDatos(String userId) {
        databaseReference.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Usuario usuario = dataSnapshot.getValue(Usuario.class);
                    if (usuario != null) {
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Error: usuario no encontrado en la base de datos", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Error: usuario no encontrado en la base de datos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("LoginActivity", "Error al acceder a la base de datos", databaseError.toException());
                Toast.makeText(LoginActivity.this, "Error al acceder a la base de datos: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

