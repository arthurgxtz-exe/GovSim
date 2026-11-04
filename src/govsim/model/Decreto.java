package govsim.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa uma opção de decisão governamental (Decreto A ou Decreto B)
 * diante de um evento de crise econômica ou política.
 */
public class Decreto {
    private String emPoucasPalavras="";
    public Decreto comResumo(String resumo){emPoucasPalavras=resumo;return this;}
    public String getEmPoucasPalavras(){return emPoucasPalavras.isEmpty()?descricao:emPoucasPalavras;}
    private final String codigo; // "A" ou "B"
    private final String titulo;
    private final String tipoPolitica; // ex.: "Política Fiscal Expansionista", "Política Monetária Contracionista"
    private final String descricao;
    private final List<String> consequenciasEsperadas;
    private final String explicacaoTeorica;

    // Impactos imediatos diretos no turno atual
    private final double impactoCaixa;
    private final double impactoAprovacao;
    private final double impactoPib;
    private final double impactoInflacao;
    private final double impactoJuros;

    // Efeitos atrasados programados para os próximos turnos
    private final List<EfeitoAtrasado> efeitosAtrasados;

    public Decreto(String codigo, String titulo, String tipoPolitica, String descricao,
                   List<String> consequenciasEsperadas, String explicacaoTeorica,
                   double impactoCaixa, double impactoAprovacao, double impactoPib,
                   double impactoInflacao, double impactoJuros,
                   List<EfeitoAtrasado> efeitosAtrasados) {
        this.codigo = codigo;
        this.titulo = titulo;
        this.tipoPolitica = tipoPolitica;
        this.descricao = descricao;
        this.consequenciasEsperadas = consequenciasEsperadas != null
                ? new ArrayList<String>(consequenciasEsperadas)
                : new ArrayList<String>();
        this.explicacaoTeorica = explicacaoTeorica;
        this.impactoCaixa = Indicador.arredondar(impactoCaixa);
        this.impactoAprovacao = Indicador.arredondar(impactoAprovacao);
        this.impactoPib = Indicador.arredondar(impactoPib);
        this.impactoInflacao = Indicador.arredondar(impactoInflacao);
        this.impactoJuros = Indicador.arredondar(impactoJuros);
        this.efeitosAtrasados = efeitosAtrasados != null
                ? new ArrayList<EfeitoAtrasado>(efeitosAtrasados)
                : new ArrayList<EfeitoAtrasado>();
    }

    public Decreto comCodigo(String codigo) {
        return new Decreto(codigo,titulo,tipoPolitica,descricao,consequenciasEsperadas,explicacaoTeorica,impactoCaixa,impactoAprovacao,impactoPib,impactoInflacao,impactoJuros,efeitosAtrasados).comResumo(getEmPoucasPalavras());
    }
    public String getCodigo() {
        return codigo;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getTipoPolitica() {
        return tipoPolitica;
    }

    public String getDescricao() {
        return descricao;
    }

    public List<String> getConsequenciasEsperadas() {
        return Collections.unmodifiableList(consequenciasEsperadas);
    }

    public String getExplicacaoTeorica() {
        return explicacaoTeorica;
    }

    public double getImpactoCaixa() {
        return impactoCaixa;
    }

    public double getImpactoAprovacao() {
        return impactoAprovacao;
    }

    public double getImpactoPib() {
        return impactoPib;
    }

    public double getImpactoInflacao() {
        return impactoInflacao;
    }

    public double getImpactoJuros() {
        return impactoJuros;
    }

    public List<EfeitoAtrasado> getEfeitosAtrasados() {
        return Collections.unmodifiableList(efeitosAtrasados);
    }
}
