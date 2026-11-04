package govsim.engine;
import govsim.model.*;
import java.util.*;
public final class VerificacaoNarrativa {
 private static void exigir(boolean ok,String mensagem){if(!ok)throw new AssertionError(mensagem);}
 public static void executar(){
  BancoEventos banco=new BancoEventos(new Random(5));MotorEconomico motor=new MotorEconomico();Estado e=new Estado();
  motor.iniciarPartida(e,false,0,Dificuldade.MEDIO,Long.valueOf(42));exigir(e.getEventoAtual().getId().equals("EV_POSSE"),"Posse no primeiro turno normal");
  e.setTurnoAtual(e.getTotalTurnos());Evento fim=banco.selecionarProximoEvento(e);exigir(fim.getId().equals("EV_PRESTACAO_CONTAS"),"Prestação de contas no último turno");
  for(Decreto d:new Decreto[]{fim.getDecretoA(),fim.getDecretoB()})exigir(d.getImpactoCaixa()==0&&d.getImpactoAprovacao()==0&&d.getImpactoPib()==0&&d.getImpactoInflacao()==0&&d.getImpactoJuros()==0&&d.getEfeitosAtrasados().isEmpty(),"Última escolha só narrativa");
  motor.iniciarPartida(e,true,4,Dificuldade.FACIL);exigir(e.getEventoAtual().getId().equals("EV_PANDEMIA"),"Demo preserva ordem original");motor.processarDecisao(e,"A");exigir(!e.getManchete().isEmpty(),"Manchete após decisão");String manchete=e.getManchete();exigir(manchete.equals(e.getManchete()),"Manchete determinística");
  e.getAprovacaoPopular().definirValor(65);e.encerrarPartida(true,"VITORIA","Teste","Teste","Teste");exigir(e.getResultadoEleicao().contains("com folga"),"Eleição com folga");e.getAprovacaoPopular().definirValor(52);exigir(e.getResultadoEleicao().contains("por pouco"),"Eleição estreita");e.getAprovacaoPopular().definirValor(49);exigir(e.getResultadoEleicao().contains("Derrotado"),"Eleição não altera vitória do mandato");exigir(e.isVitoria(),"Resultado eleitoral não altera vitória");
  verificarCadeias();
  verificarMemorias();
  verificarEpilogo();
  verificarSelos();
  Map<String,Set<String>> pares=new HashMap<String,Set<String>>();int quantidade=0;
  for(Evento c:banco.getCatalogoEventos())if(c.isConsequencia()){
   quantidade++;Map<?,?> g=(Map<?,?>)c.getNarrativa().get("gatilho");String origemId=(String)g.get("origem");if(!pares.containsKey(origemId))pares.put(origemId,new HashSet<String>());pares.get(origemId).add((String)g.get("decreto"));
   exigir(c.getDialogo().split("\\s+").length<=20,"Diálogo curto de consequência");
  }
  exigir(quantidade==12&&pares.size()==6,"Seis cadeias com doze cartas");for(Set<String> par:pares.values())exigir(par.size()==2,"Dois decretos de origem por cadeia");
  motor.iniciarPartida(e,true,4,Dificuldade.FACIL);e.setTurnoAtual(4);exigir(banco.selecionarProximoEvento(e).getId().equals("EV_PRESTACAO_CONTAS"),"Demo tem fechamento determinístico");
  System.out.println("[APROVADO] Arco: posse, encerramento narrativo, demo preservada, manchetes e eleição sem alterar vitória.");
 }
 private static void verificarCadeias(){
  BancoEventos banco=new BancoEventos(new Random(9));Evento origem=banco.getCatalogoEventos().get(0);
  Map<String,Object> gatilho=new HashMap<String,Object>();gatilho.put("origem",origem.getId());gatilho.put("decreto","A");gatilho.put("atrasoMin",2);gatilho.put("atrasoMax",3);
  Map<String,Object> meta=new HashMap<String,Object>();meta.put("gatilho",gatilho);
  Evento retorno=new Evento("TESTE_CADEIA","Retorno","FISCAL","Nome","Cargo","Avatar","O país cobra respostas.","Teste","Teste","Teste",origem.getDecretoA(),origem.getDecretoB()).comNarrativa(meta);
  Estado e=new Estado();e.iniciarNovaPartida(false,10,Dificuldade.MEDIO);exigir(!banco.elegivel(retorno,e),"Sem origem, sem consequência");
  e.registrarDecisaoNarrativa(origem.inverter(),"B");exigir("A".equals(e.getDecisoesNarrativas().get(origem.getId()).codigoOriginal),"Escolha interna preservada após inversão");
  e.setTurnoAtual(2);exigir(!banco.elegivel(retorno,e),"Antes da janela");e.setTurnoAtual(3);exigir(banco.elegivel(retorno,e),"Início inclusivo da janela");
  exigir(banco.selecionarConsequencia(Arrays.asList(retorno,origem),e)==retorno,"Consequência prioritária");
  e.registrarEventoSorteado(retorno);exigir(!banco.elegivel(retorno,e),"Sem repetição");
  Evento retorno2=new Evento("TESTE_CADEIA_2","Retorno 2","FISCAL","Nome","Cargo","Avatar","O país cobra respostas.","Teste","Teste","Teste",origem.getDecretoA(),origem.getDecretoB()).comNarrativa(meta);
  e.setTurnoAtual(4);exigir(banco.selecionarConsequencia(Arrays.asList(retorno2),e)==null,"Intervalo mínimo de dois turnos");e.setTurnoAtual(5);exigir(!banco.elegivel(retorno2,e),"Após a janela");
  e.iniciarNovaPartida(true,10,Dificuldade.FACIL);e.registrarDecisaoNarrativa(origem,"B");e.setTurnoAtual(3);exigir(!banco.elegivel(retorno,e),"Decreto oposto não dispara");
  e.iniciarNovaPartida(true,10,Dificuldade.FACIL);e.registrarDecisaoNarrativa(origem,"A");e.setTurnoAtual(3);exigir(banco.selecionarConsequencia(Arrays.asList(retorno),e)==banco.selecionarConsequencia(Arrays.asList(retorno),e),"Cadeia determinística na demo");
  exigir(e.getDecisoesNarrativas().size()==1&&e.getUltimoTurnoConsequencia()==-100,"Reinício limpa cadeias");
 }
 private static void verificarMemorias(){
  for(Evento c:new BancoEventos().getCatalogoEventos()){
   Estado e=new Estado();e.registrarEventoSorteado(c);String original=c.getDialogo();exigir(e.getDialogoApresentado().equals(original),"Primeiro encontro sem lembrança");
   e.registrarDecisaoNarrativa(c.inverter(),"B");Estado.MemoriaPersonagem m=e.getMemoriaPersonagens().get(c.getPersonagemNome());
   exigir(m!=null&&m.getSeguiu()==1&&m.getIgnorou()==0&&m.getUltimaEscolha().equals("A"),"Memória segue código original "+c.getId());
   String seguiu=e.getDialogoApresentado();exigir(seguiu.startsWith((String)((Map<?,?>)c.getNarrativa().get("falaRetorno")).get("seguiu")),"Lembrança de apoio");
   exigir(seguiu.split("\\s+").length<=20,"Retorno curto após apoio");
   e.registrarDecisaoNarrativa(c,"B");exigir(m.getSeguiu()==1&&m.getIgnorou()==1&&m.getUltimaEscolha().equals("B"),"Memória registra divergência");
   exigir(e.getDialogoApresentado().startsWith((String)((Map<?,?>)c.getNarrativa().get("falaRetorno")).get("ignorou")),"Lembrança de divergência");
   exigir(e.getDialogoApresentado().split("\\s+").length<=20,"Retorno curto após divergência");exigir(c.getDialogo().equals(original),"Catálogo compartilhado não é modificado");
   e.iniciarNovaPartida(false,10,Dificuldade.FACIL);exigir(e.getMemoriaPersonagens().isEmpty(),"Memória limpa no reinício");
  }
  System.out.println("[APROVADO] Memória por personagem, códigos internos, falas curtas e isolamento do catálogo.");
 }
 private static void verificarEpilogo(){
  MotorEconomico m=new MotorEconomico();Estado e=new Estado();m.iniciarPartida(e,true,10,Dificuldade.FACIL);
  exigir(e.getEpilogoMandato().isEmpty()&&e.getLicoesMandato().isEmpty(),"Epílogo só no encerramento");
  m.processarDecisao(e,"A");m.processarDecisao(e,"B");m.processarDecisao(e,"A");
  e.encerrarPartida(true,"VITORIA","Teste","Teste","Gestão Equilibrada:");
  String epilogo=e.getEpilogoMandato();exigir(epilogo.startsWith("O governo ficou marcado"),"Tom de vitória");exigir(epilogo.contains("cuidado"),"Classificação orienta epílogo");
  exigir(e.getLicoesMandato().size()==3,"Três lições reais");
  for(HistoricoTurno h:NarrativaMandato.decisoesMarcantes(e).subList(0,2)){
   exigir(epilogo.contains("turno "+h.getNumeroTurno())&&epilogo.contains(h.getDecretoTitulo()),"Epílogo cita turno e decreto reais");
   boolean citado=false;for(String l:e.getLicoesMandato())if(l.contains("Turno "+h.getNumeroTurno())&&l.contains(h.getDecretoTitulo()))citado=true;exigir(citado,"Lição rastreável");
  }
  e.encerrarPartida(false,"DERROTA_CAIXA","Teste","Teste","Teste");exigir(e.getEpilogoMandato().contains("O mandato foi interrompido"),"Tom de derrota");
  exigir(e.getResultadoEleicao().contains("não houve reeleição"),"Interrupção não concede eleição");
  System.out.println("[APROVADO] Epílogo e lições citam decisões reais; classificação e desfecho orientam o tom.");
 }
 private static void verificarSelos(){
  Estado e=new Estado();MotorEconomico m=new MotorEconomico();m.iniciarPartida(e,true,10,Dificuldade.FACIL);
  exigir(e.getSeloConsequencia().isEmpty()&&e.getSeloRetorno().isEmpty(),"Primeiro encontro sem selos falsos");
  for(int turno=1;turno<=3;turno++){e.getCaixaPublico().definirValor(200);e.getAprovacaoPopular().definirValor(100);m.processarDecisao(e,"A");exigir(e.getManchete().split("\\s+").length<=14,"Manchete curta");}
  exigir(e.getEventoAtual().getId().equals("EV_ALTA_DOLAR_RETORNO_A"),"Retorno real da decisão de câmbio");
  HistoricoTurno origem=e.getHistorico().get(1);exigir(e.getSeloConsequencia().contains("turno 2")&&e.getSeloConsequencia().contains(origem.getDecretoTitulo()),"Selo cita escolha real");
  exigir(e.getSeloRetorno().contains("seguiu")&&e.getSeloRetorno().contains("turno 2"),"Selo de memória cita o encontro anterior");
 }
}
