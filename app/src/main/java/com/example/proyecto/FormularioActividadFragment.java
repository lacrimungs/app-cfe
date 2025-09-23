package com.example.proyecto;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Calendar;

public class FormularioActividadFragment extends Fragment {

    private EditText nombreActividadInput;
    private EditText motivoActividadInput;
    private EditText fechaInput;
    private Spinner recordatorioSpinner;
    private FirebaseAuth mAuth;

    public FormularioActividadFragment() {
        // Constructor vacío requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflar el layout para este fragmento
        View view = inflater.inflate(R.layout.fragment_formulario_actividad, container, false);

        // Inicializar Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Inicializar los campos de entrada
        nombreActividadInput = view.findViewById(R.id.nombre_actividad);
        motivoActividadInput = view.findViewById(R.id.motivo_actividad);
        fechaInput = view.findViewById(R.id.fecha);
        recordatorioSpinner = view.findViewById(R.id.spinner);

        // Configurar el Spinner de recordatorio
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(
                getContext(),
                R.array.spinner_items_2,
                android.R.layout.simple_spinner_item
        );
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        recordatorioSpinner.setAdapter(adapter2);

        // Configurar el OnClickListener para el EditText de fecha
        fechaInput.setOnClickListener(v -> mostrarDatePicker());

        // Botón "Enviar"
        Button enviarButton = view.findViewById(R.id.enviar);
        enviarButton.setOnClickListener(v -> enviarActividad(view));

        // Devolver la vista inflada
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

            // Obtener el ID del usuario autenticado
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();

                // Crear un objeto Actividad con los datos obtenidos y el ID del usuario
                Actividad actividad = new Actividad(
                        nombreActividad,
                        motivoActividad,
                        fecha,
                        null, // 🔹 Ya no usamos "areasInvolucradas"
                        recordatorio,
                        userId,
                        new ArrayList<>()
                );

                // Pasar la actividad al siguiente fragmento
                Bundle bundle = new Bundle();
                bundle.putSerializable("actividad", actividad);

                // Navegar al siguiente fragmento
                Navigation.findNavController(view).navigate(R.id.navigation_enviar_actividad, bundle);
            } else {
                Toast.makeText(getContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
