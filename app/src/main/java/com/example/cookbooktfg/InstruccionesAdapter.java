package com.example.cookbooktfg;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
public class InstruccionesAdapter extends RecyclerView.Adapter<InstruccionesAdapter.ViewHolder> {

    public interface OnMoveListener {
        void onItemMove(int fromPosition, int toPosition);
    }

    public interface OnEditListener {
        void onEditClick(int position);
    }

    private List<String> pasos;
    final OnMoveListener moveListener;
    private final OnEditListener editListener;

    public InstruccionesAdapter(List<String> pasos, OnMoveListener moveListener, OnEditListener editListener) {
        this.pasos = pasos;
        this.moveListener = moveListener;
        this.editListener = editListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.text.setText((position + 1) + ". " + pasos.get(position));
        holder.itemView.setOnClickListener(v -> {
            if (editListener != null) {
                editListener.onEditClick(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return pasos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(android.R.id.text1);
        }
    }
}
