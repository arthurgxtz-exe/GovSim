package govsim.server;

import govsim.model.Decreto;
import govsim.model.EfeitoAtrasado;
import govsim.model.Estado;
import govsim.model.Evento;
import govsim.model.HistoricoTurno;
import govsim.model.Indicador;
import govsim.model.ReacaoAgente;

import java.util.List;
import java.util.Locale;

/**
 * Utilitário de serialização e leitura JSON em Java puro (JDK padrão, sem bibliotecas externas).
 */
public class JsonUtil {
    public static Object lerJson(String texto) { return new Leitor(texto).lerDocumento(); }
    private static final class Leitor {
        private final String s; private int i;
        Leitor(String s) { this.s=s; }
        void espacos(){while(i<s.length() && Character.isWhitespace(s.charAt(i)))i++;}
        Object lerDocumento(){Object v=valor();espacos();if(i!=s.length())throw new IllegalArgumentException("JSON excedente");return v;}
        char consumir(){if(i>=s.length())throw new IllegalArgumentException("JSON incompleto");return s.charAt(i++);}
        Object valor(){espacos();if(i>=s.length())throw new IllegalArgumentException("JSON vazio");char c=s.charAt(i);
            if(c=='"')return texto();
            if(c=='{' || c=='['){i++;boolean objeto=c=='{';java.util.Map<String,Object> m=new java.util.LinkedHashMap<String,Object>();java.util.List<Object> l=new java.util.ArrayList<Object>();espacos();char fim=objeto?'}':']';if(i<s.length()&&s.charAt(i)==fim){i++;return objeto?m:l;}
                while(true){espacos();if(objeto){String k=texto();espacos();if(consumir()!=':')throw new IllegalArgumentException("Esperado :");if(m.containsKey(k))throw new IllegalArgumentException("Chave duplicada");m.put(k,valor());}else l.add(valor());espacos();char sep=consumir();if(sep==fim)break;if(sep!=',')throw new IllegalArgumentException("Esperada vírgula");}return objeto?m:l;}
            for(String literal:new String[]{"true","false","null"})if(s.startsWith(literal,i)){i+=literal.length();return "null".equals(literal)?null:Boolean.valueOf(literal);}
            int inicio=i;while(i<s.length()&&"-+0123456789.eE".indexOf(s.charAt(i))>=0)i++;if(inicio==i)throw new IllegalArgumentException("Valor JSON inválido");String numero=s.substring(inicio,i);if(!numero.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?"))throw new IllegalArgumentException("Número JSON inválido");if(numero.matches("-?(0|[1-9][0-9]*)"))return Long.valueOf(numero);double n=Double.parseDouble(numero);if(Double.isInfinite(n)||Double.isNaN(n))throw new IllegalArgumentException("Número inválido");return n;
        }
        String texto(){if(consumir()!='"')throw new IllegalArgumentException("Esperada string");StringBuilder b=new StringBuilder();while(true){char c=consumir();if(c=='"')return b.toString();if(c<32)throw new IllegalArgumentException("Controle em string");if(c==92){char e=consumir();switch(e){case '"':case '/':case 92:c=e;break;case 'n':c=10;break;case 'r':c=13;break;case 't':c=9;break;case 'b':c=8;break;case 'f':c=12;break;case 'u':c=(char)Integer.parseInt(s.substring(i,i+4),16);i+=4;break;default:throw new IllegalArgumentException("Escape inválido");}}b.append(c);}}
    }


