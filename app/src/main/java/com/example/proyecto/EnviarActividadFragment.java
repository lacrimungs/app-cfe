package com.example.proyecto;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

/**
 * Fragment para crear/enviar actividad.
 * - Abre un selector de usuarios (BottomSheet) tocando la lupa o el cuadro completo.
 * - Mantiene selectedUsuarios (UIDs) y muestra los nombres seleccionados.
 */
public class EnviarActividadFragment extends Fragment {

    private static final Logger LOGGER = Logger.getLogger(EnviarActividadFragment.class.getName());

    /** Claves para Fragment Result API */
    public static final String FR_KEY_RESULT = "seleccion_usuarios_result";
    public static final String FR_BUNDLE_SELECTED = "uids_seleccionados";

    // Firebase
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;

    // UI
    private TextView selectedValueTextView;
    private ImageButton btnBuscarUsuarios;

    // Selección (guardamos UIDs)
    private final List<String> selectedUsuarios = new ArrayList<>();
    private final Map<String, String> nombreToUidMap = new HashMap<>();
    private final Map<String, String> uidToNombreMap = new HashMap<>();

    // Fuente de datos cruda para armar el selector
    private final List<UsuariosSelectedAdapter.UsuarioItem> dataUsuarios = new ArrayList<>();

    public EnviarActividadFragment() { }

