package com.example.proyecto;

import android.app.DatePickerDialog;
import android.icu.util.Calendar;
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

public class FomularioReunionFragment extends Fragment {

    private EditText tituloInput;
    private EditText motivodelareunionInput;
    private EditText fechaInput;
    private Spinner medioSpinner;
    private Spinner recordatorioSpinner;
    private FirebaseAuth mAuth;

    public FomularioReunionFragment() {
        // Constructor vacío requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflar el layout para este fragmento
        View view = inflater.inflate(R.layout.fragment_fomulario_reunion, container, false);

        // Inicializar Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Inicializar los campos de entrada
        tituloInput = view.findViewById(R.id.asunto);
        motivodelareunionInput = view.findViewById(R.id.motivodelareunion);
        fechaInput = view.findViewById(R.id.fecha);
        medioSpinner = view.findViewById(R.id.my_spinner);
        recordatorioSpinner = view.findViewById(R.id.spinner);

        // Configurar los Spinners
        ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(getContext(),
                R.array.spinner_items, android.R.layout.simple_spinner_item);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        medioSpinner.setAdapter(adapter1);

        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(getContext(),
                R.array.spinner_items_2, android.R.layout.simple_spinner_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        recordatorioSpinner.setAdapter(adapter2);

        // Configurar el OnClickListener para el EditText de fecha
        fechaInput.setOnClickListener(v -> mostrarDatePicker());

        // Botón "Enviar a"
        Button enviarButton = view.findViewById(R.id.enviar);
        enviarButton.setOnClickListener(v -> enviarReunion(view));

        // Devolver la vista inflada
        return view;
    }

    private void mostrarDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                (view, year1, month1, dayOfMonth) -> fechaInput.setText(dayOfMonth + "/" + (month1 + 1) + "/" + year1),
                year, month, day);
        datePickerDialog.show();
    }

    private void enviarReunion(View view) {
        try {
            String asunto = tituloInput.getText().toString();
            String fecha = fechaInput.getText().toString();
            String medio = medioSpinner.getSelectedItem().toString();
            String motivodelareunion = motivodelareunionInput.getText().toString();
            String recordatorio = recordatorioSpinner.getSelectedItem().toString();

            // Obtener el ID del usuario autenticado
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();

                // Crear un objeto Reunion con los datos obtenidos y el ID del usuario
                Reunion reunion = new Reunion(asunto, motivodelareunion, fecha, medio, recordatorio, userId, new ArrayList<>());

                // Pasar la reunión al siguiente fragmento para agregar los participantes
                Bundle bundle = new Bundle();
                bundle.putSerializable("reunion", reunion);  // Guardar los datos en un Bundle

                // Navegar al siguiente fragmento utilizando Navigation Component
                Navigation.findNavController(view).navigate(R.id.navigation_enviar_reunion, bundle);
            } else {
                Toast.makeText(getContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();  // Imprimir el error en el Logcat para obtener más detalles
        }
    }
}
