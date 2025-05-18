package com.example.cookbooktfg;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FiltroIngredientes {
    public static void mostrar(Context context, List<IngredienteModelo> ingredientes,
                               FiltroListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Filtrar por ingredientes");

        // Agrupar ingredientes por tipo
        Map<String, List<IngredienteModelo>> ingredientesPorTipo = new HashMap<>();
        for (IngredienteModelo ingrediente : ingredientes) {
            if (!ingredientesPorTipo.containsKey(ingrediente.getTipo())) {
                ingredientesPorTipo.put(ingrediente.getTipo(), new ArrayList<>());
            }
            ingredientesPorTipo.get(ingrediente.getTipo()).add(ingrediente);
        }

        // Crear vistas para cada tipo
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        // Mapa para mantener referencia a los CheckBox de ingredientes
        Map<String, List<CheckBox>> checkBoxesPorTipo = new HashMap<>();

        // CheckBox para seleccionar todos los de un tipo
        for (String tipo : ingredientesPorTipo.keySet()) {
            // Crear CheckBox para el tipo
            CheckBox cbTipo = new CheckBox(context);
            cbTipo.setText(tipo);
            cbTipo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            cbTipo.setTypeface(null, Typeface.BOLD);

            // Layout para los ingredientes de este tipo
            LinearLayout layoutIngredientes = new LinearLayout(context);
            layoutIngredientes.setOrientation(LinearLayout.VERTICAL);
            layoutIngredientes.setPadding(32, 0, 0, 0);
            layoutIngredientes.setVisibility(View.GONE);

            // Lista para los CheckBox de este tipo
            List<CheckBox> checkBoxes = new ArrayList<>();

            // Añadir CheckBox para cada ingrediente
            for (IngredienteModelo ingrediente : ingredientesPorTipo.get(tipo)) {
                CheckBox cbIngrediente = new CheckBox(context);
                cbIngrediente.setText(ingrediente.getNombre());
                cbIngrediente.setTag(ingrediente.getId());
                layoutIngredientes.addView(cbIngrediente);
                checkBoxes.add(cbIngrediente);
            }

            checkBoxesPorTipo.put(tipo, checkBoxes);

            // Mostrar/ocultar ingredientes al hacer clic en el tipo
            cbTipo.setOnCheckedChangeListener((buttonView, isChecked) -> {
                layoutIngredientes.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            });

            layout.addView(cbTipo);
            layout.addView(layoutIngredientes);
        }

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(layout);
        builder.setView(scrollView);

        builder.setPositiveButton("Aplicar", (dialog, which) -> {
            List<String> ingredientesSeleccionados = new ArrayList<>();

            // Recorrer todos los CheckBox de ingredientes usando el mapa
            for (List<CheckBox> checkBoxes : checkBoxesPorTipo.values()) {
                for (CheckBox cb : checkBoxes) {
                    if (cb.isChecked()) {
                        ingredientesSeleccionados.add((String) cb.getTag());
                    }
                }
            }

            listener.onFiltroAplicado(ingredientesSeleccionados);
        });

        builder.setNegativeButton("Cancelar", null);
        builder.setNeutralButton("Limpiar", (dialog, which) -> {
            listener.onFiltroAplicado(new ArrayList<>());
        });

        builder.show();
    }

    public interface FiltroListener {
        void onFiltroAplicado(List<String> ingredientesSeleccionados);
    }
}