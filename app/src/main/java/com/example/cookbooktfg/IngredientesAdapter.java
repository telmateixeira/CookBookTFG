package com.example.cookbooktfg;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class IngredientesAdapter extends RecyclerView.Adapter<IngredientesAdapter.ViewHolder> {

    private List<Map<String, Object>> ingredientes;

    public IngredientesAdapter(List<Map<String, Object>> ingredientes) {
        this.ingredientes = ingredientes;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ingrediente, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> ingrediente = ingredientes.get(position);
        holder.tvTipoNombre.setText(
                ingrediente.get("tipo") + " " + ingrediente.get("nombre")
        );
        holder.tvCantidad.setText(ingrediente.get("cantidad").toString());
    }

    @Override
    public int getItemCount() {
        return ingredientes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTipoNombre, tvCantidad;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTipoNombre = itemView.findViewById(R.id.tvTipoNombre);
            tvCantidad = itemView.findViewById(R.id.tvCantidad);
        }
    }
}