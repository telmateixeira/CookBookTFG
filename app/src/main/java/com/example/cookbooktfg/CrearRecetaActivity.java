package com.example.cookbooktfg;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CrearRecetaActivity extends AppCompatActivity {

    private AutoCompleteTextView autoCompleteIngrediente;
    private ChipGroup chipGroupIngredientes;
    private Map<String, DocumentReference> referenciaMap = new HashMap<>();
    private List<DocumentReference> ingredientesSeleccionados = new ArrayList<>();
    private FirebaseFirestore db;
    private ArrayAdapter<String> adapter;
    private List<String> sugerenciasIngredientes = new ArrayList<>();
    private EditText etPaso;
    private Button btnAgregarPaso;
    private RecyclerView rvInstrucciones;

    private List<String> listaPasos = new ArrayList<>();
    private InstruccionesAdapter instruccionesAdapter;

    private List<DocumentReference> instruccionesReferencias = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crear_receta_activity);

        db = FirebaseFirestore.getInstance();
        autoCompleteIngrediente = findViewById(R.id.autoIngrediente);
        chipGroupIngredientes = findViewById(R.id.chipGroupIngredientes);
        Button btnAgregar = findViewById(R.id.btnAgregarIngrediente);
        AutoCompleteTextView autoCompleteDificultad = findViewById(R.id.autoCompleteDificultad);
        String[] opcionesDificultad = new String[] { "Fácil", "Media", "Difícil" };;

        ArrayAdapter<String> adapterDificultad = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                opcionesDificultad
        );
        autoCompleteDificultad.setAdapter(adapterDificultad);;

        // Obtener sugerencias desde Firestore
        db.collection("ingredientes")
                .get()
                .addOnSuccessListener(this::onSuccess);



        // Botón para añadir ingrediente (existente o nuevo)
        btnAgregar.setOnClickListener(v -> {
            String input = autoCompleteIngrediente.getText().toString().trim();
            if (input.isEmpty()) return;

            if (referenciaMap.containsKey(input)) {
                // Ya existe en Firestore
                agregarChipIngrediente(input, referenciaMap.get(input));
            } else {
                // Crear nuevo en Firestore
                String[] partes = input.split("-");
                String tipo = partes.length > 0 ? partes[0].trim() : "";
                String nombre = partes.length > 1 ? partes[1].trim() : "";

                if (tipo.isEmpty() || nombre.isEmpty()) return;

                Map<String, Object> nuevo = new HashMap<>();
                nuevo.put("tipo", tipo);
                nuevo.put("nombre", nombre);

                db.collection("ingredientes").add(nuevo).addOnSuccessListener(ref -> {
                    referenciaMap.put(input, ref);
                    agregarChipIngrediente(input, ref);
                });
            }

            autoCompleteIngrediente.setText(""); // limpiar input
        });
        Button btnGuardar = findViewById(R.id.btnGuardarReceta);
        btnGuardar.setOnClickListener(v -> {
            String nombre = ((EditText) findViewById(R.id.etNombreReceta)).getText().toString().trim();
            String descripcion = ((EditText) findViewById(R.id.etDescripcion)).getText().toString().trim();
            String dificultad = autoCompleteDificultad.getText().toString().trim();
            String duracion = ((EditText) findViewById(R.id.etDuracion)).getText().toString().trim();

            if (nombre.isEmpty() || descripcion.isEmpty() || duracion.isEmpty() || dificultad.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            if (listaPasos.isEmpty()) {
                Toast.makeText(this, "Agrega al menos un paso", Toast.LENGTH_SHORT).show();
                return;
            }

            guardarInstrucciones(() -> guardarRecetaEnFirestore(nombre, descripcion, dificultad, duracion, ""));
        });


        etPaso = findViewById(R.id.etPaso);
        btnAgregarPaso = findViewById(R.id.btnAgregarPaso);
        rvInstrucciones = findViewById(R.id.rvInstrucciones);

        instruccionesAdapter = new InstruccionesAdapter(listaPasos, (from, to) -> {
            Collections.swap(listaPasos, from, to);
            instruccionesAdapter.notifyItemMoved(from, to);
        });

        rvInstrucciones.setLayoutManager(new LinearLayoutManager(this));
        rvInstrucciones.setAdapter(instruccionesAdapter);

        btnAgregarPaso.setOnClickListener(v -> {
            String paso = etPaso.getText().toString().trim();
            if (!paso.isEmpty()) {
                listaPasos.add(paso);
                instruccionesAdapter.notifyItemInserted(listaPasos.size() - 1);
                etPaso.setText("");
            }
        });
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int fromPos = viewHolder.getAdapterPosition();
                int toPos = target.getAdapterPosition();
                instruccionesAdapter.moveListener.onItemMove(fromPos, toPos);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                listaPasos.remove(position);
                instruccionesAdapter.notifyItemRemoved(position);
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                return true;
            }
        });

        itemTouchHelper.attachToRecyclerView(rvInstrucciones);




    }

    private void agregarChipIngrediente(String display, DocumentReference ref) {
        if (ingredientesSeleccionados.contains(ref)) return;

        ingredientesSeleccionados.add(ref);

        Chip chip = new Chip(this);
        chip.setText(display);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            chipGroupIngredientes.removeView(chip);
            ingredientesSeleccionados.remove(ref);
        });

        chipGroupIngredientes.addView(chip);
    }

    private void guardarRecetaEnFirestore(String nombre, String descripcion, String dificultad, String duracion, String imagenUrl) {
        Map<String, Object> nuevaReceta = new HashMap<>();
        nuevaReceta.put("nombre", nombre);
        nuevaReceta.put("descripcion", descripcion);
        nuevaReceta.put("dificultad", dificultad);
        nuevaReceta.put("favorito", false);
        nuevaReceta.put("duracion", duracion);
        nuevaReceta.put("imagen", imagenUrl);
        nuevaReceta.put("fechaCreacion", FieldValue.serverTimestamp());
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        nuevaReceta.put("creadorId", FirebaseAuth.getInstance().getCurrentUser().getUid());
        nuevaReceta.put("ingredientes", ingredientesSeleccionados); // la lista de referencias
        nuevaReceta.put("instrucciones", instruccionesReferencias);
        nuevaReceta.put("favorito", false); // por defecto

        FirebaseFirestore.getInstance()
                .collection("recetas")
                .add(nuevaReceta)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Receta guardada correctamente", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al guardar receta", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }

    private void guardarInstrucciones(Runnable onComplete) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference instruccionesRef = db.collection("instrucciones");

        instruccionesReferencias.clear();
        AtomicInteger completados = new AtomicInteger(0);

        for (int i = 0; i < listaPasos.size(); i++) {
            int orden = i + 1;
            String paso = listaPasos.get(i);

            Map<String, Object> instruccion = new HashMap<>();
            instruccion.put("orden", orden);
            instruccion.put("paso", paso);

            instruccionesRef.add(instruccion)
                    .addOnSuccessListener(docRef -> {
                        instruccionesReferencias.add(docRef);
                        if (listaPasos.isEmpty()) {
                            Toast.makeText(this, "Agrega al menos un paso a la receta", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (completados.incrementAndGet() == listaPasos.size()) {
                            onComplete.run(); // todas listas, ahora sí guardar receta
                        }
                    });
        }
    }

    private void onSuccess(QuerySnapshot querySnapshot) {
        for (DocumentSnapshot doc : querySnapshot) {
            String tipo = doc.getString("tipo");
            String nombre = doc.getString("nombre");
            String display = tipo + " - " + nombre;

            sugerenciasIngredientes.add(display);
            referenciaMap.put(display, doc.getReference());
        }

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                sugerenciasIngredientes
        );
        autoCompleteIngrediente.setAdapter(adapter);
        autoCompleteIngrediente.setThreshold(1);
    }
}


