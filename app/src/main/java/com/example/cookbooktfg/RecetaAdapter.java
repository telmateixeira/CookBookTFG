package com.example.cookbooktfg;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class RecetaAdapter extends RecyclerView.Adapter<RecetaAdapter.RecetaViewHolder> {

    private List<Receta> recetaList;
    private List<Receta> recetaOriginal;
    private Context context;
    private FirebaseFirestore db;

    public RecetaAdapter(List<Receta> recetaList, Context context) {
        this.recetaList = new ArrayList<>(recetaList);
        this.recetaOriginal = new ArrayList<>(recetaList);
        this.context = context;
    }

    @NonNull
    @Override
    public RecetaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_receta, parent, false);
        return new RecetaViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull RecetaViewHolder holder, int position) {
        Receta receta = recetaList.get(position);
        db = FirebaseFirestore.getInstance();
        holder.titulo.setText(receta.getNombre());

        // Cargar imagen principal (primera imagen de la lista)
        if (receta.getImagenes() != null && !receta.getImagenes().isEmpty()) {
            String primeraImagenUrl = receta.getImagenes().get(0);

            Glide.with(context)
                    .load(primeraImagenUrl)
                    .placeholder(R.drawable.placeholder) // Imagen por defecto
                    .centerCrop()
                    .transform(new CenterCrop(), new RoundedCorners(16)) // Bordes redondeados
                    .into(holder.imagenReceta);
        } else {
            // Si no hay imágenes, mostrar placeholder
            holder.imagenReceta.setImageResource(R.drawable.placeholder);
        }


        if (receta.getCreadorId() != null && !receta.getCreadorId().isEmpty()) {
            db.collection("usuarios")
                    .document(receta.getCreadorId()) // Acceso directo por UID
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String nombreAutor = documentSnapshot.getString("nombre");
                            holder.autor.setText(nombreAutor != null ? nombreAutor : "Anónimo");
                        } else {
                            holder.autor.setText("Usuario eliminado");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("RecetaAdapter", "Error al obtener usuario", e);
                        holder.autor.setText("Error cargando autor");
                    });
        } else {
            holder.autor.setText("Anónimo");
        }


        if (receta.isFavorito()) {
            holder.botonFavorito.setImageResource(R.drawable.favoritos_icono_naranja);
        } else {
            holder.botonFavorito.setImageResource(R.drawable.favoritos_icono);
        }

        holder.botonFavorito.setOnClickListener(v -> {
            boolean nuevoEstado = !receta.isFavorito();
            receta.setFavorito(nuevoEstado);
            notifyItemChanged(holder.getAdapterPosition());

            FirebaseFirestore.getInstance().collection("recetas")
                    .document(receta.getId())
                    .update("favorito", nuevoEstado)
                    .addOnSuccessListener(unused -> Log.d("Firestore", "Favorito actualizado"))
                    .addOnFailureListener(e -> Log.e("Firestore", "Error actualizando favorito", e));
        });

        // Abrir detalle
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetalleRecetaActivity.class);
            intent.putExtra("recetaId", receta.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return recetaList.size();
    }

    public void filtrarPorIngrediente(String texto) {
        List<Receta> listaFiltrada = new ArrayList<>();

        for (Receta r : recetaOriginal) {
            for (DocumentReference ingredienteRef : r.getIngredientes()) {
                ingredienteRef.get().addOnSuccessListener(documentSnapshot -> {
                    String nombreIngrediente = documentSnapshot.getString("nombre");
                    if (nombreIngrediente != null && nombreIngrediente.toLowerCase().contains(texto.toLowerCase())) {
                        if (!listaFiltrada.contains(r)) {
                            listaFiltrada.add(r);
                            recetaList.clear();
                            recetaList.addAll(listaFiltrada);
                            notifyDataSetChanged();
                        }
                    }
                });
            }
        }
    }

    public void actualizarRecetas(List<Receta> nuevasRecetas) {
        recetaList.clear();
        recetaList.addAll(nuevasRecetas);
        recetaOriginal.clear();
        recetaOriginal.addAll(nuevasRecetas);
        notifyDataSetChanged();
    }

    public static class RecetaViewHolder extends RecyclerView.ViewHolder {

        ImageView imagenReceta;
        TextView titulo, autor;
        ImageButton botonFavorito;

        public RecetaViewHolder(@NonNull View itemView) {
            super(itemView);
            imagenReceta = itemView.findViewById(R.id.imagenReceta);
            titulo = itemView.findViewById(R.id.tituloReceta);
            autor = itemView.findViewById(R.id.autorReceta);
            botonFavorito = itemView.findViewById(R.id.botonFavorito);
        }
    }
}


