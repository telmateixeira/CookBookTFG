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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class RecetaAdapter extends RecyclerView.Adapter<RecetaAdapter.RecetaViewHolder> {

    private List<Receta> recetaList;
    private List<Receta> recetaOriginal;
    private Context context;
    private FirebaseFirestore db;
    private List<String> ingredientesFiltro = new ArrayList<>();

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

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            DocumentReference userRef = FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(user.getUid());
            DocumentReference recetaRef = FirebaseFirestore.getInstance()
                    .collection("recetas")
                    .document(receta.getId());

            if (nuevoEstado) {
                userRef.update("favoritos", FieldValue.arrayUnion(recetaRef))
                        .addOnSuccessListener(aVoid -> Log.d("Firestore", "Añadido a favoritos"));
            } else {
                userRef.update("favoritos", FieldValue.arrayRemove(recetaRef))
                        .addOnSuccessListener(aVoid -> {
                            Log.d("Firestore", "Eliminado de favoritos");
                            if (favoritoCambiadoListener != null) {
                                favoritoCambiadoListener.onFavoritoQuitado(receta, holder.getAdapterPosition());
                            }
                        });
            }
        });

        // Abrir detalle
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onRecetaClick(receta.getId());
            }
            Intent intent = new Intent(context, DetalleRecetaActivity.class);
            intent.putExtra("recetaId", receta.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        Log.d("RecetaAdapter", "Total recetas a mostrar: " + recetaList.size());
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

    public void filtrarPorNombre(String texto) {
        List<Receta> listaFiltrada = new ArrayList<>();
        texto = texto.toLowerCase().trim();

        for (Receta receta : recetaOriginal) {
            if (receta.getNombre().toLowerCase().contains(texto)) {
                if (ingredientesFiltro.isEmpty() || contieneIngredientes(receta, ingredientesFiltro)) {
                    listaFiltrada.add(receta);
                }
            }
        }

        recetaList.clear();
        recetaList.addAll(listaFiltrada);
        notifyDataSetChanged();
    }



    public void filtrarPorIngredientesSeleccionados(List<String> ingredientesSeleccionados) {
        this.ingredientesFiltro = ingredientesSeleccionados;

        if (ingredientesSeleccionados.isEmpty()) {
            recetaList.clear();
            recetaList.addAll(recetaOriginal);
        } else {
            List<Receta> listaFiltrada = new ArrayList<>();

            for (Receta receta : recetaOriginal) {
                if (contieneIngredientes(receta, ingredientesSeleccionados)) {
                    listaFiltrada.add(receta);
                }
            }

            recetaList.clear();
            recetaList.addAll(listaFiltrada);
        }

        notifyDataSetChanged();
    }



    private boolean contieneIngredientes(Receta receta, List<String> ingredientesBuscados) {
        if (receta.getIngredientes() == null) return false;

        // Necesitarás implementar esta lógica según cómo almacenas los ingredientes
        for (DocumentReference ingredienteRef : receta.getIngredientes()) {
            String idIngrediente = ingredienteRef.getId();
            if (ingredientesBuscados.contains(idIngrediente)) {
                return true;
            }
        }
        return false;
    }

    public void actualizarRecetas(List<Receta> nuevasRecetas) {
        recetaList.clear();
        recetaList.addAll(nuevasRecetas);
        recetaOriginal.clear();
        recetaOriginal.addAll(nuevasRecetas);
        notifyDataSetChanged();
    }

    public void eliminarRecetaEnPosicion(int posicion) {
        if (posicion >= 0 && posicion < recetaList.size()) {
            recetaList.remove(posicion);
            recetaOriginal.remove(posicion);
            notifyItemRemoved(posicion);
        }
    }


    public interface OnFavoritoCambiadoListener {
        void onFavoritoQuitado(Receta receta, int posicion);
    }

    private OnFavoritoCambiadoListener favoritoCambiadoListener;

    public void setOnFavoritoCambiadoListener(OnFavoritoCambiadoListener listener) {
        this.favoritoCambiadoListener = listener;
    }

    private OnRecetaClickListener clickListener;

    public void setOnRecetaClickListener(OnRecetaClickListener listener) {
        this.clickListener = listener;
    }

    public interface OnRecetaClickListener {
        void onRecetaClick(String recetaId);
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


