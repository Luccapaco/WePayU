package br.ufal.ic.p2.wepayu.models;

import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoExisteException;
import java.util.List;
import java.util.ArrayList;

public class Empregado {
    private static int contadorId = 1;
    private String id;
    private String nome;
    private String endereco;
    private String tipo;
    private double salario;
    private Double comissao;
    private boolean sindicalizado;
    private List<CartaoPonto> cartoes = new ArrayList<>();


    public Empregado(String nome, String endereco, String tipo, String salarioStr, String comissaoStr) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome nao pode ser nulo.");
        }
        if (endereco == null || endereco.trim().isEmpty()) {
            throw new IllegalArgumentException("Endereco nao pode ser nulo.");
        }
        if (tipo == null ||
                !(tipo.equalsIgnoreCase("horista") ||
                        tipo.equalsIgnoreCase("assalariado") ||
                        tipo.equalsIgnoreCase("comissionado"))) {
            throw new IllegalArgumentException("Tipo invalido.");
        }

        if (salarioStr == null || salarioStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Salario nao pode ser nulo.");
        }
        try {
            this.salario = Double.parseDouble(salarioStr.replace(",", "."));
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("Salario deve ser numerico.");
        }
        if (this.salario < 0) {
            throw new IllegalArgumentException("Salario deve ser nao-negativo.");
        }
        if (tipo.equalsIgnoreCase("comissionado")) {
            if (comissaoStr == null) {
                throw new IllegalArgumentException("Tipo nao aplicavel.");
            }
            if (comissaoStr.trim().isEmpty()) {
                throw new IllegalArgumentException("Comissao nao pode ser nula.");
            }
            try {
                this.comissao = Double.parseDouble(comissaoStr.replace(",", "."));
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("Comissao deve ser numerica.");
            }
            if (this.comissao < 0) {
                throw new IllegalArgumentException("Comissao deve ser nao-negativa.");
            }
        }
        else {
            if (comissaoStr != null) {
                throw new IllegalArgumentException("Tipo nao aplicavel.");
            }
            this.comissao = null;
        }


        this.id = "EMP" + contadorId++;
        this.nome = nome;
        this.endereco = endereco;
        this.tipo = tipo.toLowerCase();
        this.sindicalizado = false;
    }

    public void adicionarCartao(CartaoPonto c) {
        cartoes.add(c);
    }

    public List<CartaoPonto> getCartoes() {
        return cartoes;
    }

    public String getId() { return id; }
    public String getNome() {
        return nome;
    }
    public String getEndereco() {
        return endereco;
    }
    public String getTipo() {
        return tipo;
    }
    public double getSalario() {
        return salario;
    }
    public Double getComissao() { return comissao; }
    public boolean isSindicalizado() { return sindicalizado; }
}
