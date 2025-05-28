package com.example.cookbooktfg;
// SpoonacularClient.java

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
/**
 * Clase singleton que proporciona una instancia configurada de Retrofit para acceder a la API Spoonacular.
 *
 * Esta clase utiliza Retrofit para crear un cliente HTTP con la URL base de Spoonacular y el convertidor Gson
 * para parsear automáticamente las respuestas JSON en objetos Java.
 */
public class SpoonacularClient {
    private static final String BASE_URL = "https://api.spoonacular.com/";
    private static SpoonacularService instance;

    /**
     * Obtiene la instancia singleton de SpoonacularService, creando la configuración Retrofit si es necesario.
     *
     * @return Instancia de SpoonacularService para hacer llamadas a la API.
     */
    public static SpoonacularService getInstance() {
        if (instance == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            instance = retrofit.create(SpoonacularService.class);
        }
        return instance;
    }
}