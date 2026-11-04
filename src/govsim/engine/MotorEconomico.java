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
        detalhesCalculo.add("Reversãa produção ao potencial de 2,5%: " + formatarComSinal(reversaoPib) + "; inflação conserva 82% do desvio da meta de 4%: " + formatarComSinal(reversaoInflacao));
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
            detalhesCalculo.add("Interaçãa produção -> Caixa (Bônus de Arrecadação por crescimento > 3,0%): +" + formatar(bonusArrecadacao) + " pts no Caixa.");
        } else if (pibInter < 0.0) {
            double perdaArrecadacao = Indicador.arredondar(Math.abs(pibInter) * 1.2);
            deltaCaixaEndogeno -= perdaArrecadacao;
            detalhesCalculo.add("Interaçãa produção -> Caixa (Queda de Arrecadação por queda na produção): -" + formatar(perdaArrecadacao) + " pts no Caixa.");
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
            detalhesCalculo.add("Interaçãa produção -> Inflação (Pressão de demanda com produção em " + formatar(pibInter) + "%): +" + formatar(pressaoDemanda) + "% na Inflação.");
        } else if (pibInter < -1.0) {
            double desinflacaoRecessiva = Indicador.arredondar((Math.abs(pibInter) - 1.0) * 0.30);
            deltaInflacaoEndogeno -= desinflacaoRecessiva;
            detalhesCalculo.add("Interaçãa produção -> Inflação (Desaceleração de preços por queda da demanda): -" + formatar(desinflacaoRecessiva) + "% na Inflação.");
        }

        double inflacaoPosDemanda = Math.max(0.0, inflacaoInter + deltaInflacaoEndogeno - reversaoInflacao);

        // 3.4. Poder de Compra (Inflação afeta Aprovação Popular)
        if (inflacaoPosDemanda > 6.0) {
            double desgasteInflacao = Indicador.arredondar((inflacaoPosDemanda - 6.0) * 0.75);
            deltaAprovacaoEndogeno -= desgasteInflacao;
            detalhesCalculo.add("Interação Inflação -> Aprovação (Perda do o que o dinheiro consegue comprar com inflação em " + formatar(inflacaoPosDemanda) + "%): -" + formatar(desgasteInflacao) + "% na Aprovação.");
        } else if (inflacaoPosDemanda >= 2.5 && inflacaoPosDemanda <= 4.5) {
            deltaAprovacaoEndogeno += 1.0;
            detalhesCalculo.add("Interação Inflação -> Aprovação (Estabilidade do o que o dinheiro consegue comprar na meta): +1,0% na Aprovação.");
        }

        // 3.5. Nível de Emprego e Renda (PIB afeta Aprovação Popular)
        if (pibInter >= 2.5) {
            double bonusEmprego = Indicador.arredondar((pibInter - 2.0) * 0.5);
            deltaAprovacaoEndogeno += bonusEmprego;
            detalhesCalculo.add("Interaçãa produção -> Aprovação (Geração de empregos e renda): +" + formatar(bonusEmprego) + "% na Aprovação.");
        } else if (pibInter < 0.0) {
            double penalidadeDesemprego = Indicador.arredondar(Math.abs(pibInter) * 1.0);
            deltaAprovacaoEndogeno -= penalidadeDesemprego;
            detalhesCalculo.add("Interaçãa produção -> Aprovação (Desgaste social por queda na produção e desemprego): -" + formatar(penalidadeDesemprego) + "% na Aprovação.");
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
        estado.registrarDecisaoNarrativa(eventoAtual,codigoDecreto);
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
                estado.encerrarPartida(false,"DERROTA_HIPERINFLACAO","Derrota: Hiperinflação","Inflação atingiu 25%: contratos e o que o dinheiro consegue comprar entram em colapso.","Lição Econômica: estímulos precisam respeitar a capacidade produtiva e a estabilidade dos preços.");return;
            }
            if(estado.getTurnosDepressao()>=2){
                estado.encerrarPartida(false,"DERROTA_DEPRESSAO","Derrota: Depressão Econômica","produção abaixo de -3% por dois turnos: falências e desemprego interrompem o mandato.","Lição Econômica: restrição prolongada da demanda compromete emprego, renda e dinheiro recebido em impostos.");return;
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
        sb.append("O dinheiro do governo foi de ").append(formatar(caixaA)).append(" para ").append(formatar(caixaD))
                .append(" pts (").append(formatarComSinal(caixaD - caixaA)).append("); ");
        sb.append("O apoio das pessoas foi de ").append(formatar(aprovA)).append("% para ").append(formatar(aprovD))
                .append("% (").append(formatarComSinal(aprovD - aprovA)).append("%); ");
        sb.append("produção variou de ").append(formatar(pibA)).append("% para ").append(formatar(pibD))
                .append("% a.a. (").append(formatarComSinal(pibD - pibA)).append("%); ");
        sb.append("e a alta dos preços foi de ").append(formatar(infA)).append("% para ").append(formatar(infD))
                .append("% a.a. (").append(formatarComSinal(infD - infA)).append("%).");
        if (Math.abs(jurosD - jurosA) >= 0.1) {
            sb.append(" Os juros definidos pelo Banco Central, chamados Selic, passaram de ").append(formatar(jurosA))
                    .append("% para ").append(formatar(jurosD)).append("% a.a.");
        }
        return sb.toString();
    }

    private String gerarExplicacaoPorQueMudou(Decreto decreto, List<String> atrasadosAtivados,
                                              double deltaCaixaEndo, double deltaInfEndo, double deltaAprovEndo) {
        StringBuilder sb=new StringBuilder(decreto.getExplicacaoTeorica());
        if(!atrasadosAtivados.isEmpty())sb.append(" Também chegaram resultados de ").append(atrasadosAtivados.size()).append(" decisões anteriores.");
        sb.append(" O resultado inclui a influência das vendas, dos preços e do custo das dívidas nas contas do governo.");
        return sb.toString();
    }

    private String gerarExplicacaoCustoTradeOff(Decreto decreto, double dCaixa, double dAprov, double dPib, double dInf) {
        return "Ação: "+decreto.getTipoPolitica()+". Neste turno, o dinheiro do governo mudou "+formatarComSinal(dCaixa)+" pontos, o apoio das pessoas "+formatarComSinal(dAprov)+"%, a produção "+formatarComSinal(dPib)+"% e os preços "+formatarComSinal(dInf)+"%. Esses resultados também incluem escolhas anteriores.";
    }

    private String gerarExplicacaoConsequenciasFuturas(Estado estado, List<String> agendadosAgora,
                                                       double caixa, double aprovacao, double inflacao) {
        StringBuilder sb=new StringBuilder();
        if(!agendadosAgora.isEmpty())sb.append("Esta decisão deixou ").append(agendadosAgora.size()).append(" mudanças para os próximos turnos: ").append(String.join("; ",agendadosAgora)).append(". ");
        else sb.append("Esta escolha não deixou novas mudanças próprias para depois. ");
        if(caixa<=35.0)sb.append("O governo tem pouco dinheiro: ").append(formatar(caixa)).append(" pontos; gastar muito pode esgotar a conta. ");
        if(aprovacao<=30.0)sb.append("Poucas pessoas apoiam o governo: ").append(formatar(aprovacao)).append("%; novas perdas de apoio podem encerrar o mandato. ");
        if(inflacao>=7.5)sb.append("Os preços estão subindo ").append(formatar(inflacao)).append("% ao ano; isso dificulta as compras das famílias.");
        return sb.toString().trim();
    }

    private String gerarSinteseDidatica(Evento evento, Decreto decreto,
                                        double dCaixa, double dAprov, double dPib, double dInf,
                                        List<String> agendadosAgora) {
        return "Seu governo decidiu: “"+decreto.getTitulo()+"”. Neste turno, o dinheiro do governo mudou "+formatarComSinal(dCaixa)+" pontos, o apoio das pessoas "+formatarComSinal(dAprov)+"%, a produção "+formatarComSinal(dPib)+"% e os preços "+formatarComSinal(dInf)+"%.";
    }

    private String gerarAvaliacaoFinal(Estado estado, boolean vitoria) {
        double caixa = estado.getCaixaPublico().getValorAtual();
        double aprovacao = estado.getAprovacaoPopular().getValorAtual();
        double pib = estado.getPib().getValorAtual();
        double inflacao = estado.getInflacao().getValorAtual();

        StringBuilder sb = new StringBuilder();
        if (vitoria) {
            sb.append("Balanço: você tomou decisões sobre dinheiro e ajuda às pessoas durante ")
                    .append(estado.getHistorico().size()).append(" turnos. ");
            // Apenas a avaliação muda: parâmetros econômicos e condições de vitória permanecem intactos.
            double caixaExcelencia, aprovacaoExcelencia, pibExcelencia, inflacaoExcelencia;
            switch (estado.getDificuldade()) {
                case FACIL: caixaExcelencia=30; aprovacaoExcelencia=98; pibExcelencia=5; inflacaoExcelencia=12; break;
                case MEDIO: caixaExcelencia=35; aprovacaoExcelencia=85; pibExcelencia=1; inflacaoExcelencia=11; break;
                default: caixaExcelencia=25; aprovacaoExcelencia=95; pibExcelencia=0.5; inflacaoExcelencia=8.5; break;
            }
            if (caixa >= caixaExcelencia && inflacao <= inflacaoExcelencia && pib >= pibExcelencia && aprovacao >= aprovacaoExcelencia) {
                sb.append("Desempenho de Excelência (Estadista): O governo terminou com dinheiro guardado, muito apoio das pessoas e produção suficiente para os critérios desta dificuldade. Isso não significa que todos os problemas do país foram resolvidos.");
            } else if (caixa >= 20 && aprovacao >= 35 && pib >= 0.5 && inflacao <= inflacaoExcelencia) {
                sb.append("Gestão Equilibrada: o governo terminou com dinheiro guardado, apoio das pessoas e produção em crescimento. Ainda faltaram alguns critérios para Estadista.");
            } else if (inflacao > inflacaoExcelencia) {
                sb.append("Mandato Concluído sob Pressão Inflacionária: os preços subiram muito e dificultaram as compras das famílias (")
                        .append(formatar(inflacao)).append("% ao ano). O governo chegou ao fim, mas deixou esse problema para resolver.");
            } else if (pib < 0.5) {
                sb.append("Mandato de Baixo Crescimento: o governo conseguiu pagar suas contas, mas a produção cresceu pouco ou caiu (")
                        .append(formatar(pib)).append("% a.a.).");
            } else {
                sb.append("Mandato Concluído com Reservas Frágeis: sobrou pouco dinheiro ou pouco apoio das pessoas. O próximo governo terá menos espaço para agir.");
            }
        } else {
            if (caixa <= 0) {
                sb.append("Lição da Derrota Fiscal: o dinheiro do governo acabou antes do fim. Ajudar as pessoas e pagar dívidas exige pensar também em como cobrir as contas.");
            } else {
                sb.append("Lição do mandato interrompido: o governo não manteve as condições necessárias para continuar. Veja os problemas destacados na tela e as escolhas que contribuíram para o resultado.");
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
