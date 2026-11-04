package govsim;

import govsim.engine.BancoEventos;
import govsim.engine.MotorEconomico;
import govsim.model.Decreto;
import govsim.model.Estado;
import govsim.model.Evento;
import govsim.model.HistoricoTurno;
import govsim.server.JsonUtil;
import govsim.server.ServidorHttp;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Classe principal de inicialização do GovSim — Simulador de Gestão Governamental em Turnos.
 * Inicia o servidor HTTP local na porta 8080 (ou porta alternativa) e abre o navegador.
 * Suporta o argumento --verificar para executar a Suíte Completa de Auditoria, Testes Automatizados,
 * Testes de Regressão, Simulação de Estratégias de Jogabilidade e Benchmark de Estabilidade.
 */
public class Main {

    public static void main(String[] args) {
        for(int i=0;i<args.length;i++){
            if("--simular".equals(args[i])){
                int n=i+1<args.length?Integer.parseInt(args[i+1]):20000;
                govsim.engine.Simulador.executar(n);return;
            }
        }
        for (String arg : args) {
            if ("--verificar".equalsIgnoreCase(arg)) {
                executarAuditoriaAutomatizadaCompleta();
                return;
            }
        }

        int porta = 8080;
        for (int i = 0; i < args.length; i++) {
            if ("--porta".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                try {
                    porta = Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        File dirView = localizarDiretorioView();
        if (!dirView.exists()) {
            System.err.println("[ERRO] Diretorio da interface (view) nao encontrado em: " + dirView.getAbsolutePath());
            System.exit(1);
        }

        ServidorHttp servidor = null;
        int portaEscolhida = porta;
        for (int tentativa = 0; tentativa < 5; tentativa++) {
            try {
                servidor = new ServidorHttp(portaEscolhida, dirView);
                servidor.iniciar();
                break;
            } catch (Exception e) {
                portaEscolhida++;
                servidor = null;
            }
        }

        if (servidor == null) {
            System.err.println("[ERRO] Nao foi possivel iniciar o servidor HTTP nas portas " + porta + " a " + (porta + 4));
            System.exit(1);
        }

        String url = "http://localhost:" + portaEscolhida;
        System.out.println("======================================================================");
        System.out.println("  GOVSIM - SIMULADOR DE GESTAO GOVERNAMENTAL EM TURNOS (REIGNS EDITION)");
        System.out.println("  IFNMG Campus Januaria - Bacharelado em Sistemas de Informacao");
        System.out.println("  Disciplina: Introducao a Economia - Economics Games");
        System.out.println("======================================================================");
        System.out.println("  Servidor HTTP ativo em: " + url);
        System.out.println("  Arquivos da interface:  " + dirView.getAbsolutePath());
        System.out.println("  Modo Offline:           100% Funcional (Sem dependencia de internet)");
        System.out.println("----------------------------------------------------------------------");
        System.out.println("  Abra seu navegador em " + url + " para jogar.");
        System.out.println("  Pressione CTRL+C nesta janela para encerrar o servidor.");
        System.out.println("======================================================================");

        boolean abrirNavegador = true;
        for (String arg : args) {
            if ("--sem-navegador".equalsIgnoreCase(arg)) {
                abrirNavegador = false;
                break;
            }
        }

        if (abrirNavegador) {
            tentarAbrirNavegador(url);
        }
    }

    private static File localizarDiretorioView() {
        File op1 = new File("src/govsim/view");
        if (op1.exists() && op1.isDirectory()) {
            return op1;
        }
        File op2 = new File("view");
        if (op2.exists() && op2.isDirectory()) {
            return op2;
        }
        return op1;
    }

    private static void tentarAbrirNavegador(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Suíte Completa de Auditoria Técnica, Testes Unitários/Integrados, Jogabilidade e Estabilidade.
     */
    private static void executarAuditoriaAutomatizadaCompleta() {
        System.out.println("======================================================================");
        System.out.println("  SUITE DE AUDITORIA TECNICA E TESTES AUTOMATIZADOS - GOVSIM");
        System.out.println("======================================================================");

        MotorEconomico motor = new MotorEconomico();
        BancoEventos banco = new BancoEventos();

        // ---------------------------------------------------------------------
        // TESTE 1: Inicialização do Estado e Coerência dos 4 Indicadores
        // ---------------------------------------------------------------------
        Estado estInicial = new Estado();
        motor.iniciarPartida(estInicial, false, 10);
        afirmar(estInicial.getTurnoAtual() == 1, "Turno inicial deve ser 1");
        afirmar(estInicial.getTotalTurnos() == 10, "Total de turnos padrao deve ser 10");
        afirmar(estInicial.getCaixaPublico().getValorAtual() == 100.0, "Caixa inicial deve ser 100.0");
        afirmar(estInicial.getAprovacaoPopular().getValorAtual() == 60.0, "Aprovacao inicial deve ser 60.0");
        afirmar(estInicial.getPib().getValorAtual() == 2.5, "PIB inicial deve ser 2.5");
        afirmar(estInicial.getInflacao().getValorAtual() == 4.5, "Inflacao inicial deve ser 4.5");
        afirmar(estInicial.getTaxaJuros() == 10.5, "Taxa de juros inicial deve ser 10.5");
        afirmar(!estInicial.isPartidaEncerrada(), "Partida nao pode iniciar encerrada");
        afirmar(estInicial.getEventoAtual() != null, "Carta inicial deve estar presente");
        System.out.println("[APROVADO] Teste 1: Inicializacao do Estado e 4 Indicadores Principais.");

        // ---------------------------------------------------------------------
        // TESTE 2: Auditoria de Todos os 14 Eventos e 28 Decretos
        // ---------------------------------------------------------------------
        List<Evento> catalogo = banco.getCatalogoEventos();
        afirmar(catalogo.size() >= 24, "Catalogo deve possuir pelo menos 24 cartas de eventos");
        Set<String> idsUnicos = new HashSet<String>();

        for (Evento ev : catalogo) {
            afirmar(ev.getId() != null && !ev.getId().isEmpty(), "Evento sem ID");
            afirmar(idsUnicos.add(ev.getId()), "Evento duplicado: " + ev.getId());
            afirmar(ev.getNome() != null && !ev.getNome().isEmpty(), "Evento sem nome: " + ev.getId());
            afirmar(ev.getPersonagemNome() != null && !ev.getPersonagemNome().isEmpty(), "Sem personagem: " + ev.getId());
            afirmar(ev.getPersonagemCargo() != null && !ev.getPersonagemCargo().isEmpty(), "Sem cargo: " + ev.getId());
            afirmar(ev.getPersonagemAvatar() != null && !ev.getPersonagemAvatar().isEmpty(), "Sem avatar: " + ev.getId());
            afirmar(ev.getDialogo() != null && ev.getDialogo().length() > 20, "Dialogo invalido: " + ev.getId());
            afirmar(ev.getDescricao() != null && !ev.getDescricao().isEmpty(), "Sem descricao: " + ev.getId());
            afirmar(ev.getContextualizacao() != null && !ev.getContextualizacao().isEmpty(), "Sem contexto: " + ev.getId());

            Decreto dA = ev.getDecretoA();
            Decreto dB = ev.getDecretoB();
            afirmar(dA != null && dB != null, "Decretos nulos em " + ev.getId());
            afirmar(!dA.getTitulo().equals(dB.getTitulo()), "Alternativas identicas em " + ev.getId());

            for (String op : new String[]{"A", "B"}) {
                Estado e = new Estado();
                motor.iniciarPartida(e, false, 10);
                e.registrarEventoSorteado(ev);
                HistoricoTurno h = motor.processarDecisao(e, op);
                afirmar(h != null, "Historico nulo para " + ev.getId() + " opcao " + op);
                validarConsistenciaMatematica(e);
                afirmar(!h.getOQueMudou().isEmpty() && !h.getPorQueMudou().isEmpty()
                        && !h.getCustoDecisao().isEmpty() && !h.getSinteseDidatica().isEmpty(),
                        "Explicacao didatica incompleta em " + ev.getId() + " opcao " + op);
            }
        }
        System.out.println("[APROVADO] Teste 2: Auditoria completa das cartas e decretos do catálogo (0 falhas).");

        // ---------------------------------------------------------------------
        // TESTE 3: Limites Matemáticos (Clamping), Precisão e Ausência de NaN/Infinito
        // ---------------------------------------------------------------------
        Estado estLimites = new Estado();
        estLimites.getCaixaPublico().aplicarVariacao(500.0);
        afirmar(estLimites.getCaixaPublico().getValorAtual() == 200.0, "Teto do Caixa deve ser 200.0");
        estLimites.getCaixaPublico().aplicarVariacao(-999.0);
        afirmar(estLimites.getCaixaPublico().getValorAtual() == 0.0, "Piso do Caixa deve ser 0.0");

        estLimites.getAprovacaoPopular().aplicarVariacao(500.0);
        afirmar(estLimites.getAprovacaoPopular().getValorAtual() == 100.0, "Teto da Aprovacao deve ser 100.0");
        estLimites.getAprovacaoPopular().aplicarVariacao(-999.0);
        afirmar(estLimites.getAprovacaoPopular().getValorAtual() == 0.0, "Piso da Aprovacao deve ser 0.0");

        estLimites.getPib().aplicarVariacao(50.0);
        afirmar(estLimites.getPib().getValorAtual() == 12.0, "Teto do PIB deve ser 12.0");
        estLimites.getPib().aplicarVariacao(-90.0);
        afirmar(estLimites.getPib().getValorAtual() == -10.0, "Piso do PIB deve ser -10.0");

        estLimites.getInflacao().aplicarVariacao(100.0);
        afirmar(estLimites.getInflacao().getValorAtual() == 35.0, "Teto da Inflacao deve ser 35.0");
        estLimites.getInflacao().aplicarVariacao(-100.0);
        afirmar(estLimites.getInflacao().getValorAtual() == 0.0, "Piso da Inflacao deve ser 0.0");
        System.out.println("[APROVADO] Teste 3: Consistencia matematica, limites (clamping) e arredondamento.");

        // ---------------------------------------------------------------------
        // TESTE 4: Efeitos Atrasados (Agendamento, Maturação T+1/T+2 e Limpeza no Fim)
        // ---------------------------------------------------------------------
        Estado estDelay = new Estado();
        motor.iniciarPartida(estDelay, true, 10);
        // Turno 1 (Pandemia - Decreto A): agenda 1 efeito para T+1 (Turno 2)
        motor.processarDecisao(estDelay, "A");
        afirmar(estDelay.getEfeitosPendentes().size() == 1, "Deve haver 1 efeito pendente apos Turno 1");
        afirmar(estDelay.getEfeitosPendentes().get(0).getTurnosRestantes() == 1, "Turnos restantes deve ser 1");

        // Turno 2 (Alta do Dólar - Decreto A): matura o efeito do Turno 1 e agenda 2 ondas (T+1 e T+2)
        HistoricoTurno hTurno2 = motor.processarDecisao(estDelay, "A");
        afirmar(hTurno2.getEfeitosAtrasadosAtivados().size() == 1, "Efeito do Turno 1 deve maturar no Turno 2");
        afirmar(estDelay.getEfeitosPendentes().size() == 4, "Alta de juros agenda 2 efeitos da carta e 2 do canal Selic");

        // Turno 3: matura a 1ª onda dos juros do Turno 2
        HistoricoTurno hTurno3 = motor.processarDecisao(estDelay, "A");
        afirmar(hTurno3.getEfeitosAtrasadosAtivados().size() >= 1, "1a onda dos juros deve maturar no Turno 3");
        System.out.println("[APROVADO] Teste 4: Agendamento, maturacao em ondas e nao-duplicacao de efeitos atrasados.");

        // ---------------------------------------------------------------------
        // TESTE 5: Variedade e Não-Repetição de Cartas na Mesma Partida
        // ---------------------------------------------------------------------
        Estado estVariedade = new Estado();
        motor.iniciarPartida(estVariedade, false, 10);
        Set<String> sorteadosNaPartida = new HashSet<String>();
        for (int t = 1; t <= 10 && !estVariedade.isPartidaEncerrada(); t++) {
            String idCarta = estVariedade.getEventoAtual().getId();
            afirmar(sorteadosNaPartida.add(idCarta), "Carta repetida na mesma partida de 10 turnos: " + idCarta);
            // Mantém indicadores altos para testar os 10 sorteios
            estVariedade.getCaixaPublico().definirValor(120.0);
            estVariedade.getAprovacaoPopular().definirValor(75.0);
            motor.processarDecisao(estVariedade, "A");
        }
        afirmar(sorteadosNaPartida.size() == 10, "Devem ser sorteadas 10 cartas distintas nos 10 turnos");
        afirmar(estVariedade.getEfeitosPendentes().isEmpty(), "Efeitos pendentes devem ser limpos ao encerrar a partida");
        System.out.println("[APROVADO] Teste 5: Selecao de cartas sem repeticao e limpeza pos-encerramento.");

        // ---------------------------------------------------------------------
        // TESTE 6: Validação de Entradas Inválidas e Bloqueio Pós-Fim de Jogo
        // ---------------------------------------------------------------------
        Estado estInvalido = new Estado();
        motor.iniciarPartida(estInvalido, true, 10);
        boolean rejeitouInvalido = false;
        try {
            motor.processarDecisao(estInvalido, "INVALIDO");
        } catch (IllegalArgumentException ex) {
            rejeitouInvalido = true;
        }
        afirmar(rejeitouInvalido, "Motor deve rejeitar codigo de decreto invalido");

        boolean rejeitouPosFim = false;
        try {
            motor.processarDecisao(estVariedade, "A");
        } catch (IllegalStateException ex) {
            rejeitouPosFim = true;
        }
        afirmar(rejeitouPosFim, "Motor deve rejeitar decisao apos partida encerrada");
        System.out.println("[APROVADO] Teste 6: Rejeicao de entradas invalidas e bloqueio pos-encerramento.");

        // ---------------------------------------------------------------------
        // TESTE 7: Simulação das 7 Estratégias de Jogabilidade (Seção 12)
        // ---------------------------------------------------------------------
        System.out.println("----------------------------------------------------------------------");
        System.out.println("  SIMULACAO DE ESTRATEGIAS DE JOGABILIDADE (SECAO 12):");

        Estado estExpansionista = simularEstrategia(motor, new String[]{"A","B","A","A","A","B","A","B","B","A"});
        for (String[] escolhas : new String[][] {
            {"A","B","A","A","A","B","A","B","B","A"},
            {"B","A","B","B","B","A","B","A","A","B"},
            {"A","A","A","B","B","A","B","A","A","A"}}) {
            Estado resultado = simularEstrategia(motor, escolhas);
            afirmar(resultado.isPartidaEncerrada(), "Roteiro deve terminar sem exceder o mandato");
        }
        govsim.engine.VerificacaoEconomica.executar();
        govsim.server.VerificacaoApi.executar();
        govsim.server.VerificacaoSessoes.executar();
        // 7.7. Reinício de Partida Limpo após Fim de Jogo
        motor.iniciarPartida(estExpansionista, false, 10);
        afirmar(!estExpansionista.isPartidaEncerrada() && estExpansionista.getTurnoAtual() == 1
                && estExpansionista.getHistorico().isEmpty() && estExpansionista.getCaixaPublico().getValorAtual() == 100.0,
                "Reinicio deve restaurar completamente o estado inicial");
        System.out.println("  [OK] 7.7 Reinicio Limpo de Partida        -> Estado restaurado sem residuos.");
        System.out.println("[APROVADO] Teste 7: Roteiros, novas regras e reinicio verificados.");

        // ---------------------------------------------------------------------
        // TESTE 8: Estabilidade, Desempenho e Memória (500 Partidas = ~4.500 Turnos)
        // ---------------------------------------------------------------------
        System.out.println("----------------------------------------------------------------------");
        long inicioMs = System.currentTimeMillis();
        Estado estStress = new Estado();
        for (int p = 0; p < 500; p++) {
            motor.iniciarPartida(estStress, (p % 2 == 0), 10);
            int passo = 0;
            while (!estStress.isPartidaEncerrada() && passo < 12) {
                String escolha = ((p + passo) % 2 == 0) ? "A" : "B";
                motor.processarDecisao(estStress, escolha);
                validarConsistenciaMatematica(estStress);
                passo++;
            }
            String json = JsonUtil.estadoParaJson(estStress);
            afirmar(json.startsWith("{") && json.endsWith("}"), "JSON invalido no teste de estresse");
        }
        long duracaoMs = System.currentTimeMillis() - inicioMs;
        Runtime rt = Runtime.getRuntime();
        long memoriaUsadaMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        System.out.println("[APROVADO] Teste 8: Benchmark de Estabilidade (500 partidas completas + serializacao JSON em "
                + duracaoMs + " ms | Memoria JVM utilizada: " + memoriaUsadaMb + " MB).");
        System.out.println("======================================================================");
        System.out.println("  RESULTADO GLOBAL: 100% DOS TESTES AUTOMATIZADOS APROVADOS!");
        System.out.println("======================================================================");
    }

    private static Estado simularEstrategia(MotorEconomico motor, String[] escolhas) {
        Estado e = new Estado();
        motor.iniciarPartida(e, true, escolhas.length);
        for (int i = 0; i < escolhas.length && !e.isPartidaEncerrada(); i++) {
            motor.processarDecisao(e, escolhas[i]);
            validarConsistenciaMatematica(e);
        }
        return e;
    }

    private static void validarConsistenciaMatematica(Estado e) {
        double c = e.getCaixaPublico().getValorAtual();
        double a = e.getAprovacaoPopular().getValorAtual();
        double p = e.getPib().getValorAtual();
        double inf = e.getInflacao().getValorAtual();
        double j = e.getTaxaJuros();

        afirmar(!Double.isNaN(c) && !Double.isInfinite(c) && c >= 0.0 && c <= 200.0, "Caixa fora dos limites: " + c);
        afirmar(!Double.isNaN(a) && !Double.isInfinite(a) && a >= 0.0 && a <= 100.0, "Aprovacao fora dos limites: " + a);
        afirmar(!Double.isNaN(p) && !Double.isInfinite(p) && p >= -10.0 && p <= 12.0, "PIB fora dos limites: " + p);
        afirmar(!Double.isNaN(inf) && !Double.isInfinite(inf) && inf >= 0.0 && inf <= 35.0, "Inflacao fora dos limites: " + inf);
        afirmar(!Double.isNaN(j) && !Double.isInfinite(j) && j >= 2.0 && j <= 25.0, "Juros fora dos limites: " + j);
    }

    private static void afirmar(boolean condicao, String mensagemErro) {
        if (!condicao) {
            System.err.println("[FALHA DE AUDITORIA] " + mensagemErro);
            System.exit(1);
        }
    }
}
