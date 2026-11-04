package govsim.model;

/**
 * Representa uma consequência econômica com defasagem temporal (efeito atrasado).
 * Quando um decreto é assinado no turno T, seus efeitos secundários podem ser
 * programados para ocorrer em T+1, T+2, etc.
 */
public class EfeitoAtrasado {
    private final int turnoOrigem;
    private final int turnoAplicacao;
    private int turnosRestantes;
    private final String decretoOrigem;
    private final String descricao;
    private final double deltaCaixa;
    private final double deltaAprovacao;
    private final double deltaPib;
    private final double deltaInflacao;

    public EfeitoAtrasado(int turnoOrigem, int defasagemTurnos, String decretoOrigem, String descricao,
                          double deltaCaixa, double deltaAprovacao, double deltaPib, double deltaInflacao) {
        this.turnoOrigem = turnoOrigem;
        this.turnosRestantes = defasagemTurnos;
        this.turnoAplicacao = turnoOrigem + defasagemTurnos;
        this.decretoOrigem = decretoOrigem;
        this.descricao = descricao;
        this.deltaCaixa = Indicador.arredondar(deltaCaixa);
        this.deltaAprovacao = Indicador.arredondar(deltaAprovacao);
        this.deltaPib = Indicador.arredondar(deltaPib);
        this.deltaInflacao = Indicador.arredondar(deltaInflacao);
    }

    /**
     * Cria uma cópia do modelo de efeito atrasado vinculada ao turno atual da partida.
     */
    public EfeitoAtrasado instanciarParaTurno(int turnoAtual) {
        return new EfeitoAtrasado(
                turnoAtual,
                this.turnosRestantes,
                this.decretoOrigem,
                this.descricao,
                this.deltaCaixa,
                this.deltaAprovacao,
                this.deltaPib,
                this.deltaInflacao
        );
    }

    public void avancarTurno() {
        this.turnosRestantes--;
    }

    public boolean deveAplicarAgora() {
        return this.turnosRestantes <= 0;
    }

    public int getTurnoOrigem() {
        return turnoOrigem;
    }

    public int getTurnoAplicacao() {
        return turnoAplicacao;
    }

    public int getTurnosRestantes() {
        return turnosRestantes;
    }

    public String getDecretoOrigem() {
        return decretoOrigem;
    }

    public String getDescricao() {
        return descricao;
    }

    public double getDeltaCaixa() {
        return deltaCaixa;
    }

    public double getDeltaAprovacao() {
        return deltaAprovacao;
    }

    public double getDeltaPib() {
        return deltaPib;
    }

    public double getDeltaInflacao() {
        return deltaInflacao;
    }
}
