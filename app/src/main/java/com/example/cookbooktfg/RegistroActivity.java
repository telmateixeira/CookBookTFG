package com.example.cookbooktfg;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import android.Manifest;


import de.hdodenhof.circleimageview.CircleImageView;

public class RegistroActivity extends AppCompatActivity {

    private TextInputEditText etNombre, etEmail, etContraseña;
    private Button btnRegistro, btnElegirFoto;
    private CircleImageView imgPerfil;
    private ImageButton btnVolver;
    private Uri uriImagenSeleccionada;

    private static final int REQUEST_IMAGE_PICK = 100;
    private static final int REQUEST_IMAGE_CAMERA = 101;
    private static final int REQUEST_PERMISOS = 200;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registro_activity);

        etNombre = findViewById(R.id.etNombre);
        etEmail = findViewById(R.id.etEmail);
        etContraseña = findViewById(R.id.etContraseña);
        btnRegistro = findViewById(R.id.btnRegistro);
        imgPerfil = findViewById(R.id.imgPerfil);
        btnElegirFoto = findViewById(R.id.btnElegirFoto);
        btnVolver = findViewById(R.id.btnVolver);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        solicitarPermisos();

        btnVolver.setOnClickListener(v -> finish());
        btnElegirFoto.setOnClickListener(v -> mostrarOpcionesfoto());
        btnRegistro.setOnClickListener(v -> registrarUsuario());
    }

    private void solicitarPermisos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES
            }, REQUEST_PERMISOS);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, REQUEST_PERMISOS);
        }
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void mostrarOpcionesfoto() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar opción");
        String[] opciones = {"Tomar foto", "Elegir de galería"};
        builder.setItems(opciones, (dialog, which) -> {
            if (which == 0) {
                tomarFotoConCamara();
            } else {
                elegirImagenDeGaleria();
            }
        });
        builder.show();
    }

    private void elegirImagenDeGaleria() {
        if (!checkPermissions()) {
            solicitarPermisos();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private void tomarFotoConCamara() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAMERA);
        }
    }

    private File crearArchivoImagen() throws IOException {
        String nombreArchivo = "foto_" + System.currentTimeMillis();
        File directorio = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(nombreArchivo, ".jpg", directorio);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_CAMERA:
                    // Foto tomada con cámara
                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    uriImagenSeleccionada = getImageUri(this, imageBitmap);
                    imgPerfil.setImageBitmap(imageBitmap);
                    break;
                case REQUEST_IMAGE_PICK:
                    // Imagen seleccionada de galería
                    if (data != null && data.getData() != null) {
                        uriImagenSeleccionada = data.getData();
                        imgPerfil.setImageURI(uriImagenSeleccionada);
                    }
                    break;
            }
        }
    }

    // Método para convertir Bitmap a Uri (igual que en AjustesPerfil)
    private Uri getImageUri(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "Title", null);
        return Uri.parse(path);
    }


    private void subirImagenAFirebase(String userId, Uri imageUri) {
        if (imageUri == null) {
            Toast.makeText(this, "No hay imagen seleccionada", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crea una referencia única para la imagen
        StorageReference fotoRef = storageRef.child("fotosPerfil/" + userId + ".jpg");

        try {
            // Crea un archivo temporal (igual que en AjustesPerfil)
            File imageFile = createImageFile();
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            OutputStream outputStream = new FileOutputStream(imageFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            inputStream.close();
            outputStream.close();

            // Sube el archivo
            fotoRef.putFile(Uri.fromFile(imageFile))
                    .addOnSuccessListener(taskSnapshot -> {
                        fotoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            guardarDatosUsuario(userId, etNombre.getText().toString(),
                                    etEmail.getText().toString(), imageUrl);
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al subir imagen: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("FIREBASE_UPLOAD", "Error: ", e);
                    });
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
        }
    }

    // Método para crear archivo temporal (igual que en AjustesPerfil)
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
    }


    private void registrarUsuario() {
        String nombre = etNombre.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String contraseña = etContraseña.getText().toString().trim();

        if (nombre.isEmpty() || email.isEmpty() || contraseña.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, contraseña)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            if (uriImagenSeleccionada != null) {
                                subirImagenAFirebase(user.getUid(), uriImagenSeleccionada);
                            } else {
                                // Registro sin imagen
                                guardarDatosUsuario(user.getUid(), nombre, email, "");
                            }
                        }
                    } else {
                        Toast.makeText(this, "Error al registrar: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void guardarDatosUsuario(String userId, String nombre, String email, String fotoUrl) {
        Map<String, Object> datosUsuario = new HashMap<>();
        datosUsuario.put("nombre", nombre);
        datosUsuario.put("email", email);
        datosUsuario.put("fechaRegistro", FieldValue.serverTimestamp());
        datosUsuario.put("fotoPerfil", fotoUrl);

        db.collection("usuarios").document(userId)
                .set(datosUsuario)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, BienvenidosActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al guardar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    mAuth.getCurrentUser().delete(); // eliminamos usuario si falla firestore
                });
    }
}