package com.example.proyecto;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CalendarView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.proyecto.databinding.FragmentCalendarioBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CalendarioFragment extends Fragment {

    private FragmentCalendarioBinding binding;
    private static final String CHANNEL_ID = "export_channel";

    private List<Evento> eventosMes = new ArrayList<>();
    private final List<Evento> eventosDia = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCalendarioBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        cargarEventosDelMesDesdeFirebase();

        binding.calendarView.setOnDateChangeListener((CalendarView v, int year, int month, int dayOfMonth) -> {
            String fecha = dayOfMonth + "/" + (month + 1) + "/" + year;
            mostrarEventosDelDia(fecha);
        });

        AutoCompleteTextView dropdown = view.findViewById(R.id.dropdownExportar);
        String[] opciones = {"Todas", "Actividades", "Cursos", "Reuniones"};
        dropdown.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, opciones));
        dropdown.setOnItemClickListener((parent, v2, position, id) -> exportarEventosMes(opciones[position]));

        return view;
    }

    // ==================== CARGA SIN DUPLICADOS Y VISIBILIDAD POR PARTICIPACIÓN ====================

    private void cargarEventosDelMesDesdeFirebase() {
        eventosMes.clear();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();

        // ACTIVIDADES
        dbRef.child("actividades").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    String fecha = child.child("fecha").getValue(String.class);
                    String nombre = child.child("nombredelaactividad").getValue(String.class);
                    String motivo = child.child("motivodelaactividad").getValue(String.class);

                    List<String> uids = leerUidsParticipantes(child.child("participantes"));
                    if (!participaUsuarioActual(uids)) continue;

                    cargarNombresParticipantes(uids, participantes -> {
                        if (fecha != null) {
                            String lista = participantes.isEmpty() ? "No aplica" : String.join(" | ", participantes);
                            eventosMes.add(new Evento("Actividad", nombre, motivo, fecha, lista));
                        }
                    });
                }
                cargarCursos(dbRef);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error al cargar actividades", Toast.LENGTH_SHORT).show();
                cargarCursos(dbRef);
            }
        });
    }

    private void cargarCursos(DatabaseReference dbRef) {
        dbRef.child("cursos").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    String fechaCurso = child.child("inicioyfindelcurso").getValue(String.class);
                    String nombreCurso = child.child("nombredelcurso").getValue(String.class);

                    List<String> uids = leerUidsParticipantes(child.child("participantes"));
                    if (!participaUsuarioActual(uids)) continue;

                    cargarNombresParticipantes(uids, participantes -> {
                        if (fechaCurso != null) {
                            String lista = participantes.isEmpty() ? "No aplica" : String.join(" | ", participantes);
                            eventosMes.add(new Evento("Curso", nombreCurso, "Curso programado", fechaCurso, lista));
                        }
                    });
                }
                cargarReuniones(dbRef);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error al cargar cursos", Toast.LENGTH_SHORT).show();
                cargarReuniones(dbRef);
            }
        });
    }

    private void cargarReuniones(DatabaseReference dbRef) {
        dbRef.child("reuniones").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    String fechaReunion = child.child("fecha").getValue(String.class);
                    String asunto = child.child("asunto").getValue(String.class);
                    String motivoReunion = child.child("motivodelareunion").getValue(String.class);

                    List<String> uids = leerUidsParticipantes(child.child("participantes"));
                    if (!participaUsuarioActual(uids)) continue;

                    cargarNombresParticipantes(uids, participantes -> {
                        if (fechaReunion != null) {
                            String lista = participantes.isEmpty() ? "No aplica" : String.join(" | ", participantes);
                            eventosMes.add(new Evento("Reunión", asunto, motivoReunion, fechaReunion, lista));
                        }
                    });
                }

                deduplicarEventos();
                Toast.makeText(getContext(), "Eventos cargados desde Firebase", Toast.LENGTH_SHORT).show();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error al cargar reuniones", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<String> leerUidsParticipantes(DataSnapshot participantesNode) {
        List<String> uids = new ArrayList<>();
        for (DataSnapshot part : participantesNode.getChildren()) {
            String uid = part.getValue(String.class);
            if (uid != null) uids.add(uid);
        }
        return uids;
    }

    private boolean participaUsuarioActual(List<String> uidsParticipantes) {
        String uidActual = FirebaseAuth.getInstance().getUid();
        return uidActual != null && uidsParticipantes != null && uidsParticipantes.contains(uidActual);
        // Si quieres que ADMIN vea todo, aquí podrías añadir una excepción por rol.
    }

    // ==================== PARTICIPANTES: nombreCompleto o nombre ====================

    private interface OnParticipantsLoaded { void onLoaded(List<String> nombres); }

    private void cargarNombresParticipantes(List<String> uids, OnParticipantsLoaded listo) {
        if (uids == null || uids.isEmpty()) { listo.onLoaded(new ArrayList<>()); return; }
        List<String> nombres = new ArrayList<>();
        AtomicInteger pendientes = new AtomicInteger(uids.size());

        for (String uid : uids) {
            FirebaseDatabase.getInstance().getReference("usuarios")
                    .child(uid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot snap) {
                            String nombre = null;
                            if (snap.child("nombreCompleto").exists())
                                nombre = snap.child("nombreCompleto").getValue(String.class);
                            if (nombre == null && snap.child("nombre").exists())
                                nombre = snap.child("nombre").getValue(String.class);

                            if (nombre != null) nombres.add(nombre);
                            if (pendientes.decrementAndGet() == 0) listo.onLoaded(nombres);
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {
                            if (pendientes.decrementAndGet() == 0) listo.onLoaded(nombres);
                        }
                    });
        }
    }

    // ==================== UI: eventos del día ====================

    private void mostrarEventosDelDia(String fechaSeleccionada) {
        eventosDia.clear();
        for (Evento e : eventosMes) {
            if (e.fecha != null && e.fecha.equals(fechaSeleccionada)) eventosDia.add(e);
        }

        StringBuilder msg = new StringBuilder();
        if (eventosDia.isEmpty()) {
            msg.append("No hay pendientes para esta fecha.");
        } else {
            for (Evento e : eventosDia) {
                msg.append("📅 ").append(e.fecha)
                        .append(" - ").append(e.tipo)
                        .append("\nNombre: ").append(e.nombre != null ? e.nombre : "-")
                        .append("\nDescripción: ").append(e.descripcion != null ? e.descripcion : "-")
                        .append("\nParticipantes: ").append(e.participantes != null ? e.participantes : "No aplica")
                        .append("\n\n");
            }
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Eventos del " + fechaSeleccionada)
                .setMessage(msg.toString())
                .setPositiveButton("Cerrar", null)
                .show();
    }

    // ==================== Exportación CSV (UTF-16LE con BOM) ====================

    @Nullable
    private String normalizarFiltro(String seleccion) {
        if (seleccion == null) return null;
        String s = seleccion.toLowerCase(Locale.ROOT).trim();
        if (s.startsWith("todas")) return null;
        if (s.startsWith("actividad")) return "Actividad";
        if (s.startsWith("curso")) return "Curso";
        if (s.startsWith("reun")) return "Reunión";
        return null;
    }

    private void exportarEventosMes(String filtro) {
        try {
            String tipoFiltro = normalizarFiltro(filtro);

            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloads.exists()) downloads.mkdirs();

            String sufijo = (tipoFiltro == null) ? "todas" : tipoFiltro.toLowerCase(Locale.ROOT);
            File archivo = new File(downloads, "eventos_" + sufijo + ".csv");

            StringBuilder csv = new StringBuilder();
            csv.append("Fecha,Tipo,Nombre,Descripción,Participantes\n");

            boolean escribioAlgo = false;
            if (eventosMes.isEmpty()) {
                csv.append("-, -, -, -, No hay eventos este mes\n");
            } else {
                for (Evento e : eventosMes) {
                    if (tipoFiltro == null || e.tipo.equalsIgnoreCase(tipoFiltro)) {
                        csv.append(csvSafe(e.fecha)).append(",")
                                .append(csvSafe(e.tipo)).append(",")
                                .append(csvSafe(e.nombre)).append(",")
                                .append(csvSafe(e.descripcion)).append(",")
                                .append(csvSafe(e.participantes)).append("\n");
                        escribioAlgo = true;
                    }
                }
                if (!escribioAlgo) {
                    csv.append("-, ").append(tipoFiltro != null ? tipoFiltro : "Todas")
                            .append(", -, -, No hay eventos que coincidan\n");
                }
            }

            escribirCsvUtf16(archivo, csv.toString());
            mostrarNotificacion(archivo);
            Toast.makeText(getContext(), "Exportado en: " + archivo.getAbsolutePath(), Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error al exportar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String csvSafe(String s) {
        if (s == null) return "";
        String t = s.replace("\"", "\"\"");
        if (t.contains(",") || t.contains("\n")) t = "\"" + t + "\"";
        return t;
    }

    private void escribirCsvUtf16(File archivo, String contenido) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(archivo)) {
            // UTF-16LE BOM
            fos.write(0xFF);
            fos.write(0xFE);
            fos.write(contenido.getBytes(StandardCharsets.UTF_16LE));
        }
    }

    // ==================== Notificación ====================

    private void mostrarNotificacion(File archivo) {
        Context context = getContext();
        if (context == null) return;

        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", archivo);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "text/csv");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL_ID, "Exportaciones", NotificationManager.IMPORTANCE_DEFAULT));
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Exportación completada")
                .setContentText("Archivo guardado en Descargas")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        nm.notify(1, builder.build());
    }

    // ==================== Deduplicación opcional ====================

    private void deduplicarEventos() {
        Map<String, Evento> mapa = new LinkedHashMap<>();
        for (Evento e : eventosMes) {
            String key = ((e.fecha != null ? e.fecha : "") + "," +
                    (e.tipo != null ? e.tipo : "") + "," +
                    (e.nombre != null ? e.nombre : "") + "," +
                    (e.descripcion != null ? e.descripcion : ""))
                    .toLowerCase(Locale.ROOT);

            if (mapa.containsKey(key)) {
                Evento previo = mapa.get(key);
                if (previo != null) {
                    String a = previo.participantes == null ? "" : previo.participantes;
                    String b = e.participantes == null ? "" : e.participantes;
                    if (!a.contains(b) && !b.isEmpty()) {
                        previo.participantes = a.isEmpty() ? b : (a + " , " + b);
                    }
                }
            } else {
                mapa.put(key, e);
            }
        }
        eventosMes = new ArrayList<>(mapa.values());
    }

    // ==================== Modelo ====================

    static class Evento {
        String tipo, nombre, descripcion, fecha, participantes;
        Evento(String tipo, String nombre, String descripcion, String fecha, String participantes) {
            this.tipo = tipo;
            this.nombre = nombre;
            this.descripcion = descripcion;
            this.fecha = fecha;
            this.participantes = participantes;
        }
    }
}









