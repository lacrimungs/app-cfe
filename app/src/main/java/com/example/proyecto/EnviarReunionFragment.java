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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class EnviarReunionFragment extends Fragment {

    // Claves propias para este fragmento
    public static final String FR_KEY_RESULT_REU = "seleccion_usuarios_result_reu";
    public static final String FR_BUNDLE_SELECTED_REU = "uids_seleccionados_reu";

    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;

    private TextView selectedValueTextView;
    private ImageButton btnBuscarUsuarios;
    private View boxSeleccion;

    // Mantener UIDs seleccionados y mapa UID->Nombre
    private final List<String> selectedUsuarios = new ArrayList<>();
    private final Map<String, String> uidToNombreMap = new HashMap<>();

    // Fuente para el BottomSheet
    private final List<UsuariosSelectedAdapter.UsuarioItem> dataUsuarios = new ArrayList<>();

    public EnviarReunionFragment() {}

    public static EnviarReunionFragment newInstance() { return new EnviarReunionFragment(); }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_enviar_reunion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("reuniones");

        selectedValueTextView = view.findViewById(R.id.selected_value_text_reu);
        btnBuscarUsuarios     = view.findViewById(R.id.btnBuscarUsuariosReu);
        boxSeleccion          = view.findViewById(R.id.toolbarSeleccionUsuariosReu);
        Button guardarButton  = view.findViewById(R.id.enviar);

        // Abrir selector tocando la lupa o el cuadro completo
        View.OnClickListener open = v -> abrirSelectorUsuarios();
        btnBuscarUsuarios.setOnClickListener(open);
        boxSeleccion.setOnClickListener(open);

        // Cargar usuarios
        obtenerUsuarios();

        // Obtener la reunión del bundle
        Reunion reunion = null;
        Bundle bundle = getArguments();
        if (bundle != null) reunion = (Reunion) bundle.getSerializable("reunion");
        Reunion finalReunion = reunion;

        // Escuchar resultado del sheet (usamos puente para reutilizar el mismo sheet)
        getParentFragmentManager().setFragmentResultListener(
                EnviarActividadFragment.FR_KEY_RESULT, getViewLifecycleOwner(), (key, res) -> {
                    ArrayList<String> uids = res.getStringArrayList(EnviarActividadFragment.FR_BUNDLE_SELECTED);
                    Bundle out = new Bundle();
                    out.putStringArrayList(FR_BUNDLE_SELECTED_REU, uids);
                    getParentFragmentManager().setFragmentResult(FR_KEY_RESULT_REU, out);
                });

        getParentFragmentManager().setFragmentResultListener(
                FR_KEY_RESULT_REU, getViewLifecycleOwner(), (key, res) -> {
                    ArrayList<String> uids = res.getStringArrayList(FR_BUNDLE_SELECTED_REU);
                    selectedUsuarios.clear();
                    if (uids != null) selectedUsuarios.addAll(uids);
                    actualizarTextoSeleccionado();
                });

        // Guardar
        guardarButton.setOnClickListener(v -> {
            if (finalReunion == null) {
                Toast.makeText(requireContext(), "Reunión nula", Toast.LENGTH_SHORT).show();
                return;
            }
            String currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
            if (!TextUtils.isEmpty(currentUid) && !selectedUsuarios.contains(currentUid)) {
                selectedUsuarios.add(currentUid); // agrega creador
            }
            finalReunion.setParticipantes(new ArrayList<>(selectedUsuarios));
            guardarReunion(finalReunion);
        });
    }

    /** Obtiene usuarios del API y arma la lista para el selector */
    private void obtenerUsuarios() {
        Retrofit retrofit = ApiClient.getClient();
        ApiService apiService = retrofit.create(ApiService.class);

        apiService.getUsuarios().enqueue(new Callback<Map<String, Usuario>>() {
            @Override
            public void onResponse(Call<Map<String, Usuario>> call, Response<Map<String, Usuario>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful()) {
                    Map<String, Usuario> usuarios = response.body();
                    String currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

                    dataUsuarios.clear();
                    uidToNombreMap.clear();

                    if (usuarios != null) {
                        for (Usuario u : usuarios.values()) {
                            if (u.getUidFirebase() == null) continue;
                            if (currentUid != null && currentUid.equals(u.getUidFirebase())) {
                                // se agrega automáticamente al guardar
                                continue;
                            }
                            String uid = u.getUidFirebase();
                            String nombre = u.getNombreCompleto();
                            uidToNombreMap.put(uid, nombre);
                            boolean preSel = selectedUsuarios.contains(uid);
                            dataUsuarios.add(new UsuariosSelectedAdapter.UsuarioItem(uid, nombre, preSel));
                        }
                    }
                    actualizarTextoSeleccionado();
                } else {
                    Toast.makeText(requireContext(), "Error al obtener usuarios", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Usuario>> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Abre el BottomSheet de selección reutilizado */
    private void abrirSelectorUsuarios() {
        ArrayList<UsuariosSelectedAdapter.UsuarioItem> copia = new ArrayList<>();
        for (UsuariosSelectedAdapter.UsuarioItem it : dataUsuarios) {
            copia.add(new UsuariosSelectedAdapter.UsuarioItem(it.uid, it.nombre, selectedUsuarios.contains(it.uid)));
        }
        SeleccionarUsuariosBottomSheet sheet = SeleccionarUsuariosBottomSheet.newInstance(copia);
        sheet.show(getParentFragmentManager(), "SeleccionarUsuariosBottomSheetReunion");
    }

    /** Muestra los nombres elegidos */
    private void actualizarTextoSeleccionado() {
        if (selectedUsuarios.isEmpty()) {
            selectedValueTextView.setText("Participantes: ninguno");
            return;
        }
        StringBuilder sb = new StringBuilder("Participantes:\n");
        for (String uid : selectedUsuarios) {
            String nombre = uidToNombreMap.get(uid);
            sb.append(nombre != null ? nombre : uid).append('\n');
        }
        selectedValueTextView.setText(sb.toString());
    }

    /** Guarda y programa notificaciones locales */
    private void guardarReunion(Reunion reunion) {
        try {
            databaseReference.push().setValue(reunion)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(requireContext(), "Reunión guardada exitosamente", Toast.LENGTH_SHORT).show();
                        programarNotificacionesLocales(reunion);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(), "Error al guardar la reunión: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void programarNotificacionesLocales(Reunion reunion) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String creadorNombre = obtenerNombreCompletoUsuario(reunion.getUserId());

        db.collection("usuarios").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Usuario usuario = document.toObject(Usuario.class);
                    if (reunion.getParticipantes().contains(usuario.getUidFirebase())
                            || usuario.getUidFirebase().equals(reunion.getUserId())) {
                        programarNotificacion(usuario, reunion, creadorNombre);
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Error al obtener los usuarios: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void programarNotificacion(Usuario usuario, Reunion reunion, String creadorNombre) {
        Intent intent = new Intent(requireContext(), AlarmReceiver.class);
        intent.putExtra("title", "Recordatorio de Reunión");
        intent.putExtra("message", creadorNombre + " ha creado una reunión: " + reunion.getAsunto());

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(), usuario.getUidFirebase().hashCode(), intent, flags);

        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            calendar.setTime(sdf.parse(reunion.getFecha()));
        } catch (ParseException ignored) {}

        switch (reunion.getRecordatorio()) {
            case "Hora":   calendar.add(Calendar.HOUR_OF_DAY, -1);   break;
            case "Mes":    calendar.add(Calendar.MONTH, -1);         break;
            case "Semana": calendar.add(Calendar.WEEK_OF_YEAR, -1);  break;
            case "Dia":    calendar.add(Calendar.DAY_OF_YEAR, -1);   break;
        }

        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    private String obtenerNombreCompletoUsuario(String userId) {
        // Implementa consulta real si lo necesitas
        return "Nombre Creador";
    }
}
