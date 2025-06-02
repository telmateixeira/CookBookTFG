package com.example.cookbooktfg.actividades;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cookbooktfg.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
/**
 * Clase que representa la actividad de inicio de sesión de la aplicación Cookbook.
 * Permite a los usuarios autenticarse mediante correo electrónico y contraseña usando Firebase Authentication.
 *
 * Autor: Telma Teixeira
 * Proyecto: CookbookTFG
 */
public class LoginActivity extends AppCompatActivity{

    private EditText etEmail, etContraseña;
    private Button btnLogin;
    private TextView tvRegister, tvcontrasenaOlvidada;
    private ImageButton btnVolver;
    private FirebaseAuth auth;

    /**
     * Metodo llamado al crear la actividad. Inicializa los componentes de la interfaz y la instancia de FirebaseAuth.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        // Inicializamos Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Vistas
        etEmail = findViewById(R.id.etEmail);
        etContraseña = findViewById(R.id.etContraseña);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        btnVolver = findViewById(R.id.btnVolver);
        tvcontrasenaOlvidada = findViewById(R.id.tvContrasenaOlvidada);

        btnVolver.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, BienvenidosActivity.class);
            startActivity(intent);
            finish();
        });

        // Botón de Registrarse
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegistroActivity.class);
            startActivity(intent);
        });

        // Botón de Login
        btnLogin.setOnClickListener(v -> iniciarSesion());

        tvcontrasenaOlvidada.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Introduce tu correo para restablecer la contraseña");
                etEmail.requestFocus();
                return;
            }

            auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Correo de restablecimiento enviado", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(LoginActivity.this, "Error al enviar el correo. Verifica el correo.", Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
    /**
     * Metodo que valida los campos de email y contraseña e intenta iniciar sesión con Firebase Authentication.
     * En caso de éxito, redirige al usuario al menú principal. Si falla, muestra un mensaje de error adecuado.
     */
    private void iniciarSesion() {
        String email = etEmail.getText().toString().trim();
        String contraseña = etContraseña.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Introduce tu correo");
            return;
        }

        if (TextUtils.isEmpty(contraseña)) {
            etContraseña.setError("Introduce tu contraseña");
            return;
        }

        // Comprobamos en Firebase
        auth.signInWithEmailAndPassword(email, contraseña)
                .addOnCompleteListener(this, task -> {
                    // progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        // Inicio de sesión correcto
                        Toast.makeText(LoginActivity.this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, MenuPrincipalActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // Manejo específico de errores
                        String errorMessage = "Error en el inicio de sesión";
                        try {
                            throw task.getException();
                        } catch (FirebaseAuthInvalidUserException e) {
                            errorMessage = "Usuario no registrado o correo inválido";
                        } catch (FirebaseAuthInvalidCredentialsException e) {
                            errorMessage = "Contraseña incorrecta";
                        } catch (Exception e) {
                            errorMessage = "Error: " + e.getMessage();
                        }
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        Log.e("Login", errorMessage);
                    }
                });
    }

    /**
     * Maneja el comportamiento del botón de navegación "Atrás" de la barra superior si está presente.
     * Redirige a la pantalla de bienvenida.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Vuelve a la actividad de Bienvenida
            Intent intent = new Intent(LoginActivity.this, BienvenidosActivity.class);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
