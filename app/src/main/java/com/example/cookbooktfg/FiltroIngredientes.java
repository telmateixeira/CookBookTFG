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
                               List<String> seleccionadosActuales,
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

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(layout);
        builder.setView(scrollView);

        // Crear vistas para cada tipo
        for (String tipo : ingredientesPorTipo.keySet()) {
            // CheckBox para el tipo (header)
            CheckBox cbTipo = new CheckBox(context);
            cbTipo.setText(tipo);
            cbTipo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            cbTipo.setTypeface(null, Typeface.BOLD);
            layout.addView(cbTipo);

            // Layout para ingredientes de este tipo
            LinearLayout layoutIngredientes = new LinearLayout(context);
            layoutIngredientes.setOrientation(LinearLayout.VERTICAL);
            layoutIngredientes.setPadding(32, 0, 0, 0);
            layoutIngredientes.setVisibility(View.GONE);
            layout.addView(layoutIngredientes);

            // Contador para ingredientes seleccionados de este tipo
            int seleccionadosEnTipo = 0;

            // Añadir CheckBox para cada ingrediente
            for (IngredienteModelo ingrediente : ingredientesPorTipo.get(tipo)) {
                CheckBox cbIngrediente = new CheckBox(context);
                cbIngrediente.setText(ingrediente.getNombre());
                // Usar el ID del ingrediente como tag
                cbIngrediente.setTag(ingrediente.getId());

                // Verificar si este ingrediente está en los seleccionados
                if (seleccionadosActuales.contains(ingrediente.getId())) {
                    cbIngrediente.setChecked(true);
                    seleccionadosEnTipo++;
                }

                layoutIngredientes.addView(cbIngrediente);

                // Listener para actualizar el checkbox del tipo
                cbIngrediente.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    actualizarEstadoTipo(cbTipo, layoutIngredientes);
                });
            }

            // Configurar el estado inicial del checkbox del tipo
            if (seleccionadosEnTipo > 0) {
                cbTipo.setChecked(true);
                layoutIngredientes.setVisibility(View.VISIBLE);

                // Si todos los ingredientes están seleccionados, marcar el tipo como checked
                if (seleccionadosEnTipo == ingredientesPorTipo.get(tipo).size()) {
                    cbTipo.setChecked(true);
                } else {
                    cbTipo.setChecked(false);
                }
            }

            // Mostrar/ocultar ingredientes al hacer clic en el tipo
            cbTipo.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    layoutIngredientes.setVisibility(View.VISIBLE);
                } else {
                    layoutIngredientes.setVisibility(View.GONE);
                    // Desmarcar todos los ingredientes de este tipo
                    for (int i = 0; i < layoutIngredientes.getChildCount(); i++) {
                        View child = layoutIngredientes.getChildAt(i);
                        if (child instanceof CheckBox) {
                            ((CheckBox) child).setChecked(false);
                        }
                    }
                }
            });
        }

        builder.setPositiveButton("Aplicar", (dialog, which) -> {
            List<String> ingredientesSeleccionados = new ArrayList<>();

            // Recorrer todos los CheckBox de ingredientes
            for (int i = 0; i < layout.getChildCount(); i++) {
                View child = layout.getChildAt(i);
                if (child instanceof LinearLayout) {
                    LinearLayout ingredientesLayout = (LinearLayout) child;
                    for (int j = 0; j < ingredientesLayout.getChildCount(); j++) {
                        View ingredienteView = ingredientesLayout.getChildAt(j);
                        if (ingredienteView instanceof CheckBox) {
                            CheckBox cb = (CheckBox) ingredienteView;
                            if (cb.isChecked()) {
                                ingredientesSeleccionados.add((String) cb.getTag());
                            }
                        }
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

    private static void actualizarEstadoTipo(CheckBox cbTipo, LinearLayout layoutIngredientes) {
        int total = 0;
        int seleccionados = 0;

        for (int i = 0; i < layoutIngredientes.getChildCount(); i++) {
            View child = layoutIngredientes.getChildAt(i);
            if (child instanceof CheckBox) {
                total++;
                if (((CheckBox) child).isChecked()) {
                    seleccionados++;
                }
            }
        }

        if (seleccionados == 0) {
            cbTipo.setChecked(false);
        } else if (seleccionados == total) {
            cbTipo.setChecked(true);
        } else {
            cbTipo.setChecked(false);
        }
    }

    public interface FiltroListener {
        void onFiltroAplicado(List<String> ingredientesSeleccionados);
    }
}