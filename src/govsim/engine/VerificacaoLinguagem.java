package govsim.engine;
import govsim.model.*;import govsim.server.JsonUtil;import java.nio.file.*;import java.util.*;import java.text.Normalizer;import java.util.regex.Pattern;
/** Limites de escrita lidos do mesmo glossário usado pela interface. */
public final class VerificacaoLinguagem {
 private static String normalizar(String s){return Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);}
 private static void exigir(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
 @SuppressWarnings("unchecked") public static void executar(){try{
  List<Object> termos=(List<Object>)JsonUtil.lerJson(new String(Files.readAllBytes(Paths.get("src/govsim/view/glossario.json")),"UTF-8"));List<String> proibidos=new ArrayList<String>();
  for(Object o:termos){Map<String,Object> t=(Map<String,Object>)o;if(Boolean.TRUE.equals(t.get("proibidoEmTituloDialogoTag"))){proibidos.add((String)t.get("termo"));proibidos.addAll((List<String>)t.get("aliases"));}}
  Set<String> tags=new HashSet<String>(Arrays.asList("Gasto do governo","Juros","Imposto","Ajuda social","Comércio exterior","Obras","Serviços públicos","Emprego","Energia","Prestação de contas"));
  for(Evento e:new BancoEventos().getCatalogoEventos()){
   exigir(e.getDialogo().trim().split("\\s+").length<=20,"Diálogo longo: "+e.getId());verificar(e.getDialogo(),proibidos,e.getId()+" diálogo");
   for(Decreto d:new Decreto[]{e.getDecretoA(),e.getDecretoB()}){exigir(d.getTitulo().trim().split("\\s+").length<=5,"Título longo: "+e.getId());exigir(d.getEmPoucasPalavras().trim().split("\\s+").length<=14,"Resumo longo: "+e.getId());exigir(tags.contains(d.getTipoPolitica()),"Tag fora da lista: "+e.getId());verificar(d.getTitulo(),proibidos,e.getId()+" título");verificar(d.getTipoPolitica(),proibidos,e.getId()+" tag");}
  }
  boolean rejeitou=false;try{verificar("Aplicar austeridade fiscal",proibidos,"teste negativo");}catch(AssertionError esperado){rejeitou=true;}exigir(rejeitou,"Verificação deve rejeitar jargão novo");
  System.out.println("[APROVADO] Linguagem: títulos, diálogos, tags e resumos curtos; jargão proibido ausente.");
 }catch(Exception ex){throw new AssertionError(ex);}}
 private static void verificar(String texto,List<String> termos,String onde){String n=normalizar(texto);for(String t:termos)exigir(!Pattern.compile("(^|[^a-z0-9_])"+Pattern.quote(normalizar(t))+"($|[^a-z0-9_])").matcher(n).find(),"Jargão em "+onde+": "+t);}
}
