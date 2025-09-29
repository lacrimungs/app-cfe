package com.example.proyecto;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
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
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class FormularioCursoFragment extends Fragment {

    private EditText fechaEditText, nombreCursoField, motivoCursoField;
    private StringBuilder fechasSeleccionadas = new StringBuilder();
    private ImageView calendarioImagen, calendarioImagen2;
    private DatabaseReference databaseReference;
    private Spinner sistemaSpinner, recordatorioSpinner;

    // Botones micrófono
    private ImageButton btnMicNombreCurso, btnMicMotivoCurso;

    // Códigos de request para reconocimiento de voz
    private static final int REQ_CODE_NOMBRE = 100;
    private static final int REQ_CODE_MOTIVO = 101;

    public FormularioCursoFragment() {
        // Constructor vacío requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_formulario_curso, container, false);

        // Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("cursos");

        // Inicializar campos
        nombreCursoField = view.findViewById(R.id.nombre_curso);
        motivoCursoField = view.findViewById(R.id.motivo_curso);
        fechaEditText = view.findViewById(R.id.fecha_curso);
        calendarioImagen = view.findViewById(R.id.calendario);
        calendarioImagen2 = view.findViewById(R.id.calendario); // si tienes otro ícono usa otro id

        // Micrófonos
        btnMicNombreCurso = view.findViewById(R.id.btnMicNombreCurso);
        btnMicMotivoCurso = view.findViewById(R.id.btnMicMotivoCurso);

        btnMicNombreCurso.setOnClickListener(v -> startVoiceInput(REQ_CODE_NOMBRE));
        btnMicMotivoCurso.setOnClickListener(v -> startVoiceInput(REQ_CODE_MOTIVO));

        // Spinners
        sistemaSpinner = view.findViewById(R.id.sistema_spinner);
        ArrayAdapter<CharSequence> sistemaAdapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.spinner_items_3,
                android.R.layout.simple_spinner_item
        );
        sistemaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sistemaSpinner.setAdapter(sistemaAdapter);

        recordatorioSpinner = view.findViewById(R.id.recordatorio_spinner);
        ArrayAdapter<CharSequence> recordatorioAdapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.spinner_items_2,
                android.R.layout.simple_spinner_item
        );
        recordatorioAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        recordatorioSpinner.setAdapter(recordatorioAdapter);

        // Calendarios
        calendarioImagen.setOnClickListener(v -> showDatePickerDialog(1));
        calendarioImagen2.setOnClickListener(v -> showDatePickerDialog(2));

        // Botón Guardar
        Button guardarButton = view.findViewById(R.id.enviar);
        guardarButton.setOnClickListener(v -> guardarCurso());

        return view;
    }

    // Reconocimiento de voz
    private void startVoiceInput(int requestCode) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hable ahora...");

        try {
            startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getContext(), "Tu dispositivo no soporta entrada por voz", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (requestCode == REQ_CODE_NOMBRE) {
                nombreCursoField.setText(result.get(0));
            } else if (requestCode == REQ_CODE_MOTIVO) {
                motivoCursoField.setText(result.get(0));
            }
        }
    }

    // DatePicker
    private void showDatePickerDialog(int fechaSeleccionada) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year1, month1, dayOfMonth) -> {
                    String date = dayOfMonth + "/" + (month1 + 1) + "/" + year1;
                    if (fechasSeleccionadas.length() > 0) {
                        fechasSeleccionadas.append(", ");
                    }
                    fechasSeleccionadas.append(date);
                    fechaEditText.setText(fechasSeleccionadas.toString());
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    // Guardar en Firebase
    private void guardarCurso() {
        String nombreCurso = nombreCursoField.getText().toString().trim();
        String motivoCurso = motivoCursoField.getText().toString().trim();
        String fechaCurso = fechaEditText.getText().toString().trim();
        String sistema = sistemaSpinner.getSelectedItem().toString();
        String recordatorio = recordatorioSpinner.getSelectedItem().toString();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String userId = (currentUser != null) ? currentUser.getUid() : null;

        Curso curso = new Curso(
                nombreCurso,
                motivoCurso,
                fechaCurso,
                sistema,
                recordatorio,
                userId,
                null
        );

        databaseReference.push().setValue(curso)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Curso guardado exitosamente", Toast.LENGTH_SHORT).show();
                    limpiarCampos();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al guardar el curso", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }

    private void limpiarCampos() {
        nombreCursoField.setText("");
        motivoCursoField.setText("");
        fechaEditText.setText("");
        fechasSeleccionadas.setLength(0);
        sistemaSpinner.setSelection(0);
        recordatorioSpinner.setSelection(0);
    }
}

