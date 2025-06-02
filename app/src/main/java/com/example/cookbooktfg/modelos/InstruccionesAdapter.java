package com.example.cookbooktfg.modelos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookbooktfg.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador personalizado para mostrar y gestionar una lista de instrucciones o pasos
 * dentro de un RecyclerView en la aplicación de recetas.
 *
 * Autor: Telma Teixeira
 * Proyecto: CookbookTFG
 */
public class InstruccionesAdapter extends RecyclerView.Adapter<InstruccionesAdapter.ViewHolder> {

    /**
     * Interfaz para detectar cuando se reordena un elemento.
     */
    public interface OnMoveListener {
        void onItemMove(int fromPosition, int toPosition);
    }

    /**
     * Interfaz para detectar cuando se quiere editar un paso.
     */
    public interface OnEditListener {
        void onEditClick(int position);
    }

    private List<String> pasos;
    public final OnMoveListener moveListener;
    private final OnEditListener editListener;

    /**
     * Constructor del adaptador.
     *
     * @param pasos Lista de pasos a mostrar.
     * @param moveListener Listener para eventos de movimiento (reordenar).
     * @param editListener Listener para eventos de edición (clic).
     */
    public InstruccionesAdapter(List<String> pasos, OnMoveListener moveListener, OnEditListener editListener) {
        this.pasos = pasos != null ? pasos : new ArrayList<>();
        this.moveListener = moveListener;
        this.editListener = editListener;
    }

    /**
     * Crea una nueva vista para cada ítem del RecyclerView.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_paso, parent, false);
        return new ViewHolder(vista);
    }
    /**
     * Enlaza los datos (paso de la receta) a la vista.
     *
     * @param holder ViewHolder que contiene la vista.
     * @param position Posición actual en la lista.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String paso = pasos.get(position);
        holder.tvNumero.setText(String.valueOf(position + 1));
        holder.tvPaso.setText(paso);

        holder.itemView.setOnClickListener(v -> {
            if (editListener != null && holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                editListener.onEditClick(holder.getAdapterPosition());
            }
        });
    }
    /**
     * Devuelve el número total de elementos en la lista.
     */
    @Override
    public int getItemCount() {
        return pasos.size();
    }

    public void actualizarPasos(List<String> nuevosPasos) {
        this.pasos = nuevosPasos != null ? nuevosPasos : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * ViewHolder que representa cada ítem (paso) del RecyclerView.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumero;
        TextView tvPaso;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumero = itemView.findViewById(R.id.tvNumero);
            tvPaso = itemView.findViewById(R.id.tvPaso);
        }
    }
}
