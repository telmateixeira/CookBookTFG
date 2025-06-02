package com.example.cookbooktfg.actividades;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookbooktfg.modelos.IngredienteModelo;
import com.example.cookbooktfg.modelos.InstruccionesAdapter;
import com.example.cookbooktfg.R;
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
 * Esta actividad permite a los usuarios crear una nueva receta en la aplicación de cocina.
 * El usuario puede introducir el nombre, descripción, duración, dificultad, pasos (instrucciones),
 * ingredientes (con tipo, nombre y cantidad) y subir imágenes desde la galería o la cámara.
 *
 * Autor: Telma Teixeira
 * Proyecto: CookbookTFG
 */
public class CrearRecetaActivity extends AppCompatActivity {

    private AutoCompleteTextView autoTipoIng, autoNombreIng;
    private ChipGroup chipGroupIngredientes;
    private Map<String, DocumentReference> referenciaMap = new HashMap<>();
    private FirebaseFirestore db;
    private ArrayAdapter<String> adapter;
    private List<String> sugerenciasIngredientes = new ArrayList<>();
    private EditText etPaso, etCantidad;
    private Button btnAgregarPaso, btnAgregarIngredientes;
    private RecyclerView rvInstrucciones;
    private ImageButton btnVolver;

    private List<String> listaPasos = new ArrayList<>();
    private InstruccionesAdapter instruccionesAdapter;

    private List<DocumentReference> instruccionesReferencias = new ArrayList<>();

    private static final int REQUEST_CODE_GALERIA = 1001;
    private static final int REQUEST_CODE_CAMARA = 1002;
    private List<Uri> listaImagenesSeleccionadas = new ArrayList<>();
    private LinearLayout contenedorImagenes;
    private Button btnSeleccionarImagenes;

