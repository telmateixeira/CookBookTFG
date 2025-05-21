package com.example.cookbooktfg;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MisRecetasAdapter extends RecyclerView.Adapter<MisRecetasAdapter.MisRecetasViewHolder> {

    private List<Receta> misRecetas;
    private OnRecipeActionListener listener;
    private final Map<String, String> nombresAutores = new HashMap<>();

    public interface OnRecipeActionListener {
        void onEditRecipe(Receta receta);
        void onDeleteRecipe(Receta receta);
    }

    public MisRecetasAdapter(List<Receta> misRecetas, OnRecipeActionListener listener) {
        this.misRecetas = misRecetas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MisRecetasViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_receta_editable, parent, false);
        return new MisRecetasViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MisRecetasViewHolder holder, int position) {
        holder.bind(misRecetas.get(position));
    }

    @Override
    public int getItemCount() {
        return misRecetas.size();  // Corregido: devolver el tamaño real de la lista
    }

    // Métodos adicionales para actualizar datos
    public void actualizarLista(List<Receta> nuevasRecetas) {
        misRecetas = nuevasRecetas;
        notifyDataSetChanged();
    }

    public void eliminarReceta(int position) {
        misRecetas.remove(position);
        notifyItemRemoved(position);
    }

    class MisRecetasViewHolder extends RecyclerView.ViewHolder {
        private final TextView tituloR;
        private final TextView autorR;
        private final ImageButton btnEditar, btnBorrar;
        private final ImageView imgReceta;

        public MisRecetasViewHolder(@NonNull View itemView) {
            super(itemView);
            tituloR = itemView.findViewById(R.id.tituloReceta);
            autorR = itemView.findViewById(R.id.autorReceta);
            btnEditar = itemView.findViewById(R.id.botonEditaIcono);
            btnBorrar = itemView.findViewById(R.id.botonEliminarIcono);
            imgReceta = itemView.findViewById(R.id.imagenReceta);
        }

        public void bind(Receta receta) {
            tituloR.setText(receta.getNombre());
            String creadorId = receta.getCreadorId();

            // Cargar la primera imagen de receta si existe
            List<String> imagenes = receta.getImagenes();
            if (imagenes != null && !imagenes.isEmpty()) {
                String primeraImagenUrl = imagenes.get(0);
                Glide.with(itemView.getContext())
                        .load(primeraImagenUrl)
                        .placeholder(R.drawable.placeholder)
                        .transform(new CenterCrop(), new RoundedCorners(16)) // Bordes redondeados
                        .into(imgReceta);
            } else {
                imgReceta.setImageResource(R.drawable.placeholder);
            }


            if (nombresAutores.containsKey(creadorId)) {
                autorR.setText(nombresAutores.get(creadorId));
            } else {
                autorR.setText("Cargando autor..."); // Placeholder temporal
                FirebaseFirestore.getInstance()
                        .collection("usuarios")
                        .document(creadorId)
                        .get()
                        .addOnSuccessListener(snapshot -> {
                            if (snapshot.exists()) {
                                String nombre = snapshot.getString("nombre");
                                if (nombre != null) {
                                    autorR.setText(nombre);
                                    nombresAutores.put(creadorId, nombre);
                                } else {
                                    autorR.setText("Autor desconocido");
                                }
                            } else {
                                autorR.setText("Autor no encontrado");
                            }
                        })
                        .addOnFailureListener(e -> autorR.setText("Error al cargar autor"));
            }

            btnEditar.setOnClickListener(v -> {
                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onEditRecipe(receta);
                }
            });

            btnBorrar.setOnClickListener(v -> {
                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onDeleteRecipe(receta);
                }
            });
        }

    }
}