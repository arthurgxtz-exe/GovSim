package govsim.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registra de forma detalhada tudo o que ocorreu em um turno finalizado:
 * evento enfrentado, decreto assinado, indicadores antes e depois, efeitos imediatos,
 * interações macroeconômicas endógenas, efeitos atrasados maturados, novos efeitos
 * agendados, reações da IA e explicação didática completa.
 */
public class HistoricoTurno {
    private final int numeroTurno;
    private final String eventoId;
    private final String eventoNome;
    private final String eventoCategoria;
    private final String decretoCodigo;
    private final String decretoTitulo;
    private final String decretoTipoPolitica;

    private final double caixaAntes;
    private final double caixaDepois;
    private final double aprovacaoAntes;
    private final double aprovacaoDepois;
    private final double pibAntes;
    private final double pibDepois;
    private final double inflacaoAntes;
    private final double inflacaoDepois;
    private final double jurosAntes;
    private final double jurosDepois;

    // Explicações estruturadas para o ensino de Economia
    private final String oQueMudou;
    private final String porQueMudou;
    private final String custoDecisao;
    private final String consequenciasFuturas;
    private final String sinteseDidatica;

    private final List<String> detalhesCalculo;
    private final List<String> efeitosAtrasadosAtivados;
    private final List<String> efeitosAtrasadosAgendados;
    private final List<ReacaoAgente> reacoesTurno;

    public HistoricoTurno(int numeroTurno, String eventoId, String eventoNome, String eventoCategoria,
                          String decretoCodigo, String decretoTitulo, String decretoTipoPolitica,
                          double caixaAntes, double caixaDepois,
                          double aprovacaoAntes, double aprovacaoDepois,
                          double pibAntes, double pibDepois,
                          double inflacaoAntes, double inflacaoDepois,
                          double jurosAntes, double jurosDepois,
                          String oQueMudou, String porQueMudou, String custoDecisao,
                          String consequenciasFuturas, String sinteseDidatica,
                          List<String> detalhesCalculo,
                          List<String> efeitosAtrasadosAtivados,
                          List<String> efeitosAtrasadosAgendados,
                          List<ReacaoAgente> reacoesTurno) {
        this.numeroTurno = numeroTurno;
        this.eventoId = eventoId;
        this.eventoNome = eventoNome;
        this.eventoCategoria = eventoCategoria;
        this.decretoCodigo = decretoCodigo;
        this.decretoTitulo = decretoTitulo;
        this.decretoTipoPolitica = decretoTipoPolitica;
        this.caixaAntes = Indicador.arredondar(caixaAntes);
        this.caixaDepois = Indicador.arredondar(caixaDepois);
        this.aprovacaoAntes = Indicador.arredondar(aprovacaoAntes);
        this.aprovacaoDepois = Indicador.arredondar(aprovacaoDepois);
        this.pibAntes = Indicador.arredondar(pibAntes);
        this.pibDepois = Indicador.arredondar(pibDepois);
        this.inflacaoAntes = Indicador.arredondar(inflacaoAntes);
        this.inflacaoDepois = Indicador.arredondar(inflacaoDepois);
        this.jurosAntes = Indicador.arredondar(jurosAntes);
        this.jurosDepois = Indicador.arredondar(jurosDepois);
        this.oQueMudou = oQueMudou;
        this.porQueMudou = porQueMudou;
        this.custoDecisao = custoDecisao;
        this.consequenciasFuturas = consequenciasFuturas;
        this.sinteseDidatica = sinteseDidatica;
        this.detalhesCalculo = detalhesCalculo != null
                ? new ArrayList<String>(detalhesCalculo)
                : new ArrayList<String>();
        this.efeitosAtrasadosAtivados = efeitosAtrasadosAtivados != null
                ? new ArrayList<String>(efeitosAtrasadosAtivados)
                : new ArrayList<String>();
        this.efeitosAtrasadosAgendados = efeitosAtrasadosAgendados != null
                ? new ArrayList<String>(efeitosAtrasadosAgendados)
                : new ArrayList<String>();
        this.reacoesTurno = reacoesTurno != null
                ? new ArrayList<ReacaoAgente>(reacoesTurno)
                : new ArrayList<ReacaoAgente>();
    }

    public int getNumeroTurno() {
        return numeroTurno;
    }

    public String getEventoId() {
        return eventoId;
    }

    public String getEventoNome() {
        return eventoNome;
    }

    public String getEventoCategoria() {
        return eventoCategoria;
    }

    public String getDecretoCodigo() {
        return decretoCodigo;
    }

    public String getDecretoTitulo() {
        return decretoTitulo;
    }

    public String getDecretoTipoPolitica() {
        return decretoTipoPolitica;
    }

    public double getCaixaAntes() {
        return caixaAntes;
    }

    public double getCaixaDepois() {
        return caixaDepois;
    }

    public double getDeltaCaixa() {
        return Indicador.arredondar(caixaDepois - caixaAntes);
    }

    public double getAprovacaoAntes() {
        return aprovacaoAntes;
    }

    public double getAprovacaoDepois() {
        return aprovacaoDepois;
    }

    public double getDeltaAprovacao() {
        return Indicador.arredondar(aprovacaoDepois - aprovacaoAntes);
    }

    public double getPibAntes() {
        return pibAntes;
    }

    public double getPibDepois() {
        return pibDepois;
    }

    public double getDeltaPib() {
        return Indicador.arredondar(pibDepois - pibAntes);
    }

    public double getInflacaoAntes() {
        return inflacaoAntes;
    }

    public double getInflacaoDepois() {
        return inflacaoDepois;
    }

    public double getDeltaInflacao() {
        return Indicador.arredondar(inflacaoDepois - inflacaoAntes);
    }

    public double getJurosAntes() {
        return jurosAntes;
    }

    public double getJurosDepois() {
        return jurosDepois;
    }

    public double getDeltaJuros() {
        return Indicador.arredondar(jurosDepois - jurosAntes);
    }

    public String getOQueMudou() {
        return oQueMudou;
    }

    public String getPorQueMudou() {
        return porQueMudou;
    }

    public String getCustoDecisao() {
        return custoDecisao;
    }

    public String getConsequenciasFuturas() {
        return consequenciasFuturas;
    }

    public String getSinteseDidatica() {
        return sinteseDidatica;
    }

    public List<String> getDetalhesCalculo() {
        return Collections.unmodifiableList(detalhesCalculo);
    }

    public List<String> getEfeitosAtrasadosAtivados() {
        return Collections.unmodifiableList(efeitosAtrasadosAtivados);
    }

    public List<String> getEfeitosAtrasadosAgendados() {
        return Collections.unmodifiableList(efeitosAtrasadosAgendados);
    }

    public List<ReacaoAgente> getReacoesTurno() {
        return Collections.unmodifiableList(reacoesTurno);
    }
}
