package com.example.cookbooktfg;
// SpoonacularClient.java

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SpoonacularClient {
    private static final String BASE_URL = "https://api.spoonacular.com/";
    private static SpoonacularService instance;

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