package com.example.cookbooktfg;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class RegistroActivity extends AppCompatActivity {

    private EditText etNombre, etEmail, etContrasena, etRepContrasena;
    private Button btnRegistro;
    private TextView tvInicioSesion;
    private ImageButton btnVolver;
    private CircleImageView imgPerfil;
    private Button btnElegirFoto;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private StorageReference storageRef;

    private Uri imagenUri;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private String imagenTemp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registro_activity);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etNombre = findViewById(R.id.etNombre);
        etEmail = findViewById(R.id.etEmail);
        etContrasena = findViewById(R.id.etContrasena);
        etRepContrasena = findViewById(R.id.etRepContrasena);
        btnRegistro = findViewById(R.id.btnRegistro);
        tvInicioSesion = findViewById(R.id.tvInicioSesion);
        btnVolver = findViewById(R.id.btnVolver);

        imgPerfil = findViewById(R.id.imgPerfil);
        btnElegirFoto = findViewById(R.id.btnElegirFoto);
        storageRef = FirebaseStorage.getInstance().getReference();

        btnElegirFoto.setOnClickListener(v -> verificarYPedirPermisos());

        btnRegistro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registrarUsuario();
            }
        });

        tvInicioSesion.setOnClickListener(v -> {
            startActivity(new Intent(RegistroActivity.this, LoginActivity.class));
            finish();
        });

        btnVolver.setOnClickListener(v -> finish());
    }

    private void registrarUsuario() {
        String nombre = etNombre.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etContrasena.getText().toString().trim();
        String password2 = etRepContrasena.getText().toString().trim();

        if (TextUtils.isEmpty(nombre)) {
            etNombre.setError("Ingresa tu nombre");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Ingresa tu correo");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Formato de correo inválido");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etContrasena.setError("Ingresa una contraseña");
            return;
        }

        if(TextUtils.isEmpty(password2)) {
            etRepContrasena.setError("Debes de repetir la contraseña");
            return;
        }

        if (password.length() < 6) {
            etContrasena.setError("La contraseña debe tener al menos 6 caracteres");
            return;
        }

        if (password2.length() < 6) {
            etRepContrasena.setError("La contraseña debe tener al menos 6 caracteres");
            return;
        }

        if(!password.equals(password2)){
            etRepContrasena.setError("Las contraseñas deben de ser las mismas");
            return;
        }

        // Mostrar ProgressBar al iniciar registro
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Registrando usuario...");
        progressDialog.setCancelable(false);
        progressDialog.show();


        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String uid = user.getUid();

                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("nombre", nombre);
                            userMap.put("email", email);
                            userMap.put("fechaRegistro", FieldValue.serverTimestamp());
                            userMap.put("favoritos", new ArrayList<String>());
                            userMap.put("recetasCreadas", new ArrayList<String>());

                            db.collection("usuarios").document(uid)
                                    .set(userMap)
                                    .addOnSuccessListener(aVoid -> {
                                        if (imagenUri != null) {
                                            subirImagenYRegistrar(uid, nombre, email, progressDialog);
                                        } else {
                                            progressDialog.dismiss();
                                            Toast.makeText(RegistroActivity.this, "Registro exitoso. Inicia sesión para continuar", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(RegistroActivity.this, LoginActivity.class));
                                            finish();


                                        }
                                    });


                        }

                } else {
                        progressDialog.dismiss();
                        String errorMsg = task.getException().getMessage();
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(RegistroActivity.this, "Este correo ya está registrado.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(RegistroActivity.this, "Error: " + errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void verificarYPedirPermisos() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    PERMISSION_REQUEST_CODE);
        } else {
            mostrarDialogoSeleccionImagen();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mostrarDialogoSeleccionImagen();
            } else {
                Toast.makeText(this, "Se necesitan los permisos para continuar", Toast.LENGTH_SHORT).show();
            }
        }
    }

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

    private void abrirCamara() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = crearArchivoImagen();
            } catch (IOException ex) {
                Toast.makeText(this, "Error al crear el archivo", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                imagenUri = FileProvider.getUriForFile(this,
                        "com.example.cookbooktfg.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imagenUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File crearArchivoImagen() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        imagenTemp = image.getAbsolutePath();
        return image;
    }


    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }



    private void subirImagenYRegistrar(String uid, String nombre, String email, ProgressDialog progressDialog) {
        if (imagenUri == null) {
            Toast.makeText(this, "No se ha seleccionado ninguna imagen", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
            return;
        }

        // Verifica que la URI sea accesible
        try {
            InputStream stream = getContentResolver().openInputStream(imagenUri);
            if (stream == null) {
                throw new FileNotFoundException("No se pudo abrir el stream");
            }
            stream.close();
        } catch (IOException e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Error al acceder a la imagen: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        // Crea referencia con estructura organizada
        StorageReference fileRef = storageRef.child("usuarios/" + uid + "/perfil.jpg");

        // Sube el archivo con manejo mejorado
        UploadTask uploadTask = fileRef.putFile(imagenUri);

        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            progressDialog.setMessage("Subiendo imagen: " + (int) progress + "%");
        }).addOnSuccessListener(taskSnapshot -> {
            // Obtiene la URL de descarga
            fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                // Actualiza el perfil del usuario
                db.collection("usuarios").document(uid)
                        .update("fotoPerfil", uri.toString())
                        .addOnSuccessListener(aVoid -> {
                            progressDialog.dismiss();
                            Toast.makeText(RegistroActivity.this, "Registro exitoso. Inicia sesión para continuar", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RegistroActivity.this, LoginActivity.class));
                            finish();

                        })
                        .addOnFailureListener(e -> {
                            progressDialog.dismiss();
                            Toast.makeText(RegistroActivity.this, "Error al guardar URL de imagen", Toast.LENGTH_SHORT).show();
                        });
            });
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Log.e("FIREBASE_STORAGE", "Error al subir imagen", e);

            // Mensaje más descriptivo
            if (e instanceof StorageException) {
                StorageException se = (StorageException) e;
                String errorMsg = "Error " + se.getErrorCode() + ": ";
                switch (se.getErrorCode()) {
                    case StorageException.ERROR_OBJECT_NOT_FOUND:
                        errorMsg += "Ubicación no válida en Storage";
                        break;
                    case StorageException.ERROR_BUCKET_NOT_FOUND:
                        errorMsg += "Bucket no configurado correctamente";
                        break;
                    default:
                        errorMsg += se.getMessage();
                }
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Error al subir imagen: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_CAPTURE:
                    // La imagen ya está guardada en imagenUri
                    if (imagenUri != null) {
                        imgPerfil.setImageURI(imagenUri);
                    }
                    break;
                case REQUEST_IMAGE_PICK:
                    if (data != null && data.getData() != null) {
                        imagenUri = data.getData();
                        imgPerfil.setImageURI(imagenUri);
                    }
                    break;
            }
        }
    }


}
 