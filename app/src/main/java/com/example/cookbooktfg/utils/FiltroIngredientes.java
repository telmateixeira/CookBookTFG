package com.example.cookbooktfg.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.example.cookbooktfg.modelos.IngredienteModelo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clase para mostrar un cuadro de diálogo con un filtro por ingredientes agrupados por tipo.
 * Permite al usuario seleccionar ingredientes específicos y aplicar un filtro.
 *
 *  Autor: Telma Teixeira
 *  Proyecto: CookbookTFG
 */
public class FiltroIngredientes {
    /**
     * Muestra un diálogo de filtro por ingredientes agrupados por tipo.
     *
     * @param context               Contexto de la aplicación.
     * @param ingredientes          Lista de ingredientes disponibles para filtrar.
     * @param seleccionadosActuales Lista de IDs de ingredientes que ya estaban seleccionados.
     * @param listener              Interfaz para recibir los ingredientes seleccionados al aplicar el filtro.
     */
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

        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(16, 16, 16, 16);

        // Mapa para mantener referencia a los layouts de ingredientes
        Map<String, LinearLayout> tipoLayoutMap = new HashMap<>();

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(mainLayout);
        builder.setView(scrollView);

        // Crear vistas para cada tipo
        for (String tipo : ingredientesPorTipo.keySet()) {
            // CheckBox para el tipo
            CheckBox cbTipo = new CheckBox(context);
            cbTipo.setText(tipo);
            cbTipo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            cbTipo.setTypeface(null, Typeface.BOLD);
            mainLayout.addView(cbTipo);

            // Layout para ingredientes de este tipo
            LinearLayout layoutIngredientes = new LinearLayout(context);
            layoutIngredientes.setOrientation(LinearLayout.VERTICAL);
            layoutIngredientes.setPadding(32, 0, 0, 0);
            mainLayout.addView(layoutIngredientes);

            // Guardar referencia al layout
            tipoLayoutMap.put(tipo, layoutIngredientes);

            // Contador para ingredientes seleccionados de este tipo
            int seleccionadosEnTipo = 0;

            // Añadir CheckBox para cada ingrediente
            for (IngredienteModelo ingrediente : ingredientesPorTipo.get(tipo)) {
                CheckBox cbIngrediente = new CheckBox(context);
                cbIngrediente.setText(ingrediente.getNombre());
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

            // Configurar visibilidad inicial
            layoutIngredientes.setVisibility(View.GONE);  // Siempre ocultos al principio

// Si hay ingredientes seleccionados, marcar el tipo y mostrar sus hijos
            if (seleccionadosEnTipo > 0) {
                cbTipo.setChecked(true);
                layoutIngredientes.setVisibility(View.VISIBLE);
            }

            cbTipo.setOnClickListener(v -> {
                boolean isChecked = cbTipo.isChecked();
                layoutIngredientes.setVisibility(isChecked ? View.VISIBLE : View.GONE);

                if (!isChecked) {
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

            // Recorrer todos los layouts de ingredientes
            for (LinearLayout layoutIngredientes : tipoLayoutMap.values()) {
                for (int i = 0; i < layoutIngredientes.getChildCount(); i++) {
                    View child = layoutIngredientes.getChildAt(i);
                    if (child instanceof CheckBox) {
                        CheckBox cb = (CheckBox) child;
                        if (cb.isChecked()) {
                            ingredientesSeleccionados.add((String) cb.getTag());
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

    /**
     * Actualiza el estado del CheckBox de tipo en función de cuántos ingredientes están seleccionados.
     *
     * @param cbTipo             CheckBox del tipo de ingrediente.
     * @param layoutIngredientes Layout que contiene los ingredientes de ese tipo.
     */
    private static void actualizarEstadoTipo(CheckBox cbTipo, LinearLayout layoutIngredientes) {
        int seleccionados = 0;

        for (int i = 0; i < layoutIngredientes.getChildCount(); i++) {
            View child = layoutIngredientes.getChildAt(i);
            if (child instanceof CheckBox && ((CheckBox) child).isChecked()) {
                seleccionados++;
            }
        }

        cbTipo.setChecked(seleccionados > 0);
    }


    /**
     * Interfaz para manejar el resultado del filtro.
     */
    public interface FiltroListener {
        void onFiltroAplicado(List<String> ingredientesSeleccionados);
    }
}