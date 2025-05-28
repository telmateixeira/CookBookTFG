package com.example.cookbooktfg;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
// ... imports existentes ...
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

/**
 * Actividad para editar el perfil del usuario en la aplicación CookbookTFG.
 * Permite al usuario modificar su nombre, cambiar su foto de perfil y actualizar su contraseña.
 *
 *  Autor: Telma Teixeira
 *  Proyecto: CookbookTFG
 */
public class EditarPerfilActivity extends AppCompatActivity {
    private EditText editNombre;
    private ImageView imgPerfil;
    private Button btnGuardar, btnCambiarFoto, btnCambiarContrasena;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private StorageReference storageRef;

    private Uri imagenUri;
    private String imagentemp;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;

    /**
     * Inicializa la actividad y los componentes de la UI, obtiene los datos actuales del usuario
     * y configura los listeners de los botones.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.editar_perfil_activity);

        editNombre = findViewById(R.id.editNombre);
        imgPerfil = findViewById(R.id.imgPerfilEditar);
        btnGuardar = findViewById(R.id.btnGuardarCambios);
        btnCambiarFoto = findViewById(R.id.btnCambiarFoto);
        btnCambiarContrasena = findViewById(R.id.btnCambiarContrasena);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("fotos_perfil");

        inicializarLaunchers();
        cargarDatosUsuario();

        btnCambiarFoto.setOnClickListener(v -> verificarYPedirPermisos());
        btnGuardar.setOnClickListener(v -> guardarCambios());
        btnCambiarContrasena.setOnClickListener(v -> mostrarDialogoCambioContrasena());
        imgPerfil.setOnClickListener(v -> verificarYPedirPermisos());
    }
    /**
     * Registra los ActivityResultLaunchers para tomar fotos o seleccionar imágenes de la galería.
     */
    private void inicializarLaunchers() {
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    if (result && imagenUri != null) {
                        Glide.with(this).load(imagenUri).into(imgPerfil);
                    } else {
                        Toast.makeText(this, "No se tomó la foto", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imagenUri = result.getData().getData();
                        Glide.with(this).load(imagenUri).into(imgPerfil);
                    }
                }
        );
    }
    /**
     * Verifica si se tienen permisos de cámara, y si no los tiene, los solicita.
     * Si ya tiene permisos, muestra el diálogo para seleccionar entre tomar una foto o elegir de galería.
     */
    private void verificarYPedirPermisos() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CODE);
        } else {
            mostrarDialogoSeleccionImagen();
        }
    }
    /**
     * Callback para el resultado de la solicitud de permisos.
     * Si se conceden, abre el diálogo para elegir imagen; si no, muestra un mensaje de advertencia.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mostrarDialogoSeleccionImagen();
            } else {
                Toast.makeText(this, "Se necesitan los permisos para cambiar la foto", Toast.LENGTH_SHORT).show();
            }
        }
    }
    /**
     * Muestra un diálogo para que el usuario elija entre tomar una foto con la cámara
     * o seleccionar una imagen desde la galería.
     */
    private void mostrarDialogoSeleccionImagen() {
        new AlertDialog.Builder(this)
                .setTitle("Seleccionar imagen de perfil")
                .setItems(new String[]{"Tomar foto", "Elegir de galería"}, (dialog, which) -> {
                    if (which == 0) {
                        abrirCamara();
                    } else {
                        abrirGaleria();
                    }
                })
                .show();
    }
    /**
     * Abre la cámara para tomar una foto y guardar la imagen en un archivo temporal.
     */
    private void abrirCamara() {
        try {
            File photoFile = crearArchivoImagen();
            if (photoFile != null) {
                imagenUri = FileProvider.getUriForFile(this,
                        "com.example.cookbooktfg.fileprovider",
                        photoFile);
                takePictureLauncher.launch(imagenUri);
            }
        } catch (IOException ex) {
            Toast.makeText(this, "Error al crear el archivo: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("Camera", "Error al crear archivo", ex);
        }
    }
    /**
     * Abre la galería del dispositivo para que el usuario seleccione una imagen.
     */
    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        pickImageLauncher.launch(Intent.createChooser(intent, "Selecciona una imagen"));
    }
    /**
     * Crea un archivo temporal donde se almacenará la imagen capturada con la cámara.
     *
     * @return Archivo temporal de imagen.
     * @throws IOException si ocurre un error al crear el archivo.
     */
    private File crearArchivoImagen() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        imagentemp = image.getAbsolutePath();
        return image;
    }
    /**
     * Carga los datos actuales del usuario (nombre y foto de perfil) desde Firestore
     * y los muestra en la UI.
     */
    private void cargarDatosUsuario() {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("usuarios").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String nombre = doc.getString("nombre");
                String foto = doc.getString("fotoPerfil");

                editNombre.setText(nombre);

                if (foto != null && !foto.isEmpty()) {
                    Glide.with(this).load(foto).into(imgPerfil);
                }
            }
        });
    }
    /**
     * Guarda los cambios realizados por el usuario (nombre y foto de perfil) en Firestore y Firebase Storage.
     */
    private void guardarCambios() {
        String nuevoNombre = editNombre.getText().toString().trim();
        if (nuevoNombre.isEmpty()) {
            Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        if (imagenUri != null) {
            StorageReference fileRef = storageRef.child(uid + ".jpg");
            fileRef.putFile(imagenUri)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        return fileRef.getDownloadUrl();
                    })
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Uri downloadUri = task.getResult();
                            actualizarPerfil(uid, nuevoNombre, downloadUri.toString());
                        } else {
                            Toast.makeText(this, "Error al subir la imagen", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            actualizarPerfil(uid, nuevoNombre, null);
        }
    }
    /**
     * Actualiza el nombre y/o URL de la foto de perfil del usuario en la base de datos de Firestore.
     *
     * @param uid ID del usuario.
     * @param nombre Nuevo nombre del usuario.
     * @param fotoUrl URL de la nueva foto de perfil (puede ser null si no se cambió).
     */
    private void actualizarPerfil(String uid, String nombre, String fotoUrl) {
        if (fotoUrl != null) {
            db.collection("usuarios").document(uid)
                    .update("nombre", nombre, "fotoPerfil", fotoUrl)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show();
                        redirigirAAjustes();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al actualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            db.collection("usuarios").document(uid)
                    .update("nombre", nombre)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Nombre actualizado", Toast.LENGTH_SHORT).show();
                        redirigirAAjustes();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al actualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
    /**
     * Redirige al usuario de vuelta a la actividad de ajustes después de guardar los cambios.
     */
    private void redirigirAAjustes() {
        Intent intent = new Intent(this, AjustesUserActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
    /**
     * Muestra un diálogo para permitir al usuario cambiar su contraseña.
     * Incluye campos para la contraseña actual, nueva y confirmación.
     */
    private void mostrarDialogoCambioContrasena() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cambiar Contraseña");

        // Configurar el layout del diálogo
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        // Campo para contraseña actual
        TextInputEditText etContrasenaActual = new TextInputEditText(this);
        etContrasenaActual.setHint("Contraseña actual");
        etContrasenaActual.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etContrasenaActual);

        // Campo para nueva contraseña
        TextInputEditText etNuevaContrasena = new TextInputEditText(this);
        etNuevaContrasena.setHint("Nueva contraseña");
        etNuevaContrasena.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etNuevaContrasena);

        // Campo para confirmar nueva contraseña
        TextInputEditText etConfirmarContrasena = new TextInputEditText(this);
        etConfirmarContrasena.setHint("Confirmar nueva contraseña");
        etConfirmarContrasena.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etConfirmarContrasena);

        builder.setView(layout);

        builder.setPositiveButton("Cambiar", (dialog, which) -> {
            String contrasenaActual = etContrasenaActual.getText().toString().trim();
            String nuevaContrasena = etNuevaContrasena.getText().toString().trim();
            String confirmarContrasena = etConfirmarContrasena.getText().toString().trim();

            validarYCambiarContrasena(contrasenaActual, nuevaContrasena, confirmarContrasena);
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }
    /**
     * Valida las contraseñas introducidas por el usuario y realiza el cambio de contraseña
     * si pasa la reautenticación con Firebase.
     *
     * @param contrasenaActual Contraseña actual del usuario.
     * @param nuevaContrasena Nueva contraseña deseada.
     * @param confirmarContrasena Confirmación de la nueva contraseña.
     */
    private void validarYCambiarContrasena(String contrasenaActual, String nuevaContrasena, String confirmarContrasena) {
        // Validaciones básicas
        if (contrasenaActual.isEmpty() || nuevaContrasena.isEmpty() || confirmarContrasena.isEmpty()) {
            Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!nuevaContrasena.equals(confirmarContrasena)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        if (nuevaContrasena.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            // Primero reautenticar al usuario
            AuthCredential credential = EmailAuthProvider
                    .getCredential(user.getEmail(), contrasenaActual);

            user.reauthenticate(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            user.updatePassword(nuevaContrasena)
                                    .addOnCompleteListener(updateTask -> {
                                        if (updateTask.isSuccessful()) {
                                            Toast.makeText(EditarPerfilActivity.this,
                                                    "Contraseña cambiada con éxito",
                                                    Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(EditarPerfilActivity.this,
                                                    "Error al cambiar contraseña: " +
                                                            updateTask.getException().getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(EditarPerfilActivity.this,
                                    "Contraseña actual incorrecta",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

}
