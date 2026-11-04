package govsim.model;

/**
 * Representa um indicador macroeconômico ou político do país no GovSim.
 * Mantém valor numérico, limites operacionais, unidade de medida e descrição didática.
 */
public class Indicador {
    private final String codigo;
    private final String nome;
    private final String unidade;
    private final String descricao;
    private double valorInicial;
    private final double valorMinimo;
    private final double valorMaximo;
    private double valorAtual;
    private double variacaoUltimoTurno;

    public Indicador(String codigo, String nome, String unidade, String descricao,
                     double valorInicial, double valorMinimo, double valorMaximo) {
        this.codigo = codigo;
        this.nome = nome;
        this.unidade = unidade;
        this.descricao = descricao;
        this.valorInicial = arredondar(valorInicial);
        this.valorMinimo = valorMinimo;
        this.valorMaximo = valorMaximo;
        this.valorAtual = this.valorInicial;
        this.variacaoUltimoTurno = 0.0;
    }

    /**
     * Atualiza o valor do indicador aplicando limites mínimo e máximo (clamping)
     * e registra a variação real ocorrida no turno.
     */
    public void aplicarVariacao(double delta) {
        double valorAnterior = this.valorAtual;
        double novoValor = this.valorAtual + delta;
        if (novoValor < this.valorMinimo) {
            novoValor = this.valorMinimo;
        } else if (novoValor > this.valorMaximo) {
            novoValor = this.valorMaximo;
        }
        this.valorAtual = arredondar(novoValor);
        this.variacaoUltimoTurno = arredondar(this.valorAtual - valorAnterior);
    }

    public void definirValor(double novoValor) {
        double valorAnterior = this.valorAtual;
        if (novoValor < this.valorMinimo) {
            novoValor = this.valorMinimo;
        } else if (novoValor > this.valorMaximo) {
            novoValor = this.valorMaximo;
        }
        this.valorAtual = arredondar(novoValor);
        this.variacaoUltimoTurno = arredondar(this.valorAtual - valorAnterior);
    }

    /** Define a base deste mandato sem produzir uma variação de turno. */
    public void iniciar(double valorInicial) {
        this.valorInicial = arredondar(Math.max(valorMinimo, Math.min(valorMaximo, valorInicial)));
        resetar();
    }

    public void resetar() {
        this.valorAtual = this.valorInicial;
        this.variacaoUltimoTurno = 0.0;
    }

    /**
     * Retorna o percentual de preenchimento (0 a 100) para barras visuais na interface.
     */
    public double getPercentualBarra() {
        if ("PIB".equals(codigo)) {
            // Mapeia [-6%, +10%] para [0%, 100%] para melhor leitura visual
            double minVisual = -6.0;
            double maxVisual = 10.0;
            double pct = ((valorAtual - minVisual) / (maxVisual - minVisual)) * 100.0;
            return Math.max(5.0, Math.min(100.0, arredondar(pct)));
        }
        if ("INFLACAO".equals(codigo)) {
            // Mapeia [0%, 20%] para [0%, 100%]
            double pct = (valorAtual / 20.0) * 100.0;
            return Math.max(4.0, Math.min(100.0, arredondar(pct)));
        }
        if ("CAIXA".equals(codigo)) {
            // Mapeia [0, 160] para [0%, 100%]
            double pct = (valorAtual / 160.0) * 100.0;
            return Math.max(0.0, Math.min(100.0, arredondar(pct)));
        }
        double range = valorMaximo - valorMinimo;
        if (range <= 0) return 50.0;
        double pct = ((valorAtual - valorMinimo) / range) * 100.0;
        return Math.max(0.0, Math.min(100.0, arredondar(pct)));
    }

    /**
     * Classifica a situação atual do indicador para exibição didática.
     */
    public String getClassificacaoStatus() {
        if ("CAIXA".equals(codigo)) {
            if (valorAtual <= 20.0) return "CRITICO";
            if (valorAtual <= 45.0) return "ALERTA";
            if (valorAtual >= 95.0) return "FORTE";
            return "ESTAVEL";
        }
        if ("APROVACAO".equals(codigo)) {
            if (valorAtual <= 20.0) return "CRITICO";
            if (valorAtual <= 40.0) return "ALERTA";
            if (valorAtual >= 65.0) return "FORTE";
            return "ESTAVEL";
        }
        if ("PIB".equals(codigo)) {
            if (valorAtual < -1.0) return "CRITICO";
            if (valorAtual < 1.0) return "ALERTA";
            if (valorAtual >= 3.0) return "FORTE";
            return "ESTAVEL";
        }
        if ("INFLACAO".equals(codigo)) {
            // Na inflação, valores altos são críticos e valores próximos à meta (3% a 5%) são fortes/estáveis
            if (valorAtual >= 9.0) return "CRITICO";
            if (valorAtual >= 6.0 || valorAtual < 1.0) return "ALERTA";
            if (valorAtual >= 2.5 && valorAtual <= 4.5) return "FORTE";
            return "ESTAVEL";
        }
        return "ESTAVEL";
    }

    public String getRotuloStatus() {
        String status = getClassificacaoStatus();
        if ("CAIXA".equals(codigo)) {
            if ("CRITICO".equals(status)) return "Risco de Insolvência";
            if ("ALERTA".equals(status)) return "Pressão Fiscal";
            if ("FORTE".equals(status)) return "Superávit Sólido";
            return "Contas Equilibradas";
        }
        if ("APROVACAO".equals(codigo)) {
            if ("CRITICO".equals(status)) return "Crise de Governabilidade";
            if ("ALERTA".equals(status)) return "Desgaste Político";
            if ("FORTE".equals(status)) return "Amplo Apoio Popular";
            return "Apoio Moderado";
        }
        if ("PIB".equals(codigo)) {
            if ("CRITICO".equals(status)) return "Recessão Severa";
            if ("ALERTA".equals(status)) return "Desaceleração / Estagnação";
            if ("FORTE".equals(status)) return "Expansão Econômica";
            return "Crescimento Moderado";
        }
        if ("INFLACAO".equals(codigo)) {
            if ("CRITICO".equals(status)) return "Descontrole Inflacionário";
            if ("ALERTA".equals(status)) return "Acima do Teto da Meta";
            if ("FORTE".equals(status)) return "Na Meta de Inflação";
            return "Sob Controle";
        }
        return "Normal";
    }

    public static double arredondar(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public String getUnidade() {
        return unidade;
    }

    public String getDescricao() {
        return descricao;
    }

    public double getValorInicial() {
        return valorInicial;
    }

    public double getValorMinimo() {
        return valorMinimo;
    }

    public double getValorMaximo() {
        return valorMaximo;
    }

    public double getValorAtual() {
        return valorAtual;
    }

    public double getVariacaoUltimoTurno() {
        return variacaoUltimoTurno;
    }

    public void setVariacaoUltimoTurno(double variacaoUltimoTurno) {
        this.variacaoUltimoTurno = arredondar(variacaoUltimoTurno);
    }
}