    public static String estadoParaJson(Estado estado) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"turnoAtual\":").append(estado.getTurnoAtual()).append(",");
        sb.append("\"totalTurnos\":").append(estado.getTotalTurnos()).append(",");
        sb.append("\"modoDemonstracao\":").append(estado.isModoDemonstracao()).append(",");
        sb.append("\"identificacaoMandato\":").append(quote(estado.getIdentificacaoMandato())).append(",");
        sb.append("\"partidaEncerrada\":").append(estado.isPartidaEncerrada()).append(",");
        sb.append("\"vitoria\":").append(estado.isVitoria()).append(",");
        sb.append("\"statusPartida\":").append(quote(estado.getStatusPartida())).append(",");
        sb.append("\"tituloFimDeJogo\":").append(quote(estado.getTituloFimDeJogo())).append(",");
        sb.append("\"motivoFimDeJogo\":").append(quote(estado.getMotivoFimDeJogo())).append(",");
        sb.append("\"avaliacaoFinalEconomica\":").append(quote(estado.getAvaliacaoFinalEconomica())).append(",");
        sb.append("\"taxaJuros\":").append(num(estado.getTaxaJuros())).append(",");
        sb.append("\"variacaoJurosUltimoTurno\":").append(num(estado.getVariacaoJurosUltimoTurno())).append(",");

        sb.append("\"dificuldade\":").append(quote(estado.getDificuldade().name())).append(",");
        sb.append("\"mostrarNumeros\":").append(estado.getDificuldade().isMostrarNumeros()).append(",");
        sb.append("\"herancaMandato\":").append(quote(estado.getHerancaMandato())).append(",");
        sb.append("\"manchete\":").append(quote(estado.getManchete())).append(",");
        sb.append("\"resultadoEleicao\":").append(quote(estado.getResultadoEleicao())).append(",");
        sb.append("\"dialogoApresentado\":").append(quote(estado.getDialogoApresentado())).append(",");
        sb.append("\"memoriaPersonagens\":{");boolean primeiroPersonagem=true;
        for(java.util.Map.Entry<String,Estado.MemoriaPersonagem> entrada:estado.getMemoriaPersonagens().entrySet()){
            if(!primeiroPersonagem)sb.append(",");primeiroPersonagem=false;Estado.MemoriaPersonagem m=entrada.getValue();
            sb.append(quote(entrada.getKey())).append(":{\"seguiu\":").append(m.getSeguiu()).append(",\"ignorou\":").append(m.getIgnorou()).append(",\"ultimaEscolha\":").append(quote(m.getUltimaEscolha())).append("}");
        }sb.append("},");
        sb.append("\"epilogoMandato\":").append(quote(estado.getEpilogoMandato())).append(",");
        sb.append("\"licoesMandato\":").append(listaStringsParaJson(estado.getLicoesMandato())).append(",");
        sb.append("\"seloConsequencia\":").append(quote(estado.getSeloConsequencia())).append(",");
        sb.append("\"seloRetorno\":").append(quote(estado.getSeloRetorno())).append(",");
        // Indicadores principais
        sb.append("\"indicadores\":{");
        sb.append("\"caixa\":").append(indicadorParaJson(estado.getCaixaPublico())).append(",");
        sb.append("\"aprovacao\":").append(indicadorParaJson(estado.getAprovacaoPopular())).append(",");
        sb.append("\"pib\":").append(indicadorParaJson(estado.getPib())).append(",");
        sb.append("\"inflacao\":").append(indicadorParaJson(estado.getInflacao()));
        sb.append("},");

        // Evento atual (Carta)
        sb.append("\"eventoAtual\":");
        if (estado.getEventoAtual() != null) {
            sb.append(estado.getDificuldade().isMostrarNumeros() ? eventoParaJson(estado.getEventoAtual()) : eventoPublico(estado.getEventoAtual()));
        } else {
            sb.append("null");
        }
        sb.append(",");

        // Efeitos atrasados pendentes
        sb.append("\"efeitosPendentes\":[");
        List<EfeitoAtrasado> pendentes = estado.getEfeitosPendentes();
        for (int i = 0; i < pendentes.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(efeitoAtrasadoParaJson(pendentes.get(i)));
        }
        sb.append("],");

        // Reações da IA (Oposição e Mercado)
        sb.append("\"reacoesAtuais\":[");
        List<ReacaoAgente> reacoes = estado.getReacoesAtuais();
        for (int i = 0; i < reacoes.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(reacaoParaJson(reacoes.get(i)));
        }
        sb.append("],");

