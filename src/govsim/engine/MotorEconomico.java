package govsim.engine;

import govsim.model.Decreto;
import govsim.model.Dificuldade;
import java.util.Random;
import govsim.model.EfeitoAtrasado;
import govsim.model.Estado;
import govsim.model.Evento;
import govsim.model.HistoricoTurno;
import govsim.model.Indicador;
import govsim.model.ReacaoAgente;

import java.util.ArrayList;
import java.util.List;

/**
 * Motor Econômico configurável e didático do GovSim.
 * Calcula os efeitos diretos dos decretos, a maturação de efeitos atrasados,
 * as interações endógenas entre indicadores (Fiscal, Monetário, Demanda, Poder de Compra),
 * as reações da IA baseada em regras e verifica as condições de vitória e derrota.
 */
public class MotorEconomico {
    private final BancoEventos bancoEventos;
    private final AvaliadorIA avaliadorIA;

    private final Random random;
    public MotorEconomico(){this(new Random());}
    public MotorEconomico(Random random) {
        this.random=random;
        this.bancoEventos = new BancoEventos(random);
        this.avaliadorIA = new AvaliadorIA();
    }

    /**
     * Inicializa ou reinicia uma partida configurando os valores iniciais,
     * as reações iniciais da IA e sorteando o primeiro evento de crise.
     */
    public void iniciarPartida(Estado estado, boolean modoDemonstracao, int totalTurnos) {
        iniciarPartida(estado,modoDemonstracao,totalTurnos,Dificuldade.FACIL);
    }
    public void iniciarPartida(Estado estado,boolean modoDemonstracao,int totalTurnos,Dificuldade dificuldade){
        if(totalTurnos>bancoEventos.getCatalogoEventos().size())throw new IllegalArgumentException("Turnos excedem o catálogo sem repetição");
        estado.iniciarNovaPartida(modoDemonstracao,totalTurnos,dificuldade);
        if(!modoDemonstracao)estado.sortearPosicoes(random,bancoEventos.getCatalogoEventos());
        estado.atualizarReacoes(avaliadorIA.gerarReacoesIniciais());
        Evento primeiroEvento = bancoEventos.selecionarProximoEvento(estado);
        estado.registrarEventoSorteado(primeiroEvento);
    }

