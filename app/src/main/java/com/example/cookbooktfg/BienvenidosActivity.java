package com.example.cookbooktfg;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

/**
 *  Esta actividad ofrece dos opciones al usuario:
 *  Iniciar sesión mediante el botón "Login", o registrarse mediante el botón "Register".
 *  Al pulsar cualquiera de los botones, se redirige al usuario a la actividad correspondiente.
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
