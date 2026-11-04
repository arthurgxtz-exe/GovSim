package govsim.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mantém o estado completo de uma partida do GovSim:
 * indicadores principais, taxa básica de juros, turno atual, fila de eventos,
 * efeitos atrasados pendentes, reações da IA, histórico de turnos e status de vitória/derrota.
 */
public class Estado {
    public static final int TURNOS_PADRAO = 10;

    // Valores iniciais documentados na proposta e em REGRAS_ECONOMICAS.md
    public static final double CAIXA_INICIAL = 100.0;
    public static final double APROVACAO_INICIAL = 60.0;
    public static final double PIB_INICIAL = 2.5;
    public static final double INFLACAO_INICIAL = 4.5;
    public static final double JUROS_INICIAL = 10.5;

    private final java.util.Map<String,Boolean> inversoes = new java.util.HashMap<String,Boolean>();
    private double paciencia=1, confianca=1;
    public double getPaciencia(){return paciencia;}
    public double getConfianca(){return confianca;}
    public void atualizarMemoria(boolean impopular, boolean risco){paciencia=Math.max(0,Math.min(1,paciencia+(impopular?-0.15:0.10)));confianca=Math.max(0.4,Math.min(1.6,confianca+(risco?-0.10:0.05)));}
    public void sortearPosicoes(java.util.Random r,java.util.List<Evento> cartas){inversoes.clear();for(Evento c:cartas)inversoes.put(c.getId(),r.nextBoolean());}
    public boolean deveInverter(String id){return Boolean.TRUE.equals(inversoes.get(id));}
    private Dificuldade dificuldade;
    private int turnosDepressao;
    public Dificuldade getDificuldade(){return dificuldade;}
    public int getTurnosDepressao(){return turnosDepressao;}
    public void registrarAtividade(){turnosDepressao = pib.getValorAtual() < -3 ? turnosDepressao+1 : 0;}
    private int turnoAtual;
    private int totalTurnos;
    private boolean modoDemonstracao;
    private String identificacaoMandato;

    // Os 4 indicadores principais obrigatórios da proposta original
    private final Indicador caixaPublico;
    private final Indicador aprovacaoPopular;
    private final Indicador pib;
    private final Indicador inflacao;

    // Instrumento macroeconômico complementar (Taxa Básica de Juros - Selic)
    private double taxaJuros;
    private double variacaoJurosUltimoTurno;

    private Evento eventoAtual;
    private final List<String> idsEventosSorteados;
    private final List<EfeitoAtrasado> efeitosPendentes;
    private final List<HistoricoTurno> historico;
    private final List<ReacaoAgente> reacoesAtuais;

    // Controle de encerramento da partida
    private boolean partidaEncerrada;
    private boolean vitoria;
    private String statusPartida; // "EM_ANDAMENTO", "VITORIA", "DERROTA_CAIXA", "DERROTA_APROVACAO", "DERROTA_AMBOS"
    private String tituloFimDeJogo;
    private String motivoFimDeJogo;
    private String avaliacaoFinalEconomica;

    public Estado() {
        this.caixaPublico = new Indicador(
                "CAIXA",
                "Caixa Público",
                "R$ bi (índice)",
                "Representa os recursos financeiros disponíveis para o governo custear serviços, investimentos e dívida pública. Se chegar a 0, o Estado entra em colapso fiscal (Derrota).",
                CAIXA_INICIAL,
                0.0,
                200.0
        );
        this.aprovacaoPopular = new Indicador(
                "APROVACAO",
                "Aprovação Popular",
                "%",
                "Representa o nível de apoio político e social da população ao governo. Se chegar a 0%, o governo perde governabilidade e sofre impeachment/queda (Derrota).",
                APROVACAO_INICIAL,
                0.0,
                100.0
        );
        this.pib = new Indicador(
                "PIB",
                "PIB (Crescimento)",
                "% a.a.",
                "Representa a variação da atividade econômica (bens e serviços produzidos). Crescimento gera emprego e arrecadação; recessão reduz receitas e aprofunda crises.",
                PIB_INICIAL,
                -10.0,
                12.0
        );
        this.inflacao = new Indicador(
                "INFLACAO",
                "Inflação (IPCA)",
                "% a.a.",
                "Representa o aumento generalizado dos preços. Inflação alta corrói o poder de compra das famílias, derruba a aprovação popular e exige juros mais elevados.",
                INFLACAO_INICIAL,
                0.0,
                35.0
        );

        this.idsEventosSorteados = new ArrayList<String>();
        this.efeitosPendentes = new ArrayList<EfeitoAtrasado>();
        this.historico = new ArrayList<HistoricoTurno>();
        this.reacoesAtuais = new ArrayList<ReacaoAgente>();

        iniciarNovaPartida(false, TURNOS_PADRAO);
    }

