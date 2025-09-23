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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class EnviarReunionFragment extends Fragment {

    private DatabaseReference databaseReference;
    private Spinner usuariosSpinner;
    private List<String> selectedUsuarios = new ArrayList<>(); // Guardamos UIDs
    private TextView selectedValueTextView;
    private FirebaseAuth mAuth;

    // Map: Nombre completo → UID de FirebaseAuth
    private Map<String, String> usuariosMapUid = new HashMap<>();

    public EnviarReunionFragment() {}

    public static EnviarReunionFragment newInstance() {
        return new EnviarReunionFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_enviar_reunion, container, false);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("reuniones");

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
        Reunion finalReunion = reunion;
        Button guardarButton = view.findViewById(R.id.enviar);
        guardarButton.setOnClickListener(v -> {
            if (finalReunion != null) {
                String currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

                // 🔹 Agregar creador automáticamente
                if (currentUid != null && !selectedUsuarios.contains(currentUid)) {
                    selectedUsuarios.add(currentUid);
                }

                finalReunion.setParticipantes(selectedUsuarios);
                guardarReunion(finalReunion);
            }
        });

        // Listener para seleccionar usuarios
        usuariosSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedNombre = parent.getItemAtPosition(position).toString();

                // 🔹 Si es el placeholder, no hacer nada
                if (selectedNombre.equals("-- Selecciona un participante --")) {
                    return;
                }

                String selectedUid = usuariosMapUid.get(selectedNombre);

                if (selectedUid != null && !selectedUsuarios.contains(selectedUid)) {
                    selectedUsuarios.add(selectedUid); // Guardamos UID
                    actualizarTextoSeleccionado();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        return view;
    }

    private void obtenerUsuarios() {
        Retrofit retrofit = ApiClient.getClient();
        ApiService apiService = retrofit.create(ApiService.class);

        Call<Map<String, Usuario>> call = apiService.getUsuarios();
        call.enqueue(new Callback<Map<String, Usuario>>() {
            @Override
            public void onResponse(Call<Map<String, Usuario>> call, Response<Map<String, Usuario>> response) {
                if (response.isSuccessful()) {
                    Map<String, Usuario> usuarios = response.body();
                    List<String> nombresCompletos = new ArrayList<>();

                    String currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

                    // 🔹 Agregar placeholder inicial
                    nombresCompletos.add("-- Selecciona un participante --");

                    if (usuarios != null) {
                        for (Usuario usuario : usuarios.values()) {
                            // Excluir al creador de la reunión
                            if (usuario.getUidFirebase() != null && !usuario.getUidFirebase().equals(currentUid)) {
                                nombresCompletos.add(usuario.getNombreCompleto());
                                usuariosMapUid.put(usuario.getNombreCompleto(), usuario.getUidFirebase());
                            }
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            getContext(),
                            android.R.layout.simple_spinner_item,
                            nombresCompletos
                    );
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
        StringBuilder seleccionados = new StringBuilder("Participantes:\n");
        for (String uid : selectedUsuarios) {
            for (Map.Entry<String, String> entry : usuariosMapUid.entrySet()) {
                if (entry.getValue().equals(uid)) {
                    seleccionados.append(entry.getKey()).append("\n");
                }
            }
        }
        selectedValueTextView.setText(seleccionados.toString());
    }

    private void guardarReunion(Reunion reunion) {
        try {
            databaseReference.push().setValue(reunion)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Reunión guardada exitosamente", Toast.LENGTH_SHORT).show();
                        programarNotificacionesLocales(reunion);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Error al guardar la reunión: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void programarNotificacionesLocales(Reunion reunion) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String creadorNombre = obtenerNombreCompletoUsuario(reunion.getUserId());

        db.collection("usuarios").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Usuario usuario = document.toObject(Usuario.class);
                    if (reunion.getParticipantes().contains(usuario.getUidFirebase()) ||
                            usuario.getUidFirebase().equals(reunion.getUserId())) {
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

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                getContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            calendar.setTime(sdf.parse(reunion.getFecha()));
        } catch (ParseException e) {
            e.printStackTrace();
        }

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
        // Aquí deberías implementar la lógica real para obtener el nombre del usuario según su UID
        return "Nombre Creador";
    }
}

