package govsim.engine;

import govsim.model.Decreto;
import govsim.model.Estado;
import govsim.model.Evento;
import govsim.model.Indicador;
import govsim.model.ReacaoAgente;

import java.util.ArrayList;
import java.util.List;

/**
 * Motor de IA Baseada em Regras para simular o comportamento e as pressões de dois
 * agentes institucionais fundamentais na Economia Política:
 * 1. OPOSIÇÃO (Parlamento, Sindicatos e Movimentos Sociais)
 * 2. MERCADO (Investidores, Sistema Financeiro e Setor Produtivo)
 */
public class AvaliadorIA {

    public static class ResultadoIA {
        private final double deltaCaixaIA;
        private final double deltaAprovacaoIA;
        private final double deltaPibIA;
        private final double deltaInflacaoIA;
        private final List<ReacaoAgente> reacoes;

        public ResultadoIA(double deltaCaixaIA, double deltaAprovacaoIA,
                           double deltaPibIA, double deltaInflacaoIA,
                           List<ReacaoAgente> reacoes) {
            this.deltaCaixaIA = Indicador.arredondar(deltaCaixaIA);
            this.deltaAprovacaoIA = Indicador.arredondar(deltaAprovacaoIA);
            this.deltaPibIA = Indicador.arredondar(deltaPibIA);
            this.deltaInflacaoIA = Indicador.arredondar(deltaInflacaoIA);
            this.reacoes = reacoes;
        }

        public double getDeltaCaixaIA() {
            return deltaCaixaIA;
        }

        public double getDeltaAprovacaoIA() {
            return deltaAprovacaoIA;
        }

        public double getDeltaPibIA() {
            return deltaPibIA;
        }

        public double getDeltaInflacaoIA() {
            return deltaInflacaoIA;
        }

        public List<ReacaoAgente> getReacoes() {
            return reacoes;
        }
    }

    /**
     * Avalia o cenário inicial da partida antes da primeira decisão.
     */
    public List<ReacaoAgente> gerarReacoesIniciais() {
        List<ReacaoAgente> lista = new ArrayList<ReacaoAgente>();
        lista.add(new ReacaoAgente(
                "OPOSICAO",
                "Oposição Parlamentar e Sociedade",
                "CAUTELA",
                "Vigilância no Início do Mandato",
                "\"Acompanharemos de perto as primeiras medidas do governo. Não aceitaremos que o peso das crises recaia sobre os trabalhadores e os serviços públicos.\"",
                "Regra inicial: A Oposição monitora o impacto social dos decretos, a inflação sobre a cesta básica e o nível de emprego (PIB).",
                "Sem penalidades no Turno 1 (Aprovação inicial em 60%)."
        ));
        lista.add(new ReacaoAgente(
                "MERCADO",
                "Mercado Financeiro e Setor Produtivo",
                "CAUTELA",
                "Expectativas Ancoradas no Início do Mandato",
                "\"Os indicadores partem em equilíbrio (Caixa em 100 pts e Inflação em 4,5%). O mercado aguarda compromisso com a solvência fiscal e o controle inflacionário.\"",
                "Regra inicial: O Mercado monitora a trajetória do Caixa Público, a ancoragem da Inflação e a coerência da taxa de juros.",
                "Sem prêmio de risco adicional no Turno 1."
        ));
        return lista;
    }

