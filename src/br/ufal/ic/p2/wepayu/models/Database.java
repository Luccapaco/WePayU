package br.ufal.ic.p2.wepayu.models;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Database {
    private static final String ARQUIVO = "empregados.ser";
    private static Map<String, Empregado> empregados = new HashMap<>();

    static {
        carregar();
    }

    public static void adicionarEmpregado(Empregado empregado) {
        empregados.put(empregado.getId(), empregado);
    }

    public static Empregado getEmpregado(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Identificacao do empregado nao pode ser nula.");
        }
        Empregado e = empregados.get(id);
        if (e == null) {
            throw new IllegalArgumentException("Empregado nao existe.");
        }
        return e;
    }

    public static void removerEmpregado(String id){
        if(id == null || id.trim().isEmpty()){
            throw new IllegalArgumentException("Identificacao do empregado nao pode ser nula.");
        }
        if (!empregados.containsKey(id)) {
            throw new IllegalArgumentException("Empregado nao existe.");
        }
        empregados.remove(id);
    }

    public static String getEmpregadoPorNome(String nome, int indice) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome nao pode ser nulo.");
        }

        List<Empregado> encontrados = new ArrayList<>();
        for (Empregado e : empregados.values()) {
            if (e.getNome().equalsIgnoreCase(nome)) {
                encontrados.add(e);
            }
        }

        if (encontrados.isEmpty()) {
            throw new IllegalArgumentException("Nao ha empregado com esse nome.");
        }

        if (indice <= 0 || indice > encontrados.size()) {
            throw new IllegalArgumentException("Indice invalido.");
        }

        return encontrados.get(indice - 1).getId();
    }

    public static void alteraEmpregado(String empId, String atributo, String valor) {
        Empregado e = getEmpregado(empId);

        if (atributo == null || atributo.trim().isEmpty()) {
            throw new IllegalArgumentException("Atributo nao pode ser nulo.");
        }

        switch (atributo.toLowerCase()) {
            case "nome":
                e.setNome(valor);
                break;
            case "endereco":
                e.setEndereco(valor);
                break;
            case "tipo":
                alterarTipo(e, valor, null);
                break;
            case "salario":
                e.setSalario(parseSalario(valor));
                break;
            case "comissao":
                if (!"comissionado".equals(e.getTipo())) {
                    throw new IllegalArgumentException("Empregado nao eh comissionado.");
                }
                e.setComissao(parseComissao(valor));
                break;
            case "metodopagamento":
                alterarMetodoPagamento(e, valor, null, null, null);
                break;
            case "sindicalizado":
                alterarSindicalizado(e, valor, null, null);
                break;
            default:
                throw new IllegalArgumentException("Atributo nao existe.");
        }
    }

    public static void alteraEmpregado(String empId, String atributo, String valor,
                                       String idSindicato, String taxaSindical) {
        Empregado e = getEmpregado(empId);

        if (atributo == null || atributo.trim().isEmpty()) {
            throw new IllegalArgumentException("Atributo nao pode ser nulo.");
        }

        if (!"sindicalizado".equalsIgnoreCase(atributo)) {
            throw new IllegalArgumentException("Atributo nao existe.");
        }

        alterarSindicalizado(e, valor, idSindicato, taxaSindical);
    }

    public static void alteraEmpregado(String empId, String atributo, String valor, String valorAuxiliar) {
        Empregado e = getEmpregado(empId);

        if (atributo == null || atributo.trim().isEmpty()) {
            throw new IllegalArgumentException("Atributo nao pode ser nulo.");
        }

        if (!"tipo".equalsIgnoreCase(atributo)) {
            throw new IllegalArgumentException("Atributo nao existe.");
        }

        alterarTipo(e, valor, valorAuxiliar);
    }

    public static void alteraEmpregado(String empId, String atributo, String valor1,
                                       String banco, String agencia, String contaCorrente) {
        Empregado e = getEmpregado(empId);

        if (atributo == null || atributo.trim().isEmpty()) {
            throw new IllegalArgumentException("Atributo nao pode ser nulo.");
        }

        if (!"metodopagamento".equalsIgnoreCase(atributo)) {
            throw new IllegalArgumentException("Atributo nao existe.");
        }

        alterarMetodoPagamento(e, valor1, banco, agencia, contaCorrente);
    }

    private static void alterarSindicalizado(Empregado e, String valor, String idSindicato, String taxaSindical) {
        if (valor == null || (!valor.equalsIgnoreCase("true") && !valor.equalsIgnoreCase("false"))) {
            throw new IllegalArgumentException("Valor deve ser true ou false.");
        }

        if (valor.equalsIgnoreCase("false")) {
            e.setSindicalizado(false);
            e.setIdSindicato(null);
            e.setTaxaSindical(0);
            return;
        }

        if (idSindicato == null || idSindicato.trim().isEmpty()) {
            throw new IllegalArgumentException("Identificacao do sindicato nao pode ser nula.");
        }
        if (taxaSindical == null || taxaSindical.trim().isEmpty()) {
            throw new IllegalArgumentException("Taxa sindical nao pode ser nula.");
        }

        double taxa;
        try {
            taxa = Double.parseDouble(taxaSindical.replace(",", "."));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Taxa sindical deve ser numerica.");
        }
        if (taxa < 0) {
            throw new IllegalArgumentException("Taxa sindical deve ser nao-negativa.");
        }

        for (Empregado outro : empregados.values()) {
            if (outro != e && outro.isSindicalizado() && idSindicato.equals(outro.getIdSindicato())) {
                throw new IllegalArgumentException("Ha outro empregado com esta identificacao de sindicato");
            }
        }

        e.setSindicalizado(true);
        e.setIdSindicato(idSindicato);
        e.setTaxaSindical(taxa);
    }

    private static void alterarTipo(Empregado e, String tipo, String valorAuxiliar) {
        if (tipo == null || tipo.trim().isEmpty()) {
            throw new IllegalArgumentException("Tipo invalido.");
        }

        String novoTipo = tipo.toLowerCase();
        switch (novoTipo) {
            case "horista":
                double salarioHorista = parseSalario(valorAuxiliar);
                e.setTipo("horista");
                e.setSalario(salarioHorista);
                e.setComissao(null);
                break;
            case "assalariado":
                e.setTipo("assalariado");
                e.setComissao(null);
                break;
            case "comissionado":
                double novaComissao = parseComissao(valorAuxiliar);
                e.setTipo("comissionado");
                e.setComissao(novaComissao);
                break;
            default:
                throw new IllegalArgumentException("Tipo invalido.");
        }
    }

    private static void alterarMetodoPagamento(Empregado e, String metodo, String banco, String agencia, String contaCorrente) {
        if (metodo == null || metodo.trim().isEmpty()) {
            throw new IllegalArgumentException("Metodo de pagamento invalido.");
        }

        if (metodo.equalsIgnoreCase("emmaos")) {
            e.setMetodoPagamento("emMaos");
            e.setBanco(null);
            e.setAgencia(null);
            e.setContaCorrente(null);
        } else if (metodo.equalsIgnoreCase("correios")) {
            e.setMetodoPagamento("correios");
            e.setBanco(null);
            e.setAgencia(null);
            e.setContaCorrente(null);
        } else if (metodo.equalsIgnoreCase("banco")) {
            if (banco == null || banco.trim().isEmpty()) {
                throw new IllegalArgumentException("Banco nao pode ser nulo.");
            }
            if (agencia == null || agencia.trim().isEmpty()) {
                throw new IllegalArgumentException("Agencia nao pode ser nulo.");
            }
            if (contaCorrente == null || contaCorrente.trim().isEmpty()) {
                throw new IllegalArgumentException("Conta corrente nao pode ser nulo.");
            }

            e.setMetodoPagamento("banco");
            e.setBanco(banco);
            e.setAgencia(agencia);
            e.setContaCorrente(contaCorrente);
        } else {
            throw new IllegalArgumentException("Metodo de pagamento invalido.");
        }
    }

    private static double parseSalario(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("Salario nao pode ser nulo.");
        }

        double salario;
        try {
            salario = Double.parseDouble(valor.replace(",", "."));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Salario deve ser numerico.");
        }

        if (salario < 0) {
            throw new IllegalArgumentException("Salario deve ser nao-negativo.");
        }
        return salario;
    }

    private static double parseComissao(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("Comissao nao pode ser nula.");
        }

        double comissao;
        try {
            comissao = Double.parseDouble(valor.replace(",", "."));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Comissao deve ser numerica.");
        }

        if (comissao < 0) {
            throw new IllegalArgumentException("Comissao deve ser nao-negativa.");
        }
        return comissao;
    }

    public static void lancaCartao(String empId, String data, String horas) {
        if (empId == null || empId.trim().isEmpty()) {
            throw new IllegalArgumentException("Identificacao do empregado nao pode ser nula.");
        }

        Empregado e = getEmpregado(empId);

        if (!e.getTipo().equals("horista")) {
            throw new IllegalArgumentException("Empregado nao eh horista.");
        }

        validarData(data, false);

        CartaoPonto cartao = new CartaoPonto(data, horas);
        e.adicionarCartao(cartao);
    }

    public static void lancaVenda(String empId, String data, String valor) {
        if (empId == null || empId.trim().isEmpty()) {
            throw new IllegalArgumentException("Identificacao do empregado nao pode ser nula.");
        }

        Empregado e = getEmpregado(empId);

        if (!e.getTipo().equals("comissionado")) {
            throw new IllegalArgumentException("Empregado nao eh comissionado.");
        }

        validarData(data, false);

        Venda v = new Venda(data, valor);
        e.adicionarVenda(v);
    }


    public static void lancaTaxaServico(String membroId, String data, String valor) {
        if (membroId == null || membroId.trim().isEmpty()) {
            throw new IllegalArgumentException("Identificacao do membro nao pode ser nula.");
        }

        Empregado e = null;
        for (Empregado emp : empregados.values()) {
            if (emp.isSindicalizado() && membroId.equals(emp.getIdSindicato())) {
                e = emp;
                break;
            }
        }

        if (e == null) {
            throw new IllegalArgumentException("Membro nao existe.");
        }

        validarData(data, false);

        TaxaServico t = new TaxaServico(data, valor);
        e.adicionarTaxa(t);
    }

    public static String getHorasNormaisTrabalhadas(String empId, String dataInicial, String dataFinal) {
        return formatarNumero(calcularHoras(empId, dataInicial, dataFinal, false));
    }

    public static String getHorasExtrasTrabalhadas(String empId, String dataInicial, String dataFinal) {
        return formatarNumero(calcularHoras(empId, dataInicial, dataFinal, true));
    }


    public static String getTaxasServico(String empId, String dataInicial, String dataFinal) {
        Empregado e = getEmpregado(empId);

        if (!e.isSindicalizado()) {
            throw new IllegalArgumentException("Empregado nao eh sindicalizado.");
        }

        Date inicio = validarData(dataInicial, true);
        Date fim = validarData(dataFinal, false);

        if (inicio.after(fim)) {
            throw new IllegalArgumentException("Data inicial nao pode ser posterior aa data final.");
        }

        double total = 0;
        for (TaxaServico t : e.getTaxas()) {
            Date d = validarData(t.getData(), false);
            if (!d.before(inicio) && d.before(fim)) {
                total += t.getValor();
            }
        }

        return formatarValor(total);
    }

    public static String getVendasRealizadas(String empId, String dataInicial, String dataFinal) {
        Empregado e = getEmpregado(empId);

        if (!e.getTipo().equals("comissionado")) {
            throw new IllegalArgumentException("Empregado nao eh comissionado.");
        }

        Date inicio = validarData(dataInicial, true);
        Date fim = validarData(dataFinal, false);

        if (inicio.after(fim)) {
            throw new IllegalArgumentException("Data inicial nao pode ser posterior aa data final.");
        }

        double total = 0;
        for (Venda v : e.getVendas()) {
            Date data = validarData(v.getData(), false);
            if (!data.before(inicio) && data.before(fim)) {
                total += v.getValor();
            }
        }

        return formatarValor(total);
    }

    private static double calcularHoras(String empId, String dataInicial, String dataFinal, boolean extras) {
        Empregado e = getEmpregado(empId);

        if (!e.getTipo().equals("horista")) {
            throw new IllegalArgumentException("Empregado nao eh horista.");
        }

        Date inicio = validarData(dataInicial, true);
        Date fim = validarData(dataFinal, false);

        if (inicio.after(fim)) {
            throw new IllegalArgumentException("Data inicial nao pode ser posterior aa data final.");
        }

        Map<String, Double> horasPorDia = new HashMap<>();

        for (CartaoPonto c : e.getCartoes()) {
            Date data = validarData(c.getData(), false);

            // intervalo [início, fim)
            if (!data.before(inicio) && data.before(fim)) {
                horasPorDia.put(c.getData(), horasPorDia.getOrDefault(c.getData(), 0.0) + c.getHoras());
            }
        }

        double normais = 0;
        double extrasTotal = 0;

        for (double horas : horasPorDia.values()) {
            if (horas > 8) {
                normais += 8;
                extrasTotal += horas - 8;
            } else {
                normais += horas;
            }
        }

        return extras ? extrasTotal : normais;
    }

    private static Date validarData(String data, boolean inicial) {
        if (data == null || data.trim().isEmpty()) {
            throw new IllegalArgumentException("Data invalida.");
        }

        String[] partes = data.split("/");
        if (partes.length != 3) {
            throw new IllegalArgumentException("Data invalida.");
        }

        try {
            int dia = Integer.parseInt(partes[0]);
            int mes = Integer.parseInt(partes[1]);
            int ano = Integer.parseInt(partes[2]);

            if (dia <= 0) {
                if (inicial) throw new IllegalArgumentException("Data inicial invalida.");
                else throw new IllegalArgumentException("Data final invalida.");
            }
            if (mes <= 0 || mes > 12) {
                throw new IllegalArgumentException("Data invalida.");
            }

            SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy");
            sdf.setLenient(false);
            return sdf.parse(data);

        } catch (NumberFormatException e) {
            if (inicial) throw new IllegalArgumentException("Data inicial invalida.");
            else throw new IllegalArgumentException("Data final invalida.");
        } catch (ParseException e) {
            if (inicial) throw new IllegalArgumentException("Data inicial invalida.");
            else throw new IllegalArgumentException("Data final invalida.");
        }
    }

    private static String formatarNumero(double valor) {
        if (valor == (long) valor) {
            return String.valueOf((long) valor); // sem casas decimais
        } else {
            String s = String.valueOf(valor);
            return s.replace('.', ','); // usa vírgula se decimal
        }
    }

    private static String formatarValor(double valor) {
        return String.format("%.2f", valor).replace('.', ',');
    }


    private static void carregar() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(ARQUIVO))) {
            Object obj = in.readObject();
            if (obj instanceof Map) {
                empregados = (Map<String, Empregado>) obj;
            }
        } catch (Exception e) {
            empregados = new HashMap<>();
        }
    }

    private static void salvar() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(ARQUIVO))) {
            out.writeObject(empregados);
        } catch (IOException e) {
            // ignora problemas de IO
        }
    }

    public static void encerrarSistema() {
        salvar();
    }

    public static void zerarSistema() {
        empregados.clear();
        Empregado.resetContador();
        File f = new File(ARQUIVO);
        if (f.exists()) {
            f.delete();
        }
    }
}