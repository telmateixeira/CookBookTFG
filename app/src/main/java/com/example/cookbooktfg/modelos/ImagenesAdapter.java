package com.example.cookbooktfg.modelos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cookbooktfg.R;

import java.util.List;

/**
 * Adapter para un RecyclerView que muestra una lista de imágenes.
 * Cada elemento muestra una imagen cargada desde una URL o path local.
 *
 *  Autor: Telma Teixeira
 *  Proyecto: CookbookTFG
 */

public class ImagenesAdapter extends RecyclerView.Adapter<ImagenesAdapter.ImagenViewHolder> {

    private final List<String> imagenesUrls;

    /**
     * Constructor que recibe la lista de imágenes.
     *
     * @param imagenesUrls Lista de URLs o paths de las imágenes.
     */
    public ImagenesAdapter(List<String> imagenesUrls) {
        this.imagenesUrls = imagenesUrls;
    }

    /**
     * Crea y devuelve el ViewHolder para un elemento del RecyclerView.
     * Infla el layout del item y lo encapsula en un ViewHolder.
     *
     * @param parent   ViewGroup padre donde se insertará la vista.
     * @param viewType Tipo de vista, no se usa en este caso.
     * @return Nuevo ViewHolder con la vista inflada.
     */
    @NonNull
    @Override
    public ImagenViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_imagen_receta, parent, false);
        return new ImagenViewHolder(view);
    }

    /**
     * Vincula los datos (la URL de la imagen) con el ViewHolder.
     * Carga la imagen con Glide en el ImageView del ViewHolder.
     *
     * @param holder   ViewHolder donde se cargará la imagen.
     * @param position Posición del elemento en la lista.
     */
    @Override
    public void onBindViewHolder(@NonNull ImagenViewHolder holder, int position) {
        Glide.with(holder.itemView.getContext())
                .load(imagenesUrls.get(position))
                .placeholder(R.drawable.placeholder)
                .into(holder.imageView);
    }

    /**
     * Devuelve el número total de elementos (imágenes) en la lista.
     *
     * @return Número de imágenes.
     */
    @Override
    public int getItemCount() {
        return imagenesUrls.size();
    }

    /**
     * ViewHolder que contiene la vista para cada imagen.
     */
    public static class ImagenViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImagenViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewItem);
        }
    }
}