    /**
     * Avalia a decisão tomada pelo jogador e os indicadores proyectados no turno,
     * gerando reações explicadas e pequenos efeitos secundários determinísticos.
     */
    public ResultadoIA avaliarTurno(Estado estado, Evento evento, Decreto decreto,
                                    double caixaProj, double aprovacaoProj,
                                    double pibProj, double inflacaoProj, double jurosProj) {
        List<ReacaoAgente> reacoes=new ArrayList<ReacaoAgente>();
        estado.atualizarMemoria(decreto.getImpactoAprovacao()<0,caixaProj<=40 || inflacaoProj>=8);
        double paciencia=estado.getPaciencia(), confianca=estado.getConfianca();
        double aprov=0,caixa=0,pib=0,inflacao=0;
        if(aprovacaoProj<=35){aprov-=2;regra(reacoes,"OPOSICAO","Governabilidade frágil","Aprovação projetada <=35",-2);}
        if(decreto.getImpactoAprovacao()<=-8){double custo=1+(1-paciencia);aprov-=custo;regra(reacoes,"OPOSICAO","Medida impopular","Paciência acumulada: "+formatar(paciencia),-custo);}
        if(inflacaoProj>=8){aprov-=1.5;regra(reacoes,"OPOSICAO","Carestia","Inflação >=8",-1.5);}
        if(pibProj<0){aprov-=1;regra(reacoes,"OPOSICAO","Desemprego","PIB negativo",-1);}
        if(decreto.getImpactoAprovacao()>=8){aprov+=0.5;regra(reacoes,"OPOSICAO","Trégua social","Decreto popular recupera paciência",0.5);}
        if(caixaProj<=40){caixa+=ajustarMercado(-2.5,confianca,false);pib+=ajustarMercado(-0.4,confianca,false);inflacao+=ajustarMercado(0.4,confianca,true);regra(reacoes,"MERCADO","Risco fiscal","Caixa <=40; caixa -2,5; PIB -0,4; inflação +0,4",confianca);}
        if(inflacaoProj>=8 && decreto.getImpactoJuros()<=0){inflacao+=ajustarMercado(0.5,confianca,true);pib+=ajustarMercado(-0.3,confianca,false);regra(reacoes,"MERCADO","Expectativas desancoradas","Inflação >=8 sem alta de juros; inflação +0,5; PIB -0,3",confianca);}
        if(decreto.getImpactoJuros()>=1.5 && inflacaoProj>=5){inflacao+=ajustarMercado(-0.3,confianca,true);regra(reacoes,"MERCADO","Credibilidade monetária","Alta de juros com inflação >=5; inflação -0,3",confianca);}
        if(decreto.getImpactoCaixa()>=10){caixa+=ajustarMercado(1.5,confianca,false);pib+=ajustarMercado(0.3,confianca,false);regra(reacoes,"MERCADO","Disciplina fiscal","Recomposição >=10; caixa +1,5; PIB +0,3",confianca);}
        if(decreto.getImpactoCaixa()<=-18){inflacao+=ajustarMercado(0.2,confianca,true);regra(reacoes,"MERCADO","Expansão fiscal","Gasto >=18; inflação +0,2",confianca);}
        if(caixaProj>40 && inflacaoProj<8){pib+=ajustarMercado(0.2,confianca,false);regra(reacoes,"MERCADO","Previsibilidade","Caixa >40 e inflação <8; PIB +0,2",confianca);}
        return new ResultadoIA(caixa,aprov,pib,inflacao,reacoes);
    }
    /** Benefícios escalam com confiança; penalidades com seu inverso, por regra. */
    static double ajustarMercado(double efeito, double confianca, boolean inflacao) {
        boolean beneficio = inflacao ? efeito < 0 : efeito > 0;
        return efeito * (beneficio ? confianca : 1.0 / confianca);
    }

    private void regra(List<ReacaoAgente> r,String agente,String titulo,String condicao,double valor){
        r.add(new ReacaoAgente(agente,agente,"CAUTELA",titulo,"Acompanhamos a trajetória econômica.",condicao,
            "OPOSICAO".equals(agente)?"Aprovação: "+formatar(valor):"Confiança: "+formatar(valor)+"; benefícios x"+formatar(valor)+"; penalidades x"+formatar(1.0/valor)));
    }

    private String formatar(double valor) {
        return String.format(java.util.Locale.US, "%.1f", valor).replace('.', ',');
    }

    private String formatarComSinal(double valor) {
        String s = formatar(valor);
        return valor > 0 ? "+" + s : s;
    }
}