    public static EnviarActividadFragment newInstance() {
        return new EnviarActividadFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_enviar_actividad, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("actividades");
        mAuth = FirebaseAuth.getInstance();

        // UI
        selectedValueTextView = view.findViewById(R.id.selected_value_text);
        btnBuscarUsuarios     = view.findViewById(R.id.btnBuscarUsuarios);
        Button guardarButton  = view.findViewById(R.id.enviar);

        // >>> NUEVO: el cuadro completo también abre el selector <<<
        View boxSeleccion = view.findViewById(R.id.toolbarSeleccionUsuarios);
        View.OnClickListener openSelector = v -> abrirSelectorUsuarios();
        btnBuscarUsuarios.setOnClickListener(openSelector);
        boxSeleccion.setOnClickListener(openSelector);
        // <<< FIN NUEVO >>>

        // Cargar usuarios (para el selector)
        obtenerUsuarios();

        // Recibir actividad desde argumentos (si la mandas en el Bundle)
        Bundle bundle = getArguments();
        Actividad actividad = null;
        if (bundle != null) {
            actividad = (Actividad) bundle.getSerializable("actividad");
        }
        Actividad finalActividad = actividad;

        // Escuchar resultado del BottomSheet (Fragment Result API)
        getParentFragmentManager().setFragmentResultListener(FR_KEY_RESULT, getViewLifecycleOwner(),
                (requestKey, result) -> {
                    ArrayList<String> uids = result.getStringArrayList(FR_BUNDLE_SELECTED);
                    selectedUsuarios.clear();
                    if (uids != null) selectedUsuarios.addAll(uids);
                    actualizarTextoSeleccionado();
                });

        // Guardar
        guardarButton.setOnClickListener(v -> {
            if (finalActividad != null) {
                String currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
                if (!TextUtils.isEmpty(currentUid) && !selectedUsuarios.contains(currentUid)) {
                    selectedUsuarios.add(currentUid); // agrega creador
                }
                finalActividad.setParticipantes(new ArrayList<>(selectedUsuarios));
                guardarActividad(finalActividad);
            } else {
                LOGGER.log(Level.WARNING, "Actividad es nula al intentar guardar.");
                Toast.makeText(requireContext(), "Actividad nula", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Llama la API y llena dataUsuarios + mapas de nombre/uid */
    private void obtenerUsuarios() {
        Retrofit retrofit = ApiClient.getClient();
        ApiService apiService = retrofit.create(ApiService.class);

        Call<Map<String, Usuario>> call = apiService.getUsuarios();
        call.enqueue(new Callback<Map<String, Usuario>>() {
            @Override
            public void onResponse(Call<Map<String, Usuario>> call, Response<Map<String, Usuario>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful()) {
                    Map<String, Usuario> usuariosMap = response.body();
                    String currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

                    dataUsuarios.clear();
                    nombreToUidMap.clear();
                    uidToNombreMap.clear();

                    if (usuariosMap != null) {
                        for (Usuario usuario : usuariosMap.values()) {
                            if (usuario.getUidFirebase() == null) continue;

                            // opcional: excluir creador, se agrega automático al guardar
                            if (currentUid != null && usuario.getUidFirebase().equals(currentUid)) {
                                continue;
                            }

                            String uid = usuario.getUidFirebase();
                            String nombre = usuario.getNombreCompleto();

                            nombreToUidMap.put(nombre, uid);
                            uidToNombreMap.put(uid, nombre);

                            boolean preSelected = selectedUsuarios.contains(uid);
                            dataUsuarios.add(new UsuariosSelectedAdapter.UsuarioItem(uid, nombre, preSelected));
                        }
                    }

                    actualizarTextoSeleccionado();
                } else {
                    LOGGER.log(Level.SEVERE, "Error al obtener los usuarios. Respuesta no exitosa.");
                    Toast.makeText(requireContext(), "Error al obtener los usuarios", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Usuario>> call, Throwable t) {
                if (!isAdded()) return;
                LOGGER.log(Level.SEVERE, "Error API usuarios: " + t.getMessage(), t);
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Abre el BottomSheet para seleccionar usuarios */
    private void abrirSelectorUsuarios() {
        ArrayList<UsuariosSelectedAdapter.UsuarioItem> copia = new ArrayList<>();
        for (UsuariosSelectedAdapter.UsuarioItem it : dataUsuarios) {
            copia.add(new UsuariosSelectedAdapter.UsuarioItem(it.uid, it.nombre, selectedUsuarios.contains(it.uid)));
        }
        SeleccionarUsuariosBottomSheet sheet = SeleccionarUsuariosBottomSheet.newInstance(copia);
        sheet.show(getParentFragmentManager(), "SeleccionarUsuariosBottomSheet");
    }

    /** Actualiza el cuadro con los nombres de los seleccionados */
    private void actualizarTextoSeleccionado() {
        if (selectedUsuarios.isEmpty()) {
            selectedValueTextView.setText("Participantes: ninguno");
            return;
        }
        StringBuilder sb = new StringBuilder("Participantes:\n");
        for (String uid : selectedUsuarios) {
            String nombre = uidToNombreMap.get(uid);
            sb.append(nombre != null ? nombre : uid).append("\n");
        }
        selectedValueTextView.setText(sb.toString());
    }

    /** Guarda la actividad y programa alarmas locales */
    private void guardarActividad(Actividad actividad) {
        try {
            if (actividad == null) {
                Toast.makeText(requireContext(), "Actividad nula", Toast.LENGTH_SHORT).show();
                return;
            }

            databaseReference.push().setValue(actividad)
                    .addOnSuccessListener(aVoid -> {
                        LOGGER.log(Level.INFO, "Actividad guardada exitosamente.");
                        Toast.makeText(requireContext(), "Actividad guardada exitosamente", Toast.LENGTH_SHORT).show();
                        programarNotificacionesLocales(actividad);
                    })
                    .addOnFailureListener(e -> {
                        LOGGER.log(Level.SEVERE, "Error al guardar la actividad: " + e.getMessage(), e);
                        Toast.makeText(requireContext(), "Error al guardar la actividad: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Excepción al guardar: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /** Programa notificaciones locales a participantes + creador */
    private void programarNotificacionesLocales(Actividad actividad) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String creadorNombre = obtenerNombreCompletoUsuario(actividad.getUserId());

        db.collection("usuarios").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Usuario usuario = document.toObject(Usuario.class);
                    if (actividad.getParticipantes().contains(usuario.getUidFirebase())
                            || usuario.getUidFirebase().equals(actividad.getUserId())) {
                        programarNotificacion(usuario, actividad, creadorNombre);
                    }
                }
            } else {
                if (!isAdded()) return;
                LOGGER.log(Level.SEVERE, "Error al obtener usuarios de Firestore: " + task.getException().getMessage());
                Toast.makeText(requireContext(), "Error al obtener los usuarios: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void programarNotificacion(Usuario usuario, Actividad actividad, String creadorNombre) {
        try {
            Intent intent = new Intent(requireContext(), AlarmReceiver.class);
            intent.putExtra("title", "Recordatorio de Actividad");
            intent.putExtra("message", creadorNombre + " ha creado una actividad: " + actividad.getNombredelaactividad());

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    requireContext(), usuario.getUidFirebase().hashCode(), intent, flags
            );

            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            Calendar calendar = Calendar.getInstance();

            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                calendar.setTime(sdf.parse(actividad.getFecha()));
            } catch (ParseException e) {
                LOGGER.log(Level.SEVERE, "Error al analizar la fecha: " + e.getMessage(), e);
            }

            switch (actividad.getRecordatorio()) {
                case "Hora":   calendar.add(Calendar.HOUR_OF_DAY, -1);   break;
                case "Mes":    calendar.add(Calendar.MONTH, -1);         break;
                case "Semana": calendar.add(Calendar.WEEK_OF_YEAR, -1);  break;
                case "Dia":    calendar.add(Calendar.DAY_OF_YEAR, -1);   break;
            }

            if (alarmManager != null) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al programar la notificación: " + e.getMessage(), e);
        }
    }

    private String obtenerNombreCompletoUsuario(String userId) {
        return "Participante";
    }
}