    /**
     * Metodo principal que inicializa la actividad, configura vistas, listeners,
     * adaptadores y funcionalidades clave como añadir pasos, ingredientes e imágenes.
     * */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crear_receta_activity);

        db = FirebaseFirestore.getInstance();
        btnVolver = findViewById(R.id.btnVolver);
        btnSeleccionarImagenes = findViewById(R.id.btnSeleccionarImagenes);
        contenedorImagenes = findViewById(R.id.contenedorImagenes);
        autoTipoIng = findViewById(R.id.autoTipoIngrediente);
        autoNombreIng = findViewById(R.id.autoNombreIngrediente);
        etCantidad = findViewById(R.id.etCantidad);
        chipGroupIngredientes = findViewById(R.id.chipGroupIngredientes);
        btnAgregarIngredientes = findViewById(R.id.btnAgregarIngrediente);
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
            String tipo = capitalizarTexto(autoTipoIng.getText().toString().trim());
            String nombre = capitalizarTexto(autoNombreIng.getText().toString().trim());
            String cantidad = capitalizarTexto(etCantidad.getText().toString().trim());

            if(!tipo.isEmpty() && !nombre.isEmpty() && !cantidad.isEmpty()) {
                String busqueda = tipo + " - " + nombre;
                if(referenciaMap.containsKey(busqueda)) {
                    // Es un ingrediente existente
                    DocumentReference ref = referenciaMap.get(busqueda);
                    IngredienteModelo ingrediente = new IngredienteModelo(tipo, nombre, cantidad);
                    ingrediente.setId(ref.getId()); // Guardar referencia al documento
                    agregarChipIngrediente(ingrediente);
                } else {
                    obtenerOcrearIngrediente(nombre, tipo, cantidad, new OnIngredienteListoListener() {
                        @Override
                        public void onIngredienteListo(DocumentReference ref) {
                            runOnUiThread(() -> {
                                IngredienteModelo ingrediente = new IngredienteModelo(tipo, nombre, cantidad);
                                ingrediente.setId(ref.getId());
                                agregarChipIngrediente(ingrediente);
                            });
                        }
                    });
                }
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

            guardarInstrucciones(() -> guardarImagenes(nombre, descripcion, dificultad, duracion));
        });


        etPaso = findViewById(R.id.etPaso);
        btnAgregarPaso = findViewById(R.id.btnAgregarPaso);
        rvInstrucciones = findViewById(R.id.rvInstrucciones);

        configurarRecycler();

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

        btnSeleccionarImagenes.setOnClickListener(v -> abrirGaleria());
    }
    /**
     * Configura el RecyclerView que muestra los pasos (instrucciones) de la receta, incluyendo
     * la lógica para reordenar y eliminar pasos con gestos táctiles.
     */
    private void configurarRecycler() {
        instruccionesAdapter = new InstruccionesAdapter(listaPasos,
                (fromPosition, toPosition) -> {
                    Collections.swap(listaPasos, fromPosition, toPosition);
                    instruccionesAdapter.notifyItemMoved(fromPosition, toPosition);
                },
                position -> {
                    String pasoActual = listaPasos.get(position);
                    EditText editText = new EditText(CrearRecetaActivity.this);
                    editText.setText(pasoActual);

                    new AlertDialog.Builder(CrearRecetaActivity.this)
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
     * Capitaliza el primer carácter del texto y convierte el resto a minúsculas.
     *
     * @param texto texto a capitalizar
     */
    private String capitalizarTexto(String texto) {
        if (texto == null || texto.isEmpty()) {
            return texto;
        }
        return texto.substring(0, 1).toUpperCase() + texto.substring(1).toLowerCase();
    }

    /**
     * Agrega un Chip visual al ChipGroup con el ingrediente especificado.
     * Permite eliminar el chip con un botón.
     *
     * @param ingrediente objeto que representa el ingrediente
     */
    private void agregarChipIngrediente(IngredienteModelo ingrediente) {
        Chip chip = new Chip(this);
        chip.setText(ingrediente.getFormatoChip()); // Ej: "Leche semidesnatada: 250ml"
        chip.setCloseIconVisible(true);

        chip.setTag(ingrediente);

        chip.setOnCloseIconClickListener(v -> {
            chipGroupIngredientes.removeView(chip);
        });

        chipGroupIngredientes.addView(chip);
    }

    /**
     * Sube todas las imágenes seleccionadas al almacenamiento de Firebase.
     * Cuando finaliza, llama a guardarRecetaEnFirestore.
     */
    private void guardarImagenes(String nombre, String descripcion, String dificultad,
                                          String duracion) {
        // Mostrar progreso
        ProgressDialog progreso = new ProgressDialog(this);
        progreso.setMessage("Subiendo receta...");
        progreso.setCancelable(false);
        progreso.show();

        // Subir imágenes primero
        List<String> urlsFinales = new ArrayList<>();
        if (listaImagenesSeleccionadas.isEmpty()) {
            Toast.makeText(this, "Agrega al menos una imagen", Toast.LENGTH_SHORT).show();
            progreso.dismiss();
            return;
        }

        FirebaseStorage storage = FirebaseStorage.getInstance();
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        String userId = (usuario != null) ? usuario.getUid() : "desconocido";

        // Controlador de imágenes subidas
        AtomicInteger imagenesSubidas = new AtomicInteger(0);

        for (int i = 0; i < listaImagenesSeleccionadas.size(); i++) {
            Uri uri = listaImagenesSeleccionadas.get(i);
            String nombreImagen = "recetas/" + userId + "/" + System.currentTimeMillis() + "_" + i + ".jpg";
            StorageReference ref = storage.getReference(nombreImagen);

            ref.putFile(uri).continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return ref.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    urlsFinales.add(downloadUri.toString());

                    if (imagenesSubidas.incrementAndGet() == listaImagenesSeleccionadas.size()) {
                        guardarRecetaEnFirestore(nombre, descripcion, dificultad, duracion, urlsFinales);
                    }
                } else {
                    progreso.dismiss();
                    Toast.makeText(this, "Error al subir imagen: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    /**
     * Guarda la receta en Firestore incluyendo todos sus datos: nombre,
     * descripción, dificultad, duración, ingredientes, instrucciones e imágenes.
     */
    private void guardarRecetaEnFirestore(String nombre, String descripcion, String dificultad,
                                          String duracion, List<String> imagenesUrl) {
        // 1. Obtener lista de ingredientes como Map
        List<DocumentReference> ingredientesData = new ArrayList<>();

        for(int i = 0; i < chipGroupIngredientes.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupIngredientes.getChildAt(i);
            IngredienteModelo ingrediente = (IngredienteModelo) chip.getTag();

            // Obtenemos la referencia al documento del ingrediente por su ID
            DocumentReference ref = FirebaseFirestore.getInstance()
                    .collection("ingredientes")
                    .document(ingrediente.getId());

            ingredientesData.add(ref);
        }

        // 2. Crear objeto receta completo
        Map<String, Object> nuevaReceta = new HashMap<>();
        nuevaReceta.put("nombre", nombre);
        nuevaReceta.put("descripcion", descripcion);
        nuevaReceta.put("dificultad", dificultad);
        nuevaReceta.put("duracion", duracion);
        nuevaReceta.put("imagenes", imagenesUrl);
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

    /**
     * Guarda cada paso de la receta en la colección instrucciones de Firestore y
     * almacena las referencias. Al completar todos los pasos, ejecuta onComplete.
     *
     * @param onComplete accion a ejecutar tras completar la carga de instrucciones
     */
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
                            onComplete.run();
                        }
                    });
        }
    }
    /**
     * Procesa el resultado de una consulta de ingredientes desde Firestore, actualiza las
     * sugerencias de autocompletado y almacena las referencias de los ingredientes.
     */
    private void onSuccess(QuerySnapshot querySnapshot) {
        sugerenciasIngredientes.clear();
        referenciaMap.clear();

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

    /**
     * Lanza un intent para seleccionar una o varias imágenes desde la galería del dispositivo.
     */
    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Selecciona imágenes"), REQUEST_CODE_GALERIA);
    }

    /**
     * Maneja el resultado de la selección de imágenes,
     * ya sea una o múltiples, y muestra sus miniaturas.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_GALERIA) {
                if (data.getClipData() != null) {
                    int totalItems = data.getClipData().getItemCount();
                    for (int i = 0; i < totalItems; i++) {
                        Uri imagenUri = data.getClipData().getItemAt(i).getUri();
                        listaImagenesSeleccionadas.add(imagenUri);
                        mostrarMiniatura(imagenUri);
                    }
                } else if (data.getData() != null) {
                    Uri imagenUri = data.getData();
                    listaImagenesSeleccionadas.add(imagenUri);
                    mostrarMiniatura(imagenUri);
                }
            }
        }
    }
    /**
     * Muestra una miniatura de la imagen seleccionada en el contenedor de imágenes.
     */
    private void mostrarMiniatura(Uri imagenUri) {
        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(250, 250));
        imageView.setPadding(8, 8, 8, 8);
        imageView.setImageURI(imagenUri);
        contenedorImagenes.addView(imageView);
    }
    /**
     * Busca un ingrediente existente por nombre en Firestore.
     * Si no existe, lo crea. Luego, llama al callback con la referencia al documento.
     *
     * @param nombre, tipo, cantidad datos del ingrediente
     * @param callback interfaz para manejar el resultado
     */
    private void obtenerOcrearIngrediente(String nombre, String tipo, String cantidad, OnIngredienteListoListener callback) {
        FirebaseFirestore.getInstance().collection("ingredientes")
                .whereEqualTo("nombre", nombre.trim().toLowerCase())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentReference ref = queryDocumentSnapshots.getDocuments().get(0).getReference();
                        callback.onIngredienteListo(ref);
                    } else {
                        Map<String, Object> nuevoIngrediente = new HashMap<>();
                        nuevoIngrediente.put("nombre", nombre.trim());
                        nuevoIngrediente.put("tipo", tipo);
                        nuevoIngrediente.put("cantidad", cantidad);

                        FirebaseFirestore.getInstance().collection("ingredientes")
                                .add(nuevoIngrediente)
                                .addOnSuccessListener(documentReference -> {
                                    callback.onIngredienteListo(documentReference);
                                });
                    }
                });
    }
    /**
     * Interfaz de callback para manejar el resultado asincrónico de obtener o crear un ingrediente.
     */

    interface OnIngredienteListoListener {
        void onIngredienteListo(DocumentReference ref);
    }

}


