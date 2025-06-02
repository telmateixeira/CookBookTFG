package com.example.cookbooktfg.actividades;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cookbooktfg.modelos.IngredienteModelo;
import com.example.cookbooktfg.modelos.InstruccionesAdapter;
import com.example.cookbooktfg.R;
import com.example.cookbooktfg.modelos.Receta;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
/**
 *  Esta actividad permite a los usuarios editar una receta ya existente en Firestore,
 *  incluyendo el nombre, descripción, duración, dificultad, ingredientes, pasos de preparación
 *  e imágenes asociadas.
 *
 *  Autor: Telma Teixeira
 *  Proyecto: CookbookTFG
 */
public class EditarRecetaActivity extends AppCompatActivity {

    private EditText etNombreReceta, etDescripcion, etDuracion, etPaso, etCantidad;
    private AutoCompleteTextView autoCompleteDificultad, autoTipoIng, autoNombreIng;
    private ChipGroup chipGroupIngredientes;
    private Button btnActualizar, btnAgregarPaso, btnAgregarIngrediente;
    private ImageButton btnVolver;
    private RecyclerView rvInstrucciones;
    private LinearLayout contenedorImagenes;
    private Button btnSeleccionarImagenes;

    private InstruccionesAdapter instruccionesAdapter;
    private List<String> listaPasos = new ArrayList<>();
    private List<DocumentReference> instruccionesReferencias = new ArrayList<>();
    private List<Uri> imagenesSeleccionadasNuevas = new ArrayList<>();
    private List<String> urlsImagenesOriginales = new ArrayList<>();
    private List<String> urlsImagenesEliminadas = new ArrayList<>();

    private Map<String, DocumentReference> referenciaMap = new HashMap<>();
    private ArrayAdapter<String> adapter;
    private List<String> sugerenciasIngredientes = new ArrayList<>();

    private FirebaseFirestore db;
    private String recetaId;
    private Receta receta;

