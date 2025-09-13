package br.ufal.ic.p2.wepayu.models;

import java.io.Serializable;

public class CartaoPonto implements Serializable {
    private String data;
    private double horas;

    public CartaoPonto(String data, String horasStr) {
        if (data == null || data.trim().isEmpty()) {
            throw new IllegalArgumentException("Data invalida.");
        }
        if (horasStr == null || horasStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Horas devem ser positivas.");
        }

        try {
            this.horas = Double.parseDouble(horasStr.replace(",", "."));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Horas devem ser numericas.");
        }

        // professor exige > 0 (não pode zero)
        if (this.horas <= 0) {
            throw new IllegalArgumentException("Horas devem ser positivas.");
        }

        this.data = data;
    }

    public String getData() {
        return data;
    }

    public double getHoras() {
        return horas;
    }
}