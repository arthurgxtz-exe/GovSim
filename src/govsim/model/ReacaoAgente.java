package govsim.model;

/**
 * Representa a reação da IA baseada em regras (Oposição Parlamentar ou Mercado Financeiro)
 * às decisões tomadas pelo jogador e ao estado macroeconômico do país.
 */
public class ReacaoAgente {
    private final String agente; // "OPOSICAO" ou "MERCADO"
    private final String nomeExibicao; // "Oposição Parlamentar e Social" ou "Mercado e Agentes Econômicos"
    private final String postura; // "CRITICA", "PRESSAO", "CAUTELA", "FAVORAVEL"
    private final String titulo;
    private final String declaracao;
    private final String explicacaoRegra;
    private final String efeitoPratico;

    public ReacaoAgente(String agente, String nomeExibicao, String postura,
                        String titulo, String declaracao, String explicacaoRegra, String efeitoPratico) {
        this.agente = agente;
        this.nomeExibicao = nomeExibicao;
        this.postura = postura;
        this.titulo = titulo;
        this.declaracao = declaracao;
        this.explicacaoRegra = explicacaoRegra;
        this.efeitoPratico = efeitoPratico;
    }

    public String getAgente() {
        return agente;
    }

    public String getNomeExibicao() {
        return nomeExibicao;
    }

    public String getPostura() {
        return postura;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDeclaracao() {
        return declaracao;
    }

    public String getExplicacaoRegra() {
        return explicacaoRegra;
    }

    public String getEfeitoPratico() {
        return efeitoPratico;
    }
}
