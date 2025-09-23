package com.example.proyecto;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;

public class FormularioCursoFragment extends Fragment {

    private EditText fechaEditText, nombreCursoField, motivoCursoField;
    private StringBuilder fechasSeleccionadas = new StringBuilder(); // Para almacenar las fechas seleccionadas
    private ImageView calendarioImagen, calendarioImagen2;
    private DatabaseReference databaseReference;
    private Spinner sistemaSpinner, recordatorioSpinner;

    public FormularioCursoFragment() {
        // Constructor vacío requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflar el layout para este fragmento
        View view = inflater.inflate(R.layout.fragment_formulario_curso, container, false);

        // Configurar Firebase Database
        databaseReference = FirebaseDatabase.getInstance().getReference("cursos");

        // Inicializar los campos de entrada
        nombreCursoField = view.findViewById(R.id.nombre_curso);
        motivoCursoField = view.findViewById(R.id.motivo_curso);
        fechaEditText = view.findViewById(R.id.fecha_curso);
        calendarioImagen = view.findViewById(R.id.calendario);
        calendarioImagen2 = view.findViewById(R.id.calendario); // Si usas otra imagen, cámbiale el id

        // Configurar los Spinners
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

        // Configurar el listener para la imagen del calendario (Primera fecha)
        calendarioImagen.setOnClickListener(v -> showDatePickerDialog(1));

        // Configurar el listener para la imagen del calendario (Segunda fecha)
        calendarioImagen2.setOnClickListener(v -> showDatePickerDialog(2));

        // Botón "Guardar"
        Button guardarButton = view.findViewById(R.id.enviar);
        guardarButton.setOnClickListener(v -> guardarCurso());

        return view;
    }

    // Método para mostrar el DatePickerDialog
    private void showDatePickerDialog(int fechaSeleccionada) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year1, month1, dayOfMonth) -> {
                    // Formato de la fecha: dd/mm/yyyy
                    String date = dayOfMonth + "/" + (month1 + 1) + "/" + year1;

                    // Dependiendo de la fecha seleccionada, actualizar el EditText
                    if (fechaSeleccionada == 1) {
                        if (fechasSeleccionadas.length() > 0) {
                            fechasSeleccionadas.append(", "); // Separador entre fechas
                        }
                        fechasSeleccionadas.append(date);
                    } else if (fechaSeleccionada == 2) {
                        if (fechasSeleccionadas.length() > 0) {
                            fechasSeleccionadas.append(", ");
                        }
                        fechasSeleccionadas.append(date);
                    }

                    // Actualizar el EditText con las fechas concatenadas
                    fechaEditText.setText(fechasSeleccionadas.toString());
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void guardarCurso() {
        // Obtener los valores de los campos de entrada
        String nombreCurso = nombreCursoField.getText().toString().trim();
        String motivoCurso = motivoCursoField.getText().toString().trim();
        String fechaCurso = fechaEditText.getText().toString().trim();
        String sistema = sistemaSpinner.getSelectedItem().toString();
        String recordatorio = recordatorioSpinner.getSelectedItem().toString();

        // Obtener el UID del usuario actual
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String userId = (currentUser != null) ? currentUser.getUid() : null;

        // Crear un objeto Curso (sin participantes por ahora)
        Curso curso = new Curso(
                nombreCurso,
                motivoCurso,
                fechaCurso,
                sistema,
                recordatorio,
                userId,
                null // participantes vacíos por ahora
        );

        // Enviar los datos a Firebase (genera un ID único)
        databaseReference.push().setValue(curso)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Curso guardado exitosamente", Toast.LENGTH_SHORT).show();
                    limpiarCampos();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al guardar el curso", Toast.LENGTH_SHORT).show();
                    e.printStackTrace(); // Imprimir el error para depuración
                });
    }

    private void limpiarCampos() {
        nombreCursoField.setText("");
        motivoCursoField.setText("");
        fechaEditText.setText("");
        fechasSeleccionadas.setLength(0); // limpiar buffer de fechas
        sistemaSpinner.setSelection(0);
        recordatorioSpinner.setSelection(0);
    }
}



