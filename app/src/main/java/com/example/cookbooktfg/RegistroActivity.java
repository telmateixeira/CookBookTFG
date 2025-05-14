package com.example.cookbooktfg;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RegistroActivity extends AppCompatActivity {

    private EditText etNombre, etEmail, etContrasena, etRepContrasena;
    private Button btnRegistro;
    private TextView tvInicioSesion;
    private ImageButton btnVolver;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;


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
                                        Toast.makeText(RegistroActivity.this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(RegistroActivity.this, MenuPrincipalActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(RegistroActivity.this, "Registro fallido: error al guardar usuario", Toast.LENGTH_LONG).show();
                                    });
                        }

                } else {
                        String errorMsg = task.getException().getMessage();
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(RegistroActivity.this, "Este correo ya está registrado.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(RegistroActivity.this, "Error: " + errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}
 