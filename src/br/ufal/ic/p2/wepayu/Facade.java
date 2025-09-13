package br.ufal.ic.p2.wepayu;

import br.ufal.ic.p2.wepayu.models.Database;
import br.ufal.ic.p2.wepayu.models.Empregado;


public class Facade {
    public void zerarSistema() {
        Database.zerarSistema();
    }

    public void encerrarSistema(){
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

    public String getAtributoEmpregado(String empId, String atributo) {
        Empregado e = Database.getEmpregado(empId);

        switch (atributo.toLowerCase()) {
            case "nome": return e.getNome();
            case "endereco": return e.getEndereco();
            case "tipo": return e.getTipo();
            case "salario": return String.format("%.2f", e.getSalario()).replace('.', ',');
            case "comissao":
                if (e.getComissao() == null) {
                    throw new IllegalArgumentException("Atributo nao existe.");
                }
                return String.format("%.2f", e.getComissao()).replace('.', ',');
            case "sindicalizado": return String.valueOf(e.isSindicalizado());
            default: throw new IllegalArgumentException("Atributo nao existe.");
        }
    }

    public String getEmpregadoPorNome(String nome, int indice) {
        return Database.getEmpregadoPorNome(nome, indice);
    }

    public void removerEmpregado(String empId){
        Database.removerEmpregado(empId);
    }

    public void lancaCartao(String empId, String data, String horas) {
        Database.lancaCartao(empId, data, horas);
    }

    public String getHorasNormaisTrabalhadas(String empId, String dataInicial, String dataFinal) {
        return Database.getHorasNormaisTrabalhadas(empId, dataInicial, dataFinal);
    }
    public String getHorasExtrasTrabalhadas(String empId, String dataInicial, String dataFinal) {
        return Database.getHorasExtrasTrabalhadas(empId, dataInicial, dataFinal);
    }

}