    /**
     * Processa a escolha de um decreto ("A" ou "B") pelo jogador no turno atual.
     */
    public HistoricoTurno processarDecisao(Estado estado, String codigoDecreto) {
        if (estado.isPartidaEncerrada()) {
            throw new IllegalStateException("A partida já foi encerrada. Inicie um novo mandato.");
        }

        Evento eventoAtual = estado.getEventoAtual();
        if (eventoAtual == null) {
            throw new IllegalStateException("Nenhum evento ativo no turno atual.");
        }

        Decreto decreto = eventoAtual.getDecreto(codigoDecreto);
        if (decreto == null) {
            throw new IllegalArgumentException("Código de decreto inválido: " + codigoDecreto);
        }

        Dificuldade dif = estado.getDificuldade();
        int turno = estado.getTurnoAtual();

        // Valores antes da decisão
        double caixaAntes = estado.getCaixaPublico().getValorAtual();
        double aprovacaoAntes = estado.getAprovacaoPopular().getValorAtual();
        double pibAntes = estado.getPib().getValorAtual();
        double inflacaoAntes = estado.getInflacao().getValorAtual();
        double jurosAntes = estado.getTaxaJuros();

        List<String> detalhesCalculo = new ArrayList<String>();
        List<String> descricoesAtrasadosAtivados = new ArrayList<String>();
        List<String> descricoesAtrasadosAgendados = new ArrayList<String>();

        // =========================================================================
        // ETAPA 1: MATURAÇÃO DOS EFEITOS ATRASADOS PENDENTES DE TURNOS ANTERIORES
        // =========================================================================
        double deltaCaixaAtrasado = 0.0;
        double deltaAprovacaoAtrasado = 0.0;
        double deltaPibAtrasado = 0.0;
        double deltaInflacaoAtrasado = 0.0;

        List<EfeitoAtrasado> efeitosAtivadosAgora = new ArrayList<EfeitoAtrasado>();
        for (EfeitoAtrasado ep : estado.getEfeitosPendentes()) {
            ep.avancarTurno();
            if (ep.deveAplicarAgora()) {
                efeitosAtivadosAgora.add(ep);
                deltaCaixaAtrasado += ep.getDeltaCaixa();
                deltaAprovacaoAtrasado += ep.getDeltaAprovacao();
                deltaPibAtrasado += ep.getDeltaPib();
                deltaInflacaoAtrasado += ep.getDeltaInflacao();

                String resumoEp = "Maturação do Turno " + ep.getTurnoOrigem() + " (" + ep.getDecretoOrigem() + "): "
                        + ep.getDescricao() + " [" + formatarImpactos(ep.getDeltaCaixa(), ep.getDeltaAprovacao(), ep.getDeltaPib(), ep.getDeltaInflacao()) + "]";
                descricoesAtrasadosAtivados.add(resumoEp);
            }
        }
        estado.removerEfeitosPendentes(efeitosAtivadosAgora);

        // =========================================================================
        // ETAPA 2: IMPACTO DIRETO IMEDIATO DO DECRETO ESCOLHIDO
        // =========================================================================
        double deltaCaixaDireto = ajustar(estado,decreto.getImpactoCaixa(),false);
        double deltaAprovacaoDireto = ajustar(estado,decreto.getImpactoAprovacao(),false);
        double deltaPibDireto = ajustar(estado,decreto.getImpactoPib(),false);
        double deltaInflacaoDireto = ajustar(estado,decreto.getImpactoInflacao(),true);
        double deltaJurosDireto = decreto.getImpactoJuros();

        estado.definirTaxaJuros(jurosAntes + deltaJurosDireto);
        double jurosDepois = estado.getTaxaJuros();

        detalhesCalculo.add("Efeito Direto do " + decreto.getTitulo() + ": "
                + formatarImpactos(deltaCaixaDireto, deltaAprovacaoDireto, deltaPibDireto, deltaInflacaoDireto)
                + (deltaJurosDireto != 0 ? " | Taxa de Juros: " + formatarComSinal(deltaJurosDireto) + "% a.a." : ""));

        if (!descricoesAtrasadosAtivados.isEmpty()) {
            detalhesCalculo.add("Efeitos Atrasados Maturados neste turno: "
                    + formatarImpactos(deltaCaixaAtrasado, deltaAprovacaoAtrasado, deltaPibAtrasado, deltaInflacaoAtrasado));
        }

        // Valores intermediários para o cálculo das relações endógenas entre indicadores
        double reversaoPib=0.3*(2.5-pibAntes);
        double reversaoInflacao=0.18*(4-inflacaoAntes);
        detalhesCalculo.add("Reversão PIB ao potencial de 2,5%: " + formatarComSinal(reversaoPib) + "; inflação conserva 82% do desvio da meta de 4%: " + formatarComSinal(reversaoInflacao));
        double pibInter = pibAntes + reversaoPib + deltaPibDireto + deltaPibAtrasado;
        double inflacaoInter = Math.max(0.0, inflacaoAntes + reversaoInflacao + deltaInflacaoDireto + deltaInflacaoAtrasado);

        // =========================================================================
        // ETAPA 3: RELAÇÕES MACROECONÔMICAS ENDÓGENAS (INTERAÇÃO ENTRE INDICADORES)
        // =========================================================================
        double deltaCaixaEndogeno = 0.0;
        double deltaAprovacaoEndogeno = 0.0;
        double deltaPibEndogeno = reversaoPib;
        double deltaInflacaoEndogeno = reversaoInflacao;

        // 3.1. Ciclo Econômico -> Arrecadação Tributária (PIB afeta Caixa Público)
        if (pibInter > 3.0) {
            double bonusArrecadacao = Indicador.arredondar((pibInter - 3.0) * 0.8);
            deltaCaixaEndogeno += bonusArrecadacao;
            detalhesCalculo.add("Interação PIB -> Caixa (Bônus de Arrecadação por crescimento > 3,0%): +" + formatar(bonusArrecadacao) + " pts no Caixa.");
        } else if (pibInter < 0.0) {
            double perdaArrecadacao = Indicador.arredondar(Math.abs(pibInter) * 1.2);
            deltaCaixaEndogeno -= perdaArrecadacao;
            detalhesCalculo.add("Interação PIB -> Caixa (Queda de Arrecadação por recessão): -" + formatar(perdaArrecadacao) + " pts no Caixa.");
        }

        // 3.2. Custo da Dívida Pública (Juros afetam Caixa Público)
        if (jurosDepois > 12.0) {
            double custoDivida = Indicador.arredondar((jurosDepois - 12.0) * 0.7);
            deltaCaixaEndogeno -= custoDivida;
            detalhesCalculo.add("Interação Juros -> Caixa (Custo de rolagem da dívida com Selic em " + formatar(jurosDepois) + "%): -" + formatar(custoDivida) + " pts no Caixa.");
        } else if (jurosDepois < 9.0) {
            double economiaDivida = Indicador.arredondar((9.0 - jurosDepois) * 0.5);
            deltaCaixaEndogeno += economiaDivida;
            detalhesCalculo.add("Interação Juros -> Caixa (Economia no serviço da dívida com Selic em " + formatar(jurosDepois) + "%): +" + formatar(economiaDivida) + " pts no Caixa.");
        }

        // 3.3. Curva de Phillips / Hiato do Produto (Demanda/PIB afeta Inflação)
        if (pibInter > 3.8) {
            double pressaoDemanda = Indicador.arredondar((pibInter - 3.8) * 0.35);
            deltaInflacaoEndogeno += pressaoDemanda;
            detalhesCalculo.add("Interação PIB -> Inflação (Pressão de demanda com PIB em " + formatar(pibInter) + "%): +" + formatar(pressaoDemanda) + "% na Inflação.");
        } else if (pibInter < -1.0) {
            double desinflacaoRecessiva = Indicador.arredondar((Math.abs(pibInter) - 1.0) * 0.30);
            deltaInflacaoEndogeno -= desinflacaoRecessiva;
            detalhesCalculo.add("Interação PIB -> Inflação (Desaceleração de preços por retração da demanda): -" + formatar(desinflacaoRecessiva) + "% na Inflação.");
        }

        double inflacaoPosDemanda = Math.max(0.0, inflacaoInter + deltaInflacaoEndogeno - reversaoInflacao);

        // 3.4. Poder de Compra (Inflação afeta Aprovação Popular)
        if (inflacaoPosDemanda > 6.0) {
            double desgasteInflacao = Indicador.arredondar((inflacaoPosDemanda - 6.0) * 0.75);
            deltaAprovacaoEndogeno -= desgasteInflacao;
            detalhesCalculo.add("Interação Inflação -> Aprovação (Perda do poder de compra com inflação em " + formatar(inflacaoPosDemanda) + "%): -" + formatar(desgasteInflacao) + "% na Aprovação.");
        } else if (inflacaoPosDemanda >= 2.5 && inflacaoPosDemanda <= 4.5) {
            deltaAprovacaoEndogeno += 1.0;
            detalhesCalculo.add("Interação Inflação -> Aprovação (Estabilidade do poder de compra na meta): +1,0% na Aprovação.");
        }

        // 3.5. Nível de Emprego e Renda (PIB afeta Aprovação Popular)
        if (pibInter >= 2.5) {
            double bonusEmprego = Indicador.arredondar((pibInter - 2.0) * 0.5);
            deltaAprovacaoEndogeno += bonusEmprego;
            detalhesCalculo.add("Interação PIB -> Aprovação (Geração de empregos e renda): +" + formatar(bonusEmprego) + "% na Aprovação.");
        } else if (pibInter < 0.0) {
            double penalidadeDesemprego = Indicador.arredondar(Math.abs(pibInter) * 1.0);
            deltaAprovacaoEndogeno -= penalidadeDesemprego;
            detalhesCalculo.add("Interação PIB -> Aprovação (Desgaste social por recessão e desemprego): -" + formatar(penalidadeDesemprego) + "% na Aprovação.");
        }

        // Projeção antes da reação da IA
        double caixaProj = caixaAntes + deltaCaixaDireto + deltaCaixaAtrasado + deltaCaixaEndogeno;
        double aprovacaoProj = aprovacaoAntes + deltaAprovacaoDireto + deltaAprovacaoAtrasado + deltaAprovacaoEndogeno;
        double pibProj = pibInter;
        double inflacaoProj = inflacaoPosDemanda;

        // =========================================================================
        // ETAPA 4: AVALIAÇÃO DA IA BASEADA EM REGRAS (OPOSIÇÃO E MERCADO)
        // =========================================================================
        AvaliadorIA.ResultadoIA resultadoIA = avaliadorIA.avaliarTurno(
                estado, eventoAtual, decreto, caixaProj, aprovacaoProj, pibProj, inflacaoProj, jurosDepois
        );
        estado.atualizarReacoes(resultadoIA.getReacoes());

        if ((resultadoIA.getDeltaCaixaIA() * dif.getMultiplicadorIA()) != 0 || (resultadoIA.getDeltaAprovacaoIA() * dif.getMultiplicadorIA()) != 0
                || (resultadoIA.getDeltaPibIA() * dif.getMultiplicadorIA()) != 0 || (resultadoIA.getDeltaInflacaoIA() * dif.getMultiplicadorIA()) != 0) {
            detalhesCalculo.add("Efeito das Reações da IA (Oposição e Mercado): "
                    + formatarImpactos((resultadoIA.getDeltaCaixaIA() * dif.getMultiplicadorIA()), (resultadoIA.getDeltaAprovacaoIA() * dif.getMultiplicadorIA()),
                    (resultadoIA.getDeltaPibIA() * dif.getMultiplicadorIA()), (resultadoIA.getDeltaInflacaoIA() * dif.getMultiplicadorIA())));
        }

        // =========================================================================
        // ETAPA 5: APLICAÇÃO TOTAL NOS QUATRO INDICADORES (COM LIMITES)
        // =========================================================================
        double totalDeltaCaixa = deltaCaixaDireto + deltaCaixaAtrasado + deltaCaixaEndogeno + (resultadoIA.getDeltaCaixaIA() * dif.getMultiplicadorIA());
        double totalDeltaAprovacao = deltaAprovacaoDireto + deltaAprovacaoAtrasado + deltaAprovacaoEndogeno + (resultadoIA.getDeltaAprovacaoIA() * dif.getMultiplicadorIA());
        double totalDeltaPib = deltaPibDireto + deltaPibAtrasado + deltaPibEndogeno + (resultadoIA.getDeltaPibIA() * dif.getMultiplicadorIA());
        double totalDeltaInflacao = deltaInflacaoDireto + deltaInflacaoAtrasado + deltaInflacaoEndogeno + (resultadoIA.getDeltaInflacaoIA() * dif.getMultiplicadorIA());

        estado.getCaixaPublico().aplicarVariacao(totalDeltaCaixa);
        estado.getAprovacaoPopular().aplicarVariacao(totalDeltaAprovacao);
        estado.getPib().aplicarVariacao(totalDeltaPib);
        estado.getInflacao().aplicarVariacao(totalDeltaInflacao);

        double caixaDepois = estado.getCaixaPublico().getValorAtual();
        double aprovacaoDepois = estado.getAprovacaoPopular().getValorAtual();
        double pibDepois = estado.getPib().getValorAtual();
        double inflacaoDepois = estado.getInflacao().getValorAtual();

        // =========================================================================
        // ETAPA 6: AGENDAMENTO DE NOVOS EFEITOS ATRASADOS PARA OS PRÓXIMOS TURNOS
        // =========================================================================
        for (EfeitoAtrasado modeloEf : decreto.getEfeitosAtrasados()) {
            EfeitoAtrasado novoEf = new EfeitoAtrasado(turno,modeloEf.getTurnosRestantes(),modeloEf.getDecretoOrigem(),modeloEf.getDescricao(),
                ajustar(estado,modeloEf.getDeltaCaixa(),false),ajustar(estado,modeloEf.getDeltaAprovacao(),false),
                ajustar(estado,modeloEf.getDeltaPib(),false),ajustar(estado,modeloEf.getDeltaInflacao(),true));
            estado.adicionarEfeitoPendente(novoEf);
            String descAgendado = "Programado para o Turno " + novoEf.getTurnoAplicacao()
                    + " (daqui a " + novoEf.getTurnosRestantes() + " turno(s)): " + novoEf.getDescricao()
                    + " [" + formatarImpactos(novoEf.getDeltaCaixa(), novoEf.getDeltaAprovacao(), novoEf.getDeltaPib(), novoEf.getDeltaInflacao()) + "]";
            descricoesAtrasadosAgendados.add(descAgendado);
        }

        // =========================================================================
        // ETAPA 7: GERAÇÃO DAS EXPLICAÇÕES DIDÁTICAS DO TURNO
        // =========================================================================
        double hiatoJuros=jurosDepois-10.5;
        if(Math.abs(hiatoJuros)>0.01){
            for(int atraso=1;atraso<=2;atraso++){
                EfeitoAtrasado ef=new EfeitoAtrasado(turno,atraso,"Canal Selic","Crédito e demanda: Selic frente ao neutro de 10,5% (T+"+atraso+")",0,0,
                    ajustar(estado,-0.08*hiatoJuros,false),ajustar(estado,-0.10*hiatoJuros,true));
                estado.adicionarEfeitoPendente(ef);
                String desc=ef.getDescricao()+" ["+formatarImpactos(0,0,ef.getDeltaPib(),ef.getDeltaInflacao())+"]";
                detalhesCalculo.add(desc); descricoesAtrasadosAgendados.add(desc);
            }
        }
        String oQueMudou = gerarExplicacaoOQueMudou(
                caixaAntes, caixaDepois, aprovacaoAntes, aprovacaoDepois,
                pibAntes, pibDepois, inflacaoAntes, inflacaoDepois, jurosAntes, jurosDepois
        );
        String porQueMudou = gerarExplicacaoPorQueMudou(decreto, descricoesAtrasadosAtivados, deltaCaixaEndogeno, deltaInflacaoEndogeno, deltaAprovacaoEndogeno);
        String custoDecisao = gerarExplicacaoCustoTradeOff(decreto, caixaDepois - caixaAntes, aprovacaoDepois - aprovacaoAntes, pibDepois - pibAntes, inflacaoDepois - inflacaoAntes);
        String consequenciasFuturas = gerarExplicacaoConsequenciasFuturas(estado, descricoesAtrasadosAgendados, caixaDepois, aprovacaoDepois, inflacaoDepois);
        String sinteseDidatica = gerarSinteseDidatica(eventoAtual, decreto, caixaDepois - caixaAntes, aprovacaoDepois - aprovacaoAntes, pibDepois - pibAntes, inflacaoDepois - inflacaoAntes, descricoesAtrasadosAgendados);

        HistoricoTurno registro = new HistoricoTurno(
                turno,
                eventoAtual.getId(),
                eventoAtual.getNome(),
                eventoAtual.getCategoria(),
                decreto.getCodigo(),
                decreto.getTitulo(),
                decreto.getTipoPolitica(),
                caixaAntes, caixaDepois,
                aprovacaoAntes, aprovacaoDepois,
                pibAntes, pibDepois,
                inflacaoAntes, inflacaoDepois,
                jurosAntes, jurosDepois,
                oQueMudou, porQueMudou, custoDecisao, consequenciasFuturas, sinteseDidatica,
                detalhesCalculo,
                descricoesAtrasadosAtivados,
                descricoesAtrasadosAgendados,
                resultadoIA.getReacoes()
        );
        estado.adicionarHistorico(registro);

        // =========================================================================
        // ETAPA 8: VERIFICAÇÃO DE CONDIÇÕES DE VITÓRIA E DERROTA OU AVANÇO DE TURNO
        // =========================================================================
        verificarCondicoesFimDeJogoOuAvancar(estado);

        return registro;
    }

