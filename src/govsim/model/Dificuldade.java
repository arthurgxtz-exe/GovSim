package govsim.model;

/** Parâmetros de partida e balanceamento, calibrados pelo Simulador. */
public enum Dificuldade {
    FACIL("Fácil", 10, 100, 60, 1.0, 1.17, 0, 0.5, true),
    MEDIO("Médio", 12, 100, 60, 0.9, 1.18, 0.55, 1.0, false),
    DIFICIL("Difícil", 15, 70, 50, 0.95, 1.035, 0.30, 1.5, false);

    private final String nome;
    private final int totalTurnos;
    private final double caixaInicial;
    private final double aprovacaoInicial;
    private final double multiplicadorBeneficio;
    private final double multiplicadorCusto;
    private final double ruido;
    private final double multiplicadorIA;
    private final boolean mostrarNumeros;

    Dificuldade(String nome, int totalTurnos, double caixaInicial, double aprovacaoInicial,
                double multiplicadorBeneficio, double multiplicadorCusto, double ruido,
                double multiplicadorIA, boolean mostrarNumeros) {
        this.nome = nome;
        this.totalTurnos = totalTurnos;
        this.caixaInicial = caixaInicial;
        this.aprovacaoInicial = aprovacaoInicial;
        this.multiplicadorBeneficio = multiplicadorBeneficio;
        this.multiplicadorCusto = multiplicadorCusto;
        this.ruido = ruido;
        this.multiplicadorIA = multiplicadorIA;
        this.mostrarNumeros = mostrarNumeros;
    }

    public String getNome() { return nome; }
    public int getTotalTurnos() { return totalTurnos; }
    public double getCaixaInicial() { return caixaInicial; }
    public double getAprovacaoInicial() { return aprovacaoInicial; }
    public double getMultiplicadorBeneficio() { return multiplicadorBeneficio; }
    public double getMultiplicadorCusto() { return multiplicadorCusto; }
    public double getRuido() { return ruido; }
    public double getMultiplicadorIA() { return multiplicadorIA; }
    public boolean isMostrarNumeros() { return mostrarNumeros; }

    /** Reduzir inflação é benefício; nos demais indicadores, aumentar é benefício. */
    public double ajustar(double valor, boolean inflacao) {
        boolean beneficio = inflacao ? valor < 0 : valor > 0;
        return valor * (beneficio ? multiplicadorBeneficio : multiplicadorCusto);
    }
}
