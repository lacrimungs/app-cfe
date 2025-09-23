package com.example.proyecto;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.os.Build;
import android.view.Window;

public class RegisterActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Cambiar el color de la barra de estado sin alterar la lógica de la actividad
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(getResources().getColor(R.color.cfe_green));  // Establecer el color CFE verde
        }

        // Cargar el fragmento solo si no hay una instancia guardada
        if (savedInstanceState == null) {
            loadFragment(new RegisterFragment());
        }
    }

    private void loadFragment(Fragment fragment) {
        // Crear una transacción de fragmento
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // Reemplazar el contenido del contenedor con el nuevo fragmento
        fragmentTransaction.replace(R.id.register_fragment_container, fragment);
        fragmentTransaction.commit();
    }
}

