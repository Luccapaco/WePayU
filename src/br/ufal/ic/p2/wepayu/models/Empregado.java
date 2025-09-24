package br.ufal.ic.p2.wepayu.models;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class Empregado implements Serializable {
    private static int contadorId = 1;
    private String id;
    private String nome;
    private String endereco;
    private String tipo;
    private double salario;
    private Double comissao;
    private boolean sindicalizado;
    private String idSindicato;
    private double taxaSindical;
    private String metodoPagamento;
    private String banco;
    private String agencia;
    private String contaCorrente;
    private List<CartaoPonto> cartoes = new ArrayList<>();
    private List<Venda> vendas = new ArrayList<>();
    private List<TaxaServico> taxas = new ArrayList<>();

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
        } catch (NumberFormatException e) {
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
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Comissao deve ser numerica.");
            }
            if (this.comissao < 0) {
                throw new IllegalArgumentException("Comissao deve ser nao-negativa.");
            }
        } else {
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
        this.idSindicato = null;
        this.taxaSindical = 0;
        this.metodoPagamento = "emMaos";
        this.banco = null;
        this.agencia = null;
        this.contaCorrente = null;
    }

    // ---- cartões ----
    public void adicionarCartao(CartaoPonto c) {
        cartoes.add(c);
    }

    public List<CartaoPonto> getCartoes() {
        return Collections.unmodifiableList(cartoes);
    }

    // ---- vendas ----
    public void adicionarVenda(Venda v) {
        vendas.add(v);
    }

    public List<Venda> getVendas() {
        return Collections.unmodifiableList(vendas);
    }

    // ---- taxas de servico ----
    public void adicionarTaxa(TaxaServico t) {
        taxas.add(t);
    }

    public List<TaxaServico> getTaxas() {
        return Collections.unmodifiableList(taxas);
    }

    public void setSindicalizado(boolean valor) { this.sindicalizado = valor; }
    public void setIdSindicato(String id) { this.idSindicato = id; }
    public void setTaxaSindical(double taxa) { this.taxaSindical = taxa; }
    public void setMetodoPagamento(String metodoPagamento) { this.metodoPagamento = metodoPagamento; }
    public void setBanco(String banco) { this.banco = banco; }
    public void setAgencia(String agencia) { this.agencia = agencia; }
    public void setContaCorrente(String contaCorrente) { this.contaCorrente = contaCorrente; }
    public void setNome(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome nao pode ser nulo.");
        }
        this.nome = nome;
    }

    public void setEndereco(String endereco) {
        if (endereco == null || endereco.trim().isEmpty()) {
            throw new IllegalArgumentException("Endereco nao pode ser nulo.");
        }
        this.endereco = endereco;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo.toLowerCase();
    }

    public void setSalario(double salario) {
        this.salario = salario;
    }

    public void setComissao(Double comissao) {
        this.comissao = comissao;
    }

    // ---- getters ----
    public String getId() { return id; }
    public String getNome() { return nome; }
    public String getEndereco() { return endereco; }
    public String getTipo() { return tipo; }
    public double getSalario() { return salario; }
    public Double getComissao() { return comissao; }
    public boolean isSindicalizado() { return sindicalizado; }
    public String getIdSindicato() { return idSindicato; }
    public double getTaxaSindical() { return taxaSindical; }
    public String getMetodoPagamento() { return metodoPagamento; }
    public String getBanco() { return banco; }
    public String getAgencia() { return agencia; }
    public String getContaCorrente() { return contaCorrente; }

    public static void resetContador() {
        contadorId = 1;
    }
}