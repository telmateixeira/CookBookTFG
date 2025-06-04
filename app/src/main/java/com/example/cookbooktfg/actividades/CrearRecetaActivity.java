package com.example.cookbooktfg.actividades;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
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

    private AutoCompleteTextView autoTipoIng, autoNombreIng, autoCompleteDificultad;
    private ChipGroup chipGroupIngredientes;
    private EditText etPaso, etCantidad, etNombreReceta, etDescripcion, etDuracion;
    private Button btnAgregarPaso, btnAgregarIngredientes, btnGuardar, btnSeleccionarImagenes;
    private RecyclerView rvInstrucciones;
    private ImageButton btnVolver;
    private LinearLayout contenedorImagenes;

    private List<String> listaPasos = new ArrayList<>();
    private List<Uri> imagenesSeleccionadas = new ArrayList<>();
    private List<DocumentReference> instruccionesReferencias = new ArrayList<>();
    private InstruccionesAdapter instruccionesAdapter;
    private ArrayAdapter<String> ingredientesAdapter;
    private Map<String, DocumentReference> referenciaMap = new HashMap<>();
    private List<String> sugerenciasIngredientes = new ArrayList<>();

    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int REQUEST_IMAGE_GALLERY = 102;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private String imagenTemp;
    private Uri imagenUri;
    private boolean esperandoPermisos = false;
    private FirebaseFirestore db;

    /**
     * M2todo llamado al crear la actividad.
     * Inicializa la vista, conecta con Firebase Firestore y configura eventos de botones y adaptadores.
     *
     * @param savedInstanceState Estado guardado de la instancia, si existe.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crear_receta_activity);

        db = FirebaseFirestore.getInstance();
        inicializarVistas();

        btnAgregarPaso.setOnClickListener(v -> agregarPaso());
        btnAgregarIngredientes.setOnClickListener(v -> agregarIngrediente());
        btnGuardar.setOnClickListener(v -> validarYGuardarReceta());
        btnVolver.setOnClickListener(v -> finish());
        btnSeleccionarImagenes.setOnClickListener(v -> verificarYPedirPermisos());

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

            ingredientesAdapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    sugerenciasIngredientes
            );
            autoTipoIng.setAdapter(ingredientesAdapter);
        });
    }
    /**
     * Inicializa todas las vistas del layout, adaptadores y listeners necesarios para la interacción del usuario.
     * También configura el autocompletado de dificultad y la lógica de selección de ingredientes.
     */
    private void inicializarVistas() {
        autoTipoIng = findViewById(R.id.autoTipoIngrediente);
        autoNombreIng = findViewById(R.id.autoNombreIngrediente);
        autoCompleteDificultad = findViewById(R.id.autoCompleteDificultad);
        chipGroupIngredientes = findViewById(R.id.chipGroupIngredientes);
        etPaso = findViewById(R.id.etPaso);
        etCantidad = findViewById(R.id.etCantidad);
        etNombreReceta = findViewById(R.id.etNombreReceta);
        etDescripcion = findViewById(R.id.etDescripcion);
        etDuracion = findViewById(R.id.etDuracion);
        btnAgregarPaso = findViewById(R.id.btnAgregarPaso);
        btnAgregarIngredientes = findViewById(R.id.btnAgregarIngrediente);
        btnGuardar = findViewById(R.id.btnGuardarReceta);
        btnSeleccionarImagenes = findViewById(R.id.btnSeleccionarImagenes);
        btnVolver = findViewById(R.id.btnVolver);
        rvInstrucciones = findViewById(R.id.rvInstrucciones);
        contenedorImagenes = findViewById(R.id.contenedorImagenes);

        configurarRecycler();

        autoCompleteDificultad.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                new String[]{"Fácil", "Media", "Difícil"}
        ));

        autoTipoIng.setOnItemClickListener((parent, view, position, id) -> {
            String seleccion = (String) parent.getItemAtPosition(position);
            String[] partes = seleccion.split(" - ");
            if (partes.length == 2) {
                autoTipoIng.setText(partes[0]);
                autoNombreIng.setText(partes[1]);
            }
        });
    }
    /**
     * Agrega un nuevo paso a la receta si el campo de texto no está vacío.
     * Si está vacío, muestra un mensaje al usuario.
     */
    private void agregarPaso() {
        String paso = etPaso.getText().toString().trim();
        if (!paso.isEmpty()) {
            listaPasos.add(paso);
            instruccionesAdapter.notifyItemInserted(listaPasos.size() - 1);
            etPaso.setText("");
        } else {
            Toast.makeText(this, "El paso no puede estar vacío", Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * Agrega un nuevo ingrediente a la receta verificando si ya existe en Firestore.
     * Si existe, utiliza su referencia. Si no existe, lo crea en Firestore.
     * Luego agrega un chip representando el ingrediente en la interfaz.
     */
    private void agregarIngrediente() {
        String tipo = capitalizarTexto(autoTipoIng.getText().toString().trim());
        String nombre = capitalizarTexto(autoNombreIng.getText().toString().trim());
        String cantidad = capitalizarTexto(etCantidad.getText().toString().trim());

        if (!tipo.isEmpty() && !nombre.isEmpty() && !cantidad.isEmpty()) {
            String busqueda = tipo + " - " + nombre;
            if (referenciaMap.containsKey(busqueda)) {
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
    }

    /**
     * Busca un ingrediente existente por nombre en Firestore.
     * Si no existe, lo crea. Luego, llama al callback con la referencia al documento.
     *
     * @param nombre,  tipo, cantidad datos del ingrediente
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
     * Agrega un Chip para representar un ingrediente seleccionado.
     *
     * @param ingrediente Objeto que contiene la información del ingrediente.
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
     * Valida los campos obligatorios del formulario y, si son válidos,
     * inicia el proceso de guardado de la receta, incluyendo pasos e imágenes.
     */
    private void validarYGuardarReceta() {
        String nombre = etNombreReceta.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String dificultad = autoCompleteDificultad.getText().toString().trim();
        String duracion = etDuracion.getText().toString().trim();

        if (nombre.isEmpty() || descripcion.isEmpty() || duracion.isEmpty() || dificultad.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        if (listaPasos.isEmpty()) {
            Toast.makeText(this, "Agrega al menos un paso", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imagenesSeleccionadas.isEmpty()) {
            Toast.makeText(this, "Agrega al menos una imagen", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progreso = new ProgressDialog(this);
        progreso.setMessage("Guardando receta...");
        progreso.setCancelable(false);
        progreso.show();

        guardarInstrucciones(() -> {
            subirImagenes(nombre, descripcion, dificultad, duracion, progreso);
        });
    }
    /**
     * Guarda en Firestore cada paso de la receta como una instrucción individual.
     * Una vez guardadas todas, ejecuta el callback.
     *
     * @param onComplete Acción a ejecutar después de guardar todas las instrucciones correctamente.
     */
    private void guardarInstrucciones(Runnable onComplete) {
        instruccionesReferencias.clear();
        AtomicInteger contador = new AtomicInteger(0);

        for (int i = 0; i < listaPasos.size(); i++) {
            Map<String, Object> instruccion = new HashMap<>();
            instruccion.put("orden", i + 1);
            instruccion.put("paso", listaPasos.get(i));

            db.collection("instrucciones").add(instruccion)
                    .addOnSuccessListener(ref -> {
                        instruccionesReferencias.add(ref);
                        if (contador.incrementAndGet() == listaPasos.size()) {
                            onComplete.run();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al guardar instrucciones", Toast.LENGTH_SHORT).show();
                        Log.e("CrearReceta", "Error guardando instrucciones", e);
                    });
        }
    }
    /**
     * Sube las imágenes seleccionadas a Firebase Storage y obtiene sus URLs públicas.
     * Una vez todas las imágenes están subidas, se continúa con el guardado de la receta completa.
     *
     * @param nombre Nombre de la receta.
     * @param descripcion Descripción de la receta.
     * @param dificultad Nivel de dificultad (fácil/media/difícil).
     * @param duracion Tiempo de duración estimado para la receta.
     * @param progreso Diálogo de progreso mostrado mientras se suben las imágenes.
     */
    private void subirImagenes(String nombre, String descripcion, String dificultad,
                               String duracion, ProgressDialog progreso) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userId = user != null ? user.getUid() : "anonimo";
        List<String> urlsImagenes = new ArrayList<>();
        AtomicInteger contador = new AtomicInteger(0);

        for (Uri uri : imagenesSeleccionadas) {
            String nombreArchivo = "recetas/" + userId + "/" + System.currentTimeMillis() + "_" + contador.get() + ".jpg";
            StorageReference ref = storage.getReference(nombreArchivo);

            ref.putFile(uri)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        return ref.getDownloadUrl();
                    })
                    .addOnSuccessListener(uriDownload -> {
                        urlsImagenes.add(uriDownload.toString());
                        if (contador.incrementAndGet() == imagenesSeleccionadas.size()) {
                            guardarRecetaCompleta(nombre, descripcion, dificultad, duracion, urlsImagenes, progreso);
                        }
                    })
                    .addOnFailureListener(e -> {
                        progreso.dismiss();
                        Toast.makeText(this, "Error al subir imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
    /**
     * Crea el objeto final de receta y lo guarda en Firestore.
     * Incluye referencias a ingredientes, instrucciones e imágenes subidas.
     *
     * @param nombre Nombre de la receta.
     * @param descripcion Descripción de la receta.
     * @param dificultad Nivel de dificultad.
     * @param duracion Duración estimada.
     * @param imagenesUrls Lista de URLs de las imágenes previamente subidas.
     * @param progreso Diálogo de progreso que se cerrará al completar o fallar la operación.
     */
    private void guardarRecetaCompleta(String nombre, String descripcion, String dificultad,
                                       String duracion, List<String> imagenesUrls, ProgressDialog progreso) {
        List<DocumentReference> ingredientesRefs = new ArrayList<>();
        for (int i = 0; i < chipGroupIngredientes.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupIngredientes.getChildAt(i);
            IngredienteModelo ing = (IngredienteModelo) chip.getTag();
            ingredientesRefs.add(db.collection("ingredientes").document(ing.getId()));
        }

        Map<String, Object> receta = new HashMap<>();
        receta.put("nombre", nombre);
        receta.put("descripcion", descripcion);
        receta.put("dificultad", dificultad);
        receta.put("duracion", duracion);
        receta.put("imagenes", imagenesUrls);
        receta.put("ingredientes", ingredientesRefs);
        receta.put("instrucciones", instruccionesReferencias);
        receta.put("fechaCreacion", FieldValue.serverTimestamp());
        receta.put("favorito", false);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            receta.put("creadorId", user.getUid());
        }

        db.collection("recetas").add(receta)
                .addOnSuccessListener(ref -> {
                    progreso.dismiss();
                    Toast.makeText(this, "Receta creada exitosamente", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progreso.dismiss();
                    Toast.makeText(this, "Error al guardar receta", Toast.LENGTH_SHORT).show();
                    Log.e("CrearReceta", "Error al guardar", e);
                });
    }
    /**
     * Configura el RecyclerView para mostrar la lista de pasos de la receta.
     * Incluye funcionalidad para reordenar elementos con arrastrar y eliminar con deslizamiento.
     */
    private void configurarRecycler() {
        instruccionesAdapter = new InstruccionesAdapter(listaPasos,
                (fromPos, toPos) -> {
                    Collections.swap(listaPasos, fromPos, toPos);
                    instruccionesAdapter.notifyItemMoved(fromPos, toPos);
                },
                pos -> mostrarDialogoEditarPaso(pos));

        rvInstrucciones.setLayoutManager(new LinearLayoutManager(this));
        rvInstrucciones.setAdapter(instruccionesAdapter);

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
                int pos = viewHolder.getAdapterPosition();
                new AlertDialog.Builder(CrearRecetaActivity.this)
                        .setTitle("Eliminar paso")
                        .setMessage("¿Estás seguro de eliminar este paso?")
                        .setPositiveButton("Eliminar", (dialog, which) -> {
                            listaPasos.remove(pos);
                            instruccionesAdapter.notifyItemRemoved(pos);
                        })
                        .setNegativeButton("Cancelar", (dialog, which) -> {
                            instruccionesAdapter.notifyItemChanged(pos);
                        })
                        .show();
            }
        });

        itemTouchHelper.attachToRecyclerView(rvInstrucciones);
    }
    /**
     * Muestra un diálogo para editar el contenido de un paso específico en la lista de instrucciones.
     *
     * @param pos Posición del paso a editar.
     */
    private void mostrarDialogoEditarPaso(int pos) {
        EditText editText = new EditText(this);
        editText.setText(listaPasos.get(pos));

        new AlertDialog.Builder(this)
                .setTitle("Editar paso")
                .setView(editText)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String nuevoPaso = editText.getText().toString().trim();
                    if (!nuevoPaso.isEmpty()) {
                        listaPasos.set(pos, nuevoPaso);
                        instruccionesAdapter.notifyItemChanged(pos);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Verifica si los permisos necesarios (cámara y almacenamiento) están concedidos.
     * Si no lo están, los solicita al usuario. Si ya están concedidos, muestra el diálogo para seleccionar imágenes.
     */
    private void verificarYPedirPermisos() {
        if (esperandoPermisos) return;

        List<String> permisosNecesarios = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            permisosNecesarios.add(Manifest.permission.CAMERA);
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permisosNecesarios.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!permisosNecesarios.isEmpty()) {
            esperandoPermisos = true;
            ActivityCompat.requestPermissions(this,
                    permisosNecesarios.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        } else {
            mostrarDialogoSeleccionImagen();
        }
    }
    /**
     * Maneja la respuesta del usuario a la solicitud de permisos.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            esperandoPermisos = false;

            boolean todosPermisosConcedidos = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    todosPermisosConcedidos = false;
                    break;
                }
            }

            if (todosPermisosConcedidos) {
                mostrarDialogoSeleccionImagen();
            } else {
                Toast.makeText(this, "Se requieren permisos para continuar", Toast.LENGTH_SHORT).show();
            }
        }
    }
    /**
     * Muestra un diálogo con opciones para seleccionar imágenes: tomar una foto o elegir desde la galería.
     */
    private void mostrarDialogoSeleccionImagen() {
        new AlertDialog.Builder(this)
                .setTitle("Agregar imágenes")
                .setItems(new CharSequence[]{"Tomar foto", "Desde galería"}, (dialog, which) -> {
                    switch (which) {
                        case 0: abrirCamara(); break;
                        case 1: abrirGaleria(); break;
                    }
                })
                .show();
    }
    /**
     * Inicia la cámara para capturar una imagen y guarda su URI temporal.
     */
    private void abrirCamara() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                File photoFile = crearArchivoImagen();
                imagenUri = FileProvider.getUriForFile(this,
                        "com.example.cookbooktfg.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imagenUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } catch (IOException e) {
                Toast.makeText(this, "Error al crear archivo", Toast.LENGTH_SHORT).show();
            }
        }
    }
    /**
     * Crea un archivo temporal para almacenar una imagen tomada con la cámara.
     *
     * @return Archivo de imagen creado.
     * @throws IOException Si ocurre un error al crear el archivo.
     */
    private File crearArchivoImagen() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        imagenTemp = image.getAbsolutePath();
        return image;
    }
    /**
     * Abre la galería del dispositivo para seleccionar una o más imágenes.
     */
    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Selecciona imágenes"), REQUEST_IMAGE_GALLERY);
    }
    /**
     * Maneja el resultado de las actividades de selección de imagen (cámara o galería).
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_GALLERY:
                    procesarImagenesGaleria(data);
                    break;
                case REQUEST_IMAGE_CAPTURE:
                    if (imagenUri != null) {
                        agregarImagen(imagenUri);
                    }
                    break;
            }
        }
    }
    /**
     * Procesa las imágenes seleccionadas desde la galería y las agrega a la lista y vista previa.
     *
     * @param data Intent con los datos de las imágenes seleccionadas.
     */
    private void procesarImagenesGaleria(Intent data) {
        if (data.getClipData() != null) {
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) {
                Uri imageUri = data.getClipData().getItemAt(i).getUri();
                agregarImagen(imageUri);
            }
        } else if (data.getData() != null) {
            Uri imageUri = data.getData();
            agregarImagen(imageUri);
        }
    }
    /**
     * Agrega una imagen a la lista de imágenes seleccionadas y la muestra visualmente en la interfaz.
     * También permite eliminarla mediante una pulsación larga.
     *
     * @param uri URI de la imagen a agregar.
     */
    private void agregarImagen(Uri uri) {
        imagenesSeleccionadas.add(uri);

        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(250, 250));
        imageView.setPadding(8, 8, 8, 8);

        Glide.with(this)
                .load(uri)
                .override(250, 250)
                .centerCrop()
                .into(imageView);

        imageView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Eliminar imagen")
                    .setMessage("¿Eliminar esta imagen?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        contenedorImagenes.removeView(imageView);
                        imagenesSeleccionadas.remove(uri);
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });

        contenedorImagenes.addView(imageView);
    }
    /**
     * Capitaliza un texto: convierte la primera letra a mayúscula y el resto a minúscula.
     *
     * @param texto Texto original.
     * @return Texto capitalizado.
     */
    private String capitalizarTexto(String texto) {
        if (texto == null || texto.isEmpty()) {
            return texto;
        }
        return texto.substring(0, 1).toUpperCase() + texto.substring(1).toLowerCase();
    }

    /**
     * Interfaz de callback para manejar el resultado asincrónico de obtener o crear un ingrediente.
     */
    interface OnIngredienteListoListener {
        void onIngredienteListo(DocumentReference ref);
    }
}