    private void verificarCondicoesFimDeJogoOuAvancar(Estado estado) {
        double caixa = estado.getCaixaPublico().getValorAtual();
        double aprovacao = estado.getAprovacaoPopular().getValorAtual();
        int turno = estado.getTurnoAtual();
        int totalTurnos = estado.getTotalTurnos();

        if (caixa <= 0.0 && aprovacao <= 0.0) {
            estado.encerrarPartida(
                    false,
                    "DERROTA_AMBOS",
                    "Derrota: Colapso Fiscal e Queda de Governabilidade",
                    "Seu governo esgotou simultaneamente o Caixa Público (0,0 pts) e a Aprovação Popular (0,0%) no Turno " + turno + ".",
                    gerarAvaliacaoFinal(estado, false)
            );
            return;
        }

        if (caixa <= 0.0) {
            estado.encerrarPartida(
                    false,
                    "DERROTA_CAIXA",
                    "Derrota por Insolvência Fiscal (Caixa Público Zerado)",
                    "O Caixa Público chegou a 0,0 no Turno " + turno + ". Sem recursos para honrar compromissos básicos e a dívida pública, o Estado entrou em moratória e o mandato foi interrompido.",
                    gerarAvaliacaoFinal(estado, false)
            );
            return;
        }

        if (aprovacao <= 0.0) {
            estado.encerrarPartida(
                    false,
                    "DERROTA_APROVACAO",
                    "Derrota Política (Aprovação Popular Zerada)",
                    "A Aprovação Popular chegou a 0,0% no Turno " + turno + ". Sem apoio social nem base parlamentar diante do desgaste acumulado, o governo sofreu impedimento antes do fim do mandato.",
                    gerarAvaliacaoFinal(estado, false)
            );
            return;
        }

        estado.registrarAtividade();
        if(estado.getDificuldade()!=Dificuldade.FACIL){
            if(estado.getInflacao().getValorAtual()>=25){
                estado.encerrarPartida(false,"DERROTA_HIPERINFLACAO","Derrota: Hiperinflação","Inflação atingiu 25%: contratos e poder de compra entram em colapso.","Lição Econômica: estímulos precisam respeitar a capacidade produtiva e a estabilidade dos preços.");return;
            }
            if(estado.getTurnosDepressao()>=2){
                estado.encerrarPartida(false,"DERROTA_DEPRESSAO","Derrota: Depressão Econômica","PIB abaixo de -3% por dois turnos: falências e desemprego interrompem o mandato.","Lição Econômica: restrição prolongada da demanda compromete emprego, renda e arrecadação.");return;
            }
        }
        if (turno >= totalTurnos) {
            estado.encerrarPartida(
                    true,
                    "VITORIA",
                    "Vitória: Mandato Presidencial Concluído com Sucesso!",
                    "Você completou todos os " + totalTurnos + " turnos do mandato mantendo o Caixa Público (" + formatar(caixa) + " pts) e a Aprovação Popular (" + formatar(aprovacao) + "%) acima de zero!",
                    gerarAvaliacaoFinal(estado, true)
            );
            return;
        }

        // Partida continua para o próximo turno
        estado.setTurnoAtual(turno + 1);
        Evento proximoEvento = bancoEventos.selecionarProximoEvento(estado);
        estado.registrarEventoSorteado(proximoEvento);
    }

