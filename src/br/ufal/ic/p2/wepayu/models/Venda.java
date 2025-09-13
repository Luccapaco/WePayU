package br.ufal.ic.p2.wepayu.models;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Venda implements Serializable {
    private String data;
    private double valor;

    public Venda(String data, String valorStr) {
        if (data == null || data.trim().isEmpty()) {
            throw new IllegalArgumentException("Data invalida.");
        }
        validarData(data);

        if (valorStr == null || valorStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Valor deve ser positivo.");
        }

        try {
            this.valor = Double.parseDouble(valorStr.replace(",", "."));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor deve ser numerico.");
        }

        if (this.valor <= 0) {
            throw new IllegalArgumentException("Valor deve ser positivo.");
        }

        this.data = data;
    }

    private void validarData(String data) {
        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy");
        sdf.setLenient(false);
        try {
            sdf.parse(data);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Data invalida.");
        }
    }

    public String getData() {
        return data;
    }

    public double getValor() {
        return valor;
    }
}