        // Histórico de turnos
        sb.append("\"historico\":[");
        List<HistoricoTurno> hist = estado.getHistorico();
        for (int i = 0; i < hist.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(historicoParaJson(hist.get(i)));
        }
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }

    private static String indicadorParaJson(Indicador ind) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"codigo\":").append(quote(ind.getCodigo())).append(",");
        sb.append("\"nome\":").append(quote(ind.getNome())).append(",");
        sb.append("\"unidade\":").append(quote(ind.getUnidade())).append(",");
        sb.append("\"descricao\":").append(quote(ind.getDescricao())).append(",");
        sb.append("\"valorInicial\":").append(num(ind.getValorInicial())).append(",");
        sb.append("\"valorMinimo\":").append(num(ind.getValorMinimo())).append(",");
        sb.append("\"valorMaximo\":").append(num(ind.getValorMaximo())).append(",");
        sb.append("\"valorAtual\":").append(num(ind.getValorAtual())).append(",");
        sb.append("\"variacaoUltimoTurno\":").append(num(ind.getVariacaoUltimoTurno())).append(",");
        sb.append("\"percentualBarra\":").append(num(ind.getPercentualBarra())).append(",");
        sb.append("\"status\":").append(quote(ind.getClassificacaoStatus())).append(",");
        sb.append("\"rotuloStatus\":").append(quote(ind.getRotuloStatus()));
        sb.append("}");
        return sb.toString();
    }

    private static String direcao(double v,double limiar){return v==0?"→":(v>0?(Math.abs(v)>=limiar?"↑↑":"↑"):(Math.abs(v)>=limiar?"↓↓":"↓"));}
    private static String decretoPublico(Decreto d){
        return "{\"codigo\":"+quote(d.getCodigo())+",\"titulo\":"+quote(semQuantidades(d.getTitulo()))+",\"emPoucasPalavras\":"+quote(semQuantidades(d.getEmPoucasPalavras()))+",\"tipoPolitica\":"+quote(semQuantidades(d.getTipoPolitica()))+",\"descricao\":"+quote(semQuantidades(d.getDescricao()))+
            ",\"direcoes\":{\"caixa\":"+quote(direcao(d.getImpactoCaixa(),12))+",\"aprovacao\":"+quote(direcao(d.getImpactoAprovacao(),8))+",\"pib\":"+quote(direcao(d.getImpactoPib(),1.2))+",\"inflacao\":"+quote(direcao(d.getImpactoInflacao(),0.9))+",\"juros\":"+quote(direcao(d.getImpactoJuros(),1.5))+"}}";
    }
    private static String semQuantidades(String texto){return texto.replaceAll("[+-]?[0-9]+([.,][0-9]+)?", "[estimativa]");}
    private static String eventoPublico(Evento e){
        return "{\"id\":"+quote(e.getId())+",\"nome\":"+quote(e.getNome())+",\"categoria\":"+quote(e.getCategoria())+",\"personagemNome\":"+quote(e.getPersonagemNome())+",\"personagemCargo\":"+quote(e.getPersonagemCargo())+",\"personagemAvatar\":"+quote(e.getPersonagemAvatar())+",\"dialogo\":"+quote(semQuantidades(e.getDialogo()))+",\"descricao\":"+quote(semQuantidades(e.getDescricao()))+",\"contextualizacao\":"+quote(semQuantidades(e.getContextualizacao()))+",\"decretoA\":"+decretoPublico(e.getDecretoA())+",\"decretoB\":"+decretoPublico(e.getDecretoB())+extrasNarrativos(e)+"}";
    }
    private static String extrasNarrativos(Evento e){StringBuilder s=new StringBuilder();
        for(String chave:new String[]{"conceito","licaoCurta"}){Object v=e.getNarrativa().get(chave);if(v instanceof String)s.append(",").append(quote(chave)).append(":").append(quote((String)v));}
        return s.toString();
    }
    private static String eventoParaJson(Evento ev) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":").append(quote(ev.getId())).append(",");
        sb.append("\"nome\":").append(quote(ev.getNome())).append(",");
        sb.append("\"categoria\":").append(quote(ev.getCategoria())).append(",");
        sb.append("\"personagemNome\":").append(quote(ev.getPersonagemNome())).append(",");
        sb.append("\"personagemCargo\":").append(quote(ev.getPersonagemCargo())).append(",");
        sb.append("\"personagemAvatar\":").append(quote(ev.getPersonagemAvatar())).append(",");
        sb.append("\"dialogo\":").append(quote(ev.getDialogo())).append(",");
        sb.append("\"descricao\":").append(quote(ev.getDescricao())).append(",");
        sb.append("\"contextualizacao\":").append(quote(ev.getContextualizacao())).append(",");
        sb.append("\"consequenciasPotenciais\":").append(quote(ev.getConsequenciasPotenciais())).append(",");
        sb.append("\"decretoA\":").append(decretoParaJson(ev.getDecretoA())).append(",");
        sb.append("\"decretoB\":").append(decretoParaJson(ev.getDecretoB()));
        sb.append(extrasNarrativos(ev));
        sb.append("}");
        return sb.toString();
    }

    private static String decretoParaJson(Decreto dec) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"codigo\":").append(quote(dec.getCodigo())).append(",");
        sb.append("\"titulo\":").append(quote(dec.getTitulo())).append(",");
        sb.append("\"emPoucasPalavras\":").append(quote(dec.getEmPoucasPalavras())).append(",");
        sb.append("\"tipoPolitica\":").append(quote(dec.getTipoPolitica())).append(",");
        sb.append("\"descricao\":").append(quote(dec.getDescricao())).append(",");
        sb.append("\"explicacaoTeorica\":").append(quote(dec.getExplicacaoTeorica())).append(",");
        sb.append("\"impactoCaixa\":").append(num(dec.getImpactoCaixa())).append(",");
        sb.append("\"impactoAprovacao\":").append(num(dec.getImpactoAprovacao())).append(",");
        sb.append("\"impactoPib\":").append(num(dec.getImpactoPib())).append(",");
        sb.append("\"impactoInflacao\":").append(num(dec.getImpactoInflacao())).append(",");
        sb.append("\"impactoJuros\":").append(num(dec.getImpactoJuros())).append(",");
        sb.append("\"consequenciasEsperadas\":").append(listaStringsParaJson(dec.getConsequenciasEsperadas())).append(",");
        sb.append("\"efeitosAtrasados\":[");
        List<EfeitoAtrasado> atrasados = dec.getEfeitosAtrasados();
        for (int i = 0; i < atrasados.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(efeitoAtrasadoParaJson(atrasados.get(i)));
        }
        sb.append("]");
        sb.append("}");
        return sb.toString();
    }

    private static String efeitoAtrasadoParaJson(EfeitoAtrasado ef) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"turnoOrigem\":").append(ef.getTurnoOrigem()).append(",");
        sb.append("\"turnoAplicacao\":").append(ef.getTurnoAplicacao()).append(",");
        sb.append("\"turnosRestantes\":").append(ef.getTurnosRestantes()).append(",");
        sb.append("\"decretoOrigem\":").append(quote(ef.getDecretoOrigem())).append(",");
        sb.append("\"descricao\":").append(quote(ef.getDescricao())).append(",");
        sb.append("\"deltaCaixa\":").append(num(ef.getDeltaCaixa())).append(",");
        sb.append("\"deltaAprovacao\":").append(num(ef.getDeltaAprovacao())).append(",");
        sb.append("\"deltaPib\":").append(num(ef.getDeltaPib())).append(",");
        sb.append("\"deltaInflacao\":").append(num(ef.getDeltaInflacao()));
        sb.append("}");
        return sb.toString();
    }

    private static String reacaoParaJson(ReacaoAgente r) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"agente\":").append(quote(r.getAgente())).append(",");
        sb.append("\"nomeExibicao\":").append(quote(r.getNomeExibicao())).append(",");
        sb.append("\"postura\":").append(quote(r.getPostura())).append(",");
        sb.append("\"titulo\":").append(quote(r.getTitulo())).append(",");
        sb.append("\"declaracao\":").append(quote(r.getDeclaracao())).append(",");
        sb.append("\"explicacaoRegra\":").append(quote(r.getExplicacaoRegra())).append(",");
        sb.append("\"efeitoPratico\":").append(quote(r.getEfeitoPratico()));
        sb.append("}");
        return sb.toString();
    }

    private static String historicoParaJson(HistoricoTurno h) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"numeroTurno\":").append(h.getNumeroTurno()).append(",");
        sb.append("\"eventoId\":").append(quote(h.getEventoId())).append(",");
        sb.append("\"eventoNome\":").append(quote(h.getEventoNome())).append(",");
        sb.append("\"eventoCategoria\":").append(quote(h.getEventoCategoria())).append(",");
        sb.append("\"decretoCodigo\":").append(quote(h.getDecretoCodigo())).append(",");
        sb.append("\"decretoTitulo\":").append(quote(h.getDecretoTitulo())).append(",");
        sb.append("\"decretoTipoPolitica\":").append(quote(h.getDecretoTipoPolitica())).append(",");
        sb.append("\"caixaAntes\":").append(num(h.getCaixaAntes())).append(",");
        sb.append("\"caixaDepois\":").append(num(h.getCaixaDepois())).append(",");
        sb.append("\"deltaCaixa\":").append(num(h.getDeltaCaixa())).append(",");
        sb.append("\"aprovacaoAntes\":").append(num(h.getAprovacaoAntes())).append(",");
        sb.append("\"aprovacaoDepois\":").append(num(h.getAprovacaoDepois())).append(",");
        sb.append("\"deltaAprovacao\":").append(num(h.getDeltaAprovacao())).append(",");
        sb.append("\"pibAntes\":").append(num(h.getPibAntes())).append(",");
        sb.append("\"pibDepois\":").append(num(h.getPibDepois())).append(",");
        sb.append("\"deltaPib\":").append(num(h.getDeltaPib())).append(",");
        sb.append("\"inflacaoAntes\":").append(num(h.getInflacaoAntes())).append(",");
        sb.append("\"inflacaoDepois\":").append(num(h.getInflacaoDepois())).append(",");
        sb.append("\"deltaInflacao\":").append(num(h.getDeltaInflacao())).append(",");
        sb.append("\"jurosAntes\":").append(num(h.getJurosAntes())).append(",");
        sb.append("\"jurosDepois\":").append(num(h.getJurosDepois())).append(",");
        sb.append("\"deltaJuros\":").append(num(h.getDeltaJuros())).append(",");
        sb.append("\"oQueMudou\":").append(quote(h.getOQueMudou())).append(",");
        sb.append("\"porQueMudou\":").append(quote(h.getPorQueMudou())).append(",");
        sb.append("\"custoDecisao\":").append(quote(h.getCustoDecisao())).append(",");
        sb.append("\"consequenciasFuturas\":").append(quote(h.getConsequenciasFuturas())).append(",");
        sb.append("\"sinteseDidatica\":").append(quote(h.getSinteseDidatica())).append(",");
        sb.append("\"detalhesCalculo\":").append(listaStringsParaJson(h.getDetalhesCalculo())).append(",");
        sb.append("\"efeitosAtrasadosAtivados\":").append(listaStringsParaJson(h.getEfeitosAtrasadosAtivados())).append(",");
        sb.append("\"efeitosAtrasadosAgendados\":").append(listaStringsParaJson(h.getEfeitosAtrasadosAgendados())).append(",");
        sb.append("\"reacoesTurno\":[");
        List<ReacaoAgente> reacoes = h.getReacoesTurno();
        for (int i = 0; i < reacoes.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(reacaoParaJson(reacoes.get(i)));
        }
        sb.append("]");
        sb.append("}");
        return sb.toString();
    }

    private static String listaStringsParaJson(List<String> lista) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        if (lista != null) {
            for (int i = 0; i < lista.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(quote(lista.get(i)));
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static String num(double valor) {
        return String.format(Locale.US, "%.1f", valor);
    }

    public static String quote(String texto) {
        if (texto == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append("\"");
        return sb.toString();
    }

}
