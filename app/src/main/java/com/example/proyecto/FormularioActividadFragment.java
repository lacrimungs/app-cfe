package com.example.proyecto;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class FormularioActividadFragment extends Fragment {

    private static final int REQ_CODE_NOMBRE = 100;
    private static final int REQ_CODE_MOTIVO = 101;

    private EditText nombreActividadInput;
    private EditText motivoActividadInput;
    private EditText fechaInput;
    private Spinner recordatorioSpinner;
    private FirebaseAuth mAuth;

    // 🎤 Botones de micrófono
    private ImageButton btnMicNombre, btnMicMotivo;

    public FormularioActividadFragment() {
        // Constructor vacío requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_formulario_actividad, container, false);

        // Inicializar Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Inicializar los campos de entrada
        nombreActividadInput = view.findViewById(R.id.nombre_actividad);
        motivoActividadInput = view.findViewById(R.id.motivo_actividad);
        fechaInput = view.findViewById(R.id.fecha);
        recordatorioSpinner = view.findViewById(R.id.spinner);

        // 🎤 Referencias a los botones de micrófono en el XML
        btnMicNombre = view.findViewById(R.id.btnMicNombre);
        btnMicMotivo = view.findViewById(R.id.btnMicMotivo);

        // Configurar el Spinner de recordatorio
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(
                getContext(),
                R.array.spinner_items_2,
                android.R.layout.simple_spinner_item
        );
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        recordatorioSpinner.setAdapter(adapter2);

        // 📅 Selector de fecha
        fechaInput.setOnClickListener(v -> mostrarDatePicker());

        // 🎤 Dictado de voz
        btnMicNombre.setOnClickListener(v -> startVoiceInput(REQ_CODE_NOMBRE));
        btnMicMotivo.setOnClickListener(v -> startVoiceInput(REQ_CODE_MOTIVO));

        // Botón "Enviar"
        Button enviarButton = view.findViewById(R.id.enviar);
        enviarButton.setOnClickListener(v -> enviarActividad(view));

        return view;
    }

    private void mostrarDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year1, month1, dayOfMonth) ->
                        fechaInput.setText(dayOfMonth + "/" + (month1 + 1) + "/" + year1),
                year, month, day
        );
        datePickerDialog.show();
    }

    private void enviarActividad(View view) {
        try {
            String nombreActividad = nombreActividadInput.getText().toString();
            String motivoActividad = motivoActividadInput.getText().toString();
            String fecha = fechaInput.getText().toString();
            String recordatorio = recordatorioSpinner.getSelectedItem().toString();

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();

                Actividad actividad = new Actividad(
                        nombreActividad,
                        motivoActividad,
                        fecha,
                        null, // 🔹 Ya no usamos "areasInvolucradas"
                        recordatorio,
                        userId,
                        new ArrayList<>()
                );

                Bundle bundle = new Bundle();
                bundle.putSerializable("actividad", actividad);

                Navigation.findNavController(view).navigate(R.id.navigation_enviar_actividad, bundle);
            } else {
                Toast.makeText(getContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    // === 🎤 Métodos para dictado por voz ===
    private void startVoiceInput(int requestCode) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivityForResult(intent, requestCode);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (requestCode == REQ_CODE_NOMBRE) {
                nombreActividadInput.setText(result.get(0));
            } else if (requestCode == REQ_CODE_MOTIVO) {
                motivoActividadInput.setText(result.get(0));
            }
        }
    }
}

