package govsim.engine;
import govsim.model.*;
import java.util.Random;
public final class VerificacaoEconomica {
 private static void exigir(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
 private static Evento neutro(){
  Decreto d=new Decreto("A","Neutro","Teste","Teste",java.util.Collections.<String>emptyList(),"Teste",0,0,0,0,0,java.util.Collections.<EfeitoAtrasado>emptyList());
  return new Evento("TESTE","Teste","Teste","Nome","Cargo","AVATAR","Dialogo","Descricao","Contexto","Consequencias",d,d);
 }
 private static Estado novo(MotorEconomico m,Dificuldade d){Estado e=new Estado();m.iniciarPartida(e,true,10,d);return e;}
 public static void executar(){
  MotorEconomico m=new MotorEconomico(new Random(7));
  for(Dificuldade d:Dificuldade.values()){
   Estado a=novo(m,d),b=novo(m,d);
   exigir(a.getCaixaPublico().getValorAtual()==d.getCaixaInicial(),"Caixa inicial");
   exigir(a.getCaixaPublico().getValorInicial()==d.getCaixaInicial(),"Base fiscal do mandato");
   exigir(a.getAprovacaoPopular().getValorInicial()==d.getAprovacaoInicial(),"Base política do mandato");
   exigir(a.getAprovacaoPopular().getValorAtual()==d.getAprovacaoInicial(),"Aprovação inicial real");
   for(Indicador ind:new Indicador[]{a.getCaixaPublico(),a.getAprovacaoPopular(),a.getPib(),a.getInflacao()})
       exigir(ind.getVariacaoUltimoTurno()==0,"Inicialização sem queda artificial: "+ind.getCodigo());
   a.getCaixaPublico().definirValor(d.getCaixaInicial()+10);
   exigir(a.getCaixaPublico().getValorAtual()-a.getCaixaPublico().getValorInicial()==10,"Saldo do mandato +10");
   m.iniciarPartida(a,true,10,d);
   for(int t=0;t<3;t++){
    m.processarDecisao(a,"A");m.processarDecisao(b,"A");
    exigir(govsim.server.JsonUtil.estadoParaJson(a).equals(govsim.server.JsonUtil.estadoParaJson(b)),"Demo deve ser determinística");
   }
  }
  Estado e=novo(m,Dificuldade.MEDIO);e.getPib().definirValor(10);e.registrarEventoSorteado(neutro());m.processarDecisao(e,"A");
  exigir(e.getPib().getValorAtual()<10,"PIB reverte ao potencial");
  e=novo(m,Dificuldade.MEDIO);e.getInflacao().definirValor(35);e.getAprovacaoPopular().definirValor(100);e.registrarEventoSorteado(neutro());m.processarDecisao(e,"A");
  exigir("DERROTA_HIPERINFLACAO".equals(e.getStatusPartida()),"Hiperinflação");
  e=novo(m,Dificuldade.MEDIO);
  for(int t=0;t<2;t++){e.getPib().definirValor(-8);e.registrarEventoSorteado(neutro());m.processarDecisao(e,"A");}
  exigir("DERROTA_DEPRESSAO".equals(e.getStatusPartida()),"Depressão persistente");
  e=novo(m,Dificuldade.FACIL);e.getPib().definirValor(-8);e.registrarAtividade();e.registrarAtividade();e.getInflacao().definirValor(35);e.getAprovacaoPopular().definirValor(100);e.registrarEventoSorteado(neutro());m.processarDecisao(e,"A");
  exigir(!e.isPartidaEncerrada(),"Fácil isento de derrotas macro");
  Estado alto=novo(m,Dificuldade.MEDIO),baixo=novo(m,Dificuldade.MEDIO);
  alto.definirTaxaJuros(15);baixo.definirTaxaJuros(6);
  for(int t=0;t<3;t++){alto.registrarEventoSorteado(neutro());baixo.registrarEventoSorteado(neutro());m.processarDecisao(alto,"A");m.processarDecisao(baixo,"A");}
  exigir(alto.getPib().getValorAtual()<baixo.getPib().getValorAtual(),"Selic reduz atividade com defasagem");
  exigir(alto.getInflacao().getValorAtual()<baixo.getInflacao().getValorAtual(),"Selic reduz inflação com defasagem");
  m.iniciarPartida(e,false,0,Dificuldade.DIFICIL);exigir(e.getTotalTurnos()==15&&e.getTurnosDepressao()==0,"Reinício difícil");
  VerificacaoConteudo.executar();
  System.out.println("[APROVADO] Dificuldades, determinismo demo, reversão, canal Selic e derrotas macroeconômicas.");
 }
}
