package com.example.proyecto;

import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.icu.util.Calendar;
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
import java.util.Locale;

public class FomularioReunionFragment extends Fragment {

    private EditText tituloInput;
    private EditText motivodelareunionInput;
    private EditText fechaInput;
    private Spinner medioSpinner;
    private Spinner recordatorioSpinner;
    private FirebaseAuth mAuth;

    // Micrófonos
    private ImageButton btnMicAsunto, btnMicMotivo;
    private static final int REQ_CODE_ASUNTO = 300;
    private static final int REQ_CODE_MOTIVO = 301;

    public FomularioReunionFragment() {
        // Constructor vacío requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflar el layout correcto
        View view = inflater.inflate(R.layout.fragment_fomulario_reunion, container, false);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();

        // Campos
        tituloInput = view.findViewById(R.id.asunto);
        motivodelareunionInput = view.findViewById(R.id.motivodelareunion);
        fechaInput = view.findViewById(R.id.fecha);
        medioSpinner = view.findViewById(R.id.my_spinner);
        recordatorioSpinner = view.findViewById(R.id.spinner);

        // Micrófonos
        btnMicAsunto = view.findViewById(R.id.btnMicAsunto);
        btnMicMotivo = view.findViewById(R.id.btnMicMotivoReunion);

        btnMicAsunto.setOnClickListener(v -> startVoiceInput(REQ_CODE_ASUNTO));
        btnMicMotivo.setOnClickListener(v -> startVoiceInput(REQ_CODE_MOTIVO));

        // Spinners
        ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(
                getContext(),
                R.array.spinner_items,
                android.R.layout.simple_spinner_item);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        medioSpinner.setAdapter(adapter1);

        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(
                getContext(),
                R.array.spinner_items_2,
                android.R.layout.simple_spinner_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        recordatorioSpinner.setAdapter(adapter2);

        // Calendario
        fechaInput.setOnClickListener(v -> mostrarDatePicker());

        // Botón enviar
        Button enviarButton = view.findViewById(R.id.enviar);
        enviarButton.setOnClickListener(v -> enviarReunion(view));

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
                year, month, day);
        datePickerDialog.show();
    }

    // 🎤 Dictado de voz
    private void startVoiceInput(int requestCode) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hable ahora...");
        try {
            startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "Tu dispositivo no soporta entrada por voz", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == getActivity().RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (requestCode == REQ_CODE_ASUNTO) {
                tituloInput.setText(result.get(0));
            } else if (requestCode == REQ_CODE_MOTIVO) {
                motivodelareunionInput.setText(result.get(0));
            }
        }
    }

    private void enviarReunion(View view) {
        try {
            String asunto = tituloInput.getText().toString();
            String fecha = fechaInput.getText().toString();
            String medio = medioSpinner.getSelectedItem().toString();
            String motivodelareunion = motivodelareunionInput.getText().toString();
            String recordatorio = recordatorioSpinner.getSelectedItem().toString();

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();

                Reunion reunion = new Reunion(asunto, motivodelareunion, fecha, medio, recordatorio, userId, new ArrayList<>());

                Bundle bundle = new Bundle();
                bundle.putSerializable("reunion", reunion);

                Navigation.findNavController(view).navigate(R.id.navigation_enviar_reunion, bundle);
            } else {
                Toast.makeText(getContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