    public void iniciarNovaPartida(boolean modoDemonstracao, int totalTurnos) {
        iniciarNovaPartida(modoDemonstracao,totalTurnos,Dificuldade.FACIL);
    }
    public void iniciarNovaPartida(boolean modoDemonstracao,int totalTurnos,Dificuldade dificuldade){
        this.dificuldade=dificuldade; paciencia=1;confianca=1;inversoes.clear(); this.turnosDepressao=0;
        this.modoDemonstracao = modoDemonstracao;
        this.totalTurnos = totalTurnos > 0 ? totalTurnos : dificuldade.getTotalTurnos();
        this.turnoAtual = 1;
        this.identificacaoMandato = modoDemonstracao
                ? "Mandato Presidencial 2026–2030 (Roteiro de Demonstração Guiada)"
                : "Mandato Presidencial 2026–2030 (Simulação Oficial)";

        this.caixaPublico.iniciar(dificuldade.getCaixaInicial());
        this.aprovacaoPopular.iniciar(dificuldade.getAprovacaoInicial());
        this.pib.resetar();
        this.inflacao.resetar();

        this.taxaJuros = JUROS_INICIAL;
        this.variacaoJurosUltimoTurno = 0.0;

        this.idsEventosSorteados.clear();
        this.efeitosPendentes.clear();
        this.historico.clear();
        this.reacoesAtuais.clear();

        this.partidaEncerrada = false;
        this.vitoria = false;
        this.statusPartida = "EM_ANDAMENTO";
        this.tituloFimDeJogo = "";
        this.motivoFimDeJogo = "";
        this.avaliacaoFinalEconomica = "";
    }

    public void definirTaxaJuros(double novaTaxa) {
        double anterior = this.taxaJuros;
        double ajustada = Math.max(2.0, Math.min(25.0, novaTaxa));
        this.taxaJuros = Indicador.arredondar(ajustada);
        this.variacaoJurosUltimoTurno = Indicador.arredondar(this.taxaJuros - anterior);
    }

    public void registrarEventoSorteado(Evento evento) {
        this.eventoAtual = evento;
        if (evento != null && !this.idsEventosSorteados.contains(evento.getId())) {
            this.idsEventosSorteados.add(evento.getId());
        }
    }

    public void adicionarEfeitoPendente(EfeitoAtrasado efeito) {
        this.efeitosPendentes.add(efeito);
    }

    public void removerEfeitosPendentes(List<EfeitoAtrasado> efeitosRemovidos) {
        this.efeitosPendentes.removeAll(efeitosRemovidos);
    }

    public void adicionarHistorico(HistoricoTurno registro) {
        this.historico.add(registro);
    }

    public void atualizarReacoes(List<ReacaoAgente> novasReacoes) {
        this.reacoesAtuais.clear();
        if (novasReacoes != null) {
            this.reacoesAtuais.addAll(novasReacoes);
        }
    }

    public void encerrarPartida(boolean vitoria, String status, String titulo, String motivo, String avaliacao) {
        this.partidaEncerrada = true;
        this.vitoria = vitoria;
        this.statusPartida = status;
        this.tituloFimDeJogo = titulo;
        this.motivoFimDeJogo = motivo;
        this.avaliacaoFinalEconomica = avaliacao;
        this.efeitosPendentes.clear();
        this.eventoAtual = null;
    }

    public int getTurnoAtual() {
        return turnoAtual;
    }

    public void setTurnoAtual(int turnoAtual) {
        this.turnoAtual = turnoAtual;
    }

    public int getTotalTurnos() {
        return totalTurnos;
    }

    public boolean isModoDemonstracao() {
        return modoDemonstracao;
    }

    public String getIdentificacaoMandato() {
        return identificacaoMandato;
    }

    public Indicador getCaixaPublico() {
        return caixaPublico;
    }

    public Indicador getAprovacaoPopular() {
        return aprovacaoPopular;
    }

    public Indicador getPib() {
        return pib;
    }

    public Indicador getInflacao() {
        return inflacao;
    }

    public double getTaxaJuros() {
        return taxaJuros;
    }

    public double getVariacaoJurosUltimoTurno() {
        return variacaoJurosUltimoTurno;
    }

    public Evento getEventoAtual() {
        return eventoAtual;
    }

    public List<String> getIdsEventosSorteados() {
        return Collections.unmodifiableList(idsEventosSorteados);
    }

    public List<EfeitoAtrasado> getEfeitosPendentes() {
        return Collections.unmodifiableList(efeitosPendentes);
    }

    public List<HistoricoTurno> getHistorico() {
        return Collections.unmodifiableList(historico);
    }

    public List<ReacaoAgente> getReacoesAtuais() {
        return Collections.unmodifiableList(reacoesAtuais);
    }

    public boolean isPartidaEncerrada() {
        return partidaEncerrada;
    }

    public boolean isVitoria() {
        return vitoria;
    }

    public String getStatusPartida() {
        return statusPartida;
    }

    public String getTituloFimDeJogo() {
        return tituloFimDeJogo;
    }

    public String getMotivoFimDeJogo() {
        return motivoFimDeJogo;
    }

    public String getAvaliacaoFinalEconomica() {
        return avaliacaoFinalEconomica;
    }
}
