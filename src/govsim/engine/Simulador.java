package govsim.engine;
import govsim.model.*;
import java.util.Random;
import java.util.Locale;
/** Experimento reproduzível: mesmas sementes iniciais por estratégia, sem usar modo demo. */
public final class Simulador {
 public static void executar(int n){
  if(n<=0)throw new IllegalArgumentException("N deve ser positivo");
  System.out.println("N="+n+" por estratégia/dificuldade; semente do motor 20261103, escolhas 781. Médias incluem derrotas.");
  System.out.println("Dificuldade Estratégia Vitória% Caixa% Aprov% Ambos% Hiper% Depressão% | Caixa Aprov PIB Inf | Estadista% Estadista/vitórias%");
  for(Dificuldade d:Dificuldade.values())for(int estrategia=0;estrategia<6;estrategia++){
   int[] resultados=new int[6]; double[] soma=new double[4]; int estadistas=0;
   MotorEconomico motor=new MotorEconomico(new Random(20261103));
   Random escolhas=new Random(781);
   Estado e=new Estado();
   for(int i=0;i<n;i++){
    motor.iniciarPartida(e,false,0,d);
    while(!e.isPartidaEncerrada())motor.processarDecisao(e,escolher(e,estrategia,escolhas));
    String[] status={"VITORIA","DERROTA_CAIXA","DERROTA_APROVACAO","DERROTA_AMBOS","DERROTA_HIPERINFLACAO","DERROTA_DEPRESSAO"};
    for(int k=0;k<6;k++)if(status[k].equals(e.getStatusPartida()))resultados[k]++;
    soma[0]+=e.getCaixaPublico().getValorAtual();soma[1]+=e.getAprovacaoPopular().getValorAtual();soma[2]+=e.getPib().getValorAtual();soma[3]+=e.getInflacao().getValorAtual();
    if(e.isVitoria()&&e.getAvaliacaoFinalEconomica().contains("(Estadista)"))estadistas++;
   }
   String[] nomes={"aleatória","sempre A","sempre B","aprovação","caixa","esperta"};
   System.out.printf(Locale.US,"%s %-10s",d.name(),nomes[estrategia]);
   for(int k:resultados)System.out.printf(Locale.US," %6.2f",100.0*k/n);
   System.out.print(" | ");for(double v:soma)System.out.printf(Locale.US," %6.2f",v/n);
   System.out.printf(Locale.US," | %.2f %.2f%n",100.0*estadistas/n,resultados[0]==0?0:100.0*estadistas/resultados[0]);
  }
 }
 private static String escolher(Estado e,int estrategia,Random r){
  if(estrategia==0)return r.nextBoolean()?"A":"B";
  if(estrategia==1)return "A";if(estrategia==2)return "B";
  Decreto a=e.getEventoAtual().getDecretoA(),b=e.getEventoAtual().getDecretoB();
  return pontuar(e,a,estrategia)>=pontuar(e,b,estrategia)?"A":"B";
 }
 private static double pontuar(Estado e,Decreto d,int estrategia){
  if(estrategia==3)return d.getImpactoAprovacao();
  if(estrategia==4)return d.getImpactoCaixa();
  if(e.getCaixaPublico().getValorAtual()<50)return d.getImpactoCaixa();
  if(e.getAprovacaoPopular().getValorAtual()<40)return d.getImpactoAprovacao();
  return d.getImpactoCaixa()*0.5+d.getImpactoAprovacao()-d.getImpactoInflacao()*2+d.getImpactoPib();
 }
}
