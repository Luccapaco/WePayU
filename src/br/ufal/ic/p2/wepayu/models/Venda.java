package br.ufal.ic.p2.wepayu.models;

public class Venda {
    private String data;
    private double valor;

    public Venda(String data, String valorStr) {
        if (data == null || data.trim().isEmpty()) {
            throw new IllegalArgumentException("Data invalida.");
        }
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

    public String getData() {
        return data;
    }

    public double getValor() {
        return valor;
    }
}