    private String gerarExplicacaoOQueMudou(double caixaA, double caixaD, double aprovA, double aprovD,
                                            double pibA, double pibD, double infA, double infD,
                                            double jurosA, double jurosD) {
        StringBuilder sb = new StringBuilder();
        sb.append("Caixa Público passou de ").append(formatar(caixaA)).append(" para ").append(formatar(caixaD))
                .append(" pts (").append(formatarComSinal(caixaD - caixaA)).append("); ");
        sb.append("Aprovação Popular foi de ").append(formatar(aprovA)).append("% para ").append(formatar(aprovD))
                .append("% (").append(formatarComSinal(aprovD - aprovA)).append("%); ");
        sb.append("PIB variou de ").append(formatar(pibA)).append("% para ").append(formatar(pibD))
                .append("% a.a. (").append(formatarComSinal(pibD - pibA)).append("%); ");
        sb.append("e a Inflação foi de ").append(formatar(infA)).append("% para ").append(formatar(infD))
                .append("% a.a. (").append(formatarComSinal(infD - infA)).append("%).");
        if (Math.abs(jurosD - jurosA) >= 0.1) {
            sb.append(" A Taxa Básica de Juros (Selic) foi ajustada de ").append(formatar(jurosA))
                    .append("% para ").append(formatar(jurosD)).append("% a.a.");
        }
        return sb.toString();
    }

