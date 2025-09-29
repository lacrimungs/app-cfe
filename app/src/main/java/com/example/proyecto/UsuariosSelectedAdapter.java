package com.example.proyecto;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UsuariosSelectedAdapter extends RecyclerView.Adapter<UsuariosSelectedAdapter.VH> {

    // ==== Listener para notificar cambios en la selección ====
    public interface OnSelectionChanged {
        void onChanged(int selectedCount, int totalVisible);
    }
    private OnSelectionChanged selectionListener;
    public void setSelectionListener(OnSelectionChanged l) { this.selectionListener = l; }

    // ==== Item ====
    public static class UsuarioItem implements java.io.Serializable {
        public final String uid;
        public final String nombre;
        public boolean selected;
        public UsuarioItem(String uid, String nombre, boolean selected) {
            this.uid = uid; this.nombre = nombre; this.selected = selected;
        }
    }

    private final List<UsuarioItem> original;  // lista completa
    private final List<UsuarioItem> visible;   // lista filtrada

    public UsuariosSelectedAdapter(List<UsuarioItem> items) {
        this.original = new ArrayList<>(items);
        this.visible  = new ArrayList<>(items);
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_usuario_select, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        UsuarioItem it = visible.get(position);
        h.tvNombre.setText(it.nombre);

        // Iniciales para avatar
        String initials = obtenerIniciales(it.nombre);
        h.tvAvatar.setText(initials);

        h.cb.setOnCheckedChangeListener(null);
        h.cb.setChecked(it.selected);

        View.OnClickListener toggle = v -> {
            it.selected = !it.selected;
            h.cb.setChecked(it.selected);
            notificarSeleccion();
        };
        h.itemView.setOnClickListener(toggle);
        h.cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            it.selected = isChecked;
            notificarSeleccion();
        });
    }

    @Override
    public int getItemCount() { return visible.size(); }

    public void filter(String q) {
        String s = q == null ? "" : q.trim().toLowerCase(Locale.getDefault());
        visible.clear();
        if (s.isEmpty()) {
            visible.addAll(original);
        } else {
            for (UsuarioItem it : original) {
                if (it.nombre != null && it.nombre.toLowerCase(Locale.getDefault()).contains(s)) {
                    visible.add(it);
                }
            }
        }
        notifyDataSetChanged();
        notificarSeleccion(); // recalcula contador con filtro aplicado
    }

    public List<UsuarioItem> getCurrentItems() {
        return new ArrayList<>(visible);
    }

    public void selectAllVisible(boolean value) {
        for (UsuarioItem it : visible) it.selected = value;
        notifyDataSetChanged();
        notificarSeleccion();
    }

    private void notificarSeleccion() {
        if (selectionListener != null) {
            int count = 0;
            for (UsuarioItem u : visible) if (u.selected) count++;
            selectionListener.onChanged(count, visible.size());
        }
    }

    private String obtenerIniciales(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) return "?";
        String[] parts = nombre.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(2, parts.length); i++) {
            sb.append(parts[i].substring(0, 1).toUpperCase());
        }
        return sb.toString();
    }

    static class VH extends RecyclerView.ViewHolder {
        CheckBox cb;
        TextView tvNombre;
        TextView tvAvatar;
        TextView tvSubtitulo;
        VH(@NonNull View itemView) {
            super(itemView);
            cb = itemView.findViewById(R.id.cbSeleccionado);
            tvNombre = itemView.findViewById(R.id.tvNombreUsuario);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvSubtitulo = itemView.findViewById(R.id.tvSubtitulo);
        }
    }
}
