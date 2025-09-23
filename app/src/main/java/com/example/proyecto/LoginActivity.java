package com.example.proyecto;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.text.Editable;
import android.text.TextWatcher;

public class LoginActivity extends AppCompatActivity {

    private EditText claveEditText;
    private EditText passwordEditText;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private SharedPreferences prefs;

    // 🔹 Auto logout variables
    private static final long TIEMPO_INACTIVIDAD = 300000; // 5 minutos
    private Handler handlerInactividad = new Handler();
    private Runnable cerrarSesionRunnable = () -> cerrarSesionPorInactividad();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(getResources().getColor(R.color.cfe_green));
        }

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("usuarios");
        prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);

        claveEditText = findViewById(R.id.clave);
        passwordEditText = findViewById(R.id.contraseñaLogin);

        validarEnTiempoReal();

        TextView crearCuentaTextView = findViewById(R.id.crear_cuenta);
        crearCuentaTextView.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        Button loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener(v -> validarClaveYAutenticar());
    }

    // ✅ Detecta interacción del usuario y reinicia el temporizador
    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        reiniciarTemporizador();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reiniciarTemporizador();
    }

    @Override
    protected void onPause() {
        super.onPause();
        detenerTemporizador();
    }

    private void reiniciarTemporizador() {
        detenerTemporizador();
        handlerInactividad.postDelayed(cerrarSesionRunnable, TIEMPO_INACTIVIDAD);
    }

    private void detenerTemporizador() {
        handlerInactividad.removeCallbacks(cerrarSesionRunnable);
    }

    private void cerrarSesionPorInactividad() {
        if (mAuth.getCurrentUser() != null) {
            mAuth.signOut();
            Toast.makeText(this, "🔒 Sesión cerrada por inactividad", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(LoginActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    // ✅ Validaciones en tiempo real
    private void validarEnTiempoReal() {
        claveEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (s.toString().trim().isEmpty()) {
                    claveEditText.setError("⚠️ La clave no puede estar vacía");
                } else {
                    claveEditText.setError(null);
                }
            }
        });
        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                String pass = s.toString().trim();
                if (pass.isEmpty()) {
                    passwordEditText.setError("⚠️ La contraseña no puede estar vacía");
                } else if (pass.length() < 6) {
                    passwordEditText.setError("⚠️ Debe tener al menos 6 caracteres");
                } else {
                    passwordEditText.setError(null);
                }
            }
        });
    }

    private void validarClaveYAutenticar() {
        String clave = claveEditText.getText().toString().trim();
        String contraseña = passwordEditText.getText().toString().trim();

        if (clave.isEmpty() || contraseña.isEmpty()) {
            Toast.makeText(this, "⚠️ Ingrese su clave y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hayConexion()) {
            Toast.makeText(this, "📶 No hay conexión a Internet", Toast.LENGTH_LONG).show();
            return;
        }

        String correoGuardado = prefs.getString("clave_" + clave, null);
        if (correoGuardado != null) {
            autenticarConCorreo(correoGuardado, contraseña, clave);
        } else {
            buscarCorreoEnFirebase(clave, contraseña);
        }
    }

    private void buscarCorreoEnFirebase(String clave, String contraseña) {
        databaseReference.orderByChild("clave").equalTo(clave)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                Usuario usuario = snapshot.getValue(Usuario.class);
                                if (usuario != null) {
                                    prefs.edit().putString("clave_" + clave, usuario.getCorreo()).apply();
                                    autenticarConCorreo(usuario.getCorreo(), contraseña, clave);
                                    return;
                                }
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "❌ No existe un usuario con esa clave", Toast.LENGTH_LONG).show();
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e("LoginActivity", "Error en DB: " + databaseError.getMessage());
                        Toast.makeText(LoginActivity.this, "❌ Error al verificar la clave en la base de datos", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void autenticarConCorreo(String correo, String contraseña, String clave) {
        mAuth.signInWithEmailAndPassword(correo, contraseña)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // 🔹 Recuperar el objeto Usuario completo para obtener idUsuario
                            databaseReference.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    Usuario usuario = snapshot.getValue(Usuario.class);
                                    if (usuario != null) {
                                        // Guardar datos del usuario en SharedPreferences
                                        prefs.edit()
                                                .putString("idUsuario", usuario.getIdUsuario())
                                                .putString("nombreCompleto", usuario.getNombreCompleto())
                                                .putString("clave", clave)
                                                .putString("correo", correo)
                                                .apply();

                                        Toast.makeText(LoginActivity.this, "✅ Bienvenido " + usuario.getNombreCompleto(), Toast.LENGTH_SHORT).show();

                                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }
                                }
                                @Override
                                public void onCancelled(DatabaseError error) {
                                    Toast.makeText(LoginActivity.this, "⚠️ Error al obtener datos del usuario", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        String error = task.getException().getMessage();
                        if (error.contains("badly formatted")) {
                            Toast.makeText(this, "⚠️ El correo no tiene un formato válido", Toast.LENGTH_LONG).show();
                        } else if (error.contains("no user record")) {
                            Toast.makeText(this, "❌ No existe una cuenta con esta clave", Toast.LENGTH_LONG).show();
                        } else if (error.contains("password is invalid")) {
                            Toast.makeText(this, "❌ La contraseña es incorrecta", Toast.LENGTH_LONG).show();
                        } else if (error.contains("blocked all requests")) {
                            Toast.makeText(this, "🚫 Demasiados intentos fallidos. Intente más tarde.", Toast.LENGTH_LONG).show();
                        } else if (error.contains("network error")) {
                            Toast.makeText(this, "📶 Error de red. Verifique su conexión a Internet.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "❌ Error desconocido: " + error, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private boolean hayConexion() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
        return false;
    }
}


