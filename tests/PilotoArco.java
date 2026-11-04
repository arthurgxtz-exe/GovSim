import govsim.model.*;import govsim.engine.*;import java.util.*;
/** Varia somente os parâmetros do enum em um processo isolado, sem modificar o catálogo. */
public class PilotoArco {
 public static void main(String[] args)throws Exception{
  int n=Integer.parseInt(args[0]);java.lang.reflect.Method escolher=Simulador.class.getDeclaredMethod("escolher",Estado.class,int.class,Random.class);escolher.setAccessible(true);
  for(Dificuldade d:Dificuldade.values()){
   java.lang.reflect.Field custo=Dificuldade.class.getDeclaredField("multiplicadorCusto");custo.setAccessible(true);custo.setDouble(d,Double.parseDouble(args[1+d.ordinal()]));
   if(args.length>4){java.lang.reflect.Field beneficio=Dificuldade.class.getDeclaredField("multiplicadorBeneficio");beneficio.setAccessible(true);beneficio.setDouble(d,Double.parseDouble(args[4+d.ordinal()]));}
   if(args.length>7){java.lang.reflect.Field caixa=Dificuldade.class.getDeclaredField("caixaInicial");caixa.setAccessible(true);caixa.setDouble(d,Double.parseDouble(args[7+d.ordinal()]));}
   if(args.length>10){java.lang.reflect.Field aprov=Dificuldade.class.getDeclaredField("aprovacaoInicial");aprov.setAccessible(true);aprov.setDouble(d,Double.parseDouble(args[10+d.ordinal()]));}
   for(int estrategia:new int[]{0,5}){
    MotorEconomico motor=new MotorEconomico(new Random(20261103));Random r=new Random(781);Estado e=new Estado();int v=0,estadistas=0;
    for(int i=0;i<n;i++){motor.iniciarPartida(e,false,0,d);while(!e.isPartidaEncerrada())motor.processarDecisao(e,(String)escolher.invoke(null,e,estrategia,r));if(e.isVitoria()){v++;if(e.getAvaliacaoFinalEconomica().contains("(Estadista)"))estadistas++;}}
    System.out.printf(Locale.US,"%s %s custo=%.3f beneficio=%.3f vitoria=%.2f Estadista/todas=%.2f Estadista/vitorias=%.2f%n",d,estrategia==0?"aleatoria":"esperta",d.getMultiplicadorCusto(),d.getMultiplicadorBeneficio(),100.0*v/n,100.0*estadistas/n,v==0?0:100.0*estadistas/v);
   }
  }
 }
}