    private String gerarExplicacaoPorQueMudou(Decreto decreto, List<String> atrasadosAtivados,
                                              double deltaCaixaEndo, double deltaInfEndo, double deltaAprovEndo) {
        StringBuilder sb = new StringBuilder();
        sb.append(decreto.getExplicacaoTeorica());
        if (!atrasadosAtivados.isEmpty()) {
            sb.append(" Além disso, neste turno maturaram efeitos atrasados de decisões anteriores (")
                    .append(atrasadosAtivados.size()).append(" efeito(s) defasado(s) entrou/entraram em vigor).");
        }
        if (Math.abs(deltaCaixaEndo) >= 0.2 || Math.abs(deltaInfEndo) >= 0.2 || Math.abs(deltaAprovEndo) >= 0.2) {
            sb.append(" O motor econômico também contabilizou interações endógenas entre os indicadores (efeito do PIB sobre a arrecadação, do nível de preços sobre a popularidade e dos juros sobre a dívida).");
        }
        return sb.toString();
    }

    private String gerarExplicacaoCustoTradeOff(Decreto decreto, double dCaixa, double dAprov, double dPib, double dInf) {
        List<String> ganhos = new ArrayList<String>();
        List<String> custos = new ArrayList<String>();

        if (dCaixa > 0) ganhos.add("fortalecimento do Caixa Público (" + formatarComSinal(dCaixa) + " pts)");
        else if (dCaixa < 0) custos.add("redução do Caixa Público (" + formatarComSinal(dCaixa) + " pts)");

        if (dAprov > 0) ganhos.add("ganho de Aprovação Popular (" + formatarComSinal(dAprov) + "%)");
        else if (dAprov < 0) custos.add("desgaste na Aprovação Popular (" + formatarComSinal(dAprov) + "%)");

        if (dPib > 0) ganhos.add("expansão do PIB (" + formatarComSinal(dPib) + "% a.a.)");
        else if (dPib < 0) custos.add("desaceleração do PIB (" + formatarComSinal(dPib) + "% a.a.)");

        if (dInf < 0) ganhos.add("alívio na Inflação (" + formatarComSinal(dInf) + "% a.a.)");
        else if (dInf > 0) custos.add("maior pressão sobre a Inflação (" + formatarComSinal(dInf) + "% a.a.)");

        StringBuilder sb = new StringBuilder();
        sb.append("Classificação da medida: ").append(decreto.getTipoPolitica()).append(". ");
        if (!ganhos.isEmpty() && !custos.isEmpty()) {
            sb.append("O benefício obtido com ").append(juntarLista(ganhos))
                    .append(" teve como custo econômico direto ").append(juntarLista(custos)).append(".");
        } else if (!custos.isEmpty()) {
            sb.append("A decisão impôs custos imediatos em ").append(juntarLista(custos))
                    .append(", visando conter riscos estruturais nos turnos seguintes.");
        } else {
            sb.append("A conjuntura permitiu resultados favoráveis no curto prazo, mas exige vigilância sobre os compromissos futuros.");
        }
        return sb.toString();
    }

