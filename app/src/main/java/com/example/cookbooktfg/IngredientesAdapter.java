package com.example.cookbooktfg;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

/**
 * Adaptador personalizado para mostrar una lista de ingredientes en un RecyclerView.
 * Cada ingrediente se representa como un Map<String, Object> con claves como "tipo", "nombre" y "cantidad".
 * Se utiliza, por ejemplo, para mostrar los ingredientes asociados a una receta.
 *
 *  Autor: Telma Teixeira
 *  Proyecto: CookbookTFG
 */
public class IngredientesAdapter extends RecyclerView.Adapter<IngredientesAdapter.ViewHolder> {

    private List<Map<String, Object>> ingredientes;

    /**
     * Constructor del adaptador.
     *
     * @param ingredientes Lista de mapas que contienen los datos de los ingredientes.
     */
    public IngredientesAdapter(List<Map<String, Object>> ingredientes) {
        this.ingredientes = ingredientes;
    }

    /**
     * Infla el layout personalizado para cada ítem del RecyclerView.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ingrediente, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Asigna los valores del ingrediente a los TextViews del layout.
     *
     * @param holder ViewHolder que representa la vista actual.
     * @param position Posición del ítem dentro de la lista.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> ingrediente = ingredientes.get(position);
        holder.tvTipoNombre.setText(
                ingrediente.get("tipo") + " " + ingrediente.get("nombre")
        );
        holder.tvCantidad.setText(ingrediente.get("cantidad").toString());
    }

    /**
     * Devuelve el número de ingredientes en la lista.
     */
    @Override
    public int getItemCount() {
        return ingredientes.size();
    }

    /**
     * ViewHolder que mantiene las referencias a las vistas de cada ítem de ingrediente.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTipoNombre, tvCantidad;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTipoNombre = itemView.findViewById(R.id.tvTipoNombre);
            tvCantidad = itemView.findViewById(R.id.tvCantidad);
        }
    }
}