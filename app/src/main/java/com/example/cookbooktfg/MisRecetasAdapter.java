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
/**
 * Adaptador para RecyclerView que muestra una lista de recetas creadas por el usuario.
 * Permite editar y eliminar recetas desde la interfaz.
 *
 * Cada ítem muestra el título de la receta, el nombre del autor (obtenido desde Firestore),
 * y una imagen de la receta (primera imagen de la lista). Además incluye botones para editar
 * y eliminar la receta.
 */
public class MisRecetasAdapter extends RecyclerView.Adapter<MisRecetasAdapter.MisRecetasViewHolder> {

    private List<Receta> misRecetas;
    private OnRecipeActionListener listener;
    private final Map<String, String> nombresAutores = new HashMap<>();
    /**
     * Interfaz para gestionar las acciones sobre una receta:
     * editar o eliminar.
     */
    public interface OnRecipeActionListener {
        void onEditRecipe(Receta receta);
        void onDeleteRecipe(Receta receta);
    }
    /**
     * Constructor del adaptador.
     *
     * @param misRecetas lista inicial de recetas.
     * @param listener   listener para eventos de edición y eliminación.
     */
    public MisRecetasAdapter(List<Receta> misRecetas, OnRecipeActionListener listener) {
        this.misRecetas = misRecetas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MisRecetasViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Infla el layout de un ítem editable para la receta
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_receta_editable, parent, false);
        return new MisRecetasViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MisRecetasViewHolder holder, int position) {
        // Enlaza la receta en la posición indicada con el ViewHolder
        holder.bind(misRecetas.get(position));
    }

    @Override
    public int getItemCount() {
        // Devuelve el número de recetas en la lista
        return misRecetas.size();
    }

    /**
     * ViewHolder que representa un ítem en la lista de recetas.
     * Contiene referencias a vistas y lógica para mostrar los datos.
     */
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
        /**
         * Asocia los datos de una receta a las vistas del ViewHolder.
         *
         * @param receta objeto Receta a mostrar.
         */
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
                autorR.setText("Cargando autor...");
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