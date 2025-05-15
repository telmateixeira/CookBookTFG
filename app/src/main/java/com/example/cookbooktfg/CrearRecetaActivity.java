package com.example.cookbooktfg;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.InsertGesture;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

    private AutoCompleteTextView autoTipoIng, autoNombreIng;
    private ChipGroup chipGroupIngredientes;
    private Map<String, DocumentReference> referenciaMap = new HashMap<>();
    private FirebaseFirestore db;
    private ArrayAdapter<String> adapter;
    private List<String> sugerenciasIngredientes = new ArrayList<>();
    private EditText etPaso, etCantidad;
    private Button btnAgregarPaso;
    private RecyclerView rvInstrucciones;
    private ImageButton btnVolver;

    private List<String> listaPasos = new ArrayList<>();
    private InstruccionesAdapter instruccionesAdapter;

    private List<DocumentReference> instruccionesReferencias = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crear_receta_activity);

        db = FirebaseFirestore.getInstance();
        btnVolver = findViewById(R.id.btnVolver);
        autoTipoIng = findViewById(R.id.autoTipoIngrediente);
        autoNombreIng = findViewById(R.id.autoNombreIngrediente);
        etCantidad = findViewById(R.id.etCantidad);
        chipGroupIngredientes = findViewById(R.id.chipGroupIngredientes);
        Button btnAgregarIngredientes = findViewById(R.id.btnAgregarIngrediente);
        AutoCompleteTextView autoCompleteDificultad = findViewById(R.id.autoCompleteDificultad);
        String[] opcionesDificultad = new String[] { "Fácil", "Media", "Difícil" };;

        ArrayAdapter<String> adapterDificultad = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                opcionesDificultad
        );
        autoCompleteDificultad.setAdapter(adapterDificultad);;

        autoTipoIng.setAdapter(adapter);
        autoTipoIng.setOnItemClickListener((parent, view, position, id) -> {
            String seleccion = (String) parent.getItemAtPosition(position);
            String[] partes = seleccion.split(" - ");
            if (partes.length == 2) {
                autoTipoIng.setText(partes[0]);
                autoNombreIng.setText(partes[1]);
            }
        });

        // Obtener sugerencias desde Firestore
        db.collection("ingredientes")
                .get()
                .addOnSuccessListener(this::onSuccess);



        // Botón para añadir ingrediente (existente o nuevo)
        btnAgregarIngredientes.setOnClickListener(v -> {
            String tipo = autoTipoIng.getText().toString();
            String nombre = autoNombreIng.getText().toString();
            String cantidad = etCantidad.getText().toString();

            if(!tipo.isEmpty() && !nombre.isEmpty() && !cantidad.isEmpty()) {
                IngredienteModelo ingrediente = new IngredienteModelo(tipo, nombre, cantidad);
                agregarChipIngrediente(ingrediente);

                // Limpiar campos
                autoTipoIng.setText("");
                autoNombreIng.setText("");
                etCantidad.setText("");
            } else {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            }
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

        btnVolver.setOnClickListener(v -> {
            Intent intent = new Intent(CrearRecetaActivity.this, MenuPrincipalActivity.class);
            startActivity(intent);
            finish();
        });


    }


    private void agregarChipIngrediente(IngredienteModelo ingrediente) {
        Chip chip = new Chip(this);
        chip.setText(ingrediente.getFormatoChip()); // Ej: "Leche semidesnatada: 250ml"
        chip.setCloseIconVisible(true);

        // Guardar objeto completo como tag
        chip.setTag(ingrediente);

        chip.setOnCloseIconClickListener(v -> {
            chipGroupIngredientes.removeView(chip);
        });

        chipGroupIngredientes.addView(chip);
    }

    private void guardarRecetaEnFirestore(String nombre, String descripcion, String dificultad,
                                          String duracion, String imagenUrl) {
        // 1. Obtener lista de ingredientes como Map
        List<Map<String, Object>> ingredientesData = new ArrayList<>();

        for(int i = 0; i < chipGroupIngredientes.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupIngredientes.getChildAt(i);
            IngredienteModelo ingrediente = (IngredienteModelo) chip.getTag();

            Map<String, Object> ingredienteMap = new HashMap<>();
            ingredienteMap.put("tipo", ingrediente.getTipo());
            ingredienteMap.put("nombre", ingrediente.getNombre());
            ingredienteMap.put("cantidad", ingrediente.getCantidad());

            ingredientesData.add(ingredienteMap);
        }

        // 2. Crear objeto receta completo
        Map<String, Object> nuevaReceta = new HashMap<>();
        nuevaReceta.put("nombre", nombre);
        nuevaReceta.put("descripcion", descripcion);
        nuevaReceta.put("dificultad", dificultad);
        nuevaReceta.put("duracion", duracion);
        nuevaReceta.put("imagen", imagenUrl);
        nuevaReceta.put("favorito", false);
        nuevaReceta.put("fechaCreacion", FieldValue.serverTimestamp());
        nuevaReceta.put("ingredientes", ingredientesData);
        nuevaReceta.put("instrucciones", instruccionesReferencias);

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario != null) {
            nuevaReceta.put("creadorId", usuario.getUid());
        }

        // 3. Subir a Firestore
        FirebaseFirestore.getInstance()
                .collection("recetas")
                .add(nuevaReceta)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "¡Receta creada con éxito!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("Firestore", "Error al guardar receta", e);
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
        autoTipoIng.setAdapter(adapter);
        autoTipoIng.setThreshold(1);
    }
}