    /**
     * Metodo llamado al crear la actividad. Inicializa vistas, configura eventos y carga datos.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.editar_receta_activity);

        db = FirebaseFirestore.getInstance();
        recetaId = getIntent().getStringExtra("recetaId");
        if (recetaId == null) {
            Toast.makeText(this, "ID de receta no válido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        inicializarVistas();
        configurarRecycler();
        cargarRecetaDesdeFirestore();
        cargarSugerenciasIngredientes();

        autoTipoIng.setOnItemClickListener((parent, view, position, id) -> {
            String seleccion = (String) parent.getItemAtPosition(position);
            String[] partes = seleccion.split(" - ");
            if (partes.length == 2) {
                autoTipoIng.setText(partes[0]);
                autoNombreIng.setText(partes[1]);
            }
        });

        btnAgregarPaso.setOnClickListener(v -> {
            String paso = etPaso.getText().toString().trim();
            if (!paso.isEmpty()) {
                listaPasos.add(paso);
                instruccionesAdapter.notifyItemInserted(listaPasos.size() - 1);
                etPaso.setText("");
            }
        });

        btnAgregarIngrediente.setOnClickListener(v -> agregarIngrediente());
        btnSeleccionarImagenes.setOnClickListener(v -> abrirGaleria());
        btnVolver.setOnClickListener(v -> finish());
        btnActualizar.setOnClickListener(v -> guardarCambios());

        instruccionesAdapter = new InstruccionesAdapter(listaPasos, (from, to) -> {
            Collections.swap(listaPasos, from, to);
            instruccionesAdapter.notifyItemMoved(from, to);
        }, position -> {
            String pasoActual = listaPasos.get(position);
            EditText editText = new EditText(this);
            editText.setText(pasoActual);

            new AlertDialog.Builder(this)
                    .setTitle("Editar paso")
                    .setView(editText)
                    .setPositiveButton("Guardar", (dialog, which) -> {
                        String nuevoPaso = editText.getText().toString().trim();
                        if (!nuevoPaso.isEmpty()) {
                            listaPasos.set(position, nuevoPaso);
                            instruccionesAdapter.notifyItemChanged(position);
                        }
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
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
                Collections.swap(listaPasos, fromPos, toPos);
                instruccionesAdapter.notifyItemMoved(fromPos, toPos);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                listaPasos.remove(position);
                instruccionesAdapter.notifyItemRemoved(position);
            }
        });

        itemTouchHelper.attachToRecyclerView(rvInstrucciones);
    }

    /**
     * Inicializa los componentes de la interfaz de usuario y adapta la dificultad.
     */
    private void inicializarVistas() {
        etNombreReceta = findViewById(R.id.etNombreReceta);
        etDescripcion = findViewById(R.id.etDescripcion);
        etDuracion = findViewById(R.id.etDuracion);
        etPaso = findViewById(R.id.etPaso);
        etCantidad = findViewById(R.id.etCantidad);
        autoCompleteDificultad = findViewById(R.id.autoCompleteDificultad);
        autoTipoIng = findViewById(R.id.autoTipoIngrediente);
        autoNombreIng = findViewById(R.id.autoNombreIngrediente);
        chipGroupIngredientes = findViewById(R.id.chipGroupIngredientes);
        btnActualizar = findViewById(R.id.btnActualizarReceta);
        btnAgregarPaso = findViewById(R.id.btnAgregarPaso);
        btnAgregarIngrediente = findViewById(R.id.btnAgregarIngrediente);
        btnVolver = findViewById(R.id.btnVolver);
        rvInstrucciones = findViewById(R.id.rvInstrucciones);
        contenedorImagenes = findViewById(R.id.contenedorImagenes);
        btnSeleccionarImagenes = findViewById(R.id.btnSeleccionarImagenes);

        autoCompleteDificultad.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                new String[]{"Fácil", "Media", "Difícil"}
        ));
    }
    /**
     * Carga los datos de la receta desde Firestore y actualiza los campos de la UI.
     */
    private void cargarRecetaDesdeFirestore() {
        db.collection("recetas").document(recetaId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    receta = documentSnapshot.toObject(Receta.class);
                    if (receta != null) {
                        receta.setId(recetaId);
                        rellenarCamposConReceta();
                    }
                });
    }
    /**
     * Rellena los campos de la UI con la información de la receta cargada.
     */
    private void rellenarCamposConReceta() {
        etNombreReceta.setText(receta.getNombre());
        etDescripcion.setText(receta.getDescripcion());
        etDuracion.setText(receta.getDuracion());
        autoCompleteDificultad.setText(receta.getDificultad(), false);

        chipGroupIngredientes.removeAllViews();
        if (receta.getIngredientes() != null) {
            for (DocumentReference ref : receta.getIngredientes()) {
                ref.get().addOnSuccessListener(doc -> {
                    String tipo = doc.getString("tipo");
                    String nombre = doc.getString("nombre");
                    String cantidad = doc.getString("cantidad");

                    if (tipo != null && nombre != null && cantidad != null) {
                        IngredienteModelo ingrediente = new IngredienteModelo(tipo, nombre, cantidad);
                        ingrediente.setId(doc.getId());
                        agregarChipIngrediente(ingrediente);
                    }
                });
            }
        }

        contenedorImagenes.removeAllViews();
        for (String url : receta.getImagenes()) {
            urlsImagenesOriginales.add(url);
            ImageView imageView = new ImageView(this);
            imageView.setLayoutParams(new LinearLayout.LayoutParams(250, 250));
            imageView.setPadding(8, 8, 8, 8);
            Glide.with(this).load(url).into(imageView);

            String finalUrl = url;
            imageView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Eliminar imagen")
                        .setMessage("¿Deseas eliminar esta imagen?")
                        .setPositiveButton("Eliminar", (dialog, which) -> {
                            contenedorImagenes.removeView(imageView);
                            urlsImagenesOriginales.remove(finalUrl);
                            urlsImagenesEliminadas.add(finalUrl);
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
                return true;
            });

            contenedorImagenes.addView(imageView);
        }

        listaPasos.clear();
        if (receta.getInstrucciones() != null) {
            for (DocumentReference ref : receta.getInstrucciones()) {
                ref.get().addOnSuccessListener(doc -> {
                    String paso = doc.getString("paso");
                    if (paso != null) {
                        listaPasos.add(paso);
                        instruccionesAdapter.notifyItemInserted(listaPasos.size() - 1);
                    }
                });
            }
        }
    }
    /**
     * Agrega un nuevo ingrediente o reutiliza uno existente según el input del usuario.
     */
    private void agregarIngrediente() {
        String tipo = autoTipoIng.getText().toString().trim();
        String nombre = autoNombreIng.getText().toString().trim();
        String cantidad = etCantidad.getText().toString().trim();

        if (tipo.isEmpty() || nombre.isEmpty() || cantidad.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos del ingrediente", Toast.LENGTH_SHORT).show();
            return;
        }

        String key = tipo + " - " + nombre;
        if (referenciaMap.containsKey(key)) {
            DocumentReference ref = referenciaMap.get(key);
            IngredienteModelo ing = new IngredienteModelo(tipo, nombre, cantidad);
            ing.setId(ref.getId());
            agregarChipIngrediente(ing);
        } else {
            Map<String, Object> nuevoIngrediente = new HashMap<>();
            nuevoIngrediente.put("tipo", tipo);
            nuevoIngrediente.put("nombre", nombre);
            nuevoIngrediente.put("cantidad", cantidad);

            db.collection("ingredientes").add(nuevoIngrediente)
                    .addOnSuccessListener(documentReference -> {
                        IngredienteModelo ing = new IngredienteModelo(tipo, nombre, cantidad);
                        ing.setId(documentReference.getId());
                        agregarChipIngrediente(ing);
                    });
        }

        autoTipoIng.setText("");
        autoNombreIng.setText("");
        etCantidad.setText("");
    }
    /**
     * Crea y añade un chip visual en el ChipGroup para un ingrediente dado.
     *
     * @param ingrediente El ingrediente a mostrar.
     */
    private void agregarChipIngrediente(IngredienteModelo ingrediente) {
        Chip chip = new Chip(this);
        chip.setText(ingrediente.getFormatoChip());
        chip.setCloseIconVisible(true);
        chip.setTag(ingrediente);
        chip.setOnCloseIconClickListener(v -> chipGroupIngredientes.removeView(chip));
        chipGroupIngredientes.addView(chip);
    }
    /**
     * Carga sugerencias de ingredientes ya existentes desde Firestore
     * para autocompletado.
     */
    private void cargarSugerenciasIngredientes() {
        db.collection("ingredientes").get().addOnSuccessListener(querySnapshot -> {
            sugerenciasIngredientes.clear();
            referenciaMap.clear();

            for (DocumentSnapshot doc : querySnapshot) {
                String tipo = doc.getString("tipo");
                String nombre = doc.getString("nombre");
                if (tipo != null && nombre != null) {
                    String key = tipo + " - " + nombre;
                    sugerenciasIngredientes.add(key);
                    referenciaMap.put(key, doc.getReference());
                }
            }
            adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, sugerenciasIngredientes);
            autoTipoIng.setAdapter(adapter);
        });
    }
    /**
     * Abre la galería para que el usuario seleccione imágenes desde el dispositivo.
     */
    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Selecciona imágenes"), 101);
    }
    /**
     * Procesa el resultado de la selección de imágenes desde la galería.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK) {
            if (data.getClipData() != null) {
                int total = data.getClipData().getItemCount();
                for (int i = 0; i < total; i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    imagenesSeleccionadasNuevas.add(uri);
                    mostrarMiniatura(uri);
                }
            } else if (data.getData() != null) {
                Uri uri = data.getData();
                imagenesSeleccionadasNuevas.add(uri);
                mostrarMiniatura(uri);
            }
        }
    }
    /**
     * Permite ajustar la imagen para verla mas pequeña en la actividad
     */
    private void mostrarMiniatura(Uri uri) {
        ImageView iv = new ImageView(this);
        iv.setLayoutParams(new LinearLayout.LayoutParams(250, 250));
        iv.setPadding(8, 8, 8, 8);
        iv.setImageURI(uri);
        contenedorImagenes.addView(iv);
    }
    /**
     * Guarda los cambios realizados en la receta en Firestore y Firebase Storage.
     */
    private void guardarCambios() {
        ProgressDialog progreso = new ProgressDialog(this);
        progreso.setMessage("Guardando cambios...");
        progreso.setCancelable(false);
        progreso.show();

        FirebaseStorage storage = FirebaseStorage.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userId = (user != null) ? user.getUid() : "anonimo";

        List<String> urlsFinales = new ArrayList<>(urlsImagenesOriginales);
        AtomicInteger subidas = new AtomicInteger(0);

        if (imagenesSeleccionadasNuevas.isEmpty()) {
            eliminarImagenesDeStorage(() -> actualizarRecetaEnFirestore(urlsFinales, progreso));
        } else {
            for (int i = 0; i < imagenesSeleccionadasNuevas.size(); i++) {
                Uri uri = imagenesSeleccionadasNuevas.get(i);
                String nombre = "recetas/" + userId + "/" + System.currentTimeMillis() + "_" + i + ".jpg";
                StorageReference ref = storage.getReference(nombre);
                ref.putFile(uri).continueWithTask(task -> ref.getDownloadUrl())
                        .addOnSuccessListener(downloadUrl -> {
                            urlsFinales.add(downloadUrl.toString());
                            if (subidas.incrementAndGet() == imagenesSeleccionadasNuevas.size()) {
                                eliminarImagenesDeStorage(() -> actualizarRecetaEnFirestore(urlsFinales, progreso));
                            }
                        })
                        .addOnFailureListener(e -> {
                            progreso.dismiss();
                            Toast.makeText(this, "Error subiendo imagen", Toast.LENGTH_SHORT).show();
                        });
            }
        }
    }
    /**
     * Elimina de Firebase Storage todas las imágenes cuya URL está listada en `urlsImagenesEliminadas`.
     *
     * @param onComplete Acción a ejecutar una vez que todas las imágenes han sido eliminadas de Storage.
     */
    private void eliminarImagenesDeStorage(Runnable onComplete) {
        if (urlsImagenesEliminadas.isEmpty()) {
            onComplete.run();
            return;
        }

        FirebaseStorage storage = FirebaseStorage.getInstance();
        AtomicInteger contador = new AtomicInteger(0);

        for (String url : urlsImagenesEliminadas) {
            StorageReference ref = storage.getReferenceFromUrl(url);
            ref.delete().addOnCompleteListener(task -> {
                if (contador.incrementAndGet() == urlsImagenesEliminadas.size()) {
                    onComplete.run();
                }
            });
        }
    }
    /**
     * Configura el RecyclerView para mostrar la lista de instrucciones.
     */
    private void configurarRecycler() {
        instruccionesAdapter = new InstruccionesAdapter(listaPasos,
                (fromPosition, toPosition) -> {
                    Collections.swap(listaPasos, fromPosition, toPosition);
                    instruccionesAdapter.notifyItemMoved(fromPosition, toPosition);
                },
                position -> {
                    String pasoActual = listaPasos.get(position);
                    EditText editText = new EditText(EditarRecetaActivity.this);
                    editText.setText(pasoActual);

                    new AlertDialog.Builder(EditarRecetaActivity.this)
                            .setTitle("Editar paso")
                            .setView(editText)
                            .setPositiveButton("Guardar", (dialog, which) -> {
                                String nuevoPaso = editText.getText().toString().trim();
                                if (!nuevoPaso.isEmpty()) {
                                    listaPasos.set(position, nuevoPaso);
                                    instruccionesAdapter.notifyItemChanged(position);
                                }
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                });

        rvInstrucciones.setLayoutManager(new LinearLayoutManager(this));
        rvInstrucciones.setAdapter(instruccionesAdapter);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int fromPos = viewHolder.getAdapterPosition();
                int toPos = target.getAdapterPosition();
                Collections.swap(listaPasos, fromPos, toPos);
                instruccionesAdapter.notifyItemMoved(fromPos, toPos);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                listaPasos.remove(position);
                instruccionesAdapter.notifyItemRemoved(position);
            }
        }).attachToRecyclerView(rvInstrucciones);
    }

    /**
     * Actualiza una receta existente en Firestore con nuevos datos proporcionados por el usuario.
     *
     * @param imagenes Lista de URLs de imágenes actualizadas de la receta.
     * @param progreso Diálogo de progreso mostrado mientras se realiza la actualización.
     */
    private void actualizarRecetaEnFirestore(List<String> imagenes, ProgressDialog progreso) {
        String nombre = etNombreReceta.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String dificultad = autoCompleteDificultad.getText().toString().trim();
        String duracion = etDuracion.getText().toString().trim();

        List<DocumentReference> ingredientesRefs = new ArrayList<>();
        for (int i = 0; i < chipGroupIngredientes.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupIngredientes.getChildAt(i);
            IngredienteModelo ing = (IngredienteModelo) chip.getTag();
            ingredientesRefs.add(FirebaseFirestore.getInstance().collection("ingredientes").document(ing.getId()));
        }

        CollectionReference instruccionesRef = db.collection("instrucciones");
        instruccionesReferencias.clear();
        AtomicInteger completados = new AtomicInteger(0);

        for (int i = 0; i < listaPasos.size(); i++) {
            String paso = listaPasos.get(i);
            Map<String, Object> instruccion = new HashMap<>();
            instruccion.put("orden", i + 1);
            instruccion.put("paso", paso);
            instruccionesRef.add(instruccion).addOnSuccessListener(doc -> {
                instruccionesReferencias.add(doc);
                if (completados.incrementAndGet() == listaPasos.size()) {
                    Map<String, Object> datos = new HashMap<>();
                    datos.put("nombre", nombre);
                    datos.put("descripcion", descripcion);
                    datos.put("dificultad", dificultad);
                    datos.put("duracion", duracion);
                    datos.put("imagenes", imagenes);
                    datos.put("ingredientes", ingredientesRefs);
                    datos.put("instrucciones", instruccionesReferencias);
                    db.collection("recetas").document(recetaId)
                            .update(datos)
                            .addOnSuccessListener(aVoid -> {
                                progreso.dismiss();
                                Toast.makeText(this, "Receta actualizada", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                progreso.dismiss();
                                Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show();
                            });
                }
            });
        }
    }
}
