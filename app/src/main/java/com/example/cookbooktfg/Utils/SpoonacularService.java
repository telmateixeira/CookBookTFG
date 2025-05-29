package com.example.cookbooktfg.Utils;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Interfaz Retrofit que define los endpoints para acceder a la API Spoonacular relacionados con recetas.
 *
 * Cada metodo representa una llamada HTTP GET a la API, con los parámetros necesarios.
 */
public interface SpoonacularService {
    /**
     * Obtiene una lista de recetas aleatorias.
     *
     * Endpoint: GET /recipes/random
     *
     * @param apiKey       Clave API para autenticar la solicitud.
     * @param number       Número de recetas aleatorias que se desean obtener.
     * @return             Call que devuelve un objeto SpoonacularResponse con las recetas.
     */
    @GET("recipes/random")
    Call<SpoonacularReceta.SpoonacularResponse> getRandomRecipes(
            @Query("apiKey") String apiKey,
            @Query("number") int number
    );
}