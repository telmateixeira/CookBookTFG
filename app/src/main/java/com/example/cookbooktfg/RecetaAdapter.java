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

/**
 * Adapter personalizado para mostrar una lista de recetas en un RecyclerView.
 * Permite mostrar información de cada receta (nombre, imagen, autor),
 * gestionar favoritos y aplicar filtros por nombre o ingredientes.
 *
 *  Autor: Telma Teixeira
 *  Proyecto: CookbookTFG
 */
public class RecetaAdapter extends RecyclerView.Adapter<RecetaAdapter.RecetaViewHolder> {

    private List<Receta> recetaList; // Lista actual de recetas mostradas
    private List<Receta> recetaOriginal; // Lista completa de recetas originales (sin filtrar)
    private Context context;
    private FirebaseFirestore db;
    private List<String> ingredientesFiltro = new ArrayList<>();
    private boolean mostrarFavs = true;

    /**
     * Constructor del adaptador.
     *
     * @param recetaList   Lista inicial de recetas.
     * @param context      Contexto de la actividad o fragmento.
     * @param mostrarFavs  Si se deben mostrar o no los botones de favoritos.
     */
    public RecetaAdapter(List<Receta> recetaList, Context context, boolean mostrarFavs) {
        this.recetaList = new ArrayList<>(recetaList);
        this.recetaOriginal = new ArrayList<>(recetaList);
        this.context = context;
        this.mostrarFavs = mostrarFavs;
    }
    /**
     * Crea la vista XML de cada item de la lista
     */
    @NonNull
    @Override
    public RecetaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_receta, parent, false);
        return new RecetaViewHolder(vista);
    }

    /**
     * Metodo que permite cargar todos los elementos de la receta y visualizarlos
     */
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

        // Configurar el botón de favorito
        holder.botonFavorito.setImageResource(
                receta.isFavorito() ? R.drawable.favoritos_icono_naranja : R.drawable.favoritos_icono
        );

        holder.botonFavorito.setOnClickListener(v -> {
            boolean nuevoEstado = !receta.isFavorito();
            receta.setFavorito(nuevoEstado);
            notifyItemChanged(position);

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                DocumentReference recetaRef = db.collection("recetas").document(receta.getId());

                if (nuevoEstado) {
                    // Añadir a favoritos (array en documento principal)
                    db.collection("usuarios")
                            .document(user.getUid())
                            .update("favoritos", FieldValue.arrayUnion(recetaRef))
                            .addOnSuccessListener(aVoid -> Log.d("Favoritos", "Añadido a favoritos"))
                            .addOnFailureListener(e -> {
                                receta.setFavorito(false);
                                notifyItemChanged(position);
                            });
                } else {
                    // Eliminar de favoritos
                    db.collection("usuarios")
                            .document(user.getUid())
                            .update("favoritos", FieldValue.arrayRemove(recetaRef))
                            .addOnSuccessListener(aVoid -> {
                                Log.d("Favoritos", "Eliminado de favoritos");
                                if (favoritoCambiadoListener != null) {
                                    favoritoCambiadoListener.onFavoritoQuitado(receta, position);
                                }
                            })
                            .addOnFailureListener(e -> {
                                receta.setFavorito(true);
                                notifyItemChanged(position);
                            });
                }
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

    /**
     * Devuelve la cantidad de recetas que se deben de mostrar en el RecyclerView
     */
    @Override
    public int getItemCount() {
        Log.d("RecetaAdapter", "Total recetas a mostrar: " + recetaList.size());
        return recetaList.size();
    }
    /**
     * Filtra las recetas por nombre y por los ingredientes seleccionados.
     *
     * @param texto Texto del nombre a buscar.
     */
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
    /**
     * Filtra las recetas según los ingredientes seleccionados por el usuario.
     *
     * @param ingredientesSeleccionados Lista de IDs de ingredientes seleccionados.
     */
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
    /**
     * Verifica si una receta contiene al menos uno de los ingredientes buscados.
     *
     * @param receta               Receta a comprobar.
     * @param ingredientesBuscados Lista de IDs de ingredientes.
     * @return true si contiene alguno, false si no.
     */
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
    /**
     * Actualiza completamente la lista de recetas.
     *
     * @param nuevasRecetas Nueva lista de recetas a mostrar.
     */
    public void actualizarRecetas(List<Receta> nuevasRecetas) {
        recetaList.clear();
        recetaList.addAll(nuevasRecetas);
        recetaOriginal.clear();
        recetaOriginal.addAll(nuevasRecetas);
        notifyDataSetChanged();
    }
    /**
     * Elimina una receta en una posición específica.
     *
     * @param posicion Índice de la receta a eliminar.
     */
    public void eliminarRecetaEnPosicion(int posicion) {
        if (posicion >= 0 && posicion < recetaList.size()) {
            recetaList.remove(posicion);
            recetaOriginal.remove(posicion);
            notifyItemRemoved(posicion);
        }
    }

    /** Interface para notificar cuando se elimina un favorito. */
    public interface OnFavoritoCambiadoListener {
        void onFavoritoQuitado(Receta receta, int posicion);
    }

    private OnFavoritoCambiadoListener favoritoCambiadoListener;

    public void setOnFavoritoCambiadoListener(OnFavoritoCambiadoListener listener) {
        this.favoritoCambiadoListener = listener;
    }

    private OnRecetaClickListener clickListener;

    /** Interface para manejar clics sobre una receta. */
    public void setOnRecetaClickListener(OnRecetaClickListener listener) {
        this.clickListener = listener;
    }

    public interface OnRecetaClickListener {
        void onRecetaClick(String recetaId);
    }

    /**
     * ViewHolder para los elementos del RecyclerView.
     */
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


