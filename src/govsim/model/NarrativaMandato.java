package govsim.model;
/** Texto derivado do estado: não usa sorteio nem interfere na economia. */
public final class NarrativaMandato {
 private NarrativaMandato(){}
 @SuppressWarnings("unchecked") public static String dialogo(Estado e,Evento evento){
  Estado.MemoriaPersonagem memoria=e.getMemoriaPersonagens().get(evento.getPersonagemNome());
  Object variantes=evento.getNarrativa().get("falaRetorno");if(memoria==null||!(variantes instanceof java.util.Map))return evento.getDialogo();
  String prefixo=(String)((java.util.Map<String,Object>)variantes).get(memoria.isUltimoSeguiu()?"seguiu":"ignorou");if(prefixo==null)return evento.getDialogo();
  String problema=evento.getDialogo();java.util.regex.Matcher frase=java.util.regex.Pattern.compile("[^.!?]+[.!?]+").matcher(problema);if(frase.find())problema=frase.group().trim();
  String composto=prefixo+" "+problema;
  if(composto.trim().split("\\s+").length>20)throw new IllegalStateException("Fala de retorno excede 20 palavras: "+evento.getId());
  return composto;
 }
 public static String heranca(Estado e){return "Você é o presidente. Chegue ao fim sem acabar o dinheiro do governo nem o apoio do povo. "+(e.getDificuldade()==Dificuldade.DIFICIL?"Herdou pouco dinheiro, apoio dividido e preços subindo.":"Herdou dinheiro guardado, apoio da maioria e preços subindo.");}
 @SuppressWarnings("unchecked") public static String seloConsequencia(Estado e){
  Evento atual=e.getEventoAtual();if(atual==null||!atual.isConsequencia())return "";
  java.util.Map<String,Object> g=(java.util.Map<String,Object>)atual.getNarrativa().get("gatilho");
  for(HistoricoTurno h:e.getHistorico())if(h.getEventoId().equals(g.get("origem")))return "Consequência da sua decisão no turno "+h.getNumeroTurno()+": "+h.getDecretoTitulo();
  return "";
 }
 public static String seloRetorno(Estado e){Evento atual=e.getEventoAtual();if(atual==null)return "";Estado.MemoriaPersonagem m=e.getMemoriaPersonagens().get(atual.getPersonagemNome());if(m==null)return "";
  return "Voltou: você "+(m.isUltimoSeguiu()?"seguiu":"ignorou")+" o conselho no turno "+m.getUltimoTurno()+".";
 }
 public static String manchete(Estado e){
  if(e.getHistorico().isEmpty())return "";
  HistoricoTurno h=e.getHistorico().get(e.getHistorico().size()-1);
  String[][] textos={
   {"Após as decisões, preços altos apertam as compras das famílias","Com preços mais altos, famílias repensam compras depois das medidas","O balanço das decisões deixa o mercado mais caro"},
   {"Depois das decisões, governo fica com pouco dinheiro guardado","As últimas medidas deixam menos dinheiro para novas promessas","Governo revê planos após a redução do dinheiro disponível"},
   {"Depois das medidas, menos pessoas apoiam o governo","Apoio baixo após as decisões aumenta cobranças nas ruas","As escolhas do governo deixam parte da população insatisfeita"},
   {"Após as medidas, produção menor preocupa quem procura emprego","Menos produção depois das decisões leva empresas a rever planos","As escolhas anteriores chegam a um país com menos trabalho"},
   {"Ajuda e contas do governo seguem em debate após as decisões","Depois das escolhas, o governo mantém dinheiro e apoio","O balanço das medidas não aponta um problema dominante"}};
  int grupo=e.getInflacao().getValorAtual()>8?0:e.getCaixaPublico().getValorAtual()<30?1:e.getAprovacaoPopular().getValorAtual()<35?2:e.getPib().getValorAtual()<0?3:4;
  if(h.getDeltaAprovacao()>0&&h.getDeltaCaixa()<0)return "Apoio cresce, mas sobra menos dinheiro após as decisões";
  if(h.getDeltaAprovacao()<0&&h.getDeltaCaixa()>0)return "Governo guarda mais dinheiro, mas perde apoio após as medidas";
  return textos[grupo][(e.getHistorico().size()-1)%3];
 }
 public static String eleicao(Estado e){if(!e.isPartidaEncerrada())return "";if(!e.isVitoria())return "O mandato terminou antes do balanço eleitoral; não houve reeleição.";
  double a=e.getAprovacaoPopular().getValorAtual();String resultado=a>=60?"Reeleito com folga":a>=50?"Reeleito por pouco":"Derrotado nas urnas";
  String margem=String.format(java.util.Locale.ROOT,"%.1f",Math.abs(2*a-100)).replace('.',',');
  return resultado+": vantagem imaginada de "+margem+" pontos, calculada a partir do apoio final ao governo.";
 }
 public static java.util.List<HistoricoTurno> decisoesMarcantes(Estado e){
  java.util.List<HistoricoTurno> lista=new java.util.ArrayList<HistoricoTurno>(e.getHistorico());
  java.util.Collections.sort(lista,new java.util.Comparator<HistoricoTurno>(){public int compare(HistoricoTurno a,HistoricoTurno b){return Double.compare(impacto(b),impacto(a));}});
  return java.util.Collections.unmodifiableList(new java.util.ArrayList<HistoricoTurno>(lista.subList(0,Math.min(3,lista.size()))));
 }
 private static double impacto(HistoricoTurno h){return Math.abs(h.getDeltaCaixa())/10+Math.abs(h.getDeltaAprovacao())/8+Math.abs(h.getDeltaPib())+Math.abs(h.getDeltaInflacao());}
 private static String identidade(Estado e){String a=e.getAvaliacaoFinalEconomica();return a.contains("(Estadista)")?"apoio das pessoas e produção em alta":a.contains("Gestão Equilibrada:")?"cuidado com o dinheiro do governo e com o apoio das pessoas":a.contains("Pressão Inflacionária")?"disputas sobre o custo de vida":a.contains("Baixo Crescimento")?"o desafio de recuperar o emprego":"a dificuldade de manter dinheiro e apoio";}
 public static String epilogo(Estado e){
  if(!e.isPartidaEncerrada())return "";
  java.util.List<HistoricoTurno> marcantes=decisoesMarcantes(e);
  StringBuilder texto=new StringBuilder();if(!marcantes.isEmpty())texto.append("O governo ficou marcado pela decisão “").append(marcantes.get(0).getDecretoTitulo()).append("”. ");else texto.append("O governo terminou sem decisões registradas. ");
  texto.append(e.isVitoria()?"Você chegou ao fim com ":"O mandato foi interrompido com ").append(identidade(e)).append(". ");
  java.util.List<HistoricoTurno> lista=decisoesMarcantes(e);
  for(HistoricoTurno h:lista.subList(0,Math.min(2,lista.size())))texto.append("No turno ").append(h.getNumeroTurno()).append(", a decisão “").append(h.getDecretoTitulo()).append("” marcou ").append(marca(h)).append(". ");
  if(lista.size()<2)texto.append("O balanço reúne as escolhas que chegaram a ser executadas. ");

  return texto.toString().trim();
 }
 private static String marca(HistoricoTurno h){
  if(h.getDeltaCaixa()<0&&h.getDeltaAprovacao()>0)return "o esforço para ajudar as pessoas sem ficar sem dinheiro";
  if(h.getDeltaCaixa()>0&&h.getDeltaAprovacao()<0)return "a perda de apoio ao guardar dinheiro";
  if(h.getDeltaInflacao()>0)return "o aumento dos preços nas compras do dia a dia";
  if(h.getDeltaPib()<0)return "a preocupação com as vendas e os empregos";
  return "uma mudança de prioridades no gabinete";
 }
 public static java.util.List<String> licoes(Estado e){
  java.util.List<String> textos=new java.util.ArrayList<String>();if(!e.isPartidaEncerrada())return textos;
  for(HistoricoTurno h:decisoesMarcantes(e)){
   String licao=h.getDeltaCaixa()<0&&h.getDeltaAprovacao()>0?"apoio social pode exigir dinheiro do governo":h.getDeltaCaixa()>0&&h.getDeltaAprovacao()<0?"guardar dinheiro também pode custar apoio":h.getDeltaInflacao()>0?"preços mais altos deixam o dinheiro comprar menos":h.getDeltaPib()<0?"as decisões precisam considerar produção e emprego":"o resultado da escolha depende do que acontece no país";
   textos.add("Turno "+h.getNumeroTurno()+" · “"+h.getDecretoTitulo()+"”: "+licao+"; o saldo do turno inclui decisões anteriores e respostas de outros grupos.");
  }
  if(textos.size()==1){HistoricoTurno h=e.getHistorico().get(0);textos.add("Turno "+h.getNumeroTurno()+" · “"+h.getDecretoTitulo()+"”: a sobrevivência do governo depende de manter caixa e apoio, além de evitar problemas graves nos preços e na produção.");}
  return textos;
 }
}
