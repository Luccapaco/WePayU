package br.ufal.ic.p2.wepayu;

import br.ufal.ic.p2.wepayu.models.Database;
import br.ufal.ic.p2.wepayu.models.Empregado;


public class Facade {
    public void zerarSistema() {
        Database.zerarSistema();
    }

    public void encerrarSistema(){
        Database.encerrarSistema();
    }

    public String criarEmpregado(String nome, String endereco, String tipo, String salario) {
        Empregado e = new Empregado(nome, endereco, tipo, salario, null);
        Database.adicionarEmpregado(e);
        return e.getId();
    }

    public String criarEmpregado(String nome, String endereco, String tipo, String salario, String comissao) {
        Empregado e = new Empregado(nome, endereco, tipo, salario, comissao);
        Database.adicionarEmpregado(e);
        return e.getId();
    }

    public String getAtributoEmpregado(String emp, String atributo) {
        if (atributo == null) {
            throw new IllegalArgumentException("Atributo nao pode ser nulo.");
        }
        Empregado e = Database.getEmpregado(emp);

        switch (atributo.toLowerCase()) {
            case "nome": return e.getNome();
            case "endereco": return e.getEndereco();
            case "tipo": return e.getTipo();
            case "salario": return String.format("%.2f", e.getSalario()).replace('.', ',');
            case "comissao":
                if (!"comissionado".equals(e.getTipo())) {
                    throw new IllegalArgumentException("Empregado nao eh comissionado.");
                }
                return String.format("%.2f", e.getComissao()).replace('.', ',');
            case "sindicalizado": return String.valueOf(e.isSindicalizado());
            case "idsindicato":
                if (!e.isSindicalizado()) {
                    throw new IllegalArgumentException("Empregado nao eh sindicalizado.");
                }
                return e.getIdSindicato();
            case "taxasindical":
                if (!e.isSindicalizado()) {
                    throw new IllegalArgumentException("Empregado nao eh sindicalizado.");
                }
                return String.format("%.2f", e.getTaxaSindical()).replace('.', ',');
            case "metodopagamento":
                return e.getMetodoPagamento();
            case "banco":
                if (!"banco".equals(e.getMetodoPagamento())) {
                    throw new IllegalArgumentException("Empregado nao recebe em banco.");
                }
                return e.getBanco();
            case "agencia":
                if (!"banco".equals(e.getMetodoPagamento())) {
                    throw new IllegalArgumentException("Empregado nao recebe em banco.");
                }
                return e.getAgencia();
            case "contacorrente":
                if (!"banco".equals(e.getMetodoPagamento())) {
                    throw new IllegalArgumentException("Empregado nao recebe em banco.");
                }
                return e.getContaCorrente();
            default: throw new IllegalArgumentException("Atributo nao existe.");
        }
    }

    public String getEmpregadoPorNome(String nome, int indice) {
        return Database.getEmpregadoPorNome(nome, indice);
    }

    public void removerEmpregado(String emp){
        Database.removerEmpregado(emp);
    }

    public void lancaCartao(String emp, String data, String horas) {
        Database.lancaCartao(emp, data, horas);
    }

    public void lancaVenda(String emp, String data, String valor) {
        Database.lancaVenda(emp, data, valor);
    }

    public void lancaTaxaServico(String membroId, String data, String valor) {
        Database.lancaTaxaServico(membroId, data, valor);
    }

    public String getHorasNormaisTrabalhadas(String emp, String dataInicial, String dataFinal) {
        return Database.getHorasNormaisTrabalhadas(emp, dataInicial, dataFinal);
    }
    public String getHorasExtrasTrabalhadas(String emp, String dataInicial, String dataFinal) {
        return Database.getHorasExtrasTrabalhadas(emp, dataInicial, dataFinal);
    }

    public String getVendasRealizadas(String emp, String dataInicial, String dataFinal) {
        return Database.getVendasRealizadas(emp, dataInicial, dataFinal);
    }

    public String getTaxasServico(String emp, String dataInicial, String dataFinal) {
        return Database.getTaxasServico(emp, dataInicial, dataFinal);
    }

    public void alteraEmpregado(String emp, String atributo, String valor) {
        Database.alteraEmpregado(emp, atributo, valor);
    }

    public void alteraEmpregado(String emp, String atributo, String valor, String valor2) {
        Database.alteraEmpregado(emp, atributo, valor, valor2);
    }

    public void alteraEmpregado(String emp, String atributo, String valor, String idSindicato, String taxaSindical) {
        Database.alteraEmpregado(emp, atributo, valor, idSindicato, taxaSindical);
    }

    public void alteraEmpregado(String emp, String atributo, String valor1, String banco, String agencia, String contaCorrente) {
        Database.alteraEmpregado(emp, atributo, valor1, banco, agencia, contaCorrente);
    }

}
