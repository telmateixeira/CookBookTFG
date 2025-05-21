package com.example.cookbooktfg;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class EditarPerfilActivity extends AppCompatActivity {
    private EditText editNombre;
    private ImageView imgPerfil;
    private Button btnGuardar;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.editar_perfil_activity);

        editNombre = findViewById(R.id.editNombre);
        imgPerfil = findViewById(R.id.imgPerfilEditar);
        btnGuardar = findViewById(R.id.btnGuardarCambios);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        cargarDatosUsuario();

        btnGuardar.setOnClickListener(v -> guardarCambios());
    }

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

    private void guardarCambios() {
        String nuevoNombre = editNombre.getText().toString().trim();
        if (nuevoNombre.isEmpty()) {
            Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        db.collection("usuarios").document(uid)
                .update("nombre", nuevoNombre)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show();
                });
    }
}