    private String gerarExplicacaoConsequenciasFuturas(Estado estado, List<String> agendadosAgora,
                                                       double caixa, double aprovacao, double inflacao) {
        StringBuilder sb = new StringBuilder();
        if (!agendadosAgora.isEmpty()) {
            sb.append("A decisão deste turno programou ").append(agendadosAgora.size())
                    .append(" efeito(s) atrasado(s): ").append(String.join("; ", agendadosAgora)).append(". ");
        } else {
            sb.append("Nenhum novo efeito defasado adicional foi agendado por este decreto específico. ");
        }

        if (caixa <= 35.0) {
            sb.append("ATENÇÃO FISCAL: Com o Caixa Público em ").append(formatar(caixa))
                    .append(" pts, novos gastos elevados podem levar o governo à insolvência (Derrota). ");
        }
        if (aprovacao <= 30.0) {
            sb.append("ATENÇÃO POLÍTICA: A Aprovação Popular está em nível perigoso (").append(formatar(aprovacao))
                    .append("%); medidas impopulares ou inflação alta podem zerar sua governabilidade. ");
        }
        if (inflacao >= 7.5) {
            sb.append("ALERTA INFLACIONÁRIO: A inflação em ").append(formatar(inflacao))
                    .append("% a.a. continuará corroendo a aprovação popular a cada turno se não for combatida.");
        }
        return sb.toString().trim();
    }

