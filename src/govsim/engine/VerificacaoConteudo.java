package govsim.engine;
import govsim.model.*;
import govsim.server.JsonUtil;
import java.util.*;
import java.nio.file.*;
public final class VerificacaoConteudo {
 private static void exigir(boolean b,String m){if(!b)throw new AssertionError(m);}
 @SuppressWarnings("unchecked") public static void executar(){
  for(double confianca:new double[]{0.4,1.0,1.6}){
   exigir(Math.abs(AvaliadorIA.ajustarMercado(-2.5,confianca,false)-(-2.5/confianca))<0.00001,"Penalidade fiscal inversa");
   exigir(Math.abs(AvaliadorIA.ajustarMercado(1.5,confianca,false)-1.5*confianca)<0.00001,"Benefício fiscal direto");
   exigir(Math.abs(AvaliadorIA.ajustarMercado(0.5,confianca,true)-0.5/confianca)<0.00001,"Alta de inflação é penalidade");
   exigir(Math.abs(AvaliadorIA.ajustarMercado(-0.3,confianca,true)+0.3*confianca)<0.00001,"Desinflação é benefício");
  }
  BancoEventos banco=new BancoEventos();MotorEconomico motor=new MotorEconomico();
  try{
   List<Object> originais=(List<Object>)JsonUtil.lerJson(new String(Files.readAllBytes(Paths.get("tests/fixtures/eventos-originais.json")),"UTF-8"));
   java.lang.reflect.Method serializar=JsonUtil.class.getDeclaredMethod("eventoParaJson",Evento.class);serializar.setAccessible(true);
   for(int i=0;i<14;i++)exigir(originais.get(i).equals(JsonUtil.lerJson((String)serializar.invoke(null,banco.getCatalogoEventos().get(i)))),"Carta original alterada: "+i);
  }catch(Exception ex){throw new AssertionError(ex);}
  Estado a=new Estado(),b=new Estado();
  motor.iniciarPartida(a,false,0,Dificuldade.MEDIO,Long.valueOf(1234));
  MotorEconomico outro=new MotorEconomico();outro.iniciarPartida(b,false,0,Dificuldade.MEDIO,Long.valueOf(1234));
  Set<String> ids=new HashSet<String>();
  while(!a.isPartidaEncerrada()){
   exigir(JsonUtil.estadoParaJson(a).equals(JsonUtil.estadoParaJson(b)),"Seed não reproduz partida");
   exigir(ids.add(a.getEventoAtual().getId()),"Repetição de carta");
   Map<String,Object> json=(Map<String,Object>)JsonUtil.lerJson(JsonUtil.estadoParaJson(a));
   Map<String,Object> evento=(Map<String,Object>)json.get("eventoAtual");
   Map<String,Object> decreto=(Map<String,Object>)evento.get("decretoA");
   exigir(!decreto.containsKey("impactoCaixa")&&!decreto.containsKey("efeitosAtrasados")&&!decreto.containsKey("consequenciasEsperadas")&&decreto.containsKey("direcoes"),"Vazamento de efeito exato");
   motor.processarDecisao(a,"A");outro.processarDecisao(b,"A");
  }
  for(int i=14;i<banco.getCatalogoEventos().size();i++){
   Evento c=banco.getCatalogoEventos().get(i);Decreto x=c.getDecretoA(),y=c.getDecretoB();
   exigir((x.getImpactoCaixa()-y.getImpactoCaixa())*(x.getImpactoAprovacao()-y.getImpactoAprovacao())<0,"Nova carta dominante");
   exigir(x.getEfeitosAtrasados().size()>0&&y.getEfeitosAtrasados().size()>0,"Carta sem defasagem");
  }
  Estado baixa=new Estado();
  for(int t=0;t<10;t++)baixa.atualizarMemoria(false,true);
  Decreto misto=new Decreto("A","Ajuste","Teste","Teste",Collections.<String>emptyList(),"Teste",10,0,0,0,0,Collections.<EfeitoAtrasado>emptyList());
  AvaliadorIA.ResultadoIA mistura=new AvaliadorIA().avaliarTurno(baixa,banco.getCatalogoEventos().get(0),misto,30,60,2.5,4,10.5);
  exigir(mistura.getDeltaCaixaIA()==Indicador.arredondar(-2.5/0.4+1.5*0.4),"Benefícios e penalidades devem escalar antes da soma");
  Estado memoria=new Estado();AvaliadorIA ia=new AvaliadorIA();Evento c=banco.getCatalogoEventos().get(0);
  AvaliadorIA.ResultadoIA r=ia.avaliarTurno(memoria,c,c.getDecretoB(),30,20,-2,10,10);
  exigir(r.getReacoes().size()>=6,"Regras devem disparar juntas");
  exigir(memoria.getPaciencia()<1&&memoria.getConfianca()<1,"Memória não persiste");
  motor.iniciarPartida(memoria,false,0,Dificuldade.FACIL,Long.valueOf(5));
  exigir(memoria.getPaciencia()==1&&memoria.getConfianca()==1,"Memória não reinicia");
  Estado peso=new Estado();peso.getInflacao().definirValor(12);
  boolean mudou=false;for(Evento carta:banco.getCatalogoEventos())if(banco.peso(carta,peso)>1)mudou=true;
  exigir(mudou,"Inflação precisa aumentar peso de preços");
  System.out.println("[APROVADO] Catálogo original idêntico, 24 cartas, privacidade, seed, memória e regras simultâneas.");
 }
}
