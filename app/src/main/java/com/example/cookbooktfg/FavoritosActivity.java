package com.example.cookbooktfg;

import android.os.Bundle;
import android.os.PersistableBundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class FavoritosActivity extends AppCompatActivity {
    private RecyclerView recyclerViewRecetasFav;
    private RecetaAdapter adapter;
    private List<Receta> recetaFavs;
    private BottomNavigationView bottomNavigationViewFav;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        setContentView(R.layout.favoritos_activity);

        recyclerViewRecetasFav = findViewById(R.id.recyclerViewRecetasFav);
        bottomNavigationViewFav = findViewById(R.id.bottomNavigationViewFav);
        adapter = new RecetaAdapter(recetaList, this);

        recyclerViewRecetasFav.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewRecetasFav.setAdapter(adapter);


    }
}
