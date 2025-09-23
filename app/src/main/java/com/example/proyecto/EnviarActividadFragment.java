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
import java.util.logging.Level;
import java.util.logging.Logger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class EnviarActividadFragment extends Fragment {

    private static final Logger LOGGER = Logger.getLogger(EnviarActividadFragment.class.getName());

    private DatabaseReference databaseReference;
    private Spinner usuariosSpinner;
    private FirebaseAuth mAuth;
    private List<String> selectedUsuarios = new ArrayList<>(); // 🔹 Guardamos UIDs
    private TextView selectedValueTextView;

    // 🔹 Mapas para mostrar nombres pero guardar UIDs
    private Map<String, String> nombreToUidMap = new HashMap<>();
    private Map<String, String> uidToNombreMap = new HashMap<>();

    public EnviarActividadFragment() {}

    public static EnviarActividadFragment newInstance() {
        return new EnviarActividadFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_enviar_actividad, container, false);

        databaseReference = FirebaseDatabase.getInstance().getReference("actividades");
        mAuth = FirebaseAuth.getInstance();

        usuariosSpinner = view.findViewById(R.id.spinner);
        selectedValueTextView = view.findViewById(R.id.selected_value_text);
        obtenerUsuarios();

        // Obtener el objeto Actividad del Bundle
        Bundle bundle = getArguments();
        Actividad actividad = null;
        if (bundle != null) {
            actividad = (Actividad) bundle.getSerializable("actividad");
        }

        // Botón "Guardar"
        Button guardarButton = view.findViewById(R.id.enviar);
        Actividad finalActividad = actividad;
        guardarButton.setOnClickListener(v -> {
            if (finalActividad != null) {
                String currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

                // 🔹 Agregar creador automáticamente a la lista de participantes
                if (currentUid != null && !selectedUsuarios.contains(currentUid)) {
                    selectedUsuarios.add(currentUid);
                }

                finalActividad.setParticipantes(selectedUsuarios);
                guardarActividad(finalActividad);
            } else {
                LOGGER.log(Level.WARNING, "Actividad es nula al intentar guardar.");
            }
        });

        // Selección múltiple de usuarios
        usuariosSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedNombre = parent.getItemAtPosition(position).toString();

                // 🔹 Si es el placeholder, no hacer nada
                if (selectedNombre.equals("-- Selecciona un participante --")) {
                    return;
                }

                String uid = nombreToUidMap.get(selectedNombre);

                if (uid != null && !selectedUsuarios.contains(uid)) {
                    selectedUsuarios.add(uid); // 🔹 Guardamos UID
                    actualizarTextoSeleccionado();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                LOGGER.log(Level.INFO, "No se seleccionó ningún usuario.");
            }
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
                    Map<String, Usuario> usuariosMap = response.body();
                    List<String> nombresCompletos = new ArrayList<>();

                    String currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

                    // 🔹 Agregamos un placeholder inicial
                    nombresCompletos.add("-- Selecciona los participantes --");

                    if (usuariosMap != null) {
                        for (Usuario usuario : usuariosMap.values()) {
                            // 🔹 Excluir al creador de la actividad
                            if (usuario.getUidFirebase() != null && !usuario.getUidFirebase().equals(currentUid)) {
                                nombresCompletos.add(usuario.getNombreCompleto());
                                nombreToUidMap.put(usuario.getNombreCompleto(), usuario.getUidFirebase());
                                uidToNombreMap.put(usuario.getUidFirebase(), usuario.getNombreCompleto());
                            }
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                            android.R.layout.simple_spinner_item, nombresCompletos);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    usuariosSpinner.setAdapter(adapter);
                } else {
                    LOGGER.log(Level.SEVERE, "Error al obtener los usuarios. Respuesta no exitosa.");
                    Toast.makeText(getContext(), "Error al obtener los usuarios", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Usuario>> call, Throwable t) {
                LOGGER.log(Level.SEVERE, "Error al realizar la llamada a la API: " + t.getMessage(), t);
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void actualizarTextoSeleccionado() {
        StringBuilder seleccionados = new StringBuilder("Participantes:\n");
        for (String uid : selectedUsuarios) {
            String nombre = uidToNombreMap.get(uid);
            seleccionados.append(nombre != null ? nombre : uid).append("\n");
        }
        selectedValueTextView.setText(seleccionados.toString());
    }

    private void guardarActividad(Actividad actividad) {
        try {
            if (actividad != null) {
                databaseReference.push().setValue(actividad)
                        .addOnSuccessListener(aVoid -> {
                            LOGGER.log(Level.INFO, "Actividad guardada exitosamente.");
                            Toast.makeText(getContext(), "Actividad guardada exitosamente", Toast.LENGTH_SHORT).show();
                            programarNotificacionesLocales(actividad);
                        })
                        .addOnFailureListener(e -> {
                            LOGGER.log(Level.SEVERE, "Error al guardar la actividad: " + e.getMessage(), e);
                            Toast.makeText(getContext(), "Error al guardar la actividad: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            } else {
                LOGGER.log(Level.WARNING, "La actividad no se ha podido guardar ya que es nula.");
                Toast.makeText(getContext(), "Actividad nula", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Excepción inesperada al guardar la actividad: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void programarNotificacionesLocales(Actividad actividad) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String creadorNombre = obtenerNombreCompletoUsuario(actividad.getUserId());

        db.collection("usuarios").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Usuario usuario = document.toObject(Usuario.class);
                    if (actividad.getParticipantes().contains(usuario.getUidFirebase()) ||
                            usuario.getUidFirebase().equals(actividad.getUserId())) {
                        programarNotificacion(usuario, actividad, creadorNombre);
                    }
                }
            } else {
                LOGGER.log(Level.SEVERE, "Error al obtener los usuarios de Firestore: " + task.getException().getMessage());
                Toast.makeText(getContext(), "Error al obtener los usuarios: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void programarNotificacion(Usuario usuario, Actividad actividad, String creadorNombre) {
        try {
            Intent intent = new Intent(getContext(), AlarmReceiver.class);
            intent.putExtra("title", "Recordatorio de Actividad");
            intent.putExtra("message", creadorNombre + " ha creado una actividad: " + actividad.getNombredelaactividad());

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    getContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
            );

            AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
            Calendar calendar = Calendar.getInstance();

            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                calendar.setTime(sdf.parse(actividad.getFecha()));
            } catch (ParseException e) {
                LOGGER.log(Level.SEVERE, "Error al analizar la fecha: " + e.getMessage(), e);
            }

            switch (actividad.getRecordatorio()) {
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
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al programar la notificación: " + e.getMessage(), e);
        }
    }

    private String obtenerNombreCompletoUsuario(String userId) {
        // 🔹 Aquí puedes implementar una consulta real a Firestore para obtener el nombre
        return "Participante";
    }
}
