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

    private List<String> pasos;
    final OnMoveListener moveListener;

    public InstruccionesAdapter(List<String> pasos, OnMoveListener moveListener) {
        this.pasos = pasos;
        this.moveListener = moveListener;
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


