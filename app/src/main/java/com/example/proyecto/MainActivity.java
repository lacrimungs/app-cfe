package com.example.proyecto;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Forzar el tema claro
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_main);

        // Cambiar color de la barra de estado
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(getResources().getColor(R.color.cfe_green));
        }

        // Crear el canal de notificaciones (Android 8+)
        createNotificationChannel();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();

            // 🔹 Evita recrear fragments en cada cambio
            AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.navigation_home, R.id.navigation_campana, R.id.navigation_micuenta
            ).build();

            NavigationUI.setupWithNavController(bottomNavigationView, navController);

            // IMPORTANTE: guardar el estado de los fragments
            bottomNavigationView.setOnItemSelectedListener(item -> {
                boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
                if (handled) {
                    navController.getCurrentBackStackEntry()
                            .getSavedStateHandle()
                            .set("SAVE_STATE", true);
                }
                return handled;
            });

        } else {
            throw new IllegalStateException("NavHostFragment no encontrado");
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Recordatorios";
            String description = "Canal para notificaciones de reuniones, cursos y actividades";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("recordatorios", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