    private String gerarSinteseDidatica(Evento evento, Decreto decreto,
                                        double dCaixa, double dAprov, double dPib, double dInf,
                                        List<String> agendadosAgora) {
        StringBuilder sb = new StringBuilder();
        sb.append("Diante de “").append(evento.getNome()).append("”, seu governo decidiu: ")
                .append(decreto.getTitulo()).append(" (").append(decreto.getTipoPolitica()).append("). ");

        if (dCaixa < -8.0 && (dPib > 0 || dAprov > 0)) {
            sb.append("Seu governo aumentou o esforço fiscal para sustentar a atividade econômica e o bem-estar social, o que impulsionou ")
                    .append(dPib > 0 ? "o PIB (" + formatarComSinal(dPib) + "%) " : "")
                    .append(dAprov > 0 ? "e a aprovação popular (" + formatarComSinal(dAprov) + "%)" : "")
                    .append(", mas reduziu o Caixa Público em ").append(formatar(Math.abs(dCaixa))).append(" pontos. ");
        } else if (dCaixa >= 0 && (dPib < 0 || dAprov < 0)) {
            sb.append("Seu governo priorizou a preservação do Caixa Público (").append(formatarComSinal(dCaixa))
                    .append(" pts) e a disciplina macroeconômica, mas pagou o preço de curto prazo com ")
                    .append(dAprov < 0 ? "queda na aprovação popular (" + formatarComSinal(dAprov) + "%) " : "")
                    .append(dPib < 0 ? "e retração no PIB (" + formatarComSinal(dPib) + "%). " : ". ");
        } else {
            sb.append("A medida alterou o equilíbrio macroeconômico com variação de ")
                    .append(formatarComSinal(dCaixa)).append(" pts no Caixa, ")
                    .append(formatarComSinal(dAprov)).append("% na Aprovação, ")
                    .append(formatarComSinal(dPib)).append("% no PIB e ")
                    .append(formatarComSinal(dInf)).append("% na Inflação. ");
        }

        if (!agendadosAgora.isEmpty()) {
            sb.append("Nos próximos turnos, os efeitos defasados dessa escolha continuarão repercutindo sobre a economia.");
        }
        return sb.toString();
    }

