package br.ufal.ic.p2.wepayu.models;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Database {
    private static Map<String, Empregado> empregados = new HashMap<>();

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

    public static String getHorasNormaisTrabalhadas(String empId, String dataInicial, String dataFinal) {
        return formatarNumero(calcularHoras(empId, dataInicial, dataFinal, false));
    }

    public static String getHorasExtrasTrabalhadas(String empId, String dataInicial, String dataFinal) {
        return formatarNumero(calcularHoras(empId, dataInicial, dataFinal, true));
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

    public static void zerarSistema() {
        empregados.clear();
        Empregado.resetContador();
    }
}
