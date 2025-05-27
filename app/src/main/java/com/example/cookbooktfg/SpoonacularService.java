package com.example.cookbooktfg;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface SpoonacularService {
    @GET("recipes/random")
    Call<SpoonacularReceta.SpoonacularResponse> getRandomRecipes(
            @Query("apiKey") String apiKey,
            @Query("number") int number
    );

    @GET("recipes/{id}/information")
    Call<SpoonacularReceta.SpoonacularRecipe> getRecipeDetails(
            @Path("id") int recipeId,
            @Query("apiKey") String apiKey,
            @Query("includeNutrition") boolean includeNutrition
    );

    @GET("recipes/{id}/information")
    Call<SpoonacularReceta.SpoonacularRecipe> getRecipeInformationById(
            @Path("id") int id,
            @Query("apiKey") String apiKey
    );

}