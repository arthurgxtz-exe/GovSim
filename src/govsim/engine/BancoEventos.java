package govsim.engine;
import govsim.model.*;
import govsim.server.JsonUtil;
import java.util.*;
import java.nio.file.*;
import java.io.*;

/** Catálogo externo carregado e validado uma vez. Instâncias só mantêm o sorteador. */
public class BancoEventos {
 private static Map<String,String> temas;
 private static final Map<String,String> ETAPAS=new HashMap<String,String>();
 private static final List<Evento> CATALOGO = carregar();
 private static final Map<String,String> TEMAS = temas;

 private final Random random;
 public BancoEventos(){this(new Random());}
 public BancoEventos(Random random){this.random=random;}
 public List<Evento> getCatalogoEventos(){return CATALOGO;}
 public Evento selecionarProximoEvento(Estado e){
  for(Evento c:CATALOGO){String etapa=ETAPAS.get(c.getId());
   if((!e.isModoDemonstracao()&&e.getTurnoAtual()==1&&"POSSE".equals(etapa))||(e.getTurnoAtual()==e.getTotalTurnos()&&"ENCERRAMENTO".equals(etapa)))return e.deveInverter(c.getId())?c.inverter():c;
  }
  Evento consequencia=selecionarConsequencia(CATALOGO,e);if(consequencia!=null)return e.isModoDemonstracao()||!e.deveInverter(consequencia.getId())?consequencia:consequencia.inverter();
  List<Evento> disponiveis=new ArrayList<Evento>();double total=0;
  for(Evento c:CATALOGO)if(!c.isConsequencia()&&!ETAPAS.containsKey(c.getId())&&!e.getIdsEventosSorteados().contains(c.getId())){disponiveis.add(c);total+=peso(c,e);}
  if(disponiveis.isEmpty())throw new IllegalStateException("Catálogo esgotado sem repetição");
  if(e.isModoDemonstracao())return disponiveis.get(0);
  double alvo=random.nextDouble()*total;Evento selecionado=disponiveis.get(disponiveis.size()-1);
  for(Evento c:disponiveis){alvo-=peso(c,e);if(alvo<0){selecionado=c;break;}}
  return e.deveInverter(selecionado.getId())?selecionado.inverter():selecionado;
 }
 public Evento selecionarConsequencia(List<Evento> cartas,Estado e){
  if(e.getTurnoAtual()-e.getUltimoTurnoConsequencia()<2)return null;
  for(Evento c:cartas)if(elegivel(c,e))return c;
  return null;
 }
 @SuppressWarnings("unchecked") public boolean elegivel(Evento c,Estado e){
  if(!c.isConsequencia()||e.getIdsEventosSorteados().contains(c.getId()))return false;
  Map<String,Object> g=(Map<String,Object>)c.getNarrativa().get("gatilho");
  Estado.DecisaoNarrativa d=e.getDecisoesNarrativas().get((String)g.get("origem"));if(d==null||!d.codigoOriginal.equals(g.get("decreto")))return false;
  int atraso=e.getTurnoAtual()-d.turno;return atraso>=((Number)g.get("atrasoMin")).intValue()&&atraso<=((Number)g.get("atrasoMax")).intValue();
 }
 public double peso(Evento c,Estado e){String tema=TEMAS.get(c.getId());double p=1;
  if("PRECO".equals(tema)&&e.getInflacao().getValorAtual()>6)p+=2;
  if("FISCAL".equals(tema)&&e.getCaixaPublico().getValorAtual()<50)p+=2;
  if("DESEMPREGO".equals(tema)&&e.getPib().getValorAtual()<0)p+=2;
  if("SUPERAQUECIMENTO".equals(tema)&&e.getPib().getValorAtual()>4)p+=2;
  return p;
 }
 @SuppressWarnings("unchecked") private static List<Evento> carregar(){
  try{
   InputStream in=BancoEventos.class.getResourceAsStream("eventos.json");
   if(in==null)in=Files.newInputStream(Paths.get("src/govsim/engine/eventos.json"));
   ByteArrayOutputStream out=new ByteArrayOutputStream();try{byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}finally{in.close();}
   List<Object> dados=(List<Object>)JsonUtil.lerJson(new String(out.toByteArray(),"UTF-8"));
   List<Evento> lista=new ArrayList<Evento>();temas=new HashMap<String,String>();
   for(Object o:dados){Map<String,Object> m=(Map<String,Object>)o;String id=t(m,"id");if(m.containsKey("tipoMandato"))ETAPAS.put(id,t(m,"tipoMandato"));if(temas.put(id,t(m,"tema"))!=null)throw new IllegalArgumentException("ID repetido: "+id);
    lista.add(new Evento(id,t(m,"nome"),t(m,"categoria"),t(m,"personagemNome"),t(m,"personagemCargo"),t(m,"personagemAvatar"),t(m,"dialogo"),t(m,"descricao"),t(m,"contextualizacao"),t(m,"consequenciasPotenciais"),decreto((Map<String,Object>)m.get("decretoA")),decreto((Map<String,Object>)m.get("decretoB"))).comNarrativa(m));}
   Set<String> ids=new HashSet<String>();for(Evento c:lista)ids.add(c.getId());
   for(Evento c:lista)if(c.isConsequencia()){
    Map<String,Object> g=(Map<String,Object>)c.getNarrativa().get("gatilho");
    if(g==null||!ids.contains(g.get("origem"))||!("A".equals(g.get("decreto"))||"B".equals(g.get("decreto")))||!(g.get("atrasoMin") instanceof Number)||!(g.get("atrasoMax") instanceof Number))throw new IllegalArgumentException("Gatilho inválido: "+c.getId());
    int min=((Number)g.get("atrasoMin")).intValue(),max=((Number)g.get("atrasoMax")).intValue();if(min<1||max<min)throw new IllegalArgumentException("Janela inválida: "+c.getId());
   }
   if(lista.size()<24)throw new IllegalArgumentException("Catálogo insuficiente");return Collections.unmodifiableList(lista);
  }catch(Exception ex){throw new IllegalStateException("Falha ao carregar eventos.json",ex);}
 }
 private static String t(Map<String,Object> m,String k){Object v=m.get(k);if(!(v instanceof String)||((String)v).isEmpty())throw new IllegalArgumentException("Campo textual inválido: "+k);return (String)v;}
 private static double n(Map<String,Object> m,String k){return ((Number)m.get(k)).doubleValue();}
 @SuppressWarnings("unchecked") private static Decreto decreto(Map<String,Object> m){List<EfeitoAtrasado> efeitos=new ArrayList<EfeitoAtrasado>();
  for(Object o:(List<Object>)m.get("efeitosAtrasados")){Map<String,Object> a=(Map<String,Object>)o;efeitos.add(new EfeitoAtrasado(0,(int)n(a,"turnosRestantes"),t(a,"decretoOrigem"),t(a,"descricao"),n(a,"deltaCaixa"),n(a,"deltaAprovacao"),n(a,"deltaPib"),n(a,"deltaInflacao")));}
  return new Decreto(t(m,"codigo"),t(m,"titulo"),t(m,"tipoPolitica"),t(m,"descricao"),(List<String>)(List<?>)m.get("consequenciasEsperadas"),t(m,"explicacaoTeorica"),n(m,"impactoCaixa"),n(m,"impactoAprovacao"),n(m,"impactoPib"),n(m,"impactoInflacao"),n(m,"impactoJuros"),efeitos).comResumo(m.containsKey("emPoucasPalavras")?t(m,"emPoucasPalavras"):t(m,"descricao"));
 }
}