    private String gerarAvaliacaoFinal(Estado estado, boolean vitoria) {
        double caixa = estado.getCaixaPublico().getValorAtual();
        double aprovacao = estado.getAprovacaoPopular().getValorAtual();
        double pib = estado.getPib().getValorAtual();
        double inflacao = estado.getInflacao().getValorAtual();

        StringBuilder sb = new StringBuilder();
        if (vitoria) {
            sb.append("Análise do Mandato: Você demonstrou capacidade de gerenciar os trade-offs entre política fiscal e monetária ao longo dos ")
                    .append(estado.getHistorico().size()).append(" turnos. ");
            if (caixa >= 85 && inflacao <= 5.0 && pib >= 2.0 && aprovacao >= 75) {
                sb.append("Desempenho de Excelência (Estadista): O país encerra o mandato com contas públicas sólidas, inflação controlada, crescimento sustentável do PIB e elevado apoio popular.");
            } else if (inflacao > 8.0) {
                sb.append("Mandato Concluído sob Pressão Inflacionária: O governo sobreviveu e manteve o caixa positivo, mas deixou uma herança de inflação elevada (")
                        .append(formatar(inflacao)).append("% a.a.), ilustrando o custo de políticas expansionistas prolongadas.");
            } else if (pib < 0.5) {
                sb.append("Mandato Ortodoxo de Baixo Crescimento: O governo preservou a solvência fiscal e evitou o colapso, mas o aperto econômico deixou a atividade produtiva desacelerada (PIB em ")
                        .append(formatar(pib)).append("% a.a.).");
            } else {
                sb.append("Gestão Equilibrada: O governo navegou pelas crises alternando estímulos e ajustes fiscais/monetários, preservando a estabilidade institucional e macroeconômica.");
            }
        } else {
            if (caixa <= 0) {
                sb.append("Lição Econômica da Derrota Fiscal: O excesso de gastos públicos, subsídios ou juros elevados sobre a dívida sem contrapartida de arrecadação esgotou o Caixa Público. Na economia, estímulos fiscais sustentam o PIB e a popularidade no curto prazo, mas a restrição orçamentária impõe limites rígidos à solvência do Estado.");
            } else {
                sb.append("Lição Econômica da Derrota Política: A combinação de medidas impopulares de austeridade, desemprego (queda do PIB) ou inflação elevada corroeu completamente a Aprovação Popular. Na gestão pública, o ajuste econômico precisa considerar a viabilidade social e política para se sustentar.");
            }
        }
        return sb.toString();
    }

    public void iniciarPartida(Estado e, boolean demo, int turnos, Dificuldade d, Long seed){
        if(seed!=null)random.setSeed(seed.longValue());iniciarPartida(e,demo,turnos,d);
    }
    private double ajustar(Estado e,double v,boolean inf){
        double r=e.isModoDemonstracao()?0:e.getDificuldade().getRuido();
        return e.getDificuldade().ajustar(v,inf)*(1+(r==0?0:(2*random.nextDouble()-1)*r));
    }
    private String formatarImpactos(double dCaixa, double dAprov, double dPib, double dInf) {
        return "Caixa " + formatarComSinal(dCaixa)
                + " | Aprovação " + formatarComSinal(dAprov) + "%"
                + " | PIB " + formatarComSinal(dPib) + "%"
                + " | Inflação " + formatarComSinal(dInf) + "%";
    }

    private String juntarLista(List<String> itens) {
        if (itens.isEmpty()) return "";
        if (itens.size() == 1) return itens.get(0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < itens.size(); i++) {
            if (i > 0) {
                sb.append(i == itens.size() - 1 ? " e " : ", ");
            }
            sb.append(itens.get(i));
        }
        return sb.toString();
    }

    private String formatar(double valor) {
        return String.format(java.util.Locale.US, "%.1f", valor).replace('.', ',');
    }

    private String formatarComSinal(double valor) {
        double v = Indicador.arredondar(valor);
        String s = formatar(v);
        return v > 0 ? "+" + s : s;
    }
}
