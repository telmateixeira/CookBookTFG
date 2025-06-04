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
import android.widget.*;

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
import com.example.cookbooktfg.modelos.Receta;
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
 * Esta actividad permite a los usuarios editar una receta ya existente en Firestore,
 * incluyendo el nombre, descripción, duración, dificultad, ingredientes, pasos de preparación
 * e imágenes asociadas.
 * <p>
 * Autor: Telma Teixeira
 * Proyecto: CookbookTFG
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
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int REQUEST_IMAGE_GALLERY = 102;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private String imagenTemp; // Para guardar la ruta temporal de la foto
    private Uri imagenUri; // Para guardar el URI de la foto tomada

    private Map<String, DocumentReference> referenciaMap = new HashMap<>();
    private ArrayAdapter<String> adapter;
    private List<String> sugerenciasIngredientes = new ArrayList<>();

    private FirebaseFirestore db;
    private String recetaId;
    private Receta receta;

    private boolean esperandoPermisos = false;

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
        btnSeleccionarImagenes.setOnClickListener(v -> verificarYPedirPermisos());
        btnVolver.setOnClickListener(v -> finish());
        btnActualizar.setOnClickListener(v -> guardarCambios());


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
        configurarRecycler();
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
            AtomicInteger contador = new AtomicInteger(0);
            int totalPasos = receta.getInstrucciones().size();

            for (DocumentReference ref : receta.getInstrucciones()) {
                ref.get().addOnSuccessListener(documentSnapshot -> {
                    String paso = documentSnapshot.getString("paso");
                    if (paso != null) {
                        listaPasos.add(paso);
                    }

                    if (contador.incrementAndGet() == totalPasos) {
                        runOnUiThread(() -> {
                            instruccionesAdapter.notifyDataSetChanged();
                        });
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
     * Verifica si los permisos necesarios están otorgados. Si no, los solicita.
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
     * Muestra el diálogo que permite elegir entre tomar una foto o seleccionar una imagen.
     */
    private void mostrarDialogoSeleccionImagen() {
        new AlertDialog.Builder(this)
                .setTitle("Agregar imágenes")
                .setItems(new CharSequence[]{"Tomar foto", "Desde galería"}, (dialog, which) -> {
                    switch (which) {
                        case 0: // Cámara
                            abrirCamara();
                            break;
                        case 1: // Galería
                            abrirGaleria();
                            break;
                    }
                })
                .show();
    }

    /**
     * Abre la cámara para tomar una foto.
     */
    private void abrirCamara() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = crearArchivoImagen();
            } catch (IOException ex) {
                Toast.makeText(this, "Error al crear archivo", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                imagenUri = FileProvider.getUriForFile(this,
                        "com.example.cookbooktfg.fileprovider", // Usa tu package name + ".fileprovider"
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imagenUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    /**
     * Crea un archivo temporal donde se almacenará la foto tomada.
     */
    private File crearArchivoImagen() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir == null) {
            throw new IOException("Directorio no disponible");
        }

        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        imagenTemp = image.getAbsolutePath();
        return image;
    }

    /**
     * Abre la galería para que el usuario seleccione imágenes desde el dispositivo.
     */
    private void abrirGaleria() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Selecciona imágenes"), REQUEST_IMAGE_GALLERY);
    }

    /**
     * Procesa el resultado de la selección de imágenes desde la galería.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_GALLERY:
                    if (data != null) {
                        if (data.getClipData() != null) {
                            int count = data.getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                Uri imageUri = data.getClipData().getItemAt(i).getUri();
                                getContentResolver().takePersistableUriPermission(
                                        imageUri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                agregarImagen(imageUri);
                            }
                        } else if (data.getData() != null) {
                            Uri imageUri = data.getData();
                            getContentResolver().takePersistableUriPermission(
                                    imageUri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            agregarImagen(imageUri);
                        }
                    }
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
     * Agrega una imagen visualmente a un contenedor y la almacena en una lista para luego subirla.
     *
     * @param uri la url de la imagen seleccionada por el usuario.
     */
    private void agregarImagen(Uri uri) {
        imagenesSeleccionadasNuevas.add(uri); // Guarda la Uri para subirla luego

        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(250, 250));
        imageView.setPadding(8, 8, 8, 8);

        Glide.with(this)
                .load(uri)
                .override(250, 250)
                .centerCrop()
                .into(imageView);

        // Eliminar
        imageView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Eliminar imagen")
                    .setMessage("¿Eliminar esta imagen?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        contenedorImagenes.removeView(imageView);
                        imagenesSeleccionadasNuevas.remove(uri);
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });

        contenedorImagenes.addView(imageView);
    }

    /**
     * * Callback que se ejecuta cuando el usuario responde a la solicitud de permisos.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            esperandoPermisos = false;

            if (grantResults.length > 0 && allPermissionsGranted(grantResults)) {
                mostrarDialogoSeleccionImagen();
            } else {
                Toast.makeText(this, "Se requieren permisos para continuar", Toast.LENGTH_SHORT).show();
            }
        }
    }
    /**
     * * Verifica si todos los permisos solicitados fueron concedidos.
     */
    private boolean allPermissionsGranted(int[] results) {
        for (int result : results) {
            if (result != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }


    /**
     * Configura el RecyclerView para mostrar la lista de instrucciones
     */
    private void configurarRecycler() {
        // Inicialización única del Adapter
        instruccionesAdapter = new InstruccionesAdapter(
                listaPasos,
                (fromPosition, toPosition) -> {
                    // Lógica para mover pasos
                    Collections.swap(listaPasos, fromPosition, toPosition);
                    instruccionesAdapter.notifyItemMoved(fromPosition, toPosition);
                    actualizarOrdenEnFirestore(); // Actualiza el orden en Firestore
                },
                position -> {
                    // Lógica para editar pasos
                    mostrarDialogoEdicionPaso(position);
                });

        // Configuración del RecyclerView
        rvInstrucciones.setLayoutManager(new LinearLayoutManager(this));
        rvInstrucciones.setAdapter(instruccionesAdapter);
        rvInstrucciones.setItemAnimator(null);

        // Configuración del ItemTouchHelper para drag & drop y swipe
        configurarTouchHelper();
    }

    /**
     * Muestra diálogo para editar un paso específico
     */
    private void mostrarDialogoEdicionPaso(int position) {
        String pasoActual = listaPasos.get(position);
        EditText editText = new EditText(this);
        editText.setText(pasoActual);
        editText.setSelection(pasoActual.length());

        new AlertDialog.Builder(this)
                .setTitle("Editar paso")
                .setView(editText)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String nuevoPaso = editText.getText().toString().trim();
                    if (!nuevoPaso.isEmpty() && !nuevoPaso.equals(pasoActual)) {
                        actualizarPasoEnFirestore(position, nuevoPaso, () -> {
                            listaPasos.set(position, nuevoPaso);
                            runOnUiThread(() -> {
                                instruccionesAdapter.notifyItemChanged(position);
                                Toast.makeText(this, "Paso actualizado", Toast.LENGTH_SHORT).show();
                            });
                        });
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Configura el ItemTouchHelper para manejar movimientos y deslizamientos
     */
    private void configurarTouchHelper() {
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
                new AlertDialog.Builder(EditarRecetaActivity.this)
                        .setTitle("Eliminar paso")
                        .setMessage("¿Estás seguro de eliminar este paso?")
                        .setPositiveButton("Eliminar", (dialog, which) -> {
                            eliminarPasoDeFirestore(position, () -> {
                                listaPasos.remove(position);
                                runOnUiThread(() -> {
                                    instruccionesAdapter.notifyItemRemoved(position);
                                    Toast.makeText(EditarRecetaActivity.this,
                                            "Paso eliminado", Toast.LENGTH_SHORT).show();
                                });
                            });
                        })
                        .setNegativeButton("Cancelar", (dialog, which) -> {
                            instruccionesAdapter.notifyItemChanged(position);
                        })
                        .show();
            }
        });
        itemTouchHelper.attachToRecyclerView(rvInstrucciones);
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
     * Actualiza el orden de todos los pasos en Firestore
     */
    private void actualizarOrdenEnFirestore() {
        if (receta == null || receta.getInstrucciones() == null) return;

        WriteBatch batch = db.batch();

        for (int i = 0; i < listaPasos.size(); i++) {
            if (i < receta.getInstrucciones().size()) {
                DocumentReference ref = receta.getInstrucciones().get(i);
                batch.update(ref, "orden", i + 1);
            }
        }

        batch.commit().addOnFailureListener(e -> {
            Log.e("Firestore", "Error actualizando orden", e);
            Toast.makeText(this, "Error al guardar el orden", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Elimina un paso de Firestore
     */
    private void eliminarPasoDeFirestore(int position, Runnable onSuccess) {
        if (receta == null || receta.getInstrucciones() == null ||
                position >= receta.getInstrucciones().size()) {
            return;
        }

        receta.getInstrucciones().get(position).delete()
                .addOnSuccessListener(aVoid -> {
                    if (onSuccess != null) onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al eliminar el paso", Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Error eliminando paso", e);
                });
    }

    /**
     * Actualiza el contenido de un paso de instrucción específico en Firestore.
     */
    private void actualizarPasoEnFirestore(int position, String nuevoPaso, Runnable onComplete) {
        if (receta == null || receta.getInstrucciones() == null ||
                position >= receta.getInstrucciones().size()) {
            return;
        }

        DocumentReference pasoRef = receta.getInstrucciones().get(position);
        Map<String, Object> updates = new HashMap<>();
        updates.put("paso", nuevoPaso);
        updates.put("orden", position + 1);

        pasoRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Paso actualizado correctamente");
                    if (onComplete != null) onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error al actualizar paso", e);
                    Toast.makeText(this, "Error al guardar el paso", Toast.LENGTH_SHORT).show();
                });
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

    /**
     * Permite recargar los datos
     */
    @Override
    protected void onResume() {
        super.onResume();
    }

}
