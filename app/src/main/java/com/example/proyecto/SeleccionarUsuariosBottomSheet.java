package com.example.proyecto;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentResultOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * BottomSheet para buscar y seleccionar usuarios.
 * Recibe una lista de UsuarioItem (uid, nombre, selected).
 * Devuelve por Fragment Result un ArrayList<String> con UIDs seleccionados.
 */
public class SeleccionarUsuariosBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_ITEMS = "arg_items";

    public static SeleccionarUsuariosBottomSheet newInstance(ArrayList<UsuariosSelectedAdapter.UsuarioItem> items) {
        SeleccionarUsuariosBottomSheet f = new SeleccionarUsuariosBottomSheet();
        Bundle b = new Bundle();
        b.putSerializable(ARG_ITEMS, items);
        f.setArguments(b);
        return f;
    }

    private UsuariosSelectedAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_buscar_usuarios, container, false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextInputEditText etBuscar = view.findViewById(R.id.etBuscar);
        RecyclerView rv            = view.findViewById(R.id.rvUsuarios);
        Button btnListo            = view.findViewById(R.id.btnListo);
        CheckBox cbAll             = view.findViewById(R.id.cbSelectAll);
        Chip chip                  = view.findViewById(R.id.chipSeleccionados);

        ArrayList<UsuariosSelectedAdapter.UsuarioItem> items =
                (ArrayList<UsuariosSelectedAdapter.UsuarioItem>) getArguments().getSerializable(ARG_ITEMS);

        if (items == null) items = new ArrayList<>();

        adapter = new UsuariosSelectedAdapter(items);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        // contador
        adapter.setSelectionListener((selectedCount, totalVisible) ->
                chip.setText(selectedCount + " seleccionados"));

        // seleccionar todo
        cbAll.setOnCheckedChangeListener((buttonView, isChecked) -> adapter.selectAllVisible(isChecked));

        // filtro
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
        });

        // devolver selección
        btnListo.setOnClickListener(v -> {
            ArrayList<String> uids = new ArrayList<>();
            for (UsuariosSelectedAdapter.UsuarioItem it : adapter.getCurrentItems()) {
                if (it.selected) uids.add(it.uid);
            }
            Bundle result = new Bundle();
            result.putStringArrayList(EnviarActividadFragment.FR_BUNDLE_SELECTED, uids);
            getParentFragmentManager().setFragmentResult(EnviarActividadFragment.FR_KEY_RESULT, result);
            dismiss();
        });
    }
}
