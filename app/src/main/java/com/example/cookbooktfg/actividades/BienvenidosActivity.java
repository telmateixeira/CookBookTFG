package com.example.cookbooktfg.actividades;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cookbooktfg.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 *  Esta actividad ofrece dos opciones al usuario:
 *  Iniciar sesión mediante el botón "Login", o registrarse mediante el botón "Register".
 *  Al pulsar cualquiera de los botones, se redirige al usuario a la actividad correspondiente.
 *  Si detecta que el usuario ya esta logueado no carga esta actividad y le manda
 */
public class BienvenidosActivity extends AppCompatActivity {

    private Button btnLogin, btnRegister;

    /**
     * Metodo llamado al crear la actividad. Inicializa la interfaz y los listeners de los botones.
     *
     * @param savedInstanceState Estado previamente guardado de la actividad (si existe).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Verificar si ya hay un usuario autenticado
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Usuario ya logueado, ir directamente al menú principal
            Intent intent = new Intent(BienvenidosActivity.this, MenuPrincipalActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.bienvenidos_activity);

        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BienvenidosActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BienvenidosActivity.this, RegistroActivity.class);
                startActivity(intent);
            }
        });
    }
}
