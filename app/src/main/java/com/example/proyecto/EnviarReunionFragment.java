package com.example.proyecto;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class EnviarReunionFragment extends Fragment {

    private DatabaseReference databaseReference;
    private Spinner usuariosSpinner;
    private List<String> selectedUsuarios = new ArrayList<>();
    private TextView selectedValueTextView;
    private FirebaseAuth mAuth;

    public EnviarReunionFragment() {
        // Constructor vacío
    }

    public static EnviarReunionFragment newInstance() {
        return new EnviarReunionFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflar el layout para este fragmento
        View view = inflater.inflate(R.layout.fragment_enviar_reunion, container, false);

        // Inicializar Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Configurar Firebase Database
        databaseReference = FirebaseDatabase.getInstance().getReference("reuniones");

        // Inicializar el Spinner y el TextView para mostrar seleccionados
        usuariosSpinner = view.findViewById(R.id.spinner);
        selectedValueTextView = view.findViewById(R.id.selected_value_text);
        obtenerUsuarios();

        // Obtener el objeto Reunion del Bundle
        Bundle bundle = getArguments();
        Reunion reunion = null;
        if (bundle != null) {
            reunion = (Reunion) bundle.getSerializable("reunion");
        }

        // Botón "Guardar"
        Button guardarButton = view.findViewById(R.id.enviar);
        Reunion finalReunion = reunion;
        guardarButton.setOnClickListener(v -> {
            if (finalReunion != null) {
                finalReunion.setParticipantes(selectedUsuarios);
                guardarReunion(finalReunion);
            }
        });

        // Añadir lógica para manejar la selección múltiple
        usuariosSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedUsuario = parent.getItemAtPosition(position).toString();
                if (!selectedUsuarios.contains(selectedUsuario)) {
                    selectedUsuarios.add(selectedUsuario);
                    actualizarTextoSeleccionado();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No hacer nada aquí
            }
        });

        // Devolver la vista inflada
        return view;
    }

    private void obtenerUsuarios() {
        // Usar ApiClient para obtener Retrofit
        Retrofit retrofit = ApiClient.getClient();
        ApiService apiService = retrofit.create(ApiService.class);

        // Llamada para obtener los usuarios
        Call<Map<String, Usuario>> call = apiService.getUsuarios();
        call.enqueue(new Callback<Map<String, Usuario>>() {
            @Override
            public void onResponse(Call<Map<String, Usuario>> call, Response<Map<String, Usuario>> response) {
                if (response.isSuccessful()) {
                    Map<String, Usuario> usuariosMap = response.body();
                    List<String> nombresCompletos = new ArrayList<>();

                    // Extraer los nombres completos de los usuarios
                    if (usuariosMap != null) {
                        for (Usuario usuario : usuariosMap.values()) {
                            nombresCompletos.add(usuario.getNombreCompleto());
                        }
                    }

                    // Configurar el Spinner con los nombres completos
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, nombresCompletos);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    usuariosSpinner.setAdapter(adapter);
                } else {
                    Toast.makeText(getContext(), "Error al obtener los usuarios", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Usuario>> call, Throwable t) {
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void actualizarTextoSeleccionado() {
        StringBuilder seleccionados = new StringBuilder("Trabajadores y colaboradores seleccionados:\n");
        for (String usuario : selectedUsuarios) {
            seleccionados.append(usuario).append("\n");
        }
        selectedValueTextView.setText(seleccionados.toString());
    }

    private void guardarReunion(Reunion reunion) {
        try {
            // Subir la reunión a Firebase
            databaseReference.push().setValue(reunion)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Reunión guardada exitosamente", Toast.LENGTH_SHORT).show();
                        programarNotificacionesLocales(reunion); // Programar notificaciones locales después de guardar
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Error al guardar la reunión: " + e.getMessage(), Toast.LENGTH_LONG).show());
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();  // Imprimir el error en el Logcat para obtener más detalles
        }
    }

    private void programarNotificacionesLocales(Reunion reunion) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String creadorNombre = obtenerNombreCompletoUsuario(reunion.getUserId());

        db.collection("usuarios").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Usuario usuario = document.toObject(Usuario.class);
                    if (reunion.getParticipantes().contains(usuario.getNombreCompleto()) || usuario.getNombreCompleto().equals(creadorNombre)) {
                        programarNotificacion(usuario, reunion, creadorNombre);
                    }
                }
            } else {
                Toast.makeText(getContext(), "Error al obtener los usuarios: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void programarNotificacion(Usuario usuario, Reunion reunion, String creadorNombre) {
        Intent intent = new Intent(getContext(), AlarmReceiver.class);
        intent.putExtra("title", "Recordatorio de Reunión");
        intent.putExtra("message", creadorNombre + " ha creado una reunión: " + reunion.getAsunto());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();

        // Calcular la fecha del recordatorio basado en la preferencia del usuario
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            calendar.setTime(sdf.parse(reunion.getFecha()));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // Ajustar el recordatorio basado en el Spinner de la reunión
        switch (reunion.getRecordatorio()) {
            case "Hora":
                calendar.add(Calendar.HOUR_OF_DAY, -1);
                break;
            case "Mes":
                calendar.add(Calendar.MONTH, -1);
                break;
            case "Semana":
                calendar.add(Calendar.WEEK_OF_YEAR, -1);
                break;
            case "Dia":
                calendar.add(Calendar.DAY_OF_YEAR, -1);
                break;
        }

        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    private String obtenerNombreCompletoUsuario(String userId) {
        // Aquí implementa la lógica para obtener el nombre completo del usuario dado su userId.
        // Esta lógica depende de cómo gestiones y almacenes tus datos de usuario.
        // Puedes obtenerlo desde Firebase Authentication, Firestore, o una API existente.
        // Por simplicidad, aquí retorna un nombre fijo.
        return "Nombre Creador";
    }
}
