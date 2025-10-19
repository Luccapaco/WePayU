package br.ufal.ic.p2.wepayu.models;

import java.io.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class Database {
    private static final String ARQUIVO = "empregados.ser";
    private static Map<String, Empregado> empregados = new HashMap<>();
    private static Deque<Snapshot> undoStack = new ArrayDeque<>();
    private static Deque<Snapshot> redoStack = new ArrayDeque<>();
    private static boolean sistemaEncerrado = false;
    private static boolean descartarHistoricoNoProximoZerar = true;

    public static void iniciarNovoScript() {
        descartarHistoricoNoProximoZerar = true;
    }

    static {
        carregar();
    }

    public static String adicionarEmpregado(Empregado empregado) {
        return executarComando(() -> {
            empregados.put(empregado.getId(), empregado);
            return empregado.getId();
        });
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
        executarComando(() -> {
            if(id == null || id.trim().isEmpty()){
                throw new IllegalArgumentException("Identificacao do empregado nao pode ser nula.");
            }
            if (!empregados.containsKey(id)) {
                throw new IllegalArgumentException("Empregado nao existe.");
            }
            empregados.remove(id);
        });
    }

    public static int getNumeroDeEmpregados() {
        return empregados.size();
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
        executarComando(() -> {
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
        });
    }

    public static void alteraEmpregado(String empId, String atributo, String valor,
                                       String idSindicato, String taxaSindical) {
        executarComando(() -> {
            Empregado e = getEmpregado(empId);

            if (atributo == null || atributo.trim().isEmpty()) {
                throw new IllegalArgumentException("Atributo nao pode ser nulo.");
            }

            if (!"sindicalizado".equalsIgnoreCase(atributo)) {
                throw new IllegalArgumentException("Atributo nao existe.");
            }

            alterarSindicalizado(e, valor, idSindicato, taxaSindical);
        });
    }

    public static void alteraEmpregado(String empId, String atributo, String valor, String valorAuxiliar) {
        executarComando(() -> {
            Empregado e = getEmpregado(empId);

            if (atributo == null || atributo.trim().isEmpty()) {
                throw new IllegalArgumentException("Atributo nao pode ser nulo.");
            }

            if (!"tipo".equalsIgnoreCase(atributo)) {
                throw new IllegalArgumentException("Atributo nao existe.");
            }

            alterarTipo(e, valor, valorAuxiliar);
        });
    }

    public static void alteraEmpregado(String empId, String atributo, String valor1,
                                       String banco, String agencia, String contaCorrente) {
        executarComando(() -> {
            Empregado e = getEmpregado(empId);

            if (atributo == null || atributo.trim().isEmpty()) {
                throw new IllegalArgumentException("Atributo nao pode ser nulo.");
            }

            if (!"metodopagamento".equalsIgnoreCase(atributo)) {
                throw new IllegalArgumentException("Atributo nao existe.");
            }

            alterarMetodoPagamento(e, valor1, banco, agencia, contaCorrente);
        });
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
        executarComando(() -> {
            if (empId == null || empId.trim().isEmpty()) {
                throw new IllegalArgumentException("Identificacao do empregado nao pode ser nula.");
            }

            Empregado e = getEmpregado(empId);

            if (!e.getTipo().equals("horista")) {
                throw new IllegalArgumentException("Empregado nao eh horista.");
            }

            validarDataSimples(data);

            CartaoPonto cartao = new CartaoPonto(data, horas);
            e.adicionarCartao(cartao);
        });
    }

    public static void lancaVenda(String empId, String data, String valor) {
        executarComando(() -> {
            if (empId == null || empId.trim().isEmpty()) {
                throw new IllegalArgumentException("Identificacao do empregado nao pode ser nula.");
            }

            Empregado e = getEmpregado(empId);

            if (!e.getTipo().equals("comissionado")) {
                throw new IllegalArgumentException("Empregado nao eh comissionado.");
            }

            validarDataSimples(data);

            Venda v = new Venda(data, valor);
            e.adicionarVenda(v);
        });
    }


    public static void lancaTaxaServico(String membroId, String data, String valor) {
        executarComando(() -> {
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

            validarDataSimples(data);

            TaxaServico t = new TaxaServico(data, valor);
            e.adicionarTaxa(t);
        });
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

    public static String totalFolha(String data) {
        FolhaPagamento folha = calcularFolha(data);
        return formatarValor(folha.totalBruto());
    }

    public static void rodaFolha(String data, String saida) {
        executarComando(() -> {
            FolhaPagamento folha = calcularFolha(data);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(saida))) {
                writer.write(folha.gerarRelatorio());
            } catch (IOException e) {
                throw new RuntimeException("Erro ao escrever arquivo de folha.", e);
            }
        });
    }

    private static FolhaPagamento calcularFolha(String data) {
        LocalDate referencia = toLocalDate(validarData(data, false));
        return new FolhaPagamento(referencia);
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

    private static Date validarDataSimples(String data) {
        try {
            return validarData(data, false);
        } catch (IllegalArgumentException e) {
            String mensagem = e.getMessage();
            if ("Data final invalida.".equals(mensagem) || "Data inicial invalida.".equals(mensagem)) {
                throw new IllegalArgumentException("Data invalida.");
            }
            throw e;
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


    private static LocalDate toLocalDate(Date data) {
        return data.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static LocalDate parseData(String data) {
        return toLocalDate(validarData(data, false));
    }

    private static String formatarValor(BigDecimal valor) {
        return String.format("%.2f", valor.doubleValue()).replace('.', ',');
    }

    private static BigDecimal truncar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.DOWN);
    }

    private static class FolhaPagamento {
        private static final DateTimeFormatter CABECALHO_DATA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        private final LocalDate data;
        private final List<PagamentoHorista> horistas = new ArrayList<>();
        private final List<PagamentoAssalariado> assalariados = new ArrayList<>();
        private final List<PagamentoComissionado> comissionados = new ArrayList<>();
        private BigDecimal totalHoristaBruto = BigDecimal.ZERO;
        private BigDecimal totalHoristaDescontos = BigDecimal.ZERO;
        private BigDecimal totalHoristaLiquido = BigDecimal.ZERO;
        private int totalHoras = 0;
        private int totalExtras = 0;
        private BigDecimal totalAssalariadoBruto = BigDecimal.ZERO;
        private BigDecimal totalAssalariadoDescontos = BigDecimal.ZERO;
        private BigDecimal totalAssalariadoLiquido = BigDecimal.ZERO;
        private BigDecimal totalComFixo = BigDecimal.ZERO;
        private BigDecimal totalComVendas = BigDecimal.ZERO;
        private BigDecimal totalComComissao = BigDecimal.ZERO;
        private BigDecimal totalComBruto = BigDecimal.ZERO;
        private BigDecimal totalComDescontos = BigDecimal.ZERO;
        private BigDecimal totalComLiquido = BigDecimal.ZERO;

        private FolhaPagamento(LocalDate data) {
            this.data = data;
            processarHoristas();
            processarAssalariados();
            processarComissionados();
        }

        private boolean isSexta(LocalDate dia) {
            return dia.getDayOfWeek() == DayOfWeek.FRIDAY;
        }

        private void processarHoristas() {
            if (!isSexta(data)) {
                return;
            }

            LocalDate inicioPeriodo = data.minusDays(6);

            List<Empregado> empregadosHoristas = new ArrayList<>();
            for (Empregado e : empregados.values()) {
                if ("horista".equals(e.getTipo())) {
                    empregadosHoristas.add(e);
                }
            }

            empregadosHoristas.sort(Comparator.comparing(Empregado::getNome, String.CASE_INSENSITIVE_ORDER));

            for (Empregado e : empregadosHoristas) {
                Map<LocalDate, Double> horasPorDia = new HashMap<>();
                for (CartaoPonto cartao : e.getCartoes()) {
                    LocalDate diaCartao = parseData(cartao.getData());
                    if (!diaCartao.isBefore(inicioPeriodo) && !diaCartao.isAfter(data)) {
                        horasPorDia.put(diaCartao, horasPorDia.getOrDefault(diaCartao, 0.0) + cartao.getHoras());
                    }
                }

                double horasNormais = 0;
                double horasExtras = 0;
                for (double horas : horasPorDia.values()) {
                    if (horas > 8) {
                        horasNormais += 8;
                        horasExtras += horas - 8;
                    } else {
                        horasNormais += horas;
                    }
                }

                BigDecimal salarioHora = BigDecimal.valueOf(e.getSalario());
                BigDecimal normal = truncar(salarioHora.multiply(BigDecimal.valueOf(horasNormais)));
                BigDecimal adicional = BigDecimal.ZERO;
                if (horasExtras > 0) {
                    BigDecimal fatorHoraExtra = salarioHora.multiply(BigDecimal.valueOf(3)).divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP);
                    adicional = truncar(fatorHoraExtra.multiply(BigDecimal.valueOf(horasExtras)));
                }

                BigDecimal bruto = normal.add(adicional);
                BigDecimal descontos = BigDecimal.ZERO;
                BigDecimal liquido = bruto;

                if (e.isSindicalizado() && bruto.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal taxas = calcularTaxasSindicaisHorista(e, data);
                    descontos = descontos.add(taxas);
                    BigDecimal extrasServico = calcularTaxasServico(e, obterUltimoPagamentoHorista(e, data), data);
                    descontos = descontos.add(extrasServico);
                    if (descontos.compareTo(bruto) > 0) {
                        descontos = bruto;
                    }
                    liquido = bruto.subtract(descontos);
                }

                PagamentoHorista registro = new PagamentoHorista(e, (int)Math.round(horasNormais), (int)Math.round(horasExtras), bruto, descontos, liquido);
                horistas.add(registro);

                totalHoras += registro.horasNormais;
                totalExtras += registro.horasExtras;
                totalHoristaBruto = totalHoristaBruto.add(bruto);
                totalHoristaDescontos = totalHoristaDescontos.add(descontos);
                totalHoristaLiquido = totalHoristaLiquido.add(liquido);
            }
        }

        private void processarAssalariados() {
            if (!isUltimoDiaMes(data)) {
                return;
            }

            List<Empregado> empregadosAssalariados = new ArrayList<>();
            for (Empregado e : empregados.values()) {
                if ("assalariado".equals(e.getTipo())) {
                    empregadosAssalariados.add(e);
                }
            }

            empregadosAssalariados.sort(Comparator.comparing(Empregado::getNome, String.CASE_INSENSITIVE_ORDER));

            for (Empregado e : empregadosAssalariados) {
                BigDecimal bruto = truncar(BigDecimal.valueOf(e.getSalario()));
                BigDecimal descontos = BigDecimal.ZERO;

                if (e.isSindicalizado()) {
                    BigDecimal taxas = calcularTaxasSindicaisAssalariado(e, data);
                    descontos = descontos.add(taxas);
                    BigDecimal extrasServico = calcularTaxasServico(e, obterUltimoPagamentoAssalariado(data), data);
                    descontos = descontos.add(extrasServico);
                    if (descontos.compareTo(bruto) > 0) {
                        descontos = bruto;
                    }
                }

                BigDecimal liquido = bruto.subtract(descontos);
                PagamentoAssalariado registro = new PagamentoAssalariado(e, bruto, descontos, liquido);
                assalariados.add(registro);

                totalAssalariadoBruto = totalAssalariadoBruto.add(bruto);
                totalAssalariadoDescontos = totalAssalariadoDescontos.add(descontos);
                totalAssalariadoLiquido = totalAssalariadoLiquido.add(liquido);
            }
        }

        private void processarComissionados() {
            if (!isSexta(data)) {
                return;
            }

            if (!isPagamentoQuinzenal(data)) {
                return;
            }

            List<Empregado> empregadosComissionados = new ArrayList<>();
            for (Empregado e : empregados.values()) {
                if ("comissionado".equals(e.getTipo())) {
                    empregadosComissionados.add(e);
                }
            }

            empregadosComissionados.sort(Comparator.comparing(Empregado::getNome, String.CASE_INSENSITIVE_ORDER));

            LocalDate inicioPeriodo = data.minusDays(13);
            LocalDate ultimoPagamento = obterUltimoPagamentoQuinzenal(data);

            for (Empregado e : empregadosComissionados) {
                BigDecimal fixo = calcularSalarioFixoComissionado(e);
                BigDecimal vendas = BigDecimal.ZERO;
                for (Venda v : e.getVendas()) {
                    LocalDate dataVenda = parseData(v.getData());
                    if (!dataVenda.isBefore(inicioPeriodo) && !dataVenda.isAfter(data)) {
                        vendas = vendas.add(truncar(BigDecimal.valueOf(v.getValor())));
                    }
                }

                BigDecimal comissao = BigDecimal.ZERO;
                if (e.getComissao() != null) {
                    comissao = truncar(vendas.multiply(BigDecimal.valueOf(e.getComissao())));
                }

                BigDecimal bruto = fixo.add(comissao);
                BigDecimal descontos = BigDecimal.ZERO;

                if (e.isSindicalizado()) {
                    BigDecimal taxas = calcularTaxasSindicaisComissionado(e, data);
                    descontos = descontos.add(taxas);
                    BigDecimal extrasServico = calcularTaxasServico(e, ultimoPagamento, data);
                    descontos = descontos.add(extrasServico);
                    if (descontos.compareTo(bruto) > 0) {
                        descontos = bruto;
                    }
                }

                BigDecimal liquido = bruto.subtract(descontos);

                PagamentoComissionado registro = new PagamentoComissionado(e, fixo, vendas, comissao, bruto, descontos, liquido);
                comissionados.add(registro);

                totalComFixo = totalComFixo.add(fixo);
                totalComVendas = totalComVendas.add(vendas);
                totalComComissao = totalComComissao.add(comissao);
                totalComBruto = totalComBruto.add(bruto);
                totalComDescontos = totalComDescontos.add(descontos);
                totalComLiquido = totalComLiquido.add(liquido);
            }
        }

        private BigDecimal calcularTaxasSindicaisHorista(Empregado e, LocalDate pagamento) {
            LocalDate ultimo = obterUltimoPagamentoHorista(e, pagamento);
            if (ultimo == null) {
                LocalDate inicio = obterPrimeiroCartao(e);
                if (inicio == null) {
                    return BigDecimal.ZERO;
                }
                long dias = ChronoUnit.DAYS.between(inicio.minusDays(1), pagamento);
                return truncar(BigDecimal.valueOf(e.getTaxaSindical()).multiply(BigDecimal.valueOf(dias)));
            }
            long dias = ChronoUnit.DAYS.between(ultimo, pagamento);
            return truncar(BigDecimal.valueOf(e.getTaxaSindical()).multiply(BigDecimal.valueOf(dias)));
        }

        private BigDecimal calcularTaxasSindicaisAssalariado(Empregado e, LocalDate pagamento) {
            LocalDate ultimo = obterUltimoPagamentoAssalariado(pagamento);
            if (ultimo == null) {
                LocalDate inicio = LocalDate.of(2005, 1, 1);
                long dias = ChronoUnit.DAYS.between(inicio.minusDays(1), pagamento);
                return truncar(BigDecimal.valueOf(e.getTaxaSindical()).multiply(BigDecimal.valueOf(dias)));
            }
            long dias = ChronoUnit.DAYS.between(ultimo, pagamento);
            return truncar(BigDecimal.valueOf(e.getTaxaSindical()).multiply(BigDecimal.valueOf(dias)));
        }

        private BigDecimal calcularTaxasSindicaisComissionado(Empregado e, LocalDate pagamento) {
            LocalDate ultimo = obterUltimoPagamentoQuinzenal(pagamento);
            if (ultimo == null) {
                LocalDate inicio = LocalDate.of(2005, 1, 1);
                long dias = ChronoUnit.DAYS.between(inicio.minusDays(1), pagamento);
                return truncar(BigDecimal.valueOf(e.getTaxaSindical()).multiply(BigDecimal.valueOf(dias)));
            }
            long dias = ChronoUnit.DAYS.between(ultimo, pagamento);
            return truncar(BigDecimal.valueOf(e.getTaxaSindical()).multiply(BigDecimal.valueOf(dias)));
        }

        private BigDecimal calcularTaxasServico(Empregado e, LocalDate ultimoPagamento, LocalDate pagamento) {
            if (!e.isSindicalizado()) {
                return BigDecimal.ZERO;
            }

            BigDecimal total = BigDecimal.ZERO;
            for (TaxaServico taxa : e.getTaxas()) {
                LocalDate dataTaxa = parseData(taxa.getData());
                boolean aposUltimo = ultimoPagamento == null || dataTaxa.isAfter(ultimoPagamento);
                if (aposUltimo && !dataTaxa.isAfter(pagamento)) {
                    total = total.add(truncar(BigDecimal.valueOf(taxa.getValor())));
                }
            }
            return total;
        }

        private LocalDate obterUltimoPagamentoHorista(Empregado e, LocalDate pagamento) {
            Set<LocalDate> diasPagamento = new HashSet<>();
            for (CartaoPonto cartao : e.getCartoes()) {
                LocalDate dataCartao = parseData(cartao.getData());
                LocalDate sexta = proximaSexta(dataCartao);
                if (!sexta.isAfter(pagamento)) {
                    diasPagamento.add(sexta);
                }
            }

            LocalDate ultimo = null;
            for (LocalDate dia : diasPagamento) {
                if (dia.isBefore(pagamento)) {
                    if (ultimo == null || dia.isAfter(ultimo)) {
                        ultimo = dia;
                    }
                }
            }
            return ultimo;
        }

        private LocalDate obterPrimeiroCartao(Empregado e) {
            LocalDate primeiro = null;
            for (CartaoPonto cartao : e.getCartoes()) {
                LocalDate dataCartao = parseData(cartao.getData());
                if (primeiro == null || dataCartao.isBefore(primeiro)) {
                    primeiro = dataCartao;
                }
            }
            return primeiro;
        }

        private LocalDate proximaSexta(LocalDate data) {
            LocalDate dia = data;
            while (dia.getDayOfWeek() != DayOfWeek.FRIDAY) {
                dia = dia.plusDays(1);
            }
            return dia;
        }

        private boolean isUltimoDiaMes(LocalDate dia) {
            return dia.getDayOfMonth() == dia.lengthOfMonth();
        }

        private boolean isPagamentoQuinzenal(LocalDate dia) {
            LocalDate inicio = LocalDate.of(2005, 1, 1);
            long dias = ChronoUnit.DAYS.between(inicio, dia);
            return dias >= 13 && dias % 14 == 13;
        }

        private LocalDate obterUltimoPagamentoAssalariado(LocalDate pagamento) {
            LocalDate anterior = pagamento.minusDays(pagamento.getDayOfMonth());
            if (anterior.getDayOfMonth() == anterior.lengthOfMonth()) {
                return anterior;
            }
            return anterior.withDayOfMonth(anterior.lengthOfMonth());
        }

        private LocalDate obterUltimoPagamentoQuinzenal(LocalDate pagamento) {
            LocalDate inicio = LocalDate.of(2005, 1, 14);
            if (!pagamento.isAfter(inicio)) {
                return null;
            }
            LocalDate dia = inicio;
            LocalDate ultimo = null;
            while (!dia.isAfter(pagamento.minusDays(14))) {
                ultimo = dia;
                dia = dia.plusDays(14);
            }
            return ultimo;
        }

        private BigDecimal calcularSalarioFixoComissionado(Empregado e) {
            BigDecimal salario = BigDecimal.valueOf(e.getSalario());
            BigDecimal anual = salario.multiply(BigDecimal.valueOf(12));
            BigDecimal pagamentos = anual.divide(BigDecimal.valueOf(26), 10, RoundingMode.HALF_UP);
            return truncar(pagamentos);
        }

        private BigDecimal totalBruto() {
            return totalHoristaBruto.add(totalAssalariadoBruto).add(totalComBruto);
        }

        private String gerarRelatorio() {
            StringBuilder sb = new StringBuilder();
            sb.append("FOLHA DE PAGAMENTO DO DIA ").append(data.format(CABECALHO_DATA)).append('\n');
            sb.append("====================================\n\n");
            gerarSecaoHoristas(sb);
            gerarSecaoAssalariados(sb);
            gerarSecaoComissionados(sb);
            sb.append("TOTAL FOLHA: ").append(formatarValor(totalBruto())).append('\n');
            return sb.toString();
        }

        private void gerarSecaoHoristas(StringBuilder sb) {
            sb.append("===============================================================================================================================\n");
            sb.append("===================== HORISTAS ================================================================================================\n");
            sb.append("===============================================================================================================================\n");
            sb.append("Nome                                 Horas Extra Salario Bruto Descontos Salario Liquido Metodo\n");
            sb.append("==================================== ===== ===== ============= ========= =============== ======================================\n");

            for (PagamentoHorista registro : horistas) {
                StringBuilder linha = new StringBuilder();
                linha.append(registro.empregado.getNome());
                appendValor(linha, String.valueOf(registro.horasNormais), 41);
                appendValor(linha, String.valueOf(registro.horasExtras), 47);
                appendValor(linha, formatarValor(registro.bruto), 61);
                appendValor(linha, formatarValor(registro.descontos), 71);
                appendValor(linha, formatarValor(registro.liquido), 87);
                appendTexto(linha, formatarMetodo(registro.empregado), 89);
                sb.append(linha).append('\n');
            }

            StringBuilder total = new StringBuilder();
            total.append("TOTAL HORISTAS");
            appendValor(total, String.valueOf(totalHoras), 41);
            appendValor(total, String.valueOf(totalExtras), 47);
            appendValor(total, formatarValor(totalHoristaBruto), 61);
            appendValor(total, formatarValor(totalHoristaDescontos), 71);
            appendValor(total, formatarValor(totalHoristaLiquido), 87);
            sb.append('\n').append(total).append("\n\n");
        }

        private void gerarSecaoAssalariados(StringBuilder sb) {
            sb.append("===============================================================================================================================\n");
            sb.append("===================== ASSALARIADOS ============================================================================================\n");
            sb.append("===============================================================================================================================\n");
            sb.append("Nome                                             Salario Bruto Descontos Salario Liquido Metodo\n");
            sb.append("================================================ ============= ========= =============== ======================================\n");

            for (PagamentoAssalariado registro : assalariados) {
                StringBuilder linha = new StringBuilder();
                linha.append(registro.empregado.getNome());
                appendValor(linha, formatarValor(registro.bruto), 61);
                appendValor(linha, formatarValor(registro.descontos), 71);
                appendValor(linha, formatarValor(registro.liquido), 87);
                appendTexto(linha, formatarMetodo(registro.empregado), 89);
                sb.append(linha).append('\n');
            }

            StringBuilder total = new StringBuilder();
            total.append("TOTAL ASSALARIADOS");
            appendValor(total, formatarValor(totalAssalariadoBruto), 61);
            appendValor(total, formatarValor(totalAssalariadoDescontos), 71);
            appendValor(total, formatarValor(totalAssalariadoLiquido), 87);
            sb.append('\n').append(total).append("\n\n");
        }

        private void gerarSecaoComissionados(StringBuilder sb) {
            sb.append("===============================================================================================================================\n");
            sb.append("===================== COMISSIONADOS ===========================================================================================\n");
            sb.append("===============================================================================================================================\n");
            sb.append("Nome                  Fixo     Vendas   Comissao Salario Bruto Descontos Salario Liquido Metodo\n");
            sb.append("===================== ======== ======== ======== ============= ========= =============== ======================================\n");

            for (PagamentoComissionado registro : comissionados) {
                StringBuilder linha = new StringBuilder();
                linha.append(registro.empregado.getNome());
                appendValor(linha, formatarValor(registro.fixo), 29);
                appendValor(linha, formatarValor(registro.vendas), 38);
                appendValor(linha, formatarValor(registro.comissao), 47);
                appendValor(linha, formatarValor(registro.bruto), 61);
                appendValor(linha, formatarValor(registro.descontos), 71);
                appendValor(linha, formatarValor(registro.liquido), 87);
                appendTexto(linha, formatarMetodo(registro.empregado), 89);
                sb.append(linha).append('\n');
            }

            StringBuilder total = new StringBuilder();
            total.append("TOTAL COMISSIONADOS");
            appendValor(total, formatarValor(totalComFixo), 29);
            appendValor(total, formatarValor(totalComVendas), 38);
            appendValor(total, formatarValor(totalComComissao), 47);
            appendValor(total, formatarValor(totalComBruto), 61);
            appendValor(total, formatarValor(totalComDescontos), 71);
            appendValor(total, formatarValor(totalComLiquido), 87);
            sb.append('\n').append(total).append("\n\n");
        }

        private void appendValor(StringBuilder sb, String valor, int fimInclusivo) {
            if (valor == null) {
                valor = "";
            }
            int inicio = fimInclusivo - valor.length() + 1;
            if (inicio < 0) {
                inicio = 0;
            }
            while (sb.length() < inicio) {
                sb.append(' ');
            }
            sb.append(valor);
        }

        private void appendTexto(StringBuilder sb, String texto, int inicio) {
            if (texto == null) {
                texto = "";
            }
            while (sb.length() < inicio) {
                sb.append(' ');
            }
            sb.append(texto);
        }

        private String formatarMetodo(Empregado e) {
            switch (e.getMetodoPagamento()) {
                case "emMaos":
                    return "Em maos";
                case "correios":
                    return "Correios, " + e.getEndereco();
                case "banco":
                    return e.getBanco() + ", Ag. " + e.getAgencia() + " CC " + e.getContaCorrente();
                default:
                    return "";
            }
        }
    }

    private static class PagamentoHorista {
        private final Empregado empregado;
        private final int horasNormais;
        private final int horasExtras;
        private final BigDecimal bruto;
        private final BigDecimal descontos;
        private final BigDecimal liquido;

        private PagamentoHorista(Empregado empregado, int horasNormais, int horasExtras, BigDecimal bruto, BigDecimal descontos, BigDecimal liquido) {
            this.empregado = empregado;
            this.horasNormais = horasNormais;
            this.horasExtras = horasExtras;
            this.bruto = bruto;
            this.descontos = descontos;
            this.liquido = liquido;
        }
    }

    private static class PagamentoAssalariado {
        private final Empregado empregado;
        private final BigDecimal bruto;
        private final BigDecimal descontos;
        private final BigDecimal liquido;

        private PagamentoAssalariado(Empregado empregado, BigDecimal bruto, BigDecimal descontos, BigDecimal liquido) {
            this.empregado = empregado;
            this.bruto = bruto;
            this.descontos = descontos;
            this.liquido = liquido;
        }
    }

    private static class PagamentoComissionado {
        private final Empregado empregado;
        private final BigDecimal fixo;
        private final BigDecimal vendas;
        private final BigDecimal comissao;
        private final BigDecimal bruto;
        private final BigDecimal descontos;
        private final BigDecimal liquido;

        private PagamentoComissionado(Empregado empregado, BigDecimal fixo, BigDecimal vendas, BigDecimal comissao, BigDecimal bruto, BigDecimal descontos, BigDecimal liquido) {
            this.empregado = empregado;
            this.fixo = fixo;
            this.vendas = vendas;
            this.comissao = comissao;
            this.bruto = bruto;
            this.descontos = descontos;
            this.liquido = liquido;
        }
    }

    public static void undo() {
        verificarSistemaAtivo();
        if (undoStack.isEmpty()) {
            throw new IllegalArgumentException("Nao ha comando a desfazer.");
        }

        Snapshot atual = criarSnapshot();
        Snapshot anterior = undoStack.pop();
        redoStack.push(atual);
        restaurarSnapshot(anterior);
    }

    public static void redo() {
        verificarSistemaAtivo();
        if (redoStack.isEmpty()) {
            throw new IllegalArgumentException("Nao ha comando a refazer.");
        }

        Snapshot atual = criarSnapshot();
        Snapshot proximo = redoStack.pop();
        undoStack.push(atual);
        restaurarSnapshot(proximo);
    }

    private static void verificarSistemaAtivo() {
        if (sistemaEncerrado) {
            throw new IllegalStateException("Nao pode dar comandos depois de encerrarSistema.");
        }
    }

    private static void executarComando(Runnable acao) {
        verificarSistemaAtivo();
        Snapshot anterior = criarSnapshot();
        try {
            acao.run();
            undoStack.push(anterior);
            redoStack.clear();
        } catch (RuntimeException e) {
            restaurarSnapshot(anterior);
            throw e;
        }
    }

    private static <T> T executarComando(Supplier<T> acao) {
        verificarSistemaAtivo();
        Snapshot anterior = criarSnapshot();
        try {
            T resultado = acao.get();
            undoStack.push(anterior);
            redoStack.clear();
            return resultado;
        } catch (RuntimeException e) {
            restaurarSnapshot(anterior);
            throw e;
        }
    }

    private static Snapshot criarSnapshot() {
        return new Snapshot(copiarEmpregados(), Empregado.getProximoId());
    }

    private static void restaurarSnapshot(Snapshot snapshot) {
        empregados = snapshot.empregados;
        Empregado.definirProximoId(snapshot.proximoId);
        if (snapshot.historicoUndo != null) {
            undoStack = copiarPilha(snapshot.historicoUndo);
        }
    }

    private static Map<String, Empregado> copiarEmpregados() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(baos);
            out.writeObject(empregados);
            out.flush();
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
            return (Map<String, Empregado>) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Erro ao copiar estado dos empregados.", e);
        }
    }

    private static Deque<Snapshot> copiarPilha(Deque<Snapshot> origem) {
        if (origem == null || origem.isEmpty()) {
            return new ArrayDeque<>();
        }
        return new ArrayDeque<>(origem);
    }

    private static class Snapshot {
        private final Map<String, Empregado> empregados;
        private final int proximoId;
        private final Deque<Snapshot> historicoUndo;

        private Snapshot(Map<String, Empregado> empregados, int proximoId) {
            this(empregados, proximoId, null);
        }

        private Snapshot(Map<String, Empregado> empregados, int proximoId, Deque<Snapshot> historicoUndo) {
            this.empregados = empregados;
            this.proximoId = proximoId;
            this.historicoUndo = historicoUndo;
        }
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
        sistemaEncerrado = false;
        undoStack.clear();
        redoStack.clear();
        descartarHistoricoNoProximoZerar = true;
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
        sistemaEncerrado = true;
        undoStack.clear();
        redoStack.clear();
        descartarHistoricoNoProximoZerar = true;
    }

    public static void zerarSistema() {
        if (sistemaEncerrado) {
            limparDadosPersistidos();
            sistemaEncerrado = false;
            undoStack.clear();
            redoStack.clear();
            descartarHistoricoNoProximoZerar = false;
            return;
        }

        verificarSistemaAtivo();
        Snapshot anterior = criarSnapshot();
        Deque<Snapshot> historicoAnterior = descartarHistoricoNoProximoZerar
                ? new ArrayDeque<>()
                : copiarPilha(undoStack);
        try {
            limparDadosPersistidos();
            sistemaEncerrado = false;
            undoStack.clear();
            redoStack.clear();
            undoStack.push(new Snapshot(anterior.empregados, anterior.proximoId, historicoAnterior));
            descartarHistoricoNoProximoZerar = false;
        } catch (RuntimeException e) {
            restaurarSnapshot(anterior);
            throw e;
        }
    }

    private static void limparDadosPersistidos() {
        empregados.clear();
        Empregado.resetContador();
        File f = new File(ARQUIVO);
        if (f.exists()) {
            f.delete();
        }
    }
